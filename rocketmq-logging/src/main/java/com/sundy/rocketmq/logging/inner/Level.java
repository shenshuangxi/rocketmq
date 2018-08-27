package com.sundy.rocketmq.logging.inner;

import java.io.Serializable;

public class Level implements Serializable {

	private static final long serialVersionUID = -3853919797459810403L;
	
	transient int level;
	transient String levelName;
	transient int syslogEquivalent;
	
	public final static int OFF_INT = Integer.MAX_VALUE;
	public final static int ERROR_INT = 40000;
	public final static int WARN_INT = 30000;
	public final static int INFO_INT = 20000;
	public final static int DEBUG_INT = 10000;
	public final static int ALL_INT = Integer.MIN_VALUE;
	
	private static final String ALL_NAME = "ALL";
    private static final String DEBUG_NAME = "DEBUG";
    private static final String INFO_NAME = "INFO";
    private static final String WARN_NAME = "WARN";
    private static final String ERROR_NAME = "ERROR";
    private static final String OFF_NAME = "OFF";
    
    final static public Level OFF = new Level(OFF_INT, OFF_NAME, 0);
    final static public Level ERROR = new Level(ERROR_INT, ERROR_NAME, 3);
    final static public Level WARN = new Level(WARN_INT, WARN_NAME, 4);
    final static public Level INFO = new Level(INFO_INT, INFO_NAME, 6);
    final static public Level DEBUG = new Level(DEBUG_INT, DEBUG_NAME, 7);
    final static public Level ALL = new Level(ALL_INT, ALL_NAME, 7);
    
    protected Level(int level, String levelName, int syslogEquivalent) {
		this.level = level;
		this.levelName = levelName;
		this.syslogEquivalent = syslogEquivalent;
	}
    
    public static Level toLevel(String levelName) {
    	return toLevel(levelName, Level.DEBUG);
    }
    
	public static Level toLevel(int level) {
    	return toLevel(level, Level.DEBUG);
    }
	
	public static Level toLevel(int level, Level defaultLevel) {
		switch (level) {
		case ALL_INT:
			defaultLevel = ALL;
			break;
		case DEBUG_INT:
			defaultLevel = DEBUG;
			break;
		case INFO_INT:
			defaultLevel = INFO;
			break;
		case WARN_INT:
			defaultLevel = WARN;
			break;
		case ERROR_INT:
			defaultLevel = ERROR;
			break;
		case OFF_INT:
			defaultLevel = OFF;
			break;
		default:
			break;
		}
		return defaultLevel;
	}
	
	public static Level toLevel(String levelName, Level defaultLevel) {
		if (levelName==null) {
			return defaultLevel;
		}
		String upcaseLevelName = levelName.toUpperCase();
		switch (upcaseLevelName) {
		case ALL_NAME:
			defaultLevel = ALL;
			break;
		case DEBUG_NAME:
			defaultLevel = DEBUG;
			break;
		case INFO_NAME:
			defaultLevel = INFO;
			break;
		case WARN_NAME:
			defaultLevel = WARN;
			break;
		case ERROR_NAME:
			defaultLevel = ERROR;
			break;
		case OFF_NAME:
			defaultLevel = OFF;
			break;
		default:
			break;
		}
		return defaultLevel;
	}
	
	public boolean equals(Object o) {
        if (o instanceof Level) {
            Level r = (Level) o;
            return this.level == r.level;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = level;
        result = 31 * result + (levelName != null ? levelName.hashCode() : 0);
        result = 31 * result + syslogEquivalent;
        return result;
    }

    public boolean isGreaterOrEqual(Level r) {
        return level >= r.level;
    }

    final public String toString() {
        return levelName;
    }

    public final int toInt() {
        return level;
    }
    
    
	
}
