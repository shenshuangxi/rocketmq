package com.sundy.rocketmq.common.protocol.body;

import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import com.sundy.rocketmq.remoting.protocol.RemotingSerializable;

@Getter
@Setter
public class TopicList extends RemotingSerializable {

	private Set<String> topicList = new HashSet<String>();
	private String brokerAddr;
	
}
