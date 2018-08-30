package com.sundy.rocketmq.remoting.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslContext;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;

import com.sundy.rocketmq.remoting.ChannelEventListener;
import com.sundy.rocketmq.remoting.InvokeCallback;
import com.sundy.rocketmq.remoting.RPCHook;
import com.sundy.rocketmq.remoting.common.Pair;
import com.sundy.rocketmq.remoting.common.RemotingHelper;
import com.sundy.rocketmq.remoting.common.SemaphoreReleaseOnlyOnce;
import com.sundy.rocketmq.remoting.common.ServiceThread;
import com.sundy.rocketmq.remoting.exception.RemotingSendRequestException;
import com.sundy.rocketmq.remoting.exception.RemotingTimeoutException;
import com.sundy.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import com.sundy.rocketmq.remoting.protocol.RemotingCommand;
import com.sundy.rocketmq.remoting.protocol.RemotingSysResponseCode;

@Getter
@Setter
public abstract class NettyRemotingAbstract {

	protected final Semaphore semaphoreOneway;

	protected final Semaphore semaphoreAsync;

	protected final ConcurrentMap<Integer, ResponseFuture> responseTable = new ConcurrentHashMap<Integer, ResponseFuture>(256);

	protected final HashMap<Integer, Pair<NettyRequestProcessor, ExecutorService>> processorTable = new HashMap<Integer, Pair<NettyRequestProcessor, ExecutorService>>(64);

	protected final NettyEventExecutor nettyEventExecutor = new NettyEventExecutor();

	protected Pair<NettyRequestProcessor, ExecutorService> defaultRequestProcessor;

	protected volatile SslContext sslContext;

	public NettyRemotingAbstract(final int permitsOneway, final int permitsAsync) {
		this.semaphoreOneway = new Semaphore(permitsOneway, true);
		this.semaphoreAsync = new Semaphore(permitsAsync, true);
	}

	public abstract ChannelEventListener getChannelEventListener();

	public void putNettyEvent(final NettyEvent event) {
		this.nettyEventExecutor.putNettyEvent(event);
	}

	public void processMessageReceived(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
		final RemotingCommand cmd = msg;
		if (cmd != null) {
			switch (cmd.getType()) {
			case REQUEST_COMMAND:
				processRequestCommand(ctx, cmd);
				break;
			case RESPONSE_COMMAND:
				processResponseCommand(ctx, cmd);
				break;
			default:
				break;
			}
		}
	}

	public void processRequestCommand(final ChannelHandlerContext ctx, final RemotingCommand cmd) {
		final Pair<NettyRequestProcessor, ExecutorService> matched = this.processorTable.get(cmd.getCode());
		final Pair<NettyRequestProcessor, ExecutorService> pair = null == matched ? this.defaultRequestProcessor : matched;
		final int opaque = cmd.getOpaque();

		if (pair != null) {
			Runnable run = new Runnable() {
				@Override
				public void run() {
					try {
						RPCHook rpcHook = NettyRemotingAbstract.this.getRPCHook();
						if (rpcHook != null) {
							rpcHook.doBeforeRequest(RemotingHelper.parseChannelRemoteAddr(ctx.channel()), cmd);
						}
						final RemotingCommand response = pair.getObject1().processRequest(ctx, cmd);
						if (rpcHook != null) {
							rpcHook.doAfterResponse(RemotingHelper.parseChannelRemoteAddr(ctx.channel()), cmd, response);
						}
						if (!cmd.isOnewayRPC()) {
							if (response != null) {
								response.setOpaque(opaque);
								response.markResponseType();
								try {
									ctx.writeAndFlush(response);
								} catch (Throwable e) {
								}
							} else {

							}
						}
					} catch (Throwable e) {
						if (!cmd.isOnewayRPC()) {
							final RemotingCommand response = RemotingCommand.createResponseCommand(RemotingSysResponseCode.SYSTEM_ERROR, RemotingHelper.exceptionSimpleDesc(e));
							response.setOpaque(opaque);
							ctx.writeAndFlush(response);
						}
					}
				}
			};

			if (pair.getObject1().rejectRequest()) {
				final RemotingCommand response = RemotingCommand.createResponseCommand(RemotingSysResponseCode.SYSTEM_BUSY, "[REJECTREQUEST]system busy, start flow control for a while");
				response.setOpaque(opaque);
				ctx.writeAndFlush(response);
				return;
			}

			try {
				final RequestTask requestTask = new RequestTask(run, ctx.channel(), cmd);
				pair.getObject2().submit(requestTask);
			} catch (RejectedExecutionException e) {
				if ((System.currentTimeMillis() % 10000) == 0) {
				}
				if (!cmd.isOnewayRPC()) {
					final RemotingCommand response = RemotingCommand.createResponseCommand(RemotingSysResponseCode.SYSTEM_BUSY, "[OVERLOAD]system busy, start flow control for a while");
					response.setOpaque(opaque);
					ctx.writeAndFlush(response);
				}
			}
		} else {
			String error = " request type " + cmd.getCode() + " not supported";
			final RemotingCommand response = RemotingCommand.createResponseCommand(RemotingSysResponseCode.REQUEST_CODE_NOT_SUPPORTED, error);
			response.setOpaque(opaque);
			ctx.writeAndFlush(response);
		}
	}

