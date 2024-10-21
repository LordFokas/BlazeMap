package com.eerussianguy.blazemap.lib.io;

import java.io.IOException;
import java.util.Objects;

import com.eerussianguy.blazemap.api.util.MinecraftStreams;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectArrayMap;

public class FormatSpec<T> implements Format<T> {
    private final Byte2ObjectArrayMap<FormatVersion<T>> versions = new Byte2ObjectArrayMap<>();
    private final FormatVersion<T> currentFormat;
    private final byte currentVersion;
    private FormatVersion<T> legacy;
    boolean frozen = false;

    public FormatSpec(byte version, FormatVersion<T> current) {
        if(version < 0) throw new IllegalArgumentException("Version must be positive");
        this.currentFormat = current;
        this.currentVersion = version;
        versions.put(version, current);
    }

    public FormatSpec<T> freeze() {
        checkFrozen();
        frozen = true;
        return this;
    }

    private void checkFrozen() {
        if(frozen) throw new IllegalStateException("Format is already frozen");
    }

    public FormatSpec<T> putOutdated(byte version, FormatVersion.Outdated<T> format) {
        checkFrozen();
        if(versions.containsKey(version)) throw new IllegalArgumentException("Version " + version + " added twice!");
        if(version < 0) throw new IllegalArgumentException("Version must be positive");
        if(version > currentVersion) {
            throw new IllegalArgumentException("Outdated version cannot be greater than current");
        }
        versions.put(version, Objects.requireNonNull(format));
        return this;
    }

    public FormatSpec<T> setLegacyLoader(FormatVersion.Outdated<T> legacy) {
        checkFrozen();
        this.legacy = Objects.requireNonNull(legacy);
        return this;
    }

    @Override
    public void write(T data, MinecraftStreams.Output stream) throws IOException {
        stream.writeByte(currentVersion);
        currentFormat.write(data, stream);
    }

    @Override
    public T read(MinecraftStreams.Input stream) throws IOException {
        byte version = stream.readByte();
        if(version > currentVersion) throw UnsupportedVersionException.ahead(version, currentVersion);
        FormatVersion<T> format = versions.get(version);
        if(format == null) {
            throw UnsupportedVersionException.missing(version);
        }
        return format.read(stream);
    }

    @Override
    public T readLegacy(MinecraftStreams.Input stream) throws IOException {
        if(!hasLegacySupport()) throw UnsupportedVersionException.legacy();
        return legacy.read(stream);
    }

    @Override
    public boolean hasLegacySupport() {
        return legacy != null;
    }
}
