package com.sundy.rocketmq.common.protocol.header;

import lombok.Getter;
import lombok.Setter;

import com.sundy.rocketmq.remoting.CommandCustomHeader;
import com.sundy.rocketmq.remoting.annotation.CFNotNull;
import com.sundy.rocketmq.remoting.exception.RemotingCommandException;

@Getter
@Setter
public class PutKVConfigRequestHeader implements CommandCustomHeader {

	@CFNotNull
    private String namespace;
    @CFNotNull
    private String key;
    @CFNotNull
    private String value;
    
	@Override
	public void checkFields() throws RemotingCommandException {
		// TODO Auto-generated method stub
		
	}
	
}
