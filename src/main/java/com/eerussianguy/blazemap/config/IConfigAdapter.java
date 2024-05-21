package com.eerussianguy.blazemap.config;

public interface IConfigAdapter<T> {
    T get();

    void set(T value);
}
