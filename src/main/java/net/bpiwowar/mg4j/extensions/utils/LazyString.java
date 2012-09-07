package net.bpiwowar.mg4j.extensions.utils;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 2/8/12
 */
public class LazyString {
    private String format;
    private Object[] objects;

    public LazyString(String format, Object[] objects) {
        this.format = format;
        this.objects = objects;
    }

    @Override
    public String toString() {
        return String.format(format, objects);
    }

    public static LazyString format(String format, Object... objects) {
        return new LazyString(format, objects);
    }
}
