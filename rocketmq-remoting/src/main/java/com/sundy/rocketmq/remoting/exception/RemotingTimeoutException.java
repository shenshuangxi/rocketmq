package com.sundy.rocketmq.remoting.exception;

public class RemotingTimeoutException extends RemotingException {

	private static final long serialVersionUID = 1914353282288320280L;

	public RemotingTimeoutException(String message) {
        super(message);
    }

    public RemotingTimeoutException(String addr, long timeoutMillis) {
        this(addr, timeoutMillis, null);
    }

    public RemotingTimeoutException(String addr, long timeoutMillis, Throwable cause) {
        super("wait response on the channel <" + addr + "> timeout, " + timeoutMillis + "(ms)", cause);
    }
	
}
