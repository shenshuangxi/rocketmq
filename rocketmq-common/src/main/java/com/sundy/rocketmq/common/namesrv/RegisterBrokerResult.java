package com.sundy.rocketmq.common.namesrv;

import lombok.Getter;
import lombok.Setter;

import com.sundy.rocketmq.common.protocol.body.KVTable;

@Getter
@Setter
public class RegisterBrokerResult {

	private String haServerAddr;
	private String masterAddr;
	private KVTable kvTable;
	
	
}
