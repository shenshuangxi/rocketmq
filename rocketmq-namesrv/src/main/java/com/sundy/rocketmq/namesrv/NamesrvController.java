package com.sundy.rocketmq.namesrv;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;

import com.sundy.rocketmq.common.Configuration;
import com.sundy.rocketmq.common.ThreadFactoryImpl;
import com.sundy.rocketmq.common.constants.LoggerName;
import com.sundy.rocketmq.common.namesrv.NamesrvConfig;
import com.sundy.rocketmq.logging.InternalLogger;
import com.sundy.rocketmq.logging.InternalLoggerFactory;
import com.sundy.rocketmq.namesrv.kvconfig.KVConfigManager;
import com.sundy.rocketmq.namesrv.processor.ClusterTestRequestProcessor;
import com.sundy.rocketmq.namesrv.processor.DefaultRequestProcessor;
import com.sundy.rocketmq.namesrv.routeinfo.BrokerHousekeepingService;
import com.sundy.rocketmq.namesrv.routeinfo.RouteInfoManager;
import com.sundy.rocketmq.remoting.RemotingServer;
import com.sundy.rocketmq.remoting.common.TlsMode;
import com.sundy.rocketmq.remoting.netty.NettyRemotingServer;
import com.sundy.rocketmq.remoting.netty.NettyServerConfig;
import com.sundy.rocketmq.remoting.netty.TlsSystemConfig;
import com.sundy.rocketmq.srvutil.FileWatchService;

@Getter
@Setter
public class NamesrvController {

	private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.NAMESRV_LOGGER_NAME);
	private final NamesrvConfig namesrvConfig;
	private final NettyServerConfig nettyServerConfig;
	private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("NSScheduledThread"));
	
	private final KVConfigManager kvConfigManager;
	private final RouteInfoManager routeInfoManager;
	
	private RemotingServer remotingServer;
	
	private BrokerHousekeepingService brokerHousekeepingService;
	
	private ExecutorService remotingExecutor;
	
	private Configuration configuration;
	
	private FileWatchService fileWatchService;
	
	public NamesrvController(NamesrvConfig namesrvConfig, NettyServerConfig nettyServerConfig) {
		this.namesrvConfig = namesrvConfig;
		this.nettyServerConfig = nettyServerConfig;
		this.kvConfigManager = new KVConfigManager(this);
		this.routeInfoManager = new RouteInfoManager();
		this.brokerHousekeepingService = new BrokerHousekeepingService(this);
		this.configuration = new Configuration(log, this.namesrvConfig, this.nettyServerConfig);
		this.configuration.setStorePathFromConfig(this.namesrvConfig, "configStorePath");
	}
	
	public boolean initialize() {
		this.kvConfigManager.load();
		this.remotingServer = new NettyRemotingServer(this.nettyServerConfig, this.brokerHousekeepingService);
		this.remotingExecutor = Executors.newFixedThreadPool(nettyServerConfig.getServerWorkerThreads(), new ThreadFactoryImpl("RemotingExecutorThread_"));
		this.registerProcessor();
		this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				NamesrvController.this.routeInfoManager.scanNotActiveBroker();
			}
		}, 5, 10, TimeUnit.SECONDS);
		
		this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				NamesrvController.this.routeInfoManager.printAllPeriodically();
			}
		}, 1, 10, TimeUnit.MINUTES);
		
		if (TlsSystemConfig.tlsMode != TlsMode.DISABLED) {
			try {
				fileWatchService = new FileWatchService(new String[] {
					TlsSystemConfig.tlsServerCertPath,
					TlsSystemConfig.tlsServerKeyPath,
					TlsSystemConfig.tlsServerTrustCertPath
				}, new FileWatchService.Listener() {
					
					boolean certChanged, keyChanged = false;
					
					@Override
					public void onChanged(String path) {
						if (path.equals(TlsSystemConfig.tlsServerTrustCertPath)) {
							log.info("The trust certificate changed, reload the ssl context");
							reloadServerSslContext();
						}
						if (path.equals(TlsSystemConfig.tlsServerCertPath)) {
							certChanged = true;
						}
						if (path.equals(TlsSystemConfig.tlsServerKeyPath)) {
							keyChanged = true;
						}
						if (certChanged && keyChanged) {
							log.info("The certificate and private key changed, reload the ssl context");
							certChanged = keyChanged = false;
							reloadServerSslContext();
						}
					}
					
					private void reloadServerSslContext() {
						((NettyRemotingServer) remotingServer).loadSslContext();
					}
				});
			} catch (Exception e) {
				log.warn("FileWatchService created error, can't load the certificate dynamically");
			}
		}
		return true;
	}
	
	private void registerProcessor() {
		if (namesrvConfig.isClusterTest()) {
			this.remotingServer.registerDefaultProcessor(new ClusterTestRequestProcessor(this, namesrvConfig.getProductEnvName()), this.remotingExecutor);
		} else {
			this.remotingServer.registerDefaultProcessor(new DefaultRequestProcessor(this), this.remotingExecutor);
		}
	}

	public void start() throws Exception {
		this.remotingServer.start();

		if (this.fileWatchService != null) {
			this.fileWatchService.start();
		}
	}

	public void shutdown() {
		this.remotingServer.shutdown();
		this.remotingExecutor.shutdown();
		this.scheduledExecutorService.shutdown();

		if (this.fileWatchService != null) {
			this.fileWatchService.shutdown();
		}
	}

	public NamesrvConfig getNamesrvConfig() {
		return namesrvConfig;
	}

	public NettyServerConfig getNettyServerConfig() {
		return nettyServerConfig;
	}

	public KVConfigManager getKvConfigManager() {
		return kvConfigManager;
	}

	public RouteInfoManager getRouteInfoManager() {
		return routeInfoManager;
	}

	public RemotingServer getRemotingServer() {
		return remotingServer;
	}

	public void setRemotingServer(RemotingServer remotingServer) {
		this.remotingServer = remotingServer;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

}
