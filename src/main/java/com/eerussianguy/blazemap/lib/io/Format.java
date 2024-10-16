package com.eerussianguy.blazemap.lib.io;

import java.io.IOException;

import com.eerussianguy.blazemap.api.util.MinecraftStreams;

public interface Format<T> {
    void write(T data, MinecraftStreams.Output stream) throws IOException;
    T read(MinecraftStreams.Input stream) throws IOException;
    T readLegacy(MinecraftStreams.Input stream) throws IOException;
    boolean hasLegacySupport();
}
