package com.sundy.rocketmq.srvutil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;
import com.sundy.rocketmq.common.ServiceThread;
import com.sundy.rocketmq.common.UtilAll;
import com.sundy.rocketmq.common.constants.LoggerName;
import com.sundy.rocketmq.logging.InternalLogger;
import com.sundy.rocketmq.logging.InternalLoggerFactory;

public class FileWatchService extends ServiceThread {

	private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.COMMON_LOGGER_NAME);

	private final List<String> watchFiles;
	private final List<String> fileCurrentHash;
	private final Listener listener;
	private static final int WATCH_INTERVAL = 500;
	private MessageDigest md = MessageDigest.getInstance("MD5");

	public FileWatchService(final String[] watchFiles, final Listener listener) throws Exception {
		this.listener = listener;
		this.watchFiles = new ArrayList<>();
		this.fileCurrentHash = new ArrayList<>();
		for (int i = 0; i < watchFiles.length; i++) {
			if (!Strings.isNullOrEmpty(watchFiles[i]) && new File(watchFiles[i]).exists()) {
				this.watchFiles.add(watchFiles[i]);
				this.fileCurrentHash.add(hash(watchFiles[i]));
			}
		}
	}

	@Override
	public void run() {
		log.info(this.getServiceName() + " service started");
		while (!this.isStopped()) {
			try {
				this.waitForRunning(WATCH_INTERVAL);
				for (int i = 0; i < watchFiles.size(); i++) {
					String newHash;
					try {
						newHash = hash(watchFiles.get(i));
					} catch (Exception ignored) {
						log.warn(this.getServiceName() + " service has exception when calculate the file hash. ", ignored);
						continue;
					}
					if (!newHash.equals(fileCurrentHash.get(i))) {
						fileCurrentHash.set(i, newHash);
						listener.onChanged(watchFiles.get(i));
					}
				}
			} catch (Exception e) {
				log.warn(this.getServiceName() + " service has exception. ", e);
			}
		}
		log.info(this.getServiceName() + " service end");
	}

	@Override
	public String getServiceName() {
		return "FileWatchService";
	}

	private String hash(String filePath) throws IOException, NoSuchAlgorithmException {
		Path path = Paths.get(filePath);
		md.update(Files.readAllBytes(path));
		byte[] hash = md.digest();
		return UtilAll.bytes2string(hash);
	}

	public interface Listener {
		/**
		 * Will be called when the target files are changed
		 * 
		 * @param path
		 *            the changed file path
		 */
		void onChanged(String path);
	}

}
