package com.sundy.rocketmq.common.protocol.header;

import lombok.Getter;
import lombok.Setter;

import com.sundy.rocketmq.remoting.CommandCustomHeader;
import com.sundy.rocketmq.remoting.annotation.CFNotNull;
import com.sundy.rocketmq.remoting.exception.RemotingCommandException;

@Getter
@Setter
public class UnRegisterBrokerRequestHeader implements CommandCustomHeader {

	@CFNotNull
	private String brokerName;
	@CFNotNull
	private String brokerAddr;
	@CFNotNull
	private String clusterName;
	@CFNotNull
	private Long brokerId;

	@Override
	public void checkFields() throws RemotingCommandException {
		// TODO Auto-generated method stub

	}

}
