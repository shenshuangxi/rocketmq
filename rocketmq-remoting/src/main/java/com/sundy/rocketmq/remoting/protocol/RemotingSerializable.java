package com.sundy.rocketmq.remoting.protocol;

import java.nio.charset.Charset;

import com.alibaba.fastjson.JSON;

public abstract class RemotingSerializable {

	private final static Charset CHARSET_UTF8 = Charset.forName("UTF-8");
	
	public static byte[] encode(Object obj) {
		String json = toJson(obj, false);
		if (json != null) {
			return json.getBytes(CHARSET_UTF8);
		}
		return null;
	}

	public static String toJson(Object obj, boolean prettyFormat) {
		return JSON.toJSONString(obj, prettyFormat);
	}
	
	public static <T> T decode(byte[] data, Class<T> classOfT) {
		String json = new String(data, CHARSET_UTF8);
		return fromJson(json, classOfT);
	} 
	
	public static <T> T fromJson(String json, Class<T> classOfT) {
		return JSON.parseObject(json, classOfT);
	}
	
	public byte[] encode() {
		String json = this.toJson();
		if (json != null) {
			return json.getBytes(CHARSET_UTF8);
		}
		return null;
	}

	private String toJson() {
		return toJson(false);
	}

	private String toJson(boolean prettyFormat) {
		return toJson(this, prettyFormat);
	}
	
	
	
}
