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

	void registerProcessor(int requestCode, NettyRequestProcessor processor, ExecutorService executor);
	
	void registerDefaultProcessor(NettyRequestProcessor processor, ExecutorService executor);
	
	int localListenPort();
	
	Pair<NettyRequestProcessor, ExecutorService> getProcessorPair(int requestCode);
	
	RemotingCommand invokeSync(Channel channel, RemotingCommand request, long timeoutMillis)throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException;
	
	void invokeAsync(Channel channel, RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback)throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;
	
	void invokeOneway(Channel channel, RemotingCommand request, long timeoutMillis)throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

}
