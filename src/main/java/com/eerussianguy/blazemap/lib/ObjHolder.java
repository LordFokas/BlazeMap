package com.eerussianguy.blazemap.lib;

import java.util.function.Function;

public class ObjHolder<T> {
    private T value;

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
    }

    public void mutate(Function<T, T> transform) {
        this.value = transform.apply(value);
    }
}
