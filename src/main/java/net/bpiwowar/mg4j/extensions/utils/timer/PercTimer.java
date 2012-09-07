package net.bpiwowar.mg4j.extensions.utils.timer;

import net.bpiwowar.mg4j.extensions.utils.LazyString;
import net.bpiwowar.mg4j.extensions.utils.Memory;
import org.apache.log4j.Logger;

/**
 * A simple timer that only reports 
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class PercTimer extends Timer {
	private String message;
	private final int total;

	public PercTimer(Logger logger, long interval, String what, int total) {
		super(logger, interval);
		this.total = total;
		this.message = String.format("Processed %%d %s (%%.2f %s/hour), %%.1f%%%% done, memory: %%d%%%% available", what, what);
	}

	// Could be different for different reporters
	int count = 0;

	synchronized public void add() {
		count++;
	}

	protected void report() {
		long newTime = System.currentTimeMillis();
		double hours = (newTime - getStart()) / (double) (1000 * 60 * 60);
		logger.info(LazyString.format(message, count, count / hours, count * 100. / (double)total, Memory.percAvailableMemory()));
	}
}
