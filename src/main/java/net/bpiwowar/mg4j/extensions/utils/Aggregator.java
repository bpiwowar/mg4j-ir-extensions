package net.bpiwowar.mg4j.extensions.utils;

import java.lang.reflect.Array;
import java.util.Map.Entry;

/**
 * Interface of classes that aggregate a set of results
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public interface Aggregator<Input, Output> {
	/**
	 * Called before a new key appears
	 */
	public void reset();

	/**
	 * Called to add a new value
	 * 
	 * @param index
	 *            The iterator index
	 * @param k
	 *            The corresponding key
	 */
	public void set(int index, Input k);

	/**
	 * Called to retrieve the value
	 */
	public Output aggregate();

	/**
	 * Should the result be accepted
	 * @param n The number of non null values in the join
	 * @param size The number of joined collections
	 * @return True if the result should be accepted
	 */
	public boolean accept(int n, int size);

	
	/**
	 * A simple aggregator that creates an array of values
	 * @author B. Piwowarski <benjamin@bpiwowar.net>
	 */
	static class MapValueArray<Key, Value> implements
			Aggregator<Entry<Key, Value>, Value[]> {
		private final Class<Value> valueClass;
		private final int length;
		Value[] array;
		private final boolean unionMode;

		public MapValueArray(Class<Value> valueClass, int length) {
			this(valueClass, length, false);
		}

		public MapValueArray(Class<Value> valueClass, int length, boolean unionMode) {
			this.valueClass = valueClass;
			this.length = length;
			this.unionMode = unionMode;
		}


		@Override
		public Value[] aggregate() {
			return array;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void reset() {
			array = (Value[]) Array.newInstance(valueClass, length);
		}

		@Override
		public void set(int index, Entry<Key, Value> k) {
			array[index] = k.getValue();
		}

		@Override
		public boolean accept(int n, int size) {
			return unionMode || n == size;
		}

	}

}
