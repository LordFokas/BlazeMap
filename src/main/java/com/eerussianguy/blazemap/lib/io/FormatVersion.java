package com.eerussianguy.blazemap.lib.io;

import java.io.IOException;
import java.util.function.Function;

import com.eerussianguy.blazemap.api.util.MinecraftStreams;

public interface FormatVersion<T> {
    void write(T data, MinecraftStreams.Output stream) throws IOException;
    T read(MinecraftStreams.Input stream) throws IOException;



    @FunctionalInterface
    interface Outdated<T> extends FormatVersion<T> {
        @Override
        default void write(T data, MinecraftStreams.Output stream) throws IOException {
            throw new UnsupportedOperationException("This version is outdated and can no longer be used to write");
        }
    }
}
