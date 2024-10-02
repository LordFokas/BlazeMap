package com.eerussianguy.blazemap.api.maps;

import java.util.Collections;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.markers.Marker;
import com.eerussianguy.blazemap.api.util.RegionPos;

public abstract class Overlay extends NamedMapComponent<Overlay> {
    public final Type type;

    protected Overlay(BlazeRegistry.Key<Overlay> id, TranslatableComponent name, ResourceLocation icon) {
        this(id, Type.RENDERABLE, name, icon);
    }

    Overlay(BlazeRegistry.Key<Overlay> id, Type type, TranslatableComponent name, ResourceLocation icon) {
        super(id, name, icon);
        this.type = type;
    }

    public abstract PixelSource getPixelSource(ResourceKey<Level> dimension, RegionPos region, TileResolution resolution);

    @SuppressWarnings("unchecked")
    public Iterable<? extends Marker<?>> getMarkers(ClientLevel level, TileResolution resolution) {
        return Collections.EMPTY_LIST;
    }

    public enum Type {
        RENDERABLE(true),
        INVISIBLE(false);

        public final boolean isVisible;

        Type(boolean isVisible) {
            this.isVisible = isVisible;
        }
    }
}