package com.sundy.rocketmq.common.protocol.header;

import com.sundy.rocketmq.remoting.CommandCustomHeader;
import com.sundy.rocketmq.remoting.annotation.CFNotNull;
import com.sundy.rocketmq.remoting.exception.RemotingCommandException;

public class GetTopicsByClusterRequestHeader implements CommandCustomHeader {

	@CFNotNull
    private String cluster;

    @Override
    public void checkFields() throws RemotingCommandException {
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }
	
}
