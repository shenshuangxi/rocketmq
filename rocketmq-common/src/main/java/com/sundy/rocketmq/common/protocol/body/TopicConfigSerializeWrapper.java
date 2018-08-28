package com.sundy.rocketmq.common.protocol.body;

import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;

import com.sundy.rocketmq.common.DataVersion;
import com.sundy.rocketmq.common.TopicConfig;
import com.sundy.rocketmq.remoting.protocol.RemotingSerializable;

@Getter
@Setter
public class TopicConfigSerializeWrapper extends RemotingSerializable {

	private ConcurrentHashMap<String, TopicConfig> topicConfigTable = new ConcurrentHashMap<String, TopicConfig>();
	private DataVersion dataVersion = new DataVersion();
	
	
}
