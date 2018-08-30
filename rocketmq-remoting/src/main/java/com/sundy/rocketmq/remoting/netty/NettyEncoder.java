package com.sundy.rocketmq.remoting.netty;

import java.nio.ByteBuffer;

import com.sundy.rocketmq.remoting.common.RemotingHelper;
import com.sundy.rocketmq.remoting.protocol.RemotingCommand;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class NettyEncoder extends MessageToByteEncoder<RemotingCommand> {

	@Override
	protected void encode(ChannelHandlerContext ctx, RemotingCommand msg, ByteBuf out) throws Exception {
		try {
			ByteBuffer byteBuffer = msg.encode();
			out.writeBytes(byteBuffer);
		} catch (Exception e) {
			RemotingHelper.closeChannel(ctx.channel());
		}
		
	}

}
