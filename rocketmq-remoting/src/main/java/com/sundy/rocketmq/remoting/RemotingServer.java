package com.sundy.rocketmq.remoting;

import io.netty.channel.Channel;

import java.util.concurrent.ExecutorService;

import com.sundy.rocketmq.remoting.common.Pair;
import com.sundy.rocketmq.remoting.exception.RemotingSendRequestException;
import com.sundy.rocketmq.remoting.exception.RemotingTimeoutException;
import com.sundy.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import com.sundy.rocketmq.remoting.netty.NettyRequestProcessor;
import com.sundy.rocketmq.remoting.protocol.RemotingCommand;

public interface RemotingServer extends RemotingService {

	void registerProcessor(final int requestCode, final NettyRequestProcessor processor, final ExecutorService executor);

    void registerDefaultProcessor(final NettyRequestProcessor processor, final ExecutorService executor);

    int localListenPort();

    Pair<NettyRequestProcessor, ExecutorService> getProcessorPair(final int requestCode);

    RemotingCommand invokeSync(final Channel channel, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException;

    void invokeAsync(final Channel channel, final RemotingCommand request, final long timeoutMillis, final InvokeCallback invokeCallback) throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    void invokeOneway(final Channel channel, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;
	
}
