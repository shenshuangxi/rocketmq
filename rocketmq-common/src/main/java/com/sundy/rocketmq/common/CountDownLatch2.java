package com.sundy.rocketmq.common;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class CountDownLatch2 {

	private final Sync sync;
	
	public CountDownLatch2(int count) {
		if (count < 0)
            throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
	}
	
	public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }
	
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));}
	
	public void countDown() {
        sync.releaseShared(1);
    }
	
	public long getCount() {
        return sync.getCount();
    }

    public void reset() {
        sync.reset();
    }
    
    public String toString() {
        return super.toString() + "[Count = " + sync.getCount() + "]";
    }
	
	private static final class Sync extends AbstractQueuedSynchronizer {
		private final int startCount;
		Sync(int count) {
			this.startCount = count;
			setState(count);
		}
		
		int getCount() {
			return getState();
		}
		
		protected int tryAcquireShared(int acquires) {
			return (getState()==0) ? 1 : -1;
		}
		
		protected boolean tryReleaseShared(int releases) {
            // Decrement count; signal when transition to zero
            for (; ; ) {
                int c = getState();
                if (c == 0)
                    return false;
                int nextc = c - 1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }
		
		protected void reset() {
            setState(startCount);
        }
	}
	
}
