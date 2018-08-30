package com.sundy.rocketmq.remoting.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.io.IOException;
import java.net.SocketAddress;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.sundy.rocketmq.remoting.ChannelEventListener;
import com.sundy.rocketmq.remoting.InvokeCallback;
import com.sundy.rocketmq.remoting.RPCHook;
import com.sundy.rocketmq.remoting.RemotingClient;
import com.sundy.rocketmq.remoting.common.Pair;
import com.sundy.rocketmq.remoting.common.RemotingHelper;
import com.sundy.rocketmq.remoting.exception.RemotingConnectException;
import com.sundy.rocketmq.remoting.exception.RemotingSendRequestException;
import com.sundy.rocketmq.remoting.exception.RemotingTimeoutException;
import com.sundy.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import com.sundy.rocketmq.remoting.protocol.RemotingCommand;

public class NettyRemotingClient extends NettyRemotingAbstract implements RemotingClient {

	private static final long LOCK_TIMEOUT_MILLIS = 3000;

	private final NettyClientConfig nettyClientConfig;
	private final Bootstrap bootstrap = new Bootstrap();
	private final EventLoopGroup eventLoopGroupWorker;
	private final Lock lockChannelTables = new ReentrantLock();
	private final ConcurrentMap<String, ChannelWrapper> channelTables = new ConcurrentHashMap<String, ChannelWrapper>();

	private final Timer timer = new Timer("ClientHouseKeepingService", true);

	private final AtomicReference<List<String>> namesrvAddrList = new AtomicReference<List<String>>();
	private final AtomicReference<String> namesrvAddrChoosed = new AtomicReference<String>();
	private final AtomicInteger namesrvIndex = new AtomicInteger(initValueIndex());
	private final Lock lockNamesrvChannel = new ReentrantLock();

	private final ExecutorService publicExecutor;

	private ExecutorService callbackExecutor;
	private final ChannelEventListener channelEventListener;
	private DefaultEventExecutorGroup defaultEventExecutorGroup;
	private RPCHook rpcHook;

	public NettyRemotingClient(final NettyClientConfig nettyClientConfig) {
		this(nettyClientConfig, null);
	}

