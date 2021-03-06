package com.sundy.rocketmq.remoting.netty;

import io.netty.channel.Channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Getter;
import lombok.Setter;

import com.sundy.rocketmq.remoting.InvokeCallback;
import com.sundy.rocketmq.remoting.common.SemaphoreReleaseOnlyOnce;
import com.sundy.rocketmq.remoting.protocol.RemotingCommand;

@Getter
@Setter
public class ResponseFuture {

	private final int opaque;
	private final Channel processChannel;
	private final long timeoutMillis;
	private final InvokeCallback invokeCallback;
	private final long beginTimestamp = System.currentTimeMillis();
	private final CountDownLatch countDownLatch = new CountDownLatch(1);
	private final SemaphoreReleaseOnlyOnce once;
	private final AtomicBoolean executeCallbackOnlyOnce = new AtomicBoolean(false);
	private volatile RemotingCommand responseCommand;
	private volatile boolean sendRequestOK = true;
	private volatile Throwable cause;
	
	public ResponseFuture(Channel channel, int opaque, long timeoutMillis, InvokeCallback invokeCallback, SemaphoreReleaseOnlyOnce once) {
		this.opaque = opaque;
		this.processChannel = channel;
		this.timeoutMillis = timeoutMillis;
		this.invokeCallback = invokeCallback;
		this.once = once;
	}
	
	public void executeInvokeCallback() {
		if (this.invokeCallback!=null) {
			if (this.executeCallbackOnlyOnce.compareAndSet(false, true)) {
				this.invokeCallback.operationComplete(this);
			}
		}
	}
	
	public void release() {
		if (this.once != null) {
			this.once.release();
		}
	}
	
	public boolean isTimeout() {
		long diff = System.currentTimeMillis() - this.beginTimestamp;
		return diff > this.timeoutMillis;
	}
	
	public RemotingCommand waitResponse(long timeoutMillis) throws InterruptedException {
		this.countDownLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
		return this.responseCommand;
	}
	
	public void putResponse(RemotingCommand remotingCommand) {
		this.responseCommand = remotingCommand;
		this.countDownLatch.countDown();
	}
	
	@Override
	public String toString() {
		return "ResponseFuture [responseCommand=" + responseCommand
				+ ", sendRequestOK=" + sendRequestOK + ", cause=" + cause
				+ ", opaque=" + opaque + ", processChannel=" + processChannel
				+ ", timeoutMillis=" + timeoutMillis + ", invokeCallback="
				+ invokeCallback + ", beginTimestamp=" + beginTimestamp
				+ ", countDownLatch=" + countDownLatch + "]";
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
