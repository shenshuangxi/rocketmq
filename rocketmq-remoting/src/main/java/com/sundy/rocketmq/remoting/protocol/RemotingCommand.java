package com.sundy.rocketmq.remoting.protocol;

import java.awt.color.CMMException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;

import com.google.common.base.Strings;
import com.sundy.rocketmq.remoting.CommandCustomHeader;
import com.sundy.rocketmq.remoting.annotation.CFNotNull;
import com.sundy.rocketmq.remoting.exception.RemotingCommandException;

@Getter
@Setter
public class RemotingCommand {

	public static final String SERIALIZE_TYPE_PROPERTY = "rocketmq.serialize.type";
    public static final String SERIALIZE_TYPE_ENV = "ROCKETMQ_SERIALIZE_TYPE";
    public static final String REMOTING_VERSION_KEY = "rocketmq.remoting.version";
    
    private static final int RPC_TYPE = 0; // 0, REQUEST_COMMAND
    private static final int RPC_ONEWAY = 1; // 0, RPC
    
    private static final Map<Class<? extends CommandCustomHeader>, Field[]> CLASS_HASH_MAP = new HashMap<Class<? extends CommandCustomHeader>, Field[]>();
    private static final Map<Class, String> CANONICAL_NAME_CACHE = new HashMap<Class, String>();
    private static final Map<Field, Boolean> NULLABLE_FIELD_CACHE = new HashMap<Field, Boolean>();
    
    private static final String STRING_CANONICAL_NAME = String.class.getCanonicalName();
    private static final String DOUBLE_CANONICAL_NAME_1 = Double.class.getCanonicalName();
    private static final String DOUBLE_CANONICAL_NAME_2 = double.class.getCanonicalName();
    private static final String INTEGER_CANONICAL_NAME_1 = Integer.class.getCanonicalName();
    private static final String INTEGER_CANONICAL_NAME_2 = int.class.getCanonicalName();
    private static final String LONG_CANONICAL_NAME_1 = Long.class.getCanonicalName();
    private static final String LONG_CANONICAL_NAME_2 = long.class.getCanonicalName();
    private static final String BOOLEAN_CANONICAL_NAME_1 = Boolean.class.getCanonicalName();
    private static final String BOOLEAN_CANONICAL_NAME_2 = boolean.class.getCanonicalName();
    
    private static volatile int configVersion = -1;
    private static AtomicInteger requestId = new AtomicInteger(0);

    private static SerializeType serializeTypeConfigInThisServer = SerializeType.JSON;
    
    static {
    	final String protocol = System.getProperty(SERIALIZE_TYPE_PROPERTY, System.getenv(SERIALIZE_TYPE_ENV));
    	if (!Strings.isNullOrEmpty(protocol)) {
    		try {
				serializeTypeConfigInThisServer = SerializeType.valueOf(protocol);
    		} catch (IllegalArgumentException e) {
                throw new RuntimeException("parser specified protocol error. protocol=" + protocol, e);
            }
    	}
    }
    
    private int code;
    private LanguageCode language = LanguageCode.JAVA;
    private int version = 0;
    private int opaque = requestId.getAndIncrement();
    private int flag;
    private String remark;
    private HashMap<String, String> extFields;
    private transient CommandCustomHeader customHeader;
    
    private SerializeType serializeTypeCurrentRPC = serializeTypeConfigInThisServer;
    
    private transient byte[] body;
    
    protected RemotingCommand(){}
    

    
    public static RemotingCommand createRequestCommand(int code, CommandCustomHeader customHeader) {
    	RemotingCommand remotingCommand = new RemotingCommand();
    	remotingCommand.code = code;
    	remotingCommand.customHeader = customHeader;
    	setCmdVersion(remotingCommand);
    	return remotingCommand;
    }

	private static void setCmdVersion(RemotingCommand remotingCommand) {
		if (configVersion >= 0) {
			remotingCommand.version = configVersion;
		} else {
			String version = System.getProperty(REMOTING_VERSION_KEY);
			if (version != null) {
				remotingCommand.version = Integer.parseInt(version);
				configVersion = remotingCommand.version;
			}
		}
		
	}
	
    
	public static RemotingCommand createResponseCommand(Class<? extends CommandCustomHeader> CommandHeaderClass) {
		return createReponseCommand(RemotingSysResponsedCode.SYSTEM_ERROR, "not set any response code", CommandHeaderClass);
	}
	
	public static RemotingCommand createReponseCommand(int code, String remark) {
		return createReponseCommand(code, remark, null);
	}

	private static RemotingCommand createReponseCommand(int code, String remark, Class<? extends CommandCustomHeader> commandHeaderClass) {
		RemotingCommand remotingCommand = new RemotingCommand();
		remotingCommand.code = code;
		remotingCommand.remark = remark;
		remotingCommand.markResponseType();
		setCmdVersion(remotingCommand);
		if (commandHeaderClass != null) {
			try {
				CommandCustomHeader commandCustomHeader = commandHeaderClass.newInstance();
				remotingCommand.customHeader = commandCustomHeader;
			} catch (Exception e) {
				return null;
			}
		}
		return remotingCommand;
	}
	
	public void markResponseType() {
		int bits = 1 << RPC_TYPE;
		this.flag |= bits;
	}
	
	public static RemotingCommand decode(byte[] array) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(array);
		return decode(byteBuffer);
	}



	public static RemotingCommand decode(ByteBuffer byteBuffer) {
		int length = byteBuffer.limit();
		int oriHeader = byteBuffer.getInt();
		int headerLength = getHeaderLength(oriHeader);
		byte[] headerData = new byte[headerLength];
		byteBuffer.get(headerData);
		RemotingCommand remotingCommand = headerDecode(headerData, getProtocolType(oriHeader));
		int bodyLength = length - 4 - headerLength;
		byte[] bodyData;
		if (bodyLength > 0) {
			bodyData = new byte[bodyLength];
			byteBuffer.get(bodyData);
			remotingCommand.body = bodyData;
		}
		return remotingCommand;
	}



	public static RemotingCommand headerDecode(byte[] headerData, SerializeType type) {
		switch (type) {
			case JSON:
				RemotingCommand resultJson = RemotingSerializable.decode(headerData, RemotingCommand.class);
				resultJson.serializeTypeCurrentRPC = type;
				return resultJson;
			case ROCKETMQ:
				RemotingCommand resultMQ = RocketMQSerializable.rocketMQProtocolDecode(headerData);
				resultMQ.serializeTypeCurrentRPC = type;
				return resultMQ;
			default:
				break;
		}
		return null;
	}

	public static SerializeType getProtocolType(int source) {
		return SerializeType.valueOf((byte)(source>>24 & 0xff));
	}

	public static int getHeaderLength(int length) {
		return length & 0xffffff;
	}
	
	public static int createNewRequestId() {
		return requestId.incrementAndGet();
	}
	
	public static byte[] markProtocolType(int source, SerializeType type) {
		byte[] result = new byte[4];
		result[0] = type.getCode();
		result[1] = (byte) ((source >> 16) & 0xff);
		result[2] = (byte) ((source >> 8) & 0xff);
		result[3] = (byte) (source & 0xff);
		return result;
	}
	
	public CommandCustomHeader decodeCommandCustomHeader(Class<? extends CommandCustomHeader> commandCustomHeaderClass) throws RemotingCommandException {
		CommandCustomHeader objectHeader;
		try {
			objectHeader = commandCustomHeaderClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			return null;
		}
		
		if (this.extFields != null) {
			Field[] fields = getClassFields(commandCustomHeaderClass);
			for (Field field : fields) {
				if (!Modifier.isStatic(field.getModifiers())) {
					String fieldName = field.getName();
					if (!fieldName.startsWith("this")) {
						try {
							String value = this.extFields.get(field.getName());
							if (null == value) {
							    if (!isFieldNullable(field)) {
							        throw new RemotingCommandException("the custom field <" + fieldName + "> is null");
							    }
							    continue;
							}
							field.setAccessible(true);
							String type = getCanonicalName(field.getType());
							Object valueParsed;
							if (type.equals(STRING_CANONICAL_NAME)) {
							    valueParsed = value;
							} else if (type.equals(INTEGER_CANONICAL_NAME_1) || type.equals(INTEGER_CANONICAL_NAME_2)) {
							    valueParsed = Integer.parseInt(value);
							} else if (type.equals(LONG_CANONICAL_NAME_1) || type.equals(LONG_CANONICAL_NAME_2)) {
							    valueParsed = Long.parseLong(value);
							} else if (type.equals(BOOLEAN_CANONICAL_NAME_1) || type.equals(BOOLEAN_CANONICAL_NAME_2)) {
							    valueParsed = Boolean.parseBoolean(value);
							} else if (type.equals(DOUBLE_CANONICAL_NAME_1) || type.equals(DOUBLE_CANONICAL_NAME_2)) {
							    valueParsed = Double.parseDouble(value);
							} else {
							    throw new RemotingCommandException("the custom field <" + fieldName + "> type is not supported");
							}
							field.set(objectHeader, valueParsed);
						} catch (Exception e) {
							
						}
					}
				}
			}
			objectHeader.checkFields();
		}
		return objectHeader;
	}



	private String getCanonicalName(Class<?> clazz) {
		String name = CANONICAL_NAME_CACHE.get(clazz);
		if (name == null) {
			name = clazz.getCanonicalName();
			synchronized (CANONICAL_NAME_CACHE) {
				CANONICAL_NAME_CACHE.put(clazz, name);
			}
		}
		return CANONICAL_NAME_CACHE.get(clazz);
	}

	private boolean isFieldNullable(Field field) {
		if (!NULLABLE_FIELD_CACHE.containsKey(field)) {
			Annotation annotation = field.getAnnotation(CFNotNull.class);
			synchronized (NULLABLE_FIELD_CACHE) {
				NULLABLE_FIELD_CACHE.put(field, annotation==null);
			}
		}
		return NULLABLE_FIELD_CACHE.get(field);
	}



	private Field[] getClassFields(Class<? extends CommandCustomHeader> commandCustomHeaderClass) {
		Field[] fields = CLASS_HASH_MAP.get(commandCustomHeaderClass);
		if (fields == null) {
			fields = commandCustomHeaderClass.getDeclaredFields();
			synchronized (CLASS_HASH_MAP) {
				CLASS_HASH_MAP.put(commandCustomHeaderClass, fields);
			}
		}
		return fields;
	}
	
	
	
}
