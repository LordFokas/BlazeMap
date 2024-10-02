package com.eerussianguy.blazemap.engine.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.event.MapLabelEvent;
import com.eerussianguy.blazemap.api.maps.NamedMapComponent;
import com.eerussianguy.blazemap.api.markers.MarkerStorage;
import com.eerussianguy.blazemap.api.markers.MapComponentMarker;

class LabelStorage implements MarkerStorage.MapComponentStorage, MarkerStorage<MapComponentMarker> {
    private final HashMap<Key<? extends NamedMapComponent<?>>, MarkerStorageImpl> layers = new HashMap<>();
    private final HashSet<ResourceLocation> labelIDs = new HashSet<>();
    private final ResourceKey<Level> dimension;

    public LabelStorage(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    @Override
    public MarkerStorage<MapComponentMarker> getGlobal() {
        return this;
    }

    @Override
    public MarkerStorage<MapComponentMarker> getStorage(Key<? extends NamedMapComponent<?>> componentID) {
        return layers.computeIfAbsent(componentID, key -> new MarkerStorageImpl(key, dimension, labelIDs));
    }

    @Override
    public Collection<MapComponentMarker> getAll() {
        throw new UnsupportedOperationException("Cannot get all markers from global storage");
    }

    @Override
    public void add(MapComponentMarker marker) {
        getStorage(marker.getComponentId()).add(marker);
    }

    @Override
    public void remove(ResourceLocation id) {
        throw new UnsupportedOperationException("Cannot remove by ID in global storage");
    }

    @Override
    public void remove(MapComponentMarker marker) {
        getStorage(marker.getComponentId()).remove(marker.getID());
    }

    @Override
    public boolean has(ResourceLocation id) {
        return labelIDs.contains(id);
    }


    private static class MarkerStorageImpl implements MarkerStorage<MapComponentMarker> {
        private final HashMap<ResourceLocation, MapComponentMarker> markers = new HashMap<>();
        private final HashSet<ResourceLocation> labelIDs;
        private final ResourceKey<Level> dimension;
        private final Key<? extends NamedMapComponent<?>> componentID;

        private MarkerStorageImpl(Key<? extends NamedMapComponent<?>> componentID, ResourceKey<Level> dimension, HashSet<ResourceLocation> labelIDs) {
            this.labelIDs = labelIDs;
            this.dimension = dimension;
            this.componentID = componentID;
        }

        @Override
        public Collection<MapComponentMarker> getAll() {
            return markers.values();
        }

        @Override
        public void add(MapComponentMarker marker) {
            if(!dimension.equals(marker.getDimension())) return;
            if(!marker.getComponentId().equals(componentID)) throw new IllegalArgumentException("ComponentID mismatch");
            ResourceLocation id = marker.getID();
            if(labelIDs.contains(id)) throw new IllegalStateException("Marker already exists in storage");
            labelIDs.add(id);
            markers.put(id, marker);
            MinecraftForge.EVENT_BUS.post(new MapLabelEvent.Created(marker));
        }

        @Override
        public void remove(MapComponentMarker label) {
            ResourceLocation id = label.getID();
            if(labelIDs.contains(id)) {
                markers.remove(id);
                labelIDs.remove(id);
                MinecraftForge.EVENT_BUS.post(new MapLabelEvent.Removed(label));
            }
        }

        @Override
        public void remove(ResourceLocation id) {
            if(labelIDs.contains(id)) {
                if(!markers.containsKey(id)) throw new IllegalArgumentException("Marker is not in specified component");
                MapComponentMarker label = markers.remove(id);
                labelIDs.remove(id);
                MinecraftForge.EVENT_BUS.post(new MapLabelEvent.Removed(label));
            }
        }

        @Override
        public boolean has(ResourceLocation id) {
            return markers.containsKey(id);
        }
    }
}