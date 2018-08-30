package com.sundy.rocketmq.remoting.netty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import io.netty.channel.Channel;

@Getter
@AllArgsConstructor
public class NettyEvent {

	private final NettyEventType type;
    private final String remoteAddr;
    private final Channel channel;
	
}
