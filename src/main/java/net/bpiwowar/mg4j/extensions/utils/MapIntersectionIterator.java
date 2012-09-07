/**
 *
 */
package net.bpiwowar.mg4j.extensions.utils;

import org.apache.log4j.Logger;

import java.util.*;

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
final public class MapIntersectionIterator<Key, Value, Result> implements
        Iterator<Pair<Key, Result>> {
    final static private Logger logger = Logger.getLogger(MapIntersectionIterator.class);

    final Comparator<Key> comparator;

    /**
     * An entry that implements the heap code
     *
     * @author bpiwowar
     * @date Nov 19, 2007
     *
     * @param <U>
     * @param <V>
     */
    static class Entry<U, V> implements
            HeapElement<Entry<U, V>>,
            Comparable<MapIntersectionIterator.Entry<U, V>> {
        final Iterator<? extends java.util.Map.Entry<U, V>> iterator;

        final Comparator<U> comparator;

        Map.Entry<U, V> current;

        final int entryIndex;

        int index = -1;

        /**
         * Creates a new entry
         * @param i The index
         * @param comparator
         * @param iterator
         */
        public Entry(int i, Comparator<U> comparator,
                     Iterator<? extends Map.Entry<U, V>> iterator) {
            this.comparator = comparator;
            this.iterator = iterator;
            this.entryIndex = i;
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
        @SuppressWarnings("unchecked")
        public int compareTo(MapIntersectionIterator.Entry<U, V> o) {
            if (comparator == null)
                return ((Comparable) current.getKey()).compareTo(o.current
                        .getKey());
            return comparator.compare(current.getKey(), o.current.getKey());
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

    Heap<Entry<Key, Value>> heap = new Heap<MapIntersectionIterator.Entry<Key, Value>>();

    final ArrayList<Map.Entry<Key, Value>> list;

    int size = 0;

    private Pair<Key, Result> current;

    private final Aggregator<Map.Entry<Key, Value>, Result> aggregator;

    /**
     * Creates a new intersection iterator
     *
     * @param <T>
     * @param iterables
     */
    public <T extends Iterable<? extends Map.Entry<Key, Value>>> MapIntersectionIterator(
            Aggregator<Map.Entry<Key, Value>, Result> aggregator,
            T... iterables) {
        this.aggregator = aggregator;
        this.comparator = null;
        list = new ArrayList(iterables.length);
        for (T iterable : iterables) {
            final MapIntersectionIterator.Entry<Key, Value> entry = new MapIntersectionIterator.Entry<Key, Value>(
                    size++, comparator, iterable.iterator());
            if (entry.current != null)
                heap.add(entry);
            list.add(null);
        }
    }

    public MapIntersectionIterator(Comparator<Key> comparator,
                                   Aggregator<Map.Entry<Key, Value>, Result> aggregator,
                                   Map<Key, Value>... maps) {
        this.aggregator = aggregator;
        this.comparator = comparator;
        list = new ArrayList<Map.Entry<Key, Value>>();
        for (Map<Key, Value> map : maps) {
            final MapIntersectionIterator.Entry<Key, Value> entry = new MapIntersectionIterator.Entry<Key, Value>(
                    size++, comparator, map.entrySet().iterator());
            if (entry.current != null)
                heap.add(entry);
            list.add(null);
        }
    }

    public static final <U extends Comparable<U>, V, T extends Iterable<? extends Map.Entry<U, V>>, R> MapIntersectionIterator<U, V, R> create(
            Aggregator<Map.Entry<U, V>, R> aggregator, T... iterables) {
        return new MapIntersectionIterator<U, V, R>(aggregator, iterables);
    }

    public <T extends Map<Key, Value>> MapIntersectionIterator(
            Aggregator<Map.Entry<Key, Value>, Result> aggregator, T... maps) {
        this.aggregator = aggregator;
        this.comparator = null;
        list = new ArrayList<Map.Entry<Key, Value>>();
        for (Map<Key, Value> map : maps) {
            final MapIntersectionIterator.Entry<Key, Value> entry = new MapIntersectionIterator.Entry<Key, Value>(
                    size++, comparator, map.entrySet().iterator());
            if (entry.current != null)
                heap.add(entry);
            list.add(null);
        }
    }

    public static final <U extends Comparable<U>, V, T extends Map<U, V>, R> MapIntersectionIterator<U, V, R> create(
            Aggregator<Map.Entry<U, V>, R> aggregator, T... iterables) {
        return new MapIntersectionIterator<U, V, R>(aggregator, iterables);
    }

    /**
     * Creates a new intersection iterator
     *
     */
    public <Iterators extends Iterator<? extends Map.Entry<Key, Value>>> MapIntersectionIterator(
            Aggregator<Map.Entry<Key, Value>, Result> aggregator,
            Iterators... iterators) {
        this.aggregator = aggregator;
        this.comparator = null;
        list = new ArrayList<Map.Entry<Key, Value>>();
        for (Iterators iterator : iterators) {
            final MapIntersectionIterator.Entry<Key, Value> entry = new MapIntersectionIterator.Entry<Key, Value>(
                    size++, comparator, iterator);
            if (entry.current != null)
                heap.add(entry);
            list.add(null);
        }
    }

    // public static final <U extends Comparable<U>, V, Iterators extends
    // Iterator<? extends Map.Entry<U, V>>> MapIntersectionIterator<U, V>
    // create(
    // Class<V> vClass, Iterators... iterators) {
    // return new MapIntersectionIterator<U, V>(vClass, iterators);
    // }

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
    public Pair<Key, Result> next() {
        checkNext();
        Pair<Key, Result> x = current;
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
    public Pair<Key, Result> doNext() {
        if (heap.isEmpty())
            throw new NoSuchElementException();
        MapIntersectionIterator.Entry<Key, Value> entry = heap.peek();

        // Loop until we have found a key for which we have all the values
        int N;
        Key u;
        do {
            // Get all the values with the same key
            N = 0;
            u = entry.current.getKey();
            for (int i = 0; i < size; i++)
                list.set(i, null);
            do {
                // Set the value
                list.set(entry.entryIndex, entry.current);
                logger.debug(LazyString.format("(%d) %s: %s => %s", entry.entryIndex, u,
                        entry.current.getKey(), entry.current.getValue()));

                // get the next value from this stream and update
                entry.next();
                if (entry.current != null)
                    heap.update(entry);
                else
                    heap.pop();

                entry = heap.isEmpty() ? null : heap.peek();
                N++;
            } while (entry != null && compare(entry.current.getKey(), u) == 0);
            logger.debug(LazyString.format("End of loop N=%d/%d", N, size));
        } while (entry != null && N != size);

        if (N != size)
            return null;

        aggregator.reset();
        for (int i = 0; i < N; i++)
            aggregator.set(i, list.get(i));

        logger.debug(LazyString.format("Returns a new pair for %s, heap is empty = %b", u, heap
                .isEmpty()));

        return new Pair<Key, Result>(u, aggregator.aggregate());
    }

    @SuppressWarnings("unchecked")
    final private int compare(Key a, Key b) {
        if (comparator == null)
            return ((Comparable) a).compareTo(b);
        return comparator.compare(a, b);
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
}