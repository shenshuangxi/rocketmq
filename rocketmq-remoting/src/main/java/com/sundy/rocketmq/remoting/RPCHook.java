package com.sundy.rocketmq.remoting;

import com.sundy.rocketmq.remoting.protocol.RemotingCommand;

public interface RPCHook {

	void doBeforeRequest(String remoteAddr, RemotingCommand request);
	
	void doAfterResponse(String remoteAddr, RemotingCommand request, RemotingCommand response);
	
}
