package com.eerussianguy.blazemap.api.util;

import java.io.IOException;

import net.minecraft.resources.ResourceLocation;

public interface IStorageAccess {
    boolean exists(ResourceLocation node);
    boolean exists(ResourceLocation node, String child);

    MinecraftStreams.Input read(ResourceLocation node) throws IOException;
    MinecraftStreams.Input read(ResourceLocation node, String child) throws IOException;

    MinecraftStreams.Output write(ResourceLocation node) throws IOException;
    MinecraftStreams.Output write(ResourceLocation node, String child) throws IOException;

    void move(ResourceLocation source, ResourceLocation destination) throws IOException;
    void move(ResourceLocation node, String source, String destination) throws IOException;
}
