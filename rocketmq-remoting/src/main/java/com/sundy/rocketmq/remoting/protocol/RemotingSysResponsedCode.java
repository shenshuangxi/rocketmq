package com.sundy.rocketmq.remoting.protocol;

public interface RemotingSysResponsedCode {

	int SUCCESS = 0;
	int SYSTEM_ERROR = 1;
	int SYSTEM_BUSY = 2;
	int REQUEST_CODE_NOT_SUPPORTED = 3;
	int TRANSACTION_FAILED = 4;
	
}
