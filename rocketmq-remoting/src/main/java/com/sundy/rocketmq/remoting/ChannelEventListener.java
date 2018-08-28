package com.sundy.rocketmq.remoting;

import io.netty.channel.Channel;

public interface ChannelEventListener {

	void onChannelConnect(String remoteAddr, Channel channel);
	
	void onChannelClose(String remoteAddr, Channel channel);
	
	void onChannelException(String remoteAddr, Channel channel);
	
	void onChannelIdle(String remoteAddr, Channel channel);
	
}
