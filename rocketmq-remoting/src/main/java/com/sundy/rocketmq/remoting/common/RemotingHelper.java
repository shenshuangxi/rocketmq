package com.sundy.rocketmq.remoting.common;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Enumeration;

import com.sundy.rocketmq.remoting.exception.RemotingConnectException;
import com.sundy.rocketmq.remoting.exception.RemotingSendRequestException;
import com.sundy.rocketmq.remoting.exception.RemotingTimeoutException;
import com.sundy.rocketmq.remoting.protocol.RemotingCommand;

public class RemotingHelper {

	public static final String ROCKETMQ_REMOTING = "RocketmqRemoting";
	public static final String DEFAULT_CHARSET = "UTF-8";
	
	public static final String OS_NAME = System.getProperty("os.name");

    private static boolean isLinuxPlatform = false;
    private static boolean isWindowsPlatform = false;
    
    static {
        if (OS_NAME != null && OS_NAME.toLowerCase().contains("linux")) {
            isLinuxPlatform = true;
        }

        if (OS_NAME != null && OS_NAME.toLowerCase().contains("windows")) {
            isWindowsPlatform = true;
        }
    }
    
    public static boolean isLinuxPlatform() {
    	return isLinuxPlatform;
    }
    
    public static boolean isWindowsPlatform() {
    	return isWindowsPlatform;
    }
    
    public static Selector openSelector() throws IOException {
    	Selector selector = null;
    	if (isLinuxPlatform()) {
    		try {
				Class<?> providerClazz = Class.forName("sun.nio.ch.EPollSelectorProvider");
				if (providerClazz != null) {
					try {
						Method method = providerClazz.getMethod("provider");
						if (method != null) {
							SelectorProvider selectorProvider = (SelectorProvider) method.invoke(null);
							if (selectorProvider != null) {
								selector = selectorProvider.openSelector();
							}
						}
					} catch (Exception e) {
					}
				}
			} catch (Exception e) {
			}
    	}
    	
    	if (selector == null) {
    		selector = Selector.open();
    	}
    	return selector;
    }
    
    public static String getLocalAddress() {
    	try {
			Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
			ArrayList<String> ipv4Addrs = new ArrayList<String>();
			ArrayList<String> ipv6Addrs = new ArrayList<String>();
			while (enumeration.hasMoreElements()) {
				NetworkInterface networkInterface = enumeration.nextElement();
				Enumeration<InetAddress> en = networkInterface.getInetAddresses();
				while (en.hasMoreElements()) {
					InetAddress inetAddress = en.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						if (inetAddress instanceof Inet6Address) {
							ipv6Addrs.add(normalizeHostAddress(inetAddress));
						} else {
							ipv4Addrs.add(normalizeHostAddress(inetAddress));
						}
					}
				}
			}
			
			if (!ipv4Addrs.isEmpty()) {
				for (String ipv4Addr : ipv4Addrs) {
					if (ipv4Addr.startsWith("127.0") || ipv4Addr.startsWith("192.168")) {
						continue;
					}
					return ipv4Addr;
				}
				return ipv4Addrs.get(ipv4Addrs.size() -1);
			} else if (!ipv6Addrs.isEmpty()) {
				return ipv6Addrs.get(0);
			}
		} catch (SocketException e) {
		}
    	return null;
    }

    public static String normalizeHostAddress(InetAddress inetAddress) {
		if (inetAddress instanceof Inet6Address) {
			return "[" + inetAddress.getHostAddress() + "]";
		}
		return inetAddress.getHostAddress();
	}
	
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
	
	public static String socketAddress2String(SocketAddress socketAddress) {
		StringBuilder sb = new StringBuilder();
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        sb.append(inetSocketAddress.getAddress().getHostAddress());
        sb.append(":");
        sb.append(inetSocketAddress.getPort());
        return sb.toString();
	}
	
	public static SocketChannel connect(SocketAddress remote) {
		return connect(remote, 1000*5);
	}
	
	private static SocketChannel connect(SocketAddress remote, int timeoutMillis) {
		SocketChannel sc = null;
		try {
			sc = SocketChannel.open();
			sc.configureBlocking(true);
			sc.socket().setSoLinger(false, -1);
			sc.socket().setTcpNoDelay(true);
			sc.socket().setReceiveBufferSize(1024*64);
			sc.socket().setSendBufferSize(1024*64);
			sc.socket().connect(remote, timeoutMillis);
			sc.configureBlocking(false);
			return sc;
		} catch (Exception e) {
			if (sc != null) {
                try {
                    sc.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
		}
		return null;
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
	
	public static void closeChannel(Channel channel) {
		String addrRemote = parseChannelRemoteAddr(channel);
		channel.close().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				
			}
		});
	}
	
	public static RemotingCommand invokeSync(String addr, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
		long beginTime = System.currentTimeMillis();
		SocketAddress socketAddress = string2SocketAddress(addr);
		SocketChannel socketChannel = connect(socketAddress);
		if (socketChannel != null) {
			boolean sendRequestOK = false;
			try {
				socketChannel.configureBlocking(true);
				socketChannel.socket().setSoTimeout((int) timeoutMillis);
				
				ByteBuffer byteBufferRequest = request.encode();
				while (byteBufferRequest.hasRemaining()) {
					int length = socketChannel.write(byteBufferRequest);
					if (length > 0) {
						if (byteBufferRequest.hasRemaining()) {
							 if ((System.currentTimeMillis() - beginTime) > timeoutMillis) {
				                 throw new RemotingSendRequestException(addr);
				             }
						}
					} else {
						throw new RemotingTimeoutException(addr, timeoutMillis);
					}
					Thread.sleep(1);
				}
				
				sendRequestOK = true;
				
				ByteBuffer byteBufferReadSize = ByteBuffer.allocate(4);
				while (byteBufferRequest.hasRemaining()) {
					int length = socketChannel.read(byteBufferReadSize);
					if (length > 0) {
						if (byteBufferReadSize.hasRemaining()) {
							 if ((System.currentTimeMillis() - beginTime) > timeoutMillis) {
				                 throw new RemotingSendRequestException(addr);
				             }
						}
					} else {
						throw new RemotingTimeoutException(addr, timeoutMillis);
					}
					Thread.sleep(1);
				}
				
				int size = byteBufferReadSize.getInt();
				ByteBuffer byteBufferRead = ByteBuffer.allocate(size);
				while (byteBufferRead.hasRemaining()) {
				    int length = socketChannel.read(byteBufferRead);
				    if (length > 0) {
				        if (byteBufferRead.hasRemaining()) {
				            if ((System.currentTimeMillis() - beginTime) > timeoutMillis) {

				                throw new RemotingTimeoutException(addr, timeoutMillis);
				            }
				        }
				    } else {
				        throw new RemotingTimeoutException(addr, timeoutMillis);
				    }
				    Thread.sleep(1);
				}
				byteBufferRead.flip();
				return RemotingCommand.decode(byteBufferRead);
			} catch (Exception e) {
				if (sendRequestOK) {
                    throw new RemotingTimeoutException(addr, timeoutMillis);
                } else {
                    throw new RemotingSendRequestException(addr);
                }
			} finally {
				try {
                    socketChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
			}
		} else {
			throw new RemotingConnectException(addr);
		}
	}
	
}
