package net.bpiwowar.mg4j.extensions.utils.timer;

import net.bpiwowar.mg4j.extensions.utils.LazyString;
import net.bpiwowar.mg4j.extensions.utils.Memory;
import org.apache.log4j.Logger;

/**
 * A simple timer that only reports
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class CountTimer extends Timer {
    private String message;
    private final boolean showMemory;

    public CountTimer(Logger logger, long interval, String what) {
        this(logger, interval, what, false);
    }

    public CountTimer(Logger logger, long interval, String what,
                      boolean showMemory) {
        super(logger, interval);
        this.showMemory = showMemory;
        if (showMemory)
            this.message = String
                    .format(
                            "Processed %%d %s (%%.2f %s/hour), %%d%%%% memory available",
                            what, what);
        else
            this.message = String.format("Processed %%d %s (%%.2f %s/hour)",
                    what, what);
    }

    // Could be different for different reporters
    int count = 0;

    synchronized public void add() {
        count++;
    }

    protected void report() {
        long newTime = System.currentTimeMillis();
        double hours = (newTime - getStart()) / (double) (1000 * 60 * 60);
        if (showMemory)
            logger.info(LazyString.format(message, count, count / hours, Memory
                    .percAvailableMemory()));
        else
            logger.info(LazyString.format(message, count, count / hours));
    }
}
