package com.eerussianguy.blazemap.api.markers;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.NamedMapComponent;

public abstract class MapComponentMarker extends Marker<MapComponentMarker> {
    private final Key<NamedMapComponent<?>> componentId;

    public MapComponentMarker(ResourceLocation id, ResourceKey<Level> dimension, BlockPos position, ResourceLocation icon, Key<NamedMapComponent<?>> componentId) {
        super(id, dimension, position, icon);
        this.componentId = componentId;
    }

    public MapComponentMarker(ResourceLocation id, ResourceKey<Level> dimension, BlockPos position, ResourceLocation icon, Key<NamedMapComponent<?>> componentId, Set<String> tags) {
        super(id, dimension, position, icon, tags);
        this.componentId = componentId;
    }

    public final Key<NamedMapComponent<?>> getComponentId() {
        return componentId;
    }
}