package com.eerussianguy.blazemap.engine.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.api.util.StorageAccess;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;

public class PublicStorage implements StorageAccess.LevelStorage, StorageAccess.ServerStorage, StorageAccess.GlobalStorage {
    protected static final String PATTERN = "[%s] %s";
    protected final StorageType type;
    protected final File dir;

    public PublicStorage(StorageType type, File dir) {
        this.type = type;
        this.dir = dir;
    }

    // GENERIC STORAGE =================================================================================================
    @Override
    public boolean exists(ResourceLocation node) {
        return getFile(node).exists();
    }

    @Override
    public boolean exists(ResourceLocation node, String child) {
        return getFile(node, child).exists();
    }

    @Override
    public MinecraftStreams.Input read(ResourceLocation node) throws IOException {
        return new MinecraftStreams.Input(new FileInputStream(getFile(node)));
    }

    @Override
    public MinecraftStreams.Input read(ResourceLocation node, String child) throws IOException {
        return new MinecraftStreams.Input(new FileInputStream(getFile(node, child)));
    }

    @Override
    public MinecraftStreams.Output write(ResourceLocation node) throws IOException {
        return new MinecraftStreams.Output(new FileOutputStream(getFile(node)));
    }

    @Override
    public MinecraftStreams.Output write(ResourceLocation node, String child) throws IOException {
        return new MinecraftStreams.Output(new FileOutputStream(getFile(node, child)));
    }

    @Override
    public void move(ResourceLocation source, ResourceLocation destination) throws IOException {
        move(getFile(source), getFile(destination));
    }

    @Override
    public void move(ResourceLocation node, String source, String destination) throws IOException {
        move(getFile(node, source), getFile(node, destination));
    }

    // LEVEL STORAGE ===================================================================================================

    // SERVER STORAGE ==================================================================================================
    @Override
    public LevelStorage getLevelStorage(ResourceLocation dimension) {
        checkServer();
        return new PublicStorage(StorageType.LEVEL, getDir(dimension));
    }

    @Override
    public void forEachLevel(BiConsumer<ResourceLocation, LevelStorage> consumer) {
        checkServer();

        var files = dir.listFiles((dir1, name) -> name.startsWith("["));
        if(files == null) return;

        for(var dir : files) {
            var key = new ResourceLocation(dir.getName().replace("[", "").replace("] ", ":"));
            consumer.accept(key, new PublicStorage(StorageType.LEVEL, dir));
        }
    }

    // GLOBAL STORAGE ==================================================================================================
    @Override
    public void foreachSave(Consumer<ServerStorage> save) {
        checkGlobal();

    }

    @Override
    public void foreachServer(Consumer<ServerStorage> server) {
        checkGlobal();

    }

    // INTERNALS =======================================================================================================
    protected void move(File source, File destination) throws IOException {
        Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    protected File getFile(ResourceLocation node) {
        Objects.requireNonNull(node);
        File mod = new File(dir, node.getNamespace());
        File file = new File(mod, node.getPath());
        file.getParentFile().mkdirs();
        return file;
    }

    protected File getFile(ResourceLocation node, String child) {
        Objects.requireNonNull(node);
        File dir = getFile(node);
        dir.mkdirs();
        return new File(dir, child);
    }

    protected File getDir(ResourceLocation node) {
        Objects.requireNonNull(node);
        File file = new File(dir, String.format(PATTERN, node.getNamespace(), node.getPath()));
        file.getParentFile().mkdirs();
        return file;
    }

    protected void checkLevel() {
        if(type != StorageType.LEVEL) throw new UnsupportedOperationException("Can only be done on LevelStorage");
    }

    protected void checkServer() {
        if(type != StorageType.SERVER) throw new UnsupportedOperationException("Can only be done on ServerStorage");
    }

    protected void checkGlobal() {
        if(type != StorageType.GLOBAL) throw new UnsupportedOperationException("Can only be done on GlobalStorage");
    }
}