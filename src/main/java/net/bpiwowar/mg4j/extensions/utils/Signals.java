package net.bpiwowar.mg4j.extensions.utils;

import sun.misc.Signal;

import java.util.function.Consumer;

/**
 * Created by bpiwowar on 16/01/16.
 */
public enum Signals {
    PIPE("PIPE");

    String name;

    Signals(String name) {
        this.name = name;
    }

    public void handle(Consumer<Signals> f) {
        Signal signal = new Signal(name);
        Signal.handle(signal, s -> f.accept(this));
    }
}
