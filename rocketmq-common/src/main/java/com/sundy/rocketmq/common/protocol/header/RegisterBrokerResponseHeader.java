package com.sundy.rocketmq.common.protocol.header;

import lombok.Getter;
import lombok.Setter;

import com.sundy.rocketmq.common.annotation.CFNullable;
import com.sundy.rocketmq.remoting.CommandCustomHeader;
import com.sundy.rocketmq.remoting.exception.RemotingCommandException;

@Getter
@Setter
public class RegisterBrokerResponseHeader implements CommandCustomHeader {

	@CFNullable
    private String haServerAddr;
    @CFNullable
    private String masterAddr;
	
	@Override
	public void checkFields() throws RemotingCommandException {
		// TODO Auto-generated method stub

	}

}
