package com.swingfrog.summer.log;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.Level;

public class ColorPatternLayout extends PatternLayout {
	static final String CONFIGURATION_SEPARATOR = "/";

	static final String PREFIX = "\u001b[";
	static final String SUFFIX = "m";
	static final char SEPARATOR = ';';
	static final String END_COLOR = PREFIX + SUFFIX;

	static final String ATTR_NORMAL = "0";
	static final String ATTR_BRIGHT = "1";
	static final String ATTR_DIM = "2";
	static final String ATTR_UNDERLINE = "3";
	static final String ATTR_BLINK = "5";
	static final String ATTR_REVERSE = "7";
	static final String ATTR_HIDDEN = "8";

	static final String FG_BLACK = "30";
	static final String FG_RED = "31";
	static final String FG_GREEN = "32";
	static final String FG_YELLOW = "33";
	static final String FG_BLUE = "34";
	static final String FG_MAGENTA = "35";
	static final String FG_CYAN = "36";
	static final String FG_WHITE = "37";

	static final String BG_BLACK = "40";
	static final String BG_RED = "41";
	static final String BG_GREEN = "42";
	static final String BG_YELLOW = "44";
	static final String BG_BLUE = "44";
	static final String BG_MAGENTA = "45";
	static final String BG_CYAN = "46";
	static final String BG_WHITE = "47";

	private String fatalErrorColor = PREFIX + ATTR_DIM + SEPARATOR + FG_RED + SUFFIX;
	private String errorColor = PREFIX + ATTR_DIM + SEPARATOR + FG_MAGENTA + SUFFIX;
	private String warnColor = PREFIX + ATTR_DIM + SEPARATOR + FG_YELLOW + SUFFIX;
	private String infoColor = PREFIX + ATTR_DIM + SEPARATOR + FG_GREEN + SUFFIX;
	private String debugColor = PREFIX + ATTR_DIM + SEPARATOR + FG_CYAN + SUFFIX;
	private String traceColor = PREFIX + ATTR_DIM + SEPARATOR + FG_WHITE + SUFFIX;

	public ColorPatternLayout() {
		super();
	}

	public ColorPatternLayout(String pattern) {
		super(pattern);
	}

	public String format(LoggingEvent event) {
		if (event.getLevel() == Level.FATAL) {
			return fatalErrorColor + super.format(event) + END_COLOR;
		} else if (event.getLevel() == Level.ERROR) {
			return errorColor + super.format(event) + END_COLOR;
		} else if (event.getLevel() == Level.WARN) {
			return warnColor + super.format(event) + END_COLOR;
		} else if (event.getLevel() == Level.INFO) {
			return infoColor + super.format(event) + END_COLOR;
		} else if (event.getLevel() == Level.DEBUG) {
			return debugColor + super.format(event) + END_COLOR;
		} else if (event.getLevel() == Level.TRACE) {
			return traceColor + super.format(event) + END_COLOR;
		} else {
			throw new RuntimeException("Unsupported Level " + event.toString());
		}
	}

}