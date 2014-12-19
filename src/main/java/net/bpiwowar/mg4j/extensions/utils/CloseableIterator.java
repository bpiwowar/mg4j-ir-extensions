package net.bpiwowar.mg4j.extensions.utils;

import com.google.common.collect.AbstractIterator;

/**
 * A closeable iterator
 */
abstract public class CloseableIterator<T> extends AbstractIterator<T> implements AutoCloseable {
    @Override
    public void close() throws Exception {
    }
}
