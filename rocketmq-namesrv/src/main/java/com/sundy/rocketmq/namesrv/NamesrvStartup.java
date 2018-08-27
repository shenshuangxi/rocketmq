package com.sundy.rocketmq.namesrv;

import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import com.sundy.rocketmq.common.MQVersion;
import com.sundy.rocketmq.common.namesrv.NamesrvConfig;
import com.sundy.rocketmq.logging.InternalLogger;
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

	private static NamesrvController createNamesrvConroller(String[] args) {
		System.setProperty(RemotingCommand.REMOTING_VERSION_KEY, Integer.toString(MQVersion.CURRENT_VERSION));
		Options options = ServerUtil.buildCommandlineOptions(new Options());
		commandLine = ServerUtil.parseCmdLine("mqnamesrv", args, buildCommandlineOptions(options), new PosixParser());
		if (commandLine == null) {
			System.exit(-1);
			return null;
		}
		final NamesrvConfig namesrvConfig = new NamesrvConfig();
		
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
