package com.sundy.rocketmq.namesrv.kvconfig;

import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;

import com.sundy.rocketmq.remoting.protocol.RemotingSerializable;

@Getter
@Setter
public class KVConfigSerializeWrapper extends RemotingSerializable {

	private HashMap<String, HashMap<String, String>> configTable;
	
	
	
}
