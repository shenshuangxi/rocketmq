package com.sundy.rocketmq.common.protocol.header;

import com.sundy.rocketmq.remoting.CommandCustomHeader;
import com.sundy.rocketmq.remoting.annotation.CFNotNull;
import com.sundy.rocketmq.remoting.exception.RemotingCommandException;

public class WipeWritePermOfBrokerRequestHeader implements CommandCustomHeader {

	@CFNotNull
    private String brokerName;

    @Override
    public void checkFields() throws RemotingCommandException {

    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }
	
}
