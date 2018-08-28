package com.sundy.rocketmq.namesrv.routeinfo;

import io.netty.channel.Channel;

import com.sundy.rocketmq.common.constants.LoggerName;
import com.sundy.rocketmq.logging.InternalLogger;
import com.sundy.rocketmq.logging.InternalLoggerFactory;
import com.sundy.rocketmq.namesrv.NamesrvController;
import com.sundy.rocketmq.remoting.ChannelEventListener;

public class BrokerHousekeepingService implements ChannelEventListener{

	private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.NAMESRV_LOGGER_NAME);
	private final NamesrvController namesrvController;
	
	public BrokerHousekeepingService(NamesrvController namesrvController) {
		this.namesrvController = namesrvController;
	}

	@Override
	public void onChannelConnect(String remoteAddr, Channel channel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onChannelClose(String remoteAddr, Channel channel) {
		namesrvController.getRouteInfoManager().onChannelDestroy(remoteAddr, channel);
		
	}

	@Override
	public void onChannelException(String remoteAddr, Channel channel) {
		namesrvController.getRouteInfoManager().onChannelDestroy(remoteAddr, channel);
	}

	@Override
	public void onChannelIdle(String remoteAddr, Channel channel) {
		namesrvController.getRouteInfoManager().onChannelDestroy(remoteAddr, channel);
	}

}
