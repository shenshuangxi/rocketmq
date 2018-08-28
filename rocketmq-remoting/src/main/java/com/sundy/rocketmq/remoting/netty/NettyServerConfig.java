package com.sundy.rocketmq.remoting.netty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NettyServerConfig {

	private int listenPort = 8888;
	private int serverWorkerThreads = 8;
	private int serverCallbackExecutorThreads = 0;
	private int serverSelectorThreads = 3;
	private int serverOnewaySemaphoreValue = 256;
	private int serverAsyncSemaphoreValue = 64;
	private int serverChannelMaxIdleTimeSeconds = 120;
	
	private int serverSocketSndBufSize = NettySystemConfig.socketSndbufSize;
	private int serverSocketRcvBufSize = NettySystemConfig.socketRcvbufSize;
	private boolean serverPooledByteBufAllocatorEnable = true;
	
	private boolean useEpollNativeSelector = false;
	
	@Override
    public Object clone() throws CloneNotSupportedException {
        return (NettyServerConfig) super.clone();
    }
	
}
