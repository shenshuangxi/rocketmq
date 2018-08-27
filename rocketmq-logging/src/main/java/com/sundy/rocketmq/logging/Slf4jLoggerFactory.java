package com.sundy.rocketmq.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jLoggerFactory extends InternalLoggerFactory {

	public Slf4jLoggerFactory() {
		LoggerFactory.getILoggerFactory();
		doRegister();
	}
	
	@Override
	protected InternalLogger getLoggerInstance(String name) {
		return new Slf4jLogger(name);
	}

	@Override
	protected String getLoggerType() {
		return Slf4jLoggerFactory.LOGGER_SLF4J;
	}

	@Override
	protected void shoutdown() {
		
	}
	
	public static class Slf4jLogger implements InternalLogger {

		private Logger logger = null;
		
		public Slf4jLogger(String name) {
			this.logger = LoggerFactory.getLogger(name);
		}

		@Override
		public String getName() {
			return logger.getName();
		}

		@Override
		public void debug(String var1) {
			this.logger.debug(var1);
		}

		@Override
		public void debug(String var1, Object var2) {
			this.logger.debug(var1, var2);
		}

		@Override
		public void debug(String var1, Object var2, Object var3) {
			this.logger.debug(var1, var2, var3);
		}

		@Override
		public void debug(String var1, Object... var2) {
			this.logger.debug(var1, var2);
		}

		@Override
		public void debug(String var1, Throwable var2) {
			this.logger.debug(var1, var2);
		}

		@Override
		public void info(String var1) {
			this.logger.info(var1);
		}

		@Override
		public void info(String var1, Object var2) {
			this.logger.info(var1, var2);
		}

		@Override
		public void info(String var1, Object var2, Object var3) {
			this.logger.info(var1, var2, var3);
		}

		@Override
		public void info(String var1, Object... var2) {
			this.logger.info(var1, var2);
		}

		@Override
		public void info(String var1, Throwable var2) {
			this.logger.info(var1, var2);
		}

		@Override
		public void warn(String var1) {
			this.logger.warn(var1);
			
		}

		@Override
		public void warn(String var1, Object var2) {
			this.logger.warn(var1, var2);
		}

		@Override
		public void warn(String var1, Object var2, Object var3) {
			this.logger.warn(var1, var2, var3);
		}

		@Override
		public void warn(String var1, Object... var2) {
			this.logger.warn(var1, var2);
		}

		@Override
		public void warn(String var1, Throwable var2) {
			this.logger.warn(var1, var2);
		}

		@Override
		public void error(String var1) {
			this.logger.error(var1);
		}

		@Override
		public void error(String var1, Object var2) {
			this.logger.error(var1, var2);
		}

		@Override
		public void error(String var1, Object var2, Object var3) {
			this.logger.error(var1, var2, var3);
		}

		@Override
		public void error(String var1, Object... var2) {
			this.logger.error(var1, var2);
		}

		@Override
		public void error(String var1, Throwable var2) {
			this.logger.error(var1, var2);
		}
		
	}

}
