package com.sundy.rocketmq.remoting.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map.Entry;

public class RocketMQSerializable {

	private final static Charset CHARSET_UTF8 = Charset.forName("UTF-8");
	
	public static RemotingCommand rocketMQProtocolDecode(byte[] array) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(array);
		RemotingCommand remotingCommand = new RemotingCommand();
		remotingCommand.setCode(byteBuffer.getShort());
		remotingCommand.setLanguage(LanguageCode.valueOf(byteBuffer.get()));
		remotingCommand.setVersion(byteBuffer.getShort());
		remotingCommand.setOpaque(byteBuffer.getInt());
		remotingCommand.setFlag(byteBuffer.getInt());
		int remarkLen = byteBuffer.getInt();
		if (remarkLen > 0) {
			byte[] remarkBytes = new byte[remarkLen];
			byteBuffer.get(remarkBytes);
			remotingCommand.setRemark(new String(remarkBytes, CHARSET_UTF8));
		}
		
		int extLen = byteBuffer.getInt();
		if (extLen > 0) {
			byte[] extBytes = new byte[extLen];
			byteBuffer.get(extBytes);
			remotingCommand.setExtFields(mapDeserialize(extBytes));
		}
		
		return remotingCommand;
	}
	
	public static byte[] rocketMQProtocolEncode(RemotingCommand remotingCommand) {
		byte[] remarkBytes = null;
		int remarkLen = 0;
		if (remotingCommand.getRemark()!=null && remotingCommand.getRemark().length()>0) {
			remarkBytes = remotingCommand.getRemark().getBytes(CHARSET_UTF8);
			remarkLen = remarkBytes.length;
		}
		
		byte[] extFieldsBytes = null;
		int extLen = 0;
		if (remotingCommand.getExtFields()!=null && !remotingCommand.getExtFields().isEmpty()) {
			extFieldsBytes = mapSerialize(remotingCommand.getExtFields());
			extLen = extFieldsBytes.length;
		}
		
		int totalLen = calHeaderLen(remarkLen, extLen);
		ByteBuffer headerBuffer = ByteBuffer.allocate(totalLen);
		headerBuffer.putShort((short)remotingCommand.getCode());
		headerBuffer.put(remotingCommand.getLanguage().getCode());
		headerBuffer.putShort((short)remotingCommand.getVersion());
		headerBuffer.putInt(remotingCommand.getOpaque());
		headerBuffer.putInt(remotingCommand.getFlag());
		if (remarkBytes.length > 0) {
			headerBuffer.putInt(remarkBytes.length);
			headerBuffer.put(remarkBytes);
		} else {
			headerBuffer.putInt(0);
		}
		if (remarkBytes.length > 0) {
			headerBuffer.putInt(extFieldsBytes.length);
			headerBuffer.put(extFieldsBytes);
		} else {
			headerBuffer.putInt(0);
		}
		return headerBuffer.array();
	}

	private static int calHeaderLen(int remarkLen, int extLen) {
		//code + language + version + opaque + flag + remark + ext
		int length = 2 + 1 + 2 + 4 + 4+ 4 + remarkLen + 4 + extLen;
		return length;
	}

	public static byte[] mapSerialize(HashMap<String, String> map) {
		if (map==null || map.isEmpty()) {
			return null;
		}
		
		int totalLength = 0;
		int kvLength;
		for (Entry<String, String> entry : map.entrySet()) {
			kvLength = 2 + entry.getKey().getBytes(CHARSET_UTF8).length + 4 + entry.getValue().getBytes(CHARSET_UTF8).length;
			totalLength += kvLength;
		}
		
		ByteBuffer content = ByteBuffer.allocate(totalLength);
		byte[] key;
		byte[] val;
		for (Entry<String, String> entry : map.entrySet()) {
			key = entry.getKey().getBytes(CHARSET_UTF8);
			val = entry.getValue().getBytes(CHARSET_UTF8);
			content.putShort((short)key.length);
			content.put(key);
			content.putInt(val.length);
			content.put(val);
		}
		
		return content.array();
	}
	
	public static HashMap<String, String> mapDeserialize(byte[] bytes) {
		if (bytes==null || bytes.length <=0) {
			return null;
		}
		HashMap<String, String> map = new  HashMap<String, String>();
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		
		short keyLen;
		int valLen;
		byte[] keyBytes;
		byte[] valBytes;
		while(byteBuffer.hasRemaining()) {
			keyLen = byteBuffer.getShort();
			keyBytes = new byte[keyLen];
			byteBuffer.get(keyBytes);
			
			valLen = byteBuffer.getShort();
			valBytes = new byte[valLen];
			byteBuffer.get(valBytes);
			
			map.put(new String(keyBytes, CHARSET_UTF8), new String(valBytes, CHARSET_UTF8));
		}
		
		return map;
	}
	
}
