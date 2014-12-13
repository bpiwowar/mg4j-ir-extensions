/**
 *
 */
package net.bpiwowar.mg4j.extensions.utils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.PrintStream;

/**
 * @author bpiwowar
 * @date Jan 11, 2008
 */
public class LoggerPrintStream extends PrintStream {
    /**
     * Creates a PrintStream for a given logger at a given output level
     */
    public LoggerPrintStream(Logger logger, Level level) {
        super(new LoggerOutputStream(logger, level));
    }
}
