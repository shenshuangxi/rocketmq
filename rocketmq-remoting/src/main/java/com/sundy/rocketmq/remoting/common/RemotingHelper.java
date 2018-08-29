package com.sundy.rocketmq.remoting.common;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import com.sundy.rocketmq.remoting.protocol.RemotingCommand;

public class RemotingHelper {

	public static final String ROCKETMQ_REMOTING = "RocketmqRemoting";
	public static final String DEFAULT_CHARSET = "UTF-8";
	
	public static String exceptionSimpleDesc(Throwable e) {
		StringBuffer sb = new StringBuffer();
		if (e != null) {
			sb.append(e.toString());
			StackTraceElement[] stackTraces = e.getStackTrace();
			if (stackTraces!=null && stackTraces.length>0) {
				sb.append("," + stackTraces[0].toString());
			}
		}
		return sb.toString();
	}
	
	public static SocketAddress string2SocketAddress(String addr) {
		String[] s = addr.split(":");
		InetSocketAddress isa = new InetSocketAddress(s[0], Integer.parseInt(s[1]));
		return isa;
	}
	
	public static String parseChannelRemoteAddr(Channel channel) {
		if (channel == null) {
			return "";
		}
		SocketAddress remote = channel.remoteAddress();
		String addr = remote != null ? remote.toString() : "";
		if (addr.length() > 0) {
			int index = addr.lastIndexOf("/");
			if (index > 0) {
				return addr.substring(index+1);
			}
			return addr;
		}
		return "";
	}
	
	public static String parseSocketAddressAddr(SocketAddress socketAddress) {
		if (socketAddress != null) {
			final String addr = socketAddress.toString();
			if (addr.length() > 0) {
	            int index = addr.lastIndexOf("/");
	            if (index >= 0) {
	                return addr.substring(index + 1);
	            }

	            return addr;
	        }
		}
		return "";
	}
	
	public static RemotingCommand invokeSync(String addr, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