	public NettyRemotingClient(final NettyClientConfig nettyClientConfig, final ChannelEventListener channelEventListener) {
		super(nettyClientConfig.getClientOnewaySemaphoreValue(), nettyClientConfig.getClientAsyncSemaphoreValue());
		this.nettyClientConfig = nettyClientConfig;
		this.channelEventListener = channelEventListener;

		int publicThreadNums = nettyClientConfig.getClientCallbackExecutorThreads();
		if (publicThreadNums <= 0) {
			publicThreadNums = 4;
		}

		this.publicExecutor = Executors.newFixedThreadPool(publicThreadNums, new ThreadFactory() {
			private AtomicInteger threadIndex = new AtomicInteger(0);

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "NettyClientPublicExecutor_" + this.threadIndex.incrementAndGet());
			}
		});

		this.eventLoopGroupWorker = new NioEventLoopGroup(1, new ThreadFactory() {
			private AtomicInteger threadIndex = new AtomicInteger(0);

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, String.format("NettyClientSelector_%d", this.threadIndex.incrementAndGet()));
			}
		});

		if (nettyClientConfig.isUseTLS()) {
			try {
				sslContext = TlsHelper.buildSslContext(true);
			} catch (IOException e) {
			} catch (CertificateException e) {
				throw new RuntimeException("Failed to create SSLContext", e);
			}
		}
	}

	@Override
	public void start() {
		this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(nettyClientConfig.getClientWorkerThreads(), new ThreadFactory() {
			private AtomicInteger threadIndex = new AtomicInteger(0);

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "NettyClientWorkerThread_" + this.threadIndex.incrementAndGet());
			}
		});

		Bootstrap handler = this.bootstrap.group(this.eventLoopGroupWorker).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_KEEPALIVE, false)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyClientConfig.getConnectTimeoutMillis()).option(ChannelOption.SO_SNDBUF, nettyClientConfig.getClientSocketSndBufSize())
				.option(ChannelOption.SO_RCVBUF, nettyClientConfig.getClientSocketRcvBufSize()).handler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						if (nettyClientConfig.isUseTLS()) {
							if (null != sslContext) {
								pipeline.addFirst(defaultEventExecutorGroup, "sslHandler", sslContext.newHandler(ch.alloc()));
							} else {
							}
						}
						pipeline.addLast(defaultEventExecutorGroup, 
								new NettyEncoder(), 
								new NettyDecoder(), 
								new IdleStateHandler(0, 0, nettyClientConfig.getClientChannelMaxIdleTimeSeconds()),
								new NettyConnectManageHandler(), 
								new NettyClientHandler());
					}
				});

		this.timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					NettyRemotingClient.this.scanResponseTable();
				} catch (Throwable e) {
				}
			}
		}, 1000 * 3, 1000);

		if (this.channelEventListener != null) {
			this.nettyEventExecutor.start();
		}

	}

	@Override
	public void shutdown() {
		try {
			this.timer.cancel();
			for (ChannelWrapper cw : this.channelTables.values()) {
				this.closeChannel(null, cw.getChannel());
			}
			this.channelTables.clear();
			this.eventLoopGroupWorker.shutdownGracefully();
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
	public void updateNameServerAddressList(List<String> addrs) {
		List<String> old = this.namesrvAddrList.get();
		boolean update = false;
		if (!addrs.isEmpty()) {
			if (null == old) {
				update = true;
			} else if (addrs.size() != old.size()) {
				update = true;
			} else {
				for (int i = 0; i < addrs.size() && !update; i++) {
					if (!old.contains(addrs.get(i))) {
						update = true;
					}
				}
			}
			if (update) {
				Collections.shuffle(addrs);
				this.namesrvAddrList.set(addrs);
			}
		}
	}

	@Override
	public List<String> getNameServerAddressList() {
		return this.namesrvAddrList.get();
	}

	@Override
	public RemotingCommand invokeSync(String addr, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException,
			RemotingTimeoutException {
		long beginStartTime = System.currentTimeMillis();
		final Channel channel = this.getAndCreateChannel(addr);
		if (channel != null && channel.isActive()) {
			try {
				if (this.rpcHook != null) {
					this.rpcHook.doBeforeRequest(addr, request);
				}
				long costTime = System.currentTimeMillis() - beginStartTime;
				if (timeoutMillis < costTime) {
					throw new RemotingTimeoutException("invokeSync call timeout");
				}
				RemotingCommand response = this.invokeSyncImpl(channel, request, timeoutMillis - costTime);
				if (this.rpcHook != null) {
					this.rpcHook.doAfterResponse(RemotingHelper.parseChannelRemoteAddr(channel), request, response);
				}
				return response;
			} catch (RemotingSendRequestException e) {
				this.closeChannel(addr, channel);
				throw e;
			} catch (RemotingTimeoutException e) {
				if (nettyClientConfig.isClientCloseSocketIfTimeout()) {
					this.closeChannel(addr, channel);
				}
				throw e;
			}
		} else {
			this.closeChannel(addr, channel);
			throw new RemotingConnectException(addr);
		}
	}

	@Override
	public void invokeAsync(String addr, RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback) throws InterruptedException, RemotingConnectException,
			RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
		long beginStartTime = System.currentTimeMillis();
		final Channel channel = this.getAndCreateChannel(addr);
		if (channel != null && channel.isActive()) {
			try {
				if (this.rpcHook != null) {
					this.rpcHook.doBeforeRequest(addr, request);
				}
				long costTime = System.currentTimeMillis() - beginStartTime;
				if (timeoutMillis < costTime) {
					throw new RemotingTooMuchRequestException("invokeAsync call timeout");
				}
				this.invokeAsyncImpl(channel, request, timeoutMillis - costTime, invokeCallback);
			} catch (RemotingSendRequestException e) {
				this.closeChannel(addr, channel);
				throw e;
			}
		} else {
			this.closeChannel(addr, channel);
			throw new RemotingConnectException(addr);
		}

	}

	@Override
	public void invokeOneway(String addr, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException,
			RemotingTimeoutException, RemotingSendRequestException {
		final Channel channel = this.getAndCreateChannel(addr);
		if (channel != null && channel.isActive()) {
			try {
				if (this.rpcHook != null) {
					this.rpcHook.doBeforeRequest(addr, request);
				}
				this.invokeOnewayImpl(channel, request, timeoutMillis);
			} catch (RemotingSendRequestException e) {
				this.closeChannel(addr, channel);
				throw e;
			}
		} else {
			this.closeChannel(addr, channel);
			throw new RemotingConnectException(addr);
		}
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
	public void setCallbackExecutor(ExecutorService callbackExecutor) {
		this.callbackExecutor = callbackExecutor;
	}

	@Override
	public boolean isChannelWritable(String addr) {
		ChannelWrapper cw = this.channelTables.get(addr);
		if (cw != null && cw.isOK()) {
			return cw.isWritable();
		}
		return true;
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
		return this.callbackExecutor;
	}

	static class ChannelWrapper {
		private final ChannelFuture channelFuture;

		public ChannelWrapper(ChannelFuture channelFuture) {
			this.channelFuture = channelFuture;
		}

		public boolean isOK() {
			return this.channelFuture.channel() != null && this.channelFuture.channel().isActive();
		}

		public boolean isWritable() {
			return this.channelFuture.channel().isWritable();
		}

		private Channel getChannel() {
			return this.channelFuture.channel();
		}

		public ChannelFuture getChannelFuture() {
			return channelFuture;
		}
	}

	private static int initValueIndex() {
		Random r = new Random();
		return Math.abs(r.nextInt() % 999) % 999;
	}

	class NettyClientHandler extends SimpleChannelInboundHandler<RemotingCommand> {

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
			processMessageReceived(ctx, msg);
		}
	}

	class NettyConnectManageHandler extends ChannelDuplexHandler {
		@Override
		public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
			final String local = localAddress == null ? "UNKNOWN" : RemotingHelper.parseSocketAddressAddr(localAddress);
			final String remote = remoteAddress == null ? "UNKNOWN" : RemotingHelper.parseSocketAddressAddr(remoteAddress);

			super.connect(ctx, remoteAddress, localAddress, promise);

			if (NettyRemotingClient.this.channelEventListener != null) {
				NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.CONNECT, remote, ctx.channel()));
			}
		}

		@Override
		public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
			final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
			closeChannel(ctx.channel());
			super.disconnect(ctx, promise);

			if (NettyRemotingClient.this.channelEventListener != null) {
				NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress, ctx.channel()));
			}
		}

		@Override
		public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
			final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
			closeChannel(ctx.channel());
			super.close(ctx, promise);
			NettyRemotingClient.this.failFast(ctx.channel());
			if (NettyRemotingClient.this.channelEventListener != null) {
				NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress, ctx.channel()));
			}
		}

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			if (evt instanceof IdleStateEvent) {
				IdleStateEvent event = (IdleStateEvent) evt;
				if (event.state().equals(IdleState.ALL_IDLE)) {
					final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
					closeChannel(ctx.channel());
					if (NettyRemotingClient.this.channelEventListener != null) {
						NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.IDLE, remoteAddress, ctx.channel()));
					}
				}
			}

			ctx.fireUserEventTriggered(evt);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
			closeChannel(ctx.channel());
			if (NettyRemotingClient.this.channelEventListener != null) {
				NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.EXCEPTION, remoteAddress, ctx.channel()));
			}
		}
	}

	public void closeChannel(final Channel channel) {
		if (null == channel)
			return;

		try {
			if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
				try {
					boolean removeItemFromTable = true;
					ChannelWrapper prevCW = null;
					String addrRemote = null;
					for (Map.Entry<String, ChannelWrapper> entry : channelTables.entrySet()) {
						String key = entry.getKey();
						ChannelWrapper prev = entry.getValue();
						if (prev.getChannel() != null) {
							if (prev.getChannel() == channel) {
								prevCW = prev;
								addrRemote = key;
								break;
							}
						}
					}

					if (null == prevCW) {
						removeItemFromTable = false;
					}

					if (removeItemFromTable) {
						this.channelTables.remove(addrRemote);
						RemotingHelper.closeChannel(channel);
					}
				} catch (Exception e) {
				} finally {
					this.lockChannelTables.unlock();
				}
			} else {
			}
		} catch (InterruptedException e) {
		}
	}

	public void closeChannel(final String addr, final Channel channel) {
		if (null == channel)
			return;
		final String addrRemote = null == addr ? RemotingHelper.parseChannelRemoteAddr(channel) : addr;
		try {
			if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
				try {
					boolean removeItemFromTable = true;
					final ChannelWrapper prevCW = this.channelTables.get(addrRemote);
					if (null == prevCW) {
						removeItemFromTable = false;
					} else if (prevCW.getChannel() != channel) {
						removeItemFromTable = false;
					}
					if (removeItemFromTable) {
						this.channelTables.remove(addrRemote);
					}
					RemotingHelper.closeChannel(channel);
				} catch (Exception e) {
				} finally {
					this.lockChannelTables.unlock();
				}
			} else {
			}
		} catch (InterruptedException e) {
		}
	}

	private Channel getAndCreateChannel(final String addr) throws InterruptedException {
		if (null == addr) {
			return getAndCreateNameserverChannel();
		}

		ChannelWrapper cw = this.channelTables.get(addr);
		if (cw != null && cw.isOK()) {
			return cw.getChannel();
		}

		return this.createChannel(addr);
	}

	private Channel getAndCreateNameserverChannel() throws InterruptedException {
		String addr = this.namesrvAddrChoosed.get();
		if (addr != null) {
			ChannelWrapper cw = this.channelTables.get(addr);
			if (cw != null && cw.isOK()) {
				return cw.getChannel();
			}
		}
		final List<String> addrList = this.namesrvAddrList.get();
		if (this.lockNamesrvChannel.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
			try {
				addr = this.namesrvAddrChoosed.get();
				if (addr != null) {
					ChannelWrapper cw = this.channelTables.get(addr);
					if (cw != null && cw.isOK()) {
						return cw.getChannel();
					}
				}
				if (addrList != null && !addrList.isEmpty()) {
					for (int i = 0; i < addrList.size(); i++) {
						int index = this.namesrvIndex.incrementAndGet();
						index = Math.abs(index);
						index = index % addrList.size();
						String newAddr = addrList.get(index);
						this.namesrvAddrChoosed.set(newAddr);
						Channel channelNew = this.createChannel(newAddr);
						if (channelNew != null) {
							return channelNew;
						}
					}
				}
			} catch (Exception e) {
			} finally {
				this.lockNamesrvChannel.unlock();
			}
		} else {
		}
		return null;
	}

	private Channel createChannel(final String addr) throws InterruptedException {
		ChannelWrapper cw = this.channelTables.get(addr);
		if (cw != null && cw.isOK()) {
			cw.getChannel().close();
			channelTables.remove(addr);
		}

		if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
			try {
				boolean createNewConnection;
				cw = this.channelTables.get(addr);
				if (cw != null) {

					if (cw.isOK()) {
						cw.getChannel().close();
						this.channelTables.remove(addr);
						createNewConnection = true;
					} else if (!cw.getChannelFuture().isDone()) {
						createNewConnection = false;
					} else {
						this.channelTables.remove(addr);
						createNewConnection = true;
					}
				} else {
					createNewConnection = true;
				}

				if (createNewConnection) {
					ChannelFuture channelFuture = this.bootstrap.connect(RemotingHelper.string2SocketAddress(addr));
					cw = new ChannelWrapper(channelFuture);
					this.channelTables.put(addr, cw);
				}
			} catch (Exception e) {
			} finally {
				this.lockChannelTables.unlock();
			}
		} else {
		}

		if (cw != null) {
			ChannelFuture channelFuture = cw.getChannelFuture();
			if (channelFuture.awaitUninterruptibly(this.nettyClientConfig.getConnectTimeoutMillis())) {
				if (cw.isOK()) {
					return cw.getChannel();
				} else {
				}
			} else {
			}
		}

		return null;
	}

}
