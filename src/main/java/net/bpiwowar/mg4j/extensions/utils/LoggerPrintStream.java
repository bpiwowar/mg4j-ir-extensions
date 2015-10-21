/**
 *
 */
package net.bpiwowar.mg4j.extensions.utils;

import org.slf4j.Logger;

import java.io.PrintStream;

/**
 * @author bpiwowar
 * @date Jan 11, 2008
 */
public class LoggerPrintStream extends PrintStream {
    /**
     * Creates a PrintStream for a given logger at a given output level
     */
    public LoggerPrintStream(Logger logger, LogLevel level) {
        super(new LoggerOutputStream(logger, level));
    }
}
