package net.bpiwowar.mg4j.extensions.utils;

/**
 * @author bpiwowar
 * @date 22/03/2007
 */
public interface HeapElement<E> extends Comparable<E> {
    int getIndex();

    void setIndex(int index);
}