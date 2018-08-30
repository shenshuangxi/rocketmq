package com.sundy.rocketmq.remoting.common;

public abstract class ServiceThread implements Runnable {

	private static final long JOIN_TIME = 90 * 1000;
    protected final Thread thread;
    protected volatile boolean hasNotified = false;
    protected volatile boolean stopped = false;
	
    public ServiceThread() {
		this.thread = new Thread(this, this.getServiceName());
	}
    
	public ServiceThread(Thread thread) {
		this.thread = thread;
	}
	
	public abstract String getServiceName();
	
	public void start() {
		this.thread.start();
	}
	
	public void shutdown() {
		this.shutdown(false);
	}

	private void shutdown(boolean interrupt) {
		this.stopped = true;
		synchronized (this) {
			if (!this.hasNotified) {
				this.hasNotified = true;
				this.notify();
			}
		}
		
		try {
			if (interrupt) {
				this.thread.interrupt();
			}
			
			long beginTime = System.currentTimeMillis();
			this.thread.join(this.JOIN_TIME);
			long eclipseTime = System.currentTimeMillis() - beginTime;
		} catch (InterruptedException e) {
		}
		
	}

}
