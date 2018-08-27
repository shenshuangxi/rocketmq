package com.sundy.rocketmq.remoting;

import com.sundy.rocketmq.remoting.exception.RemotingCommandException;

public interface CommandCustomHeader {

	void checkFields() throws RemotingCommandException;
	
}
