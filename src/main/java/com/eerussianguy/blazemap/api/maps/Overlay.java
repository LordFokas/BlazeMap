package com.eerussianguy.blazemap.api.maps;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.util.RegionPos;

public abstract class Overlay extends NamedMapComponent<Overlay> {
    protected Overlay(BlazeRegistry.Key<Overlay> id, TranslatableComponent name, ResourceLocation icon) {
        super(id, name, icon);
    }

    public abstract PixelSource getPixelSource(ResourceKey<Level> dimension, RegionPos region, TileResolution resolution);
}