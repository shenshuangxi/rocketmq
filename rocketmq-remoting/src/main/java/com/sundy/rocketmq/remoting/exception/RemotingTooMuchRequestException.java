package com.sundy.rocketmq.remoting.exception;

public class RemotingTooMuchRequestException extends RemotingException {

	private static final long serialVersionUID = -1524729582752220444L;

	public RemotingTooMuchRequestException(String message) {
        super(message);
    }
	
}
