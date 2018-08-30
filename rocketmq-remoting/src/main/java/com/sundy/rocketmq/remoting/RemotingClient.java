package com.sundy.rocketmq.remoting;

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.sundy.rocketmq.remoting.exception.RemotingConnectException;
import com.sundy.rocketmq.remoting.exception.RemotingSendRequestException;
import com.sundy.rocketmq.remoting.exception.RemotingTimeoutException;
import com.sundy.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import com.sundy.rocketmq.remoting.netty.NettyRequestProcessor;
import com.sundy.rocketmq.remoting.protocol.RemotingCommand;

public interface RemotingClient extends RemotingService {

	void updateNameServerAddressList(final List<String> addrs);

    List<String> getNameServerAddressList();

    RemotingCommand invokeSync(final String addr, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException;

    void invokeAsync(final String addr, final RemotingCommand request, final long timeoutMillis, final InvokeCallback invokeCallback) throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    void invokeOneway(final String addr, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    void registerProcessor(final int requestCode, final NettyRequestProcessor processor, final ExecutorService executor);

    void setCallbackExecutor(final ExecutorService callbackExecutor);

    ExecutorService getCallbackExecutor();

    boolean isChannelWritable(final String addr);
	
}
