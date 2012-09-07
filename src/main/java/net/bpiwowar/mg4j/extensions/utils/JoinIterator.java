/**
 * 
 */
package net.bpiwowar.mg4j.extensions.utils;

import org.apache.log4j.Logger;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Intersection iterator between two sorted collections
 * 
 * @author bpiwowar
 * @date Nov 19, 2007
 * 
 * @param <Key>
 *            The key
 * @param <Value>
 *            The value
 */
public class JoinIterator<Key, Value> implements Iterator<Pair<Key, Value>> {
	final static private Logger logger = Logger.getLogger(JoinIterator.class);

	/**
	 * The heap used when joining.
	 */
	Heap<Entry> heap = new Heap<Entry>();

	/**
	 * The number of joined streams
	 */
	int size = 0;

	/**
	 * Our current value
	 */
	private Pair<Key, Value> current;

	/**
	 * The aggregator
	 */
	private final Aggregator<Key, Value> aggregator;

	/**
	 * Our comparator (null if there is none)
	 */
	private Comparator<Key> comparator;

	/**
	 * Creates a new intersection iterator with a comparator
	 * 
	 * @param <T>
	 * @param iterables
	 */
	public <T extends Iterable<? extends Key>> JoinIterator(
            Comparator<Key> comparator, Aggregator<Key, Value> aggregator,
            T... iterables) {
		this.comparator = comparator;
		this.aggregator = aggregator;
		for (T iterable : iterables) {
			final Entry entry = new Entry(size++, iterable.iterator());
			if (entry.current != null)
				heap.add(entry);
		}
	}
	
	
	/**
	 * Creates a new intersection iterator with a comparator
	 * 
	 * @param <T>
	 */
	public <T extends Iterator<? extends Key>> JoinIterator(
            Comparator<Key> comparator, Aggregator<Key, Value> aggregator,
            T... iterators) {
		this.comparator = comparator;
		this.aggregator = aggregator;
		for (T iterator : iterators) {
			final Entry entry = new Entry(size++, iterator);
			if (entry.current != null)
				heap.add(entry);
		}
	}

	static public <Key, Value, T extends Iterable<Key>> JoinIterator<Key, Value> newInstance(
			Aggregator<Key, Value> aggregator, T... iterables) {
		Comparator<Key> comparator = new Comparator<Key>() {
			@SuppressWarnings("unchecked")
			@Override
			public int compare(Key o1, Key o2) {
				return ((Comparable<Key>) o1).compareTo(o2);
			}
		};
		return new JoinIterator<Key, Value>(comparator, aggregator, iterables);

	}

	/**
	 * Creates a new intersection iterator with the default comparator
	 * 
	 * @param <T>
	 * @param iterables
	 */
	public <T extends Iterable<? extends Key>> JoinIterator(
            Aggregator<Key, Value> aggregator, T... iterables) {
		this.aggregator = aggregator;

		this.comparator = new Comparator<Key>() {
			@SuppressWarnings("unchecked")
			@Override
			public int compare(Key o1, Key o2) {
				return ((Comparable<Key>) o1).compareTo(o2);
			}
		};

		for (T iterable : iterables) {
			final Entry entry = new Entry(size++, iterable.iterator());
			if (entry.current != null)
				heap.add(entry);
		}
	}


	
	public static <Key extends Comparable<Key>> JoinIterator<Key, Integer> newInstance(
			Iterable<Key>... iterables) {
		return new JoinIterator<Key, Integer>(new CountAgregator<Key>(),
				iterables);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Iterator#hasNext()
	 */
	public boolean hasNext() {
		checkNext();
		return current != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Iterator#next()
	 */
	public Pair<Key, Value> next() {
		checkNext();
		Pair<Key, Value> x = current;
		current = null;
		return x;
	}

	void checkNext() {
		if (!heap.isEmpty() && current == null)
			current = doNext();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Iterator#next()
	 */
	public Pair<Key, Value> doNext() {
		// Check if we have not already finished
		if (heap.isEmpty())
			throw new NoSuchElementException();

		// Get the next element from the heap
		Entry entry = heap.peek();

		// Loop until we have found a key for which we have enough values
		int N;
		Key u;

		do {
			// Get all the values with the same key
			N = 0;
			// The current key
			u = entry.current;
			// Reset the aggregator
			aggregator.reset();

			do {
				// Set the value
				logger
						.debug(LazyString.format("(%d) %s: %s", entry.streamIndex, u,
								entry.current));
				aggregator.set(entry.streamIndex, entry.current);

				// get the next value from this stream and update
				entry.next();
				if (entry.current != null)
					heap.update(entry);
				else
					heap.pop();

				// Get the next entry from the heap
				entry = heap.isEmpty() ? null : heap.peek();
				N++;
			} while (entry != null && comparator.compare(entry.current, u) == 0);

			logger.debug(LazyString.format("End of loop N=%d/%d", N, size));
		} while (entry != null && !aggregator.accept(N, size));

		if (!aggregator.accept(N, size))
			return null;

		logger.debug(LazyString.format("Returns a new pair for %s, heap is empty = %b", u, heap
				.isEmpty()));

		return Pair.create(u, aggregator.aggregate());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Iterator#remove()
	 */
	public void remove() {
		throw new NoSuchMethodError(
				"remove is not implemented in UnionIterator");
	}

	/**
	 * An entry that implements the heap code
	 * 
	 * @author bpiwowar
	 * @date Nov 19, 2007
	 */
	class Entry implements HeapElement<Entry>, Comparable<Entry> {
		final Iterator<? extends Key> iterator;

		Key current;

		/**
		 * The index in the list of the provided streams
		 */
		final int streamIndex;

		/**
		 * The index for the heap
		 */
		int index = -1;

		/**
		 * @param entryIndex
		 * @param iterator
		 */
		public Entry(int entryIndex, Iterator<? extends Key> iterator) {
			this.iterator = iterator;
			this.streamIndex = entryIndex;
			next();
		}

		protected void next() {
			if (iterator.hasNext())
				current = iterator.next();
			else
				current = null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Entry o) {
			return comparator.compare(current, o.current);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see bpiwowar.logtools.sessionize.HeapElement#getIndex()
		 */
		public int getIndex() {
			return index;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see bpiwowar.logtools.sessionize.HeapElement#setIndex(int)
		 */
		public void setIndex(int index) {
			this.index = index;
		}

	}

}