package com.sundy.rocketmq.remoting.common;

public enum TlsMode {

	DISABLED("disabled"),
    PERMISSIVE("permissive"),
    ENFORCING("enforcing");
	
	private final String name;

	private TlsMode(String name) {
		this.name = name;
	}
	
	public static TlsMode parse(String mode) {
        for (TlsMode tlsMode : TlsMode.values()) {
            if (tlsMode.name.equals(mode)) {
                return tlsMode;
            }
        }
        return PERMISSIVE;
    }

    public String getName() {
        return name;
    }
	
}
