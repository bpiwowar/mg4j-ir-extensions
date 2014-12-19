package net.bpiwowar.mg4j.extensions.utils;

/**
 * Created by bpiwowar on 16/12/14.
 */
public class Exceptions {
    public static <T> T propagate(Streams.ExceptionalProducer<T> producer) {
        try {
            return producer.produce();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static interface ExceptionalStatement {
        void evaluate() throws Exception;
    }

    public static void propagateStatement(ExceptionalStatement statement) {
        try {
            statement.evaluate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
