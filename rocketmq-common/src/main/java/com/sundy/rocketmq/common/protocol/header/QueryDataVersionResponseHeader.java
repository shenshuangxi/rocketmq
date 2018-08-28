package com.sundy.rocketmq.common.protocol.header;

import lombok.Getter;
import lombok.Setter;

import com.sundy.rocketmq.remoting.CommandCustomHeader;
import com.sundy.rocketmq.remoting.annotation.CFNotNull;
import com.sundy.rocketmq.remoting.exception.RemotingCommandException;

@Getter
@Setter
public class QueryDataVersionResponseHeader implements CommandCustomHeader {

	@CFNotNull
	private Boolean changed;

	@Override
	public void checkFields() throws RemotingCommandException {
		// TODO Auto-generated method stub

	}

}
