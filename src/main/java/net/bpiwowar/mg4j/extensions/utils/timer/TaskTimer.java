package net.bpiwowar.mg4j.extensions.utils.timer;

import net.bpiwowar.mg4j.extensions.utils.LoggerPrintStream;
import net.bpiwowar.mg4j.extensions.utils.Memory;
import net.bpiwowar.mg4j.extensions.utils.Time;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

/**
 * A timer that reports upon the different tasks & subtasks
 * 
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
final public class TaskTimer extends Timer {

	public class Task {

		/**
		 * Starting timestamp
		 */
		long start;

		/**
		 * How much have we done?
		 */
		int count;

		/**
		 * How much units of works should we do? A value of 0 means unknown
		 */
		int total;

		private String what;

		private final String name;

		private boolean removed = false;

		public Task(String name, String what, int total) {
			super();
			reset(what, total);
			this.name = name;
			this.start = System.currentTimeMillis();
			addTask(this);
		}

		public Task(String name, String what) {
			this(name, what, 0);
		}

		@Override
		protected void finalize() throws Throwable {
			super.finalize();
			if (!removed)
				remove(this);
			removed = true;
		}

		/**
		 * Print the current status of the task
		 * 
		 * @param timestamp
		 *            The current timestamp
		 * @param out
		 *            The stream where things are printed
		 */
		synchronized public void print(long timestamp, PrintStream out) {
			long elapsed = timestamp - start;
			float itemPerMinute = (float) (count * 1000f * 60f)
					/ (float) (elapsed);
			if (total > 0) {
				if (count > 0) {
					int etaSeconds = (int) ((float) (total - count)
							* ((float) elapsed * 1e-3) / (float) count);
					out
							.format(
									"(%s, RT/ETA: %s/%s) %d %s (on %d) processed at %.1f/min (%.2f%%)",
									name,
									Time.formatTimeInSeconds((int) (elapsed / 1000)),
									Time.formatTimeInSeconds(etaSeconds), count,
									what, total, itemPerMinute, count * 100.
											/ total);
				} else {
					out
							.format(
									"(%s, RT/ETA: %s/?) %d %s (on %d) processed at %.1f/min (%.2f%%)",
									name,
									Time.formatTimeInSeconds((int) (elapsed / 1000)),
									count, what, total, itemPerMinute, count
											* 100. / total);
				}

			} else {
				out.format("(%s, RT: %s) %d %s processed at %.1f/min", name,
						Time.formatTimeInSeconds((int) (elapsed / 1000)), count,
						what, itemPerMinute);
			}
		}

		/**
		 * Resets the task
		 * 
		 * @param what
		 *            Give a new description
		 * @param total
		 *            Give a new total
		 */
		synchronized public void reset(String what, int total) {
			this.what = what;
			this.total = total;
			this.start = System.currentTimeMillis();
			this.count = 0;
		}

		synchronized public void regress(int d) {
			count -= d;
		}

		/**
		 * Progress of 1 unit
		 */
		synchronized public void progress() {
			count++;
		}

		/**
		 * Notify a progress of an arbitrary number of units
		 * 
		 * @param d
		 */
		synchronized public void progress(int d) {
			count += d;
		}

		/**
		 * End this task
		 */
		public void end() {
			remove(this);
		}

	}

	/**
	 * The set of tasks
	 */
	private Vector<Task> tasks = new Vector<Task>();

	/**
	 * Default constructor
	 * 
	 * @param logger
	 *            The logger that should be used to report
	 * @param interval
	 *            The interval between two reports
	 */
	public TaskTimer(Logger logger, long interval) {
		super(logger, interval);
	}

	public TaskTimer(Logger logger) {
		super(logger);
	}

	/**
	 * Remove a task from this timer (this method is private since it is called
	 * by the task)
	 * 
	 * @param task
	 *            The task to remove
	 */
	private void remove(Task task) {
		synchronized (tasks) {
			task.removed = true;
			tasks.remove(task);
		}
	}

	/**
	 * Add a new task to this timer
	 * 
	 * @param task
	 *            The task to add
	 */
	protected void addTask(Task task) {
		synchronized (tasks) {
			tasks.add(task);
		}
	}

	public void flush() {
		report();
	}

	private SimpleDateFormat formatter = (SimpleDateFormat) DateFormat
			.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale
					.getDefault());
	{
		TimeZone tz = TimeZone.getDefault();
		formatter.setTimeZone(tz);
		formatter.applyPattern("dd/MM/yyyy HH:mm:ss z");
	}

	@Override
	protected void report() {
		long ts = System.currentTimeMillis();
		logger.info(String.format("%s -- %d%% memory available (total %d Mb)", formatter
				.format(Calendar.getInstance().getTime()), Memory.percAvailableMemory(),
				Memory.maxMemory() / (1024 * 1024)));
		int i = 0;
		LoggerPrintStream out = new LoggerPrintStream(logger, Level.INFO);
		synchronized (tasks) {
			for (Task task : tasks) {
				i++;
				final int size = tasks.size();
				out.format("\t[%d/%d] ", i, size);
				task.print(ts, out);
				out.flush();
			}
		}
	}

}
