package net.bpiwowar.mg4j.extensions.utils;

import bpiwowar.argparser.utils.AbstractIterator;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * Output utilities
 *
 * @author bpiwowar
 */
public class Output {

    // ===== Using a print stream output =====
    static public interface PrintFormatter<T> {
        void print(PrintStream out, T t);
    }

    private static final class NullPrintFormatter<T> implements
            PrintFormatter<T> {
        @Override
        public void print(PrintStream out, T t) {
            out.print(t);
        }
    }

    static public <T, U extends Iterator<? extends T>> void print(
            PrintStream out, String separator, U iterator) {
        print(out, separator, iterator, new NullPrintFormatter<T>());
    }


    final public static <T> void print(PrintStream out, String separator, T[] fields, PrintFormatter<T> formatter) {
        print(out, separator, Arrays.asList(fields).iterator(), formatter);
    }


    final static public <T, U extends Iterable<? extends T>> void print(
            PrintStream out, String separator, U iterable,
            PrintFormatter<T> printer) {
        print(out, separator, iterable.iterator(), printer);
    }

    final static public <T, U extends Iterator<? extends T>> void print(
            PrintStream out, String separator, U iterator,
            PrintFormatter<T> printer) {
        boolean first = true;
        while (iterator.hasNext()) {
            T t = iterator.next();
            if (first)
                first = false;
            else
                out.print(separator);

            printer.print(out, t);
        }
    }


    // ===== Using a formatter =====

    private static final class NullFormatter<T> implements Formatter<T> {
        public String format(T t) {
            return t.toString();
        }
    }

    static public <T> void print(PrintStream out, String separator,
                                 Iterable<T> iterable) {
        print(out, separator, iterable.iterator(), new NullFormatter<T>());
    }

    public static <T> void print(PrintStream out, String separator, T[] fields) {
        print(out, separator, fields, new NullFormatter<T>());
    }

    public static void print(PrintWriter out, String separator,
                             final double[] array) {
        print(out, separator, new AbstractIterator<Double>() {
            int i = 0;

            @Override
            protected boolean storeNext() {
                if (i < array.length) {
                    value = array[i];
                    i++;
                    return true;
                }
                return false;
            }
        }, new NullFormatter<Double>());
    }

    static public <T, U extends Iterator<? extends T>> void print(
            PrintWriter out, String separator, U iterator,
            Formatter<T> formatter) {
        boolean first = true;
        while (iterator.hasNext()) {
            T t = iterator.next();
            if (first)
                first = false;
            else
                out.print(separator);
            out.print(formatter.format(t));
        }
    }

    /**
     * @param out
     * @param separator
     * @param array
     */
    public static void print(PrintStream out, String separator,
                             final double[] array) {
        print(out, separator, new AbstractIterator<Double>() {
            int i = 0;

            @Override
            protected boolean storeNext() {
                if (i < array.length) {
                    value = array[i];
                    i++;
                    return true;
                }
                return false;
            }
        }, new NullFormatter<Double>());
    }

    static public <T, U extends Iterator<T>> void print(PrintStream out,
                                                        String separator, U iterator, Formatter<T> formatter) {
        boolean first = true;
        while (iterator.hasNext()) {
            T t = iterator.next();
            if (first)
                first = false;
            else
                out.print(separator);
            out.print(formatter.format(t));
        }
    }

    static public <T, U extends Iterator<T>> void print(PrintStream out,
                                                        String separator, T[] array, Formatter<T> formatter) {
        boolean first = true;
        for (int i = 0; i < array.length; i++) {
            T t = array[i];
            if (first)
                first = false;
            else
                out.print(separator);
            out.print(formatter.format(t));
        }
    }

    static public <T, U extends Iterable<T>> void print(PrintStream out,
                                                        String separator, U iterable, Formatter<T> formatter) {
        print(out, separator, iterable.iterator(), formatter);
    }

    static public <T> void print(PrintWriter out, String separator,
                                 Iterable<T> iterable) {
        print(out, separator, iterable, new Formatter<T>() {
            public String format(T t) {
                return t.toString();
            }

        });
    }

    static public <T, U extends Iterable<? extends T>> void print(
            PrintWriter out, String separator, U iterable,
            Formatter<T> formatter) {
        boolean first = true;
        for (T t : iterable) {
            if (first)
                first = false;
            else
                out.print(separator);
            out.print(formatter.format(t));
        }
    }

    /**
     * @param buffer
     * @param string
     * @param x
     */
    public static void print(StringBuilder builder, String separator,
                             Iterable<?> iterable) {
        boolean first = true;
        for (Object t : iterable) {
            if (first)
                first = false;
            else
                builder.append(separator);
            builder.append(t);
        }
    }

    /**
     * @param buffer
     * @param string
     * @param x
     */
    public static <U> void print(StringBuilder builder, String separator,
                                 U[] iterable) {
        boolean first = true;
        for (Object t : iterable) {
            if (first)
                first = false;
            else
                builder.append(separator);
            builder.append(t);
        }
    }

    /**
     * @param buffer
     * @param string
     * @param x
     */
    public static void print(StringBuilder builder, String separator,
                             int[] iterable) {
        boolean first = true;
        for (Object t : iterable) {
            if (first)
                first = false;
            else
                builder.append(separator);
            builder.append(t);
        }
    }

    /**
     * @param buffer
     * @param string
     * @param x
     */
    static public <T, U extends Iterable<? extends T>> void print(
            StringBuilder builder, String separator, U iterable,
            Formatter<T> formatter) {
        boolean first = true;
        for (T t : iterable) {
            if (first)
                first = false;
            else
                builder.append(separator);
            builder.append(formatter.format(t));
        }
    }

    /**
     * @param string
     * @param actions
     * @return
     */
    public static <U> String toString(String separator, Iterable<U> iterable) {
        StringBuilder sb = new StringBuilder();
        print(sb, separator, iterable);
        return sb.toString();
    }

    /**
     * @param string
     * @param actions
     * @return
     */
    public static <U> String toString(String separator, U[] iterable) {
        StringBuilder sb = new StringBuilder();
        print(sb, separator, iterable);
        return sb.toString();
    }

    public static <T> String toString(String separator, T[] array,
                                      Formatter<T> formatter) {
        return toString(separator, Arrays.asList(array), formatter);
    }

    public static <T, U extends Iterable<? extends T>> String toString(
            String separator, U iterable, Formatter<T> formatter) {
        StringBuilder sb = new StringBuilder();
        print(sb, separator, iterable, formatter);
        return sb.toString();
    }

    // -------
    // ------- Formatters
    // -------

    /**
     * A formmatter that outputs the size of the collection
     */
    static final public Formatter<Collection<?>> COLLECTION_COUNT_FORMATTER = new Formatter<Collection<?>>() {
        @Override
        public String format(Collection<?> t) {
            return Integer.toString(t.size());
        }
    };

    static public interface Formatter<T> {
        public String format(T t);
    }
}
