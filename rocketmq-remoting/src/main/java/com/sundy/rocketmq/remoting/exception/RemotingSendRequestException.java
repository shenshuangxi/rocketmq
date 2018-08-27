package com.sundy.rocketmq.remoting.exception;

public class RemotingSendRequestException extends RemotingException {

	private static final long serialVersionUID = 226582639499194138L;

	public RemotingSendRequestException(String addr) {
        this(addr, null);
    }

    public RemotingSendRequestException(String addr, Throwable cause) {
        super("send request to <" + addr + "> failed", cause);
    }
	
}
