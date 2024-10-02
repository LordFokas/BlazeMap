package com.eerussianguy.blazemap.api.markers;

import java.util.Collection;
import java.util.Collections;

import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.maps.NamedMapComponent;

public interface MarkerStorage<T extends Marker<T>> {
    Collection<T> getAll();

    void add(T marker);

    default void remove(T marker) {
        remove(marker.getID());
    }

    void remove(ResourceLocation id);

    default boolean has(T marker) {
        return has(marker.getID());
    }

    boolean has(ResourceLocation id);

    class Dummy<T extends Marker<T>> implements MarkerStorage<T> {
        @SuppressWarnings("unchecked")
        public Collection<T> getAll() {return Collections.EMPTY_LIST;}
        public void add(T marker) {}
        public void remove(T marker) {}
        public void remove(ResourceLocation id) {}
        public boolean has(T marker) { return false; }
        public boolean has(ResourceLocation id) { return false; }
    }

    interface MapComponentStorage {
        MarkerStorage<MapComponentMarker> getGlobal();
        MarkerStorage<MapComponentMarker> getStorage(BlazeRegistry.Key<? extends NamedMapComponent<?>> componentID);
    }
}
