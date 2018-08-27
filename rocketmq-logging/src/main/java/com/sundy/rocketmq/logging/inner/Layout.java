package com.sundy.rocketmq.logging.inner;


public abstract class Layout {

	public abstract String format(LoggingEvent event);
	
	public String getContentType() {
		return "text/plain";
	}
	
	public String getFooter() {
		return null;
	}
	
	public String getHeader() {
		return null;
	}
	
	abstract public boolean ignoresThrowable();
	
}