	public void processResponseCommand(ChannelHandlerContext ctx, RemotingCommand cmd) {
		final int opaque = cmd.getOpaque();
		final ResponseFuture responseFuture = responseTable.get(opaque);
		if (responseFuture != null) {
			responseFuture.setResponseCommand(cmd);
			responseTable.remove(opaque);
			if (responseFuture.getInvokeCallback() != null) {
				executeInvokeCallback(responseFuture);
			} else {
				responseFuture.putResponse(cmd);
				responseFuture.release();
			}
		} else {
		}
	}

	private void executeInvokeCallback(final ResponseFuture responseFuture) {
		boolean runInThisThread = false;
		ExecutorService executor = this.getCallbackExecutor();
		if (executor != null) {
			try {
				executor.submit(new Runnable() {
					@Override
					public void run() {
						try {
							responseFuture.executeInvokeCallback();
						} catch (Throwable e) {
						} finally {
							responseFuture.release();
						}
					}
				});
			} catch (Exception e) {
				runInThisThread = true;
			}
		} else {
			runInThisThread = true;
		}

		if (runInThisThread) {
			try {
				responseFuture.executeInvokeCallback();
			} catch (Throwable e) {
			} finally {
				responseFuture.release();
			}
		}
	}

	public abstract RPCHook getRPCHook();

	public abstract ExecutorService getCallbackExecutor();

