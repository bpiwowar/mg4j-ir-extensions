package net.bpiwowar.mg4j.extensions.utils;

public class Memory {
	private final static Runtime RUNTIME = Runtime.getRuntime();

	/**
	 * Returns true if less then 5% of the available memory is free.
	 * 
	 * @return true if less then 5% of the available memory is free.
	 */
	public static boolean memoryIsLow() {
		return availableMemory() * 100 < RUNTIME.totalMemory() * 5;
	}
	
	/**
	 * Returns the amount of total memory 
	 * 
	 * @return the amount of available memory, in bytes.
	 */
	public static long maxMemory() {
		return RUNTIME.maxMemory();
	}

	/**
	 * Returns the amount of available memory (free memory plus never allocated
	 * memory).
	 * 
	 * @return the amount of available memory, in bytes.
	 */
	public static long availableMemory() {
		return RUNTIME.freeMemory()
				+ (RUNTIME.maxMemory() - RUNTIME.totalMemory());
	}

	/**
	 * Returns the percentage of available memory (free memory plus never
	 * allocated memory).
	 * 
	 * @return the percentage of available memory.
	 */
	public static int percAvailableMemory() {
		return (int) ((availableMemory() * 100) / Runtime.getRuntime()
				.maxMemory());
	}

	/**
	 * Tries to compact memory as much as possible by forcing garbage
	 * collection.
	 */
	public static void compactMemory() {
		try {
			final byte[][] unused = new byte[128][];
			for (int i = unused.length; i-- != 0;)
				unused[i] = new byte[2000000000];
		} catch (OutOfMemoryError itsWhatWeWanted) {
			System.gc();
		}
	}
}
