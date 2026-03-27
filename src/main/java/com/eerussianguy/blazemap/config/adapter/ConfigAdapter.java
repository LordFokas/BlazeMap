package com.eerussianguy.blazemap.config.adapter;

public interface ConfigAdapter<T> {
    T get();

    void set(T value);
}
