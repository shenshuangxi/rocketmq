package com.sundy.rocketmq.remoting.netty;

import io.netty.channel.ChannelHandlerContext;

import com.sundy.rocketmq.remoting.exception.RemotingCommandException;
import com.sundy.rocketmq.remoting.protocol.RemotingCommand;

public interface NettyRequestProcessor {

	RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request)throws RemotingCommandException;
	
	boolean rejectRequest();
	
}
