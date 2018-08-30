package com.sundy.rocketmq.store.config;

import java.io.File;

import lombok.Getter;
import lombok.Setter;

import com.sundy.rocketmq.store.ConsumeQueue;

@Getter
@Setter
public class MessageStoreConfig {

	private String storePathRootDir = System.getProperty("user.dir") + File.separator + "store";
	
	private String storePathCommitLog = System.getProperty("user.dir") + File.separator + "store" + File.separator + "commitlog";
	
	private int mapedFileSizeConsumeCommitLog = 1024 * 1024 * 1024;
	
	private int mapedFileSizeConsumeQueue = 300000 * ConsumeQueue.CQ_STORE_UNIT_SIZE;
	
	private boolean enableConsumeQueueExt = false;
	
	private int mappedFileSizeConsumeQueueExt = 48 * 1024 * 1024;
	
	private int bitMapLengthConsumeQueueExt = 64;
	
	private int flushIntervalCommitLog = 500;
	
	private int commitIntervalCommitLog = 200;
	
	private boolean useReentrantLockWhenPutMessage = false;
	
	private boolean flushCommitLogTimed = false;
	
	private int flushIntervalConsumeQueue = 1000;
	
	private int cleanResourceInterval = 100;
	
	private int deleteCommitLogFilesInterval = 100;
	
	private int deleteConsumeQueueFilesInterval = 100;
    private int destroyMapedFileIntervalForcibly = 1000 * 120;
    private int redeleteHangedFileInterval = 1000 * 120;
    
    private String deleteWhen = "04";
    private int diskMaxUsedSpaceRatio = 75;
    
    private int fileReservedTime = 72;
    
    private int putMsgIndexHightWater = 600000;
    
    private int maxMessageSize = 1024 * 1024 * 4;
    
    private boolean checkCRCOnRecover = true;
    
    private int flushCommitLogLeastPages = 4;
    
    private int commitCommitLogLeastPages = 4;
    
    private int flushLeastPagesWhenWarmMapedFile = 1024 / 4 * 16;
    
    private int flushConsumeQueueLeastPages = 2;
    private int flushCommitLogThoroughInterval = 1000 * 10;
    private int commitCommitLogThoroughInterval = 200;
    private int flushConsumeQueueThoroughInterval = 1000 * 60;
    
    private int maxTransferBytesOnMessageInMemory = 1024 * 256;
    private int maxTransferCountOnMessageInMemory = 32;
    private int maxTransferBytesOnMessageInDisk = 1024 * 64;
    private int maxTransferCountOnMessageInDisk = 8;
    private int accessMessageInMemoryMaxRatio = 40;
    private boolean messageIndexEnable = true;
    private int maxHashSlotNum = 5000000;
    private int maxIndexNum = 5000000 * 4;
    private int maxMsgsNumBatch = 64;
    private boolean messageIndexSafe = false;
    private int haListenPort = 10912;
    private int haSendHeartbeatInterval = 1000 * 5;
    private int haHousekeepingInterval = 1000 * 20;
    private int haTransferBatchSize = 1024 * 32;
    private String haMasterAddress = null;
    private int haSlaveFallbehindMax = 1024 * 1024 * 256;
    private BrokerRole brokerRole = BrokerRole.ASYNC_MASTER;
    private FlushDiskType flushDiskType = FlushDiskType.ASYNC_FLUSH;
    private int syncFlushTimeout = 1000 * 5;
    private String messageDelayLevel = "1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h";
    private long flushDelayOffsetInterval = 1000 * 10;
    private boolean cleanFileForciblyEnable = true;
    private boolean warmMapedFileEnable = false;
    private boolean offsetCheckInSlave = false;
    private boolean debugLockEnable = false;
    private boolean duplicationEnable = false;
    private boolean diskFallRecorded = true;
    private long osPageCacheBusyTimeOutMills = 1000;
    private int defaultQueryMaxNum = 32;

    private boolean transientStorePoolEnable = false;
    private int transientStorePoolSize = 5;
    private boolean fastFailIfNoBufferInStorePool = false;
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
	
}
