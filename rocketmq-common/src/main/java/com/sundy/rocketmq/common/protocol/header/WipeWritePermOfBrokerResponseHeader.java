package com.sundy.rocketmq.common.protocol.header;

import com.sundy.rocketmq.remoting.CommandCustomHeader;
import com.sundy.rocketmq.remoting.annotation.CFNotNull;
import com.sundy.rocketmq.remoting.exception.RemotingCommandException;

public class WipeWritePermOfBrokerResponseHeader implements CommandCustomHeader {

	@CFNotNull
	private Integer wipeTopicCount;

	@Override
	public void checkFields() throws RemotingCommandException {
	}

	public Integer getWipeTopicCount() {
		return wipeTopicCount;
	}

	public void setWipeTopicCount(Integer wipeTopicCount) {
		this.wipeTopicCount = wipeTopicCount;
	}

}
