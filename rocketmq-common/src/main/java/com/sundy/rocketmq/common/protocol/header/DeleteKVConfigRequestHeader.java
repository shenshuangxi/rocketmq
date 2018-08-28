package com.sundy.rocketmq.common.protocol.header;

import lombok.Getter;
import lombok.Setter;

import com.sundy.rocketmq.remoting.CommandCustomHeader;
import com.sundy.rocketmq.remoting.annotation.CFNotNull;
import com.sundy.rocketmq.remoting.exception.RemotingCommandException;

@Getter
@Setter
public class DeleteKVConfigRequestHeader implements CommandCustomHeader {

	@CFNotNull
    private String namespace;
    @CFNotNull
    private String key;
	
	@Override
	public void checkFields() throws RemotingCommandException {
		// TODO Auto-generated method stub

	}

}
