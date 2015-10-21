/**
 *
 */
package net.bpiwowar.mg4j.extensions.utils;

import org.slf4j.Logger;

import java.io.PrintWriter;

/**
 * @author bpiwowar
 * @date Jan 11, 2008
 */
public class LoggerPrintWriter extends PrintWriter {
    private Logger logger;
    private LogLevel level;

    /**
     * Creates a PrintStream for a given logger at a given output level
     */
    public LoggerPrintWriter(Logger logger, LogLevel level) {
        super(new LoggerOutputStream(logger, level));
        this.logger = logger;
        this.level = level;
    }

}
