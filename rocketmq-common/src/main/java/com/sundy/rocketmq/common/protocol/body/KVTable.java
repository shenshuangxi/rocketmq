package com.sundy.rocketmq.common.protocol.body;

import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;

import com.sundy.rocketmq.remoting.protocol.RemotingSerializable;

@Getter
@Setter
public class KVTable extends RemotingSerializable {

	private HashMap<String, String> table = new HashMap<String, String>();
	
	
	
}
