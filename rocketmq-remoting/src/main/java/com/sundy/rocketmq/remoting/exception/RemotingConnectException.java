package com.sundy.rocketmq.remoting.exception;

public class RemotingConnectException extends RemotingException {

	private static final long serialVersionUID = -8634248409406659224L;

	public RemotingConnectException(String addr) {
        this(addr, null);
    }

    public RemotingConnectException(String addr, Throwable cause) {
        super("connect to <" + addr + "> failed", cause);
    }
	
}
