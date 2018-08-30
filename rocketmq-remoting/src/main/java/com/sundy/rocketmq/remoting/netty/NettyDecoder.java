package com.sundy.rocketmq.remoting.netty;

import java.nio.ByteBuffer;

import com.sundy.rocketmq.remoting.common.RemotingHelper;
import com.sundy.rocketmq.remoting.protocol.RemotingCommand;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class NettyDecoder extends LengthFieldBasedFrameDecoder {

	private static final int FRAME_MAX_LENGTH = Integer.parseInt(System.getProperty("com.rocketmq.remoting.frameMaxLength", "16777216"));

	public NettyDecoder() {
		super(FRAME_MAX_LENGTH, 0, 4, 0, 4);
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		ByteBuf frame = null;
		try {
			frame = (ByteBuf) super.decode(ctx, in);
			if (frame == null) {
				return null;
			}
			ByteBuffer byteBuffer = frame.nioBuffer();
			return RemotingCommand.decode(byteBuffer);
		} catch (Exception e) {
			RemotingHelper.closeChannel(ctx.channel());
		} finally {
			if (frame != null) {
				frame.release();
			}
		}
		return null;
		
	}
	
	
	
}
