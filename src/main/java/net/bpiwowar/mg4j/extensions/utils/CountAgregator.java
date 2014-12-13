package net.bpiwowar.mg4j.extensions.utils;

public class CountAgregator<T1> implements Aggregator<T1, Integer> {
    int count = 0;

    @Override
    public Integer aggregate() {
        return count;
    }

    @Override
    public void reset() {
        count = 0;
    }

    @Override
    public void set(int index, T1 k) {
        count++;
    }

    @Override
    public boolean accept(int n, int size) {
        return n == size;
    }

}
