package com.eerussianguy.blazemap.lib;

import java.util.function.Consumer;
import java.util.function.Function;

public class ObjHolder<T> {
    private T value;
    private Consumer<T> responder = $ -> {};

    public ObjHolder() {
        this(null);
    }

    public ObjHolder(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
        responder.accept(value);
    }

    public void mutate(Function<T, T> transform) {
        set(transform.apply(value));
    }

    public void setResponder(Consumer<T> responder) {
        this.responder = responder;
        responder.accept(value);
    }
}
