package com.sundy.rocketmq.remoting.exception;

public class RemotingException extends Exception {

	private static final long serialVersionUID = 3526993896824137168L;

	public RemotingException(String message) {
        super(message);
    }

    public RemotingException(String message, Throwable cause) {
        super(message, cause);
    }
	
}
