package com.sundy.rocketmq.remoting.exception;

public class RemotingCommandException extends RemotingException {

	private static final long serialVersionUID = 1419390349122419250L;

	public RemotingCommandException(String message) {
        super(message, null);
    }

    public RemotingCommandException(String message, Throwable cause) {
        super(message, cause);
    }
	
}
