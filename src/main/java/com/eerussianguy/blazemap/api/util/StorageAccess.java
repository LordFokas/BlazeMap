package com.eerussianguy.blazemap.api.util;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraft.resources.ResourceLocation;

/**
 * Provides access to centralized local storage.
 * Addons can use this to store their data along with the Blaze Map data.
 *
 * @author LordFokas
 */
public interface StorageAccess {
    boolean exists(ResourceLocation node);
    boolean exists(ResourceLocation node, String child);

    MinecraftStreams.Input read(ResourceLocation node) throws IOException;
    MinecraftStreams.Input read(ResourceLocation node, String child) throws IOException;

    MinecraftStreams.Output write(ResourceLocation node) throws IOException;
    MinecraftStreams.Output write(ResourceLocation node, String child) throws IOException;

    void move(ResourceLocation source, ResourceLocation destination) throws IOException;
    void move(ResourceLocation node, String source, String destination) throws IOException;

    /** Per-level storage. */
    interface LevelStorage extends StorageAccess { }

    /**
     * Per-server storage.
     * On the dedicated server this is, for all purposes, global.
     * On the physical client these can be in 3 types / locations:
     * - server-sided storage attached to the save, for the integrated server
     * - client-sided storage attached to the save, for playing in local worlds
     * - client-sided storage stored in a different path, for playing in remote servers
     */
    interface ServerStorage extends StorageAccess {
        LevelStorage getLevelStorage(ResourceLocation dimension);
        void forEachLevel(BiConsumer<ResourceLocation, LevelStorage> consumer);
    }

    /**
     * For storing global stuff.
     * Only exists in the physical client because for the dedicated server the ServerStorage is already global.
     */
    interface GlobalStorage extends StorageAccess {
        void foreachSave(Consumer<ServerStorage> save);
        void foreachServer(Consumer<ServerStorage> server);
    }
}