package com.sundy.rocketmq.remoting.protocol;

public enum SerializeType {

	JSON((byte)0), ROCKETMQ((byte)1);
	
	private final byte code;

	private SerializeType(byte code) {
		this.code = code;
	}

	public byte getCode() {
		return code;
	}
	
	public static SerializeType valueOf(byte code) {
		for (SerializeType serializeType : SerializeType.values()) {
			if (serializeType.getCode() == code) {
				return serializeType;
			}
		}
		return null;
	}
	
	
	
}
