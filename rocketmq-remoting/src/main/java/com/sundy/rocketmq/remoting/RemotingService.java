package com.sundy.rocketmq.remoting;

public interface RemotingService {

	void start();

    void shutdown();

    void registerRPCHook(RPCHook rpcHook);
	
}
