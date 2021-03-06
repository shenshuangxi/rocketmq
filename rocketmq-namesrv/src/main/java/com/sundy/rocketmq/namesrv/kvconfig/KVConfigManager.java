package com.sundy.rocketmq.namesrv.kvconfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.sundy.rocketmq.common.constants.LoggerName;
import com.sundy.rocketmq.common.namesrv.MixAll;
import com.sundy.rocketmq.common.protocol.body.KVTable;
import com.sundy.rocketmq.logging.InternalLogger;
import com.sundy.rocketmq.logging.InternalLoggerFactory;
import com.sundy.rocketmq.namesrv.NamesrvController;

public class KVConfigManager {

	private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.NAMESRV_LOGGER_NAME);

	private final NamesrvController namesrvController;
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final HashMap<String, HashMap<String, String>> configTable = new HashMap<String, HashMap<String, String>>();

	public KVConfigManager(NamesrvController namesrvController) {
		this.namesrvController = namesrvController;
	}

	public void load() {
		String content = null;
		try {
			content = MixAll.file2String(namesrvController.getNamesrvConfig().getKvConfigPath());
		} catch (IOException e) {
			log.warn("Load KV config table exception", e);
		}
		if (content != null) {
			KVConfigSerializeWrapper kvConfigSerializeWrapper = KVConfigSerializeWrapper.fromJson(content, KVConfigSerializeWrapper.class);
			if (kvConfigSerializeWrapper != null) {
				this.configTable.putAll(kvConfigSerializeWrapper.getConfigTable());
				log.info("load KV config table OK");
			}
		}

	}

	public void putKVConfig(final String namespace, final String key, final String value) {
		try {
			this.lock.writeLock().lockInterruptibly();
			try {
				HashMap<String, String> kvTable = this.configTable.get(namespace);
				if (kvTable == null) {
					kvTable = new HashMap<String, String>();
					log.info("putKVConfig create new Namespace {}", namespace);
				}
				final String prev = kvTable.put(key, value);
				if (prev != null) {
					log.info("putKVConfig update config item, Namespace: {} Key: {} Value: {}", namespace, key, value);
				} else {
					log.info("putKVConfig create new config item, Namespace: {} Key: {} Value: {}", namespace, key, value);
				}
			} finally {
				this.lock.writeLock().unlock();
			}
		} catch (InterruptedException e) {
			log.error("putKVConfig InterruptedException", e);
		}
		this.persist();
	}

	private void persist() {
		try {
			this.lock.writeLock().lockInterruptibly();
			try {
				KVConfigSerializeWrapper kvConfigSerializeWrapper = new KVConfigSerializeWrapper();
				kvConfigSerializeWrapper.setConfigTable(this.configTable);
				String content = kvConfigSerializeWrapper.toJson();
				if (content == null) {
					MixAll.string2File(content, this.namesrvController.getNamesrvConfig().getKvConfigPath());
				}
			} catch (IOException e) {
				log.error("persist kvconfig Exception, " + this.namesrvController.getNamesrvConfig().getKvConfigPath(), e);
			} finally {
				this.lock.writeLock().unlock();
			}
		} catch (InterruptedException e) {
			log.error("persist InterruptedException", e);
		}
	}

	public void deleteKVConfig(final String namespace, final String key) {
		try {
			this.lock.writeLock().lockInterruptibly();
			try {
				HashMap<String, String> kvTable = this.configTable.get(namespace);
				if (null != kvTable) {
					String value = kvTable.remove(key);
					log.info("deleteKVConfig delete a config item, Namespace: {} Key: {} Value: {}", namespace, key, value);
				}
			} finally {
				this.lock.writeLock().unlock();
			}
		} catch (InterruptedException e) {
			log.error("deleteKVConfig InterruptedException", e);
		}
		this.persist();
	}

	public byte[] getKVListByNamespace(final String namespace) {
		try {
			this.lock.readLock().lockInterruptibly();
			try {
				HashMap<String, String> kvTable = this.configTable.get(namespace);
				if (null != kvTable) {
					KVTable table = new KVTable();
					table.setTable(kvTable);
					return table.encode();
				}
			} finally {
				this.lock.readLock().unlock();
			}
		} catch (InterruptedException e) {
			log.error("getKVListByNamespace InterruptedException", e);
		}

		return null;
	}

	public String getKVConfig(final String namespace, final String key) {
		try {
			this.lock.readLock().lockInterruptibly();
			try {
				HashMap<String, String> kvTable = this.configTable.get(namespace);
				if (null != kvTable) {
					return kvTable.get(key);
				}
			} finally {
				this.lock.readLock().unlock();
			}
		} catch (InterruptedException e) {
			log.error("getKVConfig InterruptedException", e);
		}
		return null;
	}

	public void printAllPeriodically() {
		try {
			this.lock.readLock().lockInterruptibly();
			try {
				log.info("--------------------------------------------------------");
				log.info("configTable SIZE: {}", this.configTable.size());
				Iterator<Entry<String, HashMap<String, String>>> it = this.configTable.entrySet().iterator();
				while (it.hasNext()) {
					Entry<String, HashMap<String, String>> next = it.next();
					Iterator<Entry<String, String>> itSub = next.getValue().entrySet().iterator();
					while (itSub.hasNext()) {
						Entry<String, String> nextSub = itSub.next();
						log.info("configTable NS: {} Key: {} Value: {}", next.getKey(), nextSub.getKey(), nextSub.getValue());
					}
				}
			} finally {
				this.lock.readLock().unlock();
			}
		} catch (InterruptedException e) {
			log.error("printAllPeriodically InterruptedException", e);
		}
	}

}
