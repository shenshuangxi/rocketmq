package com.sundy.rocketmq.remoting.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Pair<T1, T2> {

	private T1 object1;
	private T2 object2;
	
	
	
}