	public void scanResponseTable() {
		final List<ResponseFuture> rfList = new LinkedList<ResponseFuture>();
		Iterator<Entry<Integer, ResponseFuture>> it = this.responseTable.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Integer, ResponseFuture> next = it.next();
			ResponseFuture rep = next.getValue();
			if ((rep.getBeginTimestamp() + rep.getTimeoutMillis() + 1000) <= System.currentTimeMillis()) {
				rep.release();
				it.remove();
				rfList.add(rep);
			}
		}
		for (ResponseFuture rf : rfList) {
			try {
				executeInvokeCallback(rf);
			} catch (Throwable e) {
			}
		}
	}

	public RemotingCommand invokeSyncImpl(final Channel channel, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingSendRequestException,
			RemotingTimeoutException {
		final int opaque = request.getOpaque();
		try {
			final ResponseFuture responseFuture = new ResponseFuture(channel, opaque, timeoutMillis, null, null);
			this.responseTable.put(opaque, responseFuture);
			final SocketAddress addr = channel.remoteAddress();
			channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture f) throws Exception {
					if (f.isSuccess()) {
						responseFuture.setSendRequestOK(true);
						return;
					} else {
						responseFuture.setSendRequestOK(false);
					}
					responseTable.remove(opaque);
					responseFuture.setCause(f.cause());
					responseFuture.putResponse(null);
				}
			});
			RemotingCommand responseCommand = responseFuture.waitResponse(timeoutMillis);
			if (null == responseCommand) {
				if (responseFuture.isSendRequestOK()) {
					throw new RemotingTimeoutException(RemotingHelper.parseSocketAddressAddr(addr), timeoutMillis, responseFuture.getCause());
				} else {
					throw new RemotingSendRequestException(RemotingHelper.parseSocketAddressAddr(addr), responseFuture.getCause());
				}
			}
			return responseCommand;
		} finally {
			this.responseTable.remove(opaque);
		}
	}

	public void invokeAsyncImpl(final Channel channel, final RemotingCommand request, final long timeoutMillis, final InvokeCallback invokeCallback) throws InterruptedException,
			RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
		long beginStartTime = System.currentTimeMillis();
		final int opaque = request.getOpaque();
		boolean acquired = this.semaphoreAsync.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
		if (acquired) {
			final SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(this.semaphoreAsync);
			long costTime = System.currentTimeMillis() - beginStartTime;
			if (timeoutMillis < costTime) {
				throw new RemotingTooMuchRequestException("invokeAsyncImpl call timeout");
			}

			final ResponseFuture responseFuture = new ResponseFuture(channel, opaque, timeoutMillis - costTime, invokeCallback, once);
			this.responseTable.put(opaque, responseFuture);
			try {
				channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture f) throws Exception {
						if (f.isSuccess()) {
							responseFuture.setSendRequestOK(true);
							return;
						}
						requestFail(opaque);
					}
				});
			} catch (Exception e) {
				responseFuture.release();
				throw new RemotingSendRequestException(RemotingHelper.parseChannelRemoteAddr(channel), e);
			}
		} else {
			if (timeoutMillis <= 0) {
				throw new RemotingTooMuchRequestException("invokeAsyncImpl invoke too fast");
			} else {
				String info = String.format("invokeAsyncImpl tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreAsyncValue: %d", timeoutMillis, this.semaphoreAsync.getQueueLength(),
						this.semaphoreAsync.availablePermits());
				throw new RemotingTimeoutException(info);
			}
		}
	}

	public void invokeOnewayImpl(final Channel channel, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
		request.markOnewayRPC();
		boolean acquired = this.semaphoreOneway.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
		if (acquired) {
			final SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(this.semaphoreOneway);
			try {
				channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture f) throws Exception {
						once.release();
						if (!f.isSuccess()) {
						}
					}
				});
			} catch (Exception e) {
				once.release();
				throw new RemotingSendRequestException(RemotingHelper.parseChannelRemoteAddr(channel), e);
			}
		} else {
			if (timeoutMillis <= 0) {
				throw new RemotingTooMuchRequestException("invokeOnewayImpl invoke too fast");
			} else {
				String info = String.format("invokeOnewayImpl tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreAsyncValue: %d", timeoutMillis, this.semaphoreOneway.getQueueLength(), this.semaphoreOneway.availablePermits());
				throw new RemotingTimeoutException(info);
			}
		}
	}

	private void requestFail(final int opaque) {
		ResponseFuture responseFuture = responseTable.remove(opaque);
		if (responseFuture != null) {
			responseFuture.setSendRequestOK(false);
			responseFuture.putResponse(null);
			try {
				executeInvokeCallback(responseFuture);
			} catch (Throwable e) {
			} finally {
				responseFuture.release();
			}
		}
	}

	protected void failFast(final Channel channel) {
		Iterator<Entry<Integer, ResponseFuture>> it = responseTable.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Integer, ResponseFuture> entry = it.next();
			if (entry.getValue().getProcessChannel() == channel) {
				Integer opaque = entry.getKey();
				if (opaque != null) {
					requestFail(opaque);
				}
			}
		}
	}

	class NettyEventExecutor extends ServiceThread {

		private final LinkedBlockingQueue<NettyEvent> eventQueue = new LinkedBlockingQueue<NettyEvent>();
		private final int maxSize = 10000;

		public void putNettyEvent(final NettyEvent event) {
			if (this.eventQueue.size() <= maxSize) {
				this.eventQueue.add(event);
			}
		}

		@Override
		public void run() {
			final ChannelEventListener listener = NettyRemotingAbstract.this.getChannelEventListener();

			while (!this.stopped) {
				try {
					NettyEvent event = this.eventQueue.poll(3000, TimeUnit.MILLISECONDS);
					if (event != null && listener != null) {
						switch (event.getType()) {
						case IDLE:
							listener.onChannelIdle(event.getRemoteAddr(), event.getChannel());
							break;
						case CLOSE:
							listener.onChannelClose(event.getRemoteAddr(), event.getChannel());
							break;
						case CONNECT:
							listener.onChannelConnect(event.getRemoteAddr(), event.getChannel());
							break;
						case EXCEPTION:
							listener.onChannelException(event.getRemoteAddr(), event.getChannel());
							break;
						default:
							break;

						}
					}
				} catch (Exception e) {
				}
			}
		}

		@Override
		public String getServiceName() {
			return NettyEventExecutor.class.getSimpleName();
		}

	}
	
}
