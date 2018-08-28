package com.sundy.rocketmq.remoting.netty;

public interface NettySystemConfig {

	String COM_ROCKETMQ_REMOTING_NETTY_POOLED_BYTE_BUF_ALLOCATOR_ENABLE = "com.rocketmq.remoting.nettyPooledByteBufAllocatorEnable";
	String COM_ROCKETMQ_REMOTING_SOCKET_SNDBUF_SIZE = "com.rocketmq.remoting.socket.sndbuf.size";
	String COM_ROCKETMQ_REMOTING_SOCKET_RCVBUF_SIZE = "com.rocketmq.remoting.socket.rcvbuf.size";
	String COM_ROCKETMQ_REMOTING_CLIENT_ASYNC_SEMAPHORE_VALUE = "com.rocketmq.remoting.clientAsyncSemaphoreValue";
	String COM_ROCKETMQ_REMOTING_CLIENT_ONEWAY_SEMAPHORE_VALUE = "com.rocketmq.remoting.clientOnewaySemaphoreValue";
	
	boolean NETTY_POOLED_BYTE_BUF_ALLOCATOR_ENABLE = Boolean.parseBoolean(System.getProperty(COM_ROCKETMQ_REMOTING_NETTY_POOLED_BYTE_BUF_ALLOCATOR_ENABLE, "false"));
	int CLIENT_ASYNC_SEMAPHORE_VALUE = Integer.parseInt(System.getProperty(COM_ROCKETMQ_REMOTING_CLIENT_ASYNC_SEMAPHORE_VALUE, "65535"));
	int CLIENT_ONEWAY_SEMAPHORE_VALUE = Integer.parseInt(System.getProperty(COM_ROCKETMQ_REMOTING_CLIENT_ONEWAY_SEMAPHORE_VALUE, "65535"));
	int socketSndbufSize = Integer.parseInt(System.getProperty(COM_ROCKETMQ_REMOTING_SOCKET_SNDBUF_SIZE, "65535"));
	int socketRcvbufSize = Integer.parseInt(System.getProperty(COM_ROCKETMQ_REMOTING_SOCKET_RCVBUF_SIZE, "65535"));
	
	
	
	
}
