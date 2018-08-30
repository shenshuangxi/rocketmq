package com.sundy.rocketmq.remoting.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.sundy.rocketmq.remoting.ChannelEventListener;
import com.sundy.rocketmq.remoting.InvokeCallback;
import com.sundy.rocketmq.remoting.RPCHook;
import com.sundy.rocketmq.remoting.RemotingServer;
import com.sundy.rocketmq.remoting.common.Pair;
import com.sundy.rocketmq.remoting.common.RemotingHelper;
import com.sundy.rocketmq.remoting.common.TlsMode;
import com.sundy.rocketmq.remoting.exception.RemotingSendRequestException;
import com.sundy.rocketmq.remoting.exception.RemotingTimeoutException;
import com.sundy.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import com.sundy.rocketmq.remoting.protocol.RemotingCommand;

public class NettyRemotingServer extends NettyRemotingAbstract implements RemotingServer {

	private final ServerBootstrap serverBootstrap;
	private final EventLoopGroup eventLoopGroupSelector;
	private final EventLoopGroup eventLoopGroupBoss;
	private final NettyServerConfig nettyServerConfig;

	private final ExecutorService publicExecutor;
	private final ChannelEventListener channelEventListener;

	private final Timer timer = new Timer("ServerHouseKeepingService", true);
	private DefaultEventExecutorGroup defaultEventExecutorGroup;

	private RPCHook rpcHook;

	private int port = 0;

	private static final String HANDSHAKE_HANDLER_NAME = "handshakeHandler";
	private static final String TLS_HANDLER_NAME = "sslHandler";
	private static final String FILE_REGION_ENCODER_NAME = "fileRegionEncoder";

	public NettyRemotingServer(final NettyServerConfig nettyServerConfig) {
		this(nettyServerConfig, null);
	}

