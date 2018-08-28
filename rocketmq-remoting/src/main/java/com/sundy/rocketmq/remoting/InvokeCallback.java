package com.sundy.rocketmq.remoting;

import com.sundy.rocketmq.remoting.netty.ResponseFuture;

public interface InvokeCallback {

	void operationComplete(ResponseFuture responseFuture);
	
}
