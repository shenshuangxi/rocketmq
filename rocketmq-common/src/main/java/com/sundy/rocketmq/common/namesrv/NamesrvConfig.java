package com.sundy.rocketmq.common.namesrv;

import java.io.File;

import lombok.Getter;
import lombok.Setter;

import com.sundy.rocketmq.common.constants.LoggerName;
import com.sundy.rocketmq.logging.InternalLogger;
import com.sundy.rocketmq.logging.InternalLoggerFactory;

@Getter
@Setter
public class NamesrvConfig {

	private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.NAMESRV_LOGGER_NAME);

    private String rocketmqHome = System.getProperty(MixAll.ROCKETMQ_HOME_PROPERTY, System.getenv(MixAll.ROCKETMQ_HOME_ENV));
    private String kvConfigPath = System.getProperty("user.home") + File.separator + "namesrv" + File.separator + "kvConfig.json";
    private String configStorePath = System.getProperty("user.home") + File.separator + "namesrv" + File.separator + "namesrv.properties";
    private String productEnvName = "center";
    private boolean clusterTest = false;
    private boolean orderMessageEnable = false;
    
	
}
