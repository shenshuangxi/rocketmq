package com.sundy.rocketmq.namesrv;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

import com.sundy.rocketmq.common.MQVersion;
import com.sundy.rocketmq.common.constants.LoggerName;
import com.sundy.rocketmq.common.namesrv.MixAll;
import com.sundy.rocketmq.common.namesrv.NamesrvConfig;
import com.sundy.rocketmq.logging.InternalLogger;
import com.sundy.rocketmq.logging.InternalLoggerFactory;
import com.sundy.rocketmq.remoting.netty.NettyServerConfig;
import com.sundy.rocketmq.remoting.protocol.RemotingCommand;
import com.sundy.rocketmq.srvutil.ServerUtil;

public class NamesrvStartup {

	private static InternalLogger log;
	private static Properties properties = null;
	private static CommandLine commandLine = null;

	public static void main(String[] args) {
		main0(args);
	}

	private static NamesrvController main0(String[] args) {
		NamesrvController controller = createNamesrvConroller(args);

	}

	private static NamesrvController createNamesrvConroller(String[] args) throws IOException, JoranException {
		System.setProperty(RemotingCommand.REMOTING_VERSION_KEY, Integer.toString(MQVersion.CURRENT_VERSION));
		Options options = ServerUtil.buildCommandlineOptions(new Options());
		commandLine = ServerUtil.parseCmdLine("mqnamesrv", args, buildCommandlineOptions(options), new PosixParser());
		if (commandLine == null) {
			System.exit(-1);
			return null;
		}
		final NamesrvConfig namesrvConfig = new NamesrvConfig();
		final NettyServerConfig nettyServerConfig = new NettyServerConfig();
		nettyServerConfig.setListenPort(9876);
		if (commandLine.hasOption("c")) {
			String file = commandLine.getOptionValue("c");
			if (file != null) {
				InputStream in = new BufferedInputStream(new FileInputStream(file));
				properties = new Properties();
				properties.load(in);
				MixAll.properties2Object(properties, namesrvConfig);
				MixAll.properties2Object(properties, nettyServerConfig);
				namesrvConfig.setConfigStorePath(file);
			}
		}
		if (commandLine.hasOption('p')) {
			MixAll.printObjectProperties(null, namesrvConfig);
			MixAll.printObjectProperties(null, nettyServerConfig);
			System.exit(0);
		}
		MixAll.properties2Object(ServerUtil.commandLine2Properties(commandLine), namesrvConfig);
		if (namesrvConfig.getRocketmqHome() == null) {
			System.out.printf("Please set the %s variable in your environment to match the location of the RocketMQ installation%n", MixAll.ROCKETMQ_HOME_ENV);
			System.exit(-2);
		}
		
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(lc);
		lc.reset();
		configurator.doConfigure(namesrvConfig.getRocketmqHome() + "/conf/logback_namesrv.xml");
		log = InternalLoggerFactory.getLogger(LoggerName.NAMESRV_LOGGER_NAME);
		MixAll.printObjectProperties(log, namesrvConfig);
		MixAll.printObjectProperties(log, nettyServerConfig);
		final NamesrvController controller = new NamesrvController(namesrvConfig, nettyServerConfig);
		return null;
	}

	public static void shutdown(final NamesrvController controller) {
		controller.shutdown();
	}

	public static Options buildCommandlineOptions(final Options options) {
		Option opt = new Option("c", "configFile", true, "Name server config properties file");
		opt.setRequired(false);
		options.addOption(opt);

		opt = new Option("p", "printConfigItem", false, "Print all config item");
		opt.setRequired(false);
		options.addOption(opt);

		return options;
	}

	public static Properties getProperties() {
		return properties;
	}

}
