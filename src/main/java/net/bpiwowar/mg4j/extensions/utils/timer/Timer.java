package net.bpiwowar.mg4j.extensions.utils.timer;


import bpiwowar.argparser.Argument;
import org.apache.log4j.Logger;

/**
 * A timer is a thread that reports to the logger the advance of a specific task
 * 
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
abstract class Timer extends Thread {
	Logger logger;
	private long start = -1;
	boolean running = true;
	int maximum = 0;

	/**
	 * The interval between two updates (by default 30 seconds)
	 */
	@Argument(name = "timer-interval", help = "Interval between two updates of the timer (in ms)")
	long interval = 30000;

	/**
	 * @param logger
	 *            The logger
	 * @param interval
	 *            The interval between two messages in milliseconds
	 * @param maximum
	 *            The maximum value for the counter (0 if unknown)
	 */
	Timer(Logger logger, long interval) {
		this.logger = logger;
		this.interval = interval;
		start();
	}

	public Timer(Logger logger) {
		this.logger = logger;
	}

	
	/**
	 * Stop the timer
	 */
	public final void close() {
		running = false;
		interrupt();
	}

	@Override
	public synchronized void start() {
		if (start >= 0) {
			logger.warn("Timer already started - skipping");
			return;
		}
		super.start();
		start = System.currentTimeMillis();
		logger.info("Timer started");
	}

	@Override
	final public void run() {
		logger.info("Timer is running");
		while (running) {
			try {
				Thread.sleep(interval);
				report();
			} catch (InterruptedException e) {
				if (running)
					logger.warn("Timer interrupted but not stopped");
			}
		}
	}

	protected abstract void report();

	/**
	 * Change the reporting interval
	 * 
	 * @param interval
	 */
	public void setInterval(long interval) {
		this.interval = interval;
	}

	public long getStart() {
		return start;
	}

}