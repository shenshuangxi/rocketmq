package com.sundy.rocketmq.store.ha;

import java.util.HashMap;
import java.util.Map;

public class WaitNotifyObject {

	protected final Map<Long, Boolean> waitingThreadTable = new HashMap<Long, Boolean>(16);
	
	protected volatile boolean hasNotified = false;
	
	public void wakeup() {
		synchronized (this) {
			if (!this.hasNotified) {
				this.hasNotified = true;
				this.notify();
			}
		}
	}
	
	protected void waitForRunning(long interval) {
		synchronized (this) {
			if (!this.hasNotified) {
				this.hasNotified = true;
				this.onWaitEnd();
				return;
			}
		}
		
		try {
			this.wait(interval);
		} catch (InterruptedException e) {
			
		} finally {
			this.hasNotified = false;
			this.onWaitEnd();
		}
		
	}

	protected void onWaitEnd() {
		
	}
	
	public void wakeupAll() {
		synchronized (this) {
			boolean needNotify = false;
			for (Boolean value : this.waitingThreadTable.values()) {
				needNotify = needNotify || !value;
				value = true;
			}
			
			if (needNotify) {
				this.notifyAll();
			}
			
		}
	}
	
	public void allWaitForRunning(long interval) {
		long currentThreadId = Thread.currentThread().getId();
		synchronized (this) {
			Boolean notified = this.waitingThreadTable.get(currentThreadId);
			if (notified != null && notified) {
				this.waitingThreadTable.put(currentThreadId, false);
				this.onWaitEnd();
				return;
			}
		}
		
		try {
			this.wait(interval);
		} catch (InterruptedException e) {
		} finally {
			this.waitingThreadTable.put(currentThreadId, false);
			this.onWaitEnd();
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