	public NettyRemotingServer(final NettyServerConfig nettyServerConfig, final ChannelEventListener channelEventListener) {
		super(nettyServerConfig.getServerOnewaySemaphoreValue(), nettyServerConfig.getServerAsyncSemaphoreValue());
		this.serverBootstrap = new ServerBootstrap();
		this.nettyServerConfig = nettyServerConfig;
		this.channelEventListener = channelEventListener;

		int publicThreadNums = nettyServerConfig.getServerCallbackExecutorThreads();
		if (publicThreadNums <= 0) {
			publicThreadNums = 4;
		}

		this.publicExecutor = Executors.newFixedThreadPool(publicThreadNums, new ThreadFactory() {
			private AtomicInteger threadIndex = new AtomicInteger(0);

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "NettyServerPublicExecutor_" + this.threadIndex.incrementAndGet());
			}
		});

		this.eventLoopGroupBoss = new NioEventLoopGroup(1, new ThreadFactory() {
			private AtomicInteger threadIndex = new AtomicInteger(0);

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, String.format("NettyBoss_%d", this.threadIndex.incrementAndGet()));
			}
		});

		if (useEpoll()) {
			this.eventLoopGroupSelector = new EpollEventLoopGroup(nettyServerConfig.getServerSelectorThreads(), new ThreadFactory() {
				private AtomicInteger threadIndex = new AtomicInteger(0);
				private int threadTotal = nettyServerConfig.getServerSelectorThreads();

				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, String.format("NettyServerEPOLLSelector_%d_%d", threadTotal, this.threadIndex.incrementAndGet()));
				}
			});
		} else {
			this.eventLoopGroupSelector = new NioEventLoopGroup(nettyServerConfig.getServerSelectorThreads(), new ThreadFactory() {
				private AtomicInteger threadIndex = new AtomicInteger(0);
				private int threadTotal = nettyServerConfig.getServerSelectorThreads();

				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, String.format("NettyServerNIOSelector_%d_%d", threadTotal, this.threadIndex.incrementAndGet()));
				}
			});
		}

		loadSslContext();
	}
	
	public void loadSslContext() {
        TlsMode tlsMode = TlsSystemConfig.tlsMode;
        if (tlsMode != TlsMode.DISABLED) {
            try {
                sslContext = TlsHelper.buildSslContext(false);
            } catch (CertificateException e) {
            } catch (IOException e) {
            }
        }
    }
	
	private boolean useEpoll() {
        return RemotingHelper.isLinuxPlatform()
            && nettyServerConfig.isUseEpollNativeSelector()
            && Epoll.isAvailable();
    }

	@Override
	public void start() {
		this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(
	            nettyServerConfig.getServerWorkerThreads(),
	            new ThreadFactory() {

	                private AtomicInteger threadIndex = new AtomicInteger(0);

	                @Override
	                public Thread newThread(Runnable r) {
	                    return new Thread(r, "NettyServerCodecThread_" + this.threadIndex.incrementAndGet());
	                }
	            });

	        ServerBootstrap childHandler =
	            this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupSelector)
	                .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
	                .option(ChannelOption.SO_BACKLOG, 1024)
	                .option(ChannelOption.SO_REUSEADDR, true)
	                .option(ChannelOption.SO_KEEPALIVE, false)
	                .childOption(ChannelOption.TCP_NODELAY, true)
	                .childOption(ChannelOption.SO_SNDBUF, nettyServerConfig.getServerSocketSndBufSize())
	                .childOption(ChannelOption.SO_RCVBUF, nettyServerConfig.getServerSocketRcvBufSize())
	                .localAddress(new InetSocketAddress(this.nettyServerConfig.getListenPort()))
	                .childHandler(new ChannelInitializer<SocketChannel>() {
	                    @Override
	                    public void initChannel(SocketChannel ch) throws Exception {
	                        ch.pipeline()
	                            .addLast(defaultEventExecutorGroup, HANDSHAKE_HANDLER_NAME, new HandshakeHandler(TlsSystemConfig.tlsMode))
	                            .addLast(defaultEventExecutorGroup,
	                                new NettyEncoder(),
	                                new NettyDecoder(),
	                                new IdleStateHandler(0, 0, nettyServerConfig.getServerChannelMaxIdleTimeSeconds()),
	                                new NettyConnectManageHandler(),
	                                new NettyServerHandler()
	                            );
	                    }
	                });

	        if (nettyServerConfig.isServerPooledByteBufAllocatorEnable()) {
	            childHandler.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
	        }

	        try {
	            ChannelFuture sync = this.serverBootstrap.bind().sync();
	            InetSocketAddress addr = (InetSocketAddress) sync.channel().localAddress();
	            this.port = addr.getPort();
	        } catch (InterruptedException e1) {
	            throw new RuntimeException("this.serverBootstrap.bind().sync() InterruptedException", e1);
	        }

	        if (this.channelEventListener != null) {
	            this.nettyEventExecutor.start();
	        }

	        this.timer.scheduleAtFixedRate(new TimerTask() {

	            @Override
	            public void run() {
	                try {
	                    NettyRemotingServer.this.scanResponseTable();
	                } catch (Throwable e) {
	                }
	            }
	        }, 1000 * 3, 1000);
	}

	@Override
	public void shutdown() {
		try {
            if (this.timer != null) {
                this.timer.cancel();
            }

            this.eventLoopGroupBoss.shutdownGracefully();

            this.eventLoopGroupSelector.shutdownGracefully();

            if (this.nettyEventExecutor != null) {
                this.nettyEventExecutor.shutdown();
            }

            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        } catch (Exception e) {
        }

        if (this.publicExecutor != null) {
            try {
                this.publicExecutor.shutdown();
            } catch (Exception e) {
            }
        }

	}

	@Override
	public void registerRPCHook(RPCHook rpcHook) {
		this.rpcHook = rpcHook;
	}

	@Override
	public void registerProcessor(int requestCode, NettyRequestProcessor processor, ExecutorService executor) {
		ExecutorService executorThis = executor;
        if (null == executor) {
            executorThis = this.publicExecutor;
        }

        Pair<NettyRequestProcessor, ExecutorService> pair = new Pair<NettyRequestProcessor, ExecutorService>(processor, executorThis);
        this.processorTable.put(requestCode, pair);

	}

	@Override
	public void registerDefaultProcessor(NettyRequestProcessor processor, ExecutorService executor) {
		this.defaultRequestProcessor = new Pair<NettyRequestProcessor, ExecutorService>(processor, executor);
	}

	@Override
	public int localListenPort() {
		return this.port;
	}

	@Override
	public Pair<NettyRequestProcessor, ExecutorService> getProcessorPair(int requestCode) {
		return processorTable.get(requestCode);
	}

	@Override
	public RemotingCommand invokeSync(Channel channel, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException {
		return this.invokeSyncImpl(channel, request, timeoutMillis);
	}

	@Override
	public void invokeAsync(Channel channel, RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback) throws InterruptedException, RemotingTooMuchRequestException,
			RemotingTimeoutException, RemotingSendRequestException {
		this.invokeAsyncImpl(channel, request, timeoutMillis, invokeCallback);
	}

	@Override
	public void invokeOneway(Channel channel, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException,
			RemotingSendRequestException {
		this.invokeOnewayImpl(channel, request, timeoutMillis);
	}

	@Override
	public ChannelEventListener getChannelEventListener() {
		return channelEventListener;
	}

	@Override
	public RPCHook getRPCHook() {
		return this.rpcHook;
	}

	@Override
	public ExecutorService getCallbackExecutor() {
		return this.publicExecutor;
	}
	
	class HandshakeHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private final TlsMode tlsMode;

        private static final byte HANDSHAKE_MAGIC_CODE = 0x16;

        HandshakeHandler(TlsMode tlsMode) {
            this.tlsMode = tlsMode;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

            // mark the current position so that we can peek the first byte to determine if the content is starting with
            // TLS handshake
            msg.markReaderIndex();

            byte b = msg.getByte(0);

            if (b == HANDSHAKE_MAGIC_CODE) {
                switch (tlsMode) {
                    case DISABLED:
                        ctx.close();
                        break;
                    case PERMISSIVE:
                    case ENFORCING:
                        if (null != sslContext) {
                            ctx.pipeline()
                                .addAfter(defaultEventExecutorGroup, HANDSHAKE_HANDLER_NAME, TLS_HANDLER_NAME, sslContext.newHandler(ctx.channel().alloc()))
                                .addAfter(defaultEventExecutorGroup, TLS_HANDLER_NAME, FILE_REGION_ENCODER_NAME, new FileRegionEncoder());
                        } else {
                            ctx.close();
                        }
                        break;

                    default:
                        break;
                }
            } else if (tlsMode == TlsMode.ENFORCING) {
                ctx.close();
            }

            // reset the reader index so that handshake negotiation may proceed as normal.
            msg.resetReaderIndex();

            try {
                // Remove this handler
                ctx.pipeline().remove(this);
            } catch (NoSuchElementException e) {
            }

            // Hand over this message to the next .
            ctx.fireChannelRead(msg.retain());
        }
    }

    class NettyServerHandler extends SimpleChannelInboundHandler<RemotingCommand> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
            processMessageReceived(ctx, msg);
        }
    }

    class NettyConnectManageHandler extends ChannelDuplexHandler {
        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            super.channelUnregistered(ctx);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            super.channelActive(ctx);

            if (NettyRemotingServer.this.channelEventListener != null) {
                NettyRemotingServer.this.putNettyEvent(new NettyEvent(NettyEventType.CONNECT, remoteAddress, ctx.channel()));
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            super.channelInactive(ctx);
            if (NettyRemotingServer.this.channelEventListener != null) {
                NettyRemotingServer.this.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress, ctx.channel()));
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
                    RemotingHelper.closeChannel(ctx.channel());
                    if (NettyRemotingServer.this.channelEventListener != null) {
                        NettyRemotingServer.this
                            .putNettyEvent(new NettyEvent(NettyEventType.IDLE, remoteAddress, ctx.channel()));
                    }
                }
            }
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            if (NettyRemotingServer.this.channelEventListener != null) {
                NettyRemotingServer.this.putNettyEvent(new NettyEvent(NettyEventType.EXCEPTION, remoteAddress, ctx.channel()));
            }
            RemotingHelper.closeChannel(ctx.channel());
        }
    }

}
