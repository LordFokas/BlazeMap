package com.eerussianguy.blazemap.api.event;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraftforge.eventbus.api.Event;

import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;

/**
 * Fired after registry freeze to inflate MapTypes.
 * This event is a chance for addons to contribute to or modify the layer stack in maps they do not own.
 * Can also be used as a loosely coupled way to conditionally add integration layers to your own maps.
 *
 * @author LordFokas
 */
public class MapTypeInflationEvent extends Event {
    public final BlazeRegistry.Key<MapType> id;
    private final LinkedHashSet<BlazeRegistry.Key<Layer>> layers;
    private final ArrayList<BlazeRegistry.Key<Layer>> list;

    public MapTypeInflationEvent(BlazeRegistry.Key<MapType> id, LinkedHashSet<BlazeRegistry.Key<Layer>> layers) {
        this.id = id;
        this.layers = layers;
        this.list = new ArrayList<>(layers);
    }

    /** Adds your layer to the top of the stack */
    public void add(BlazeRegistry.Key<Layer> layer) {
        if(layer == null) throw new IllegalArgumentException("Layer cannot be null");
        if(list.contains(layer)) throw new IllegalStateException("Layer already in set");

        list.add(layer);
    }

    /**
     * Adds your layer to the next position immediately after ALL the targets.
     * If none of the targets is found, does nothing and returns false.
     *
     * @param layer the layer you wish to add
     * @param targets the targets after which your layer will be added
     * @return true if added, false if not
     */
    public boolean addAfter(BlazeRegistry.Key<Layer> layer, Set<BlazeRegistry.Key<Layer>> targets) {
        if(layer == null) throw new IllegalArgumentException("Layer cannot be null");
        if(targets.size() == 0) throw new IllegalArgumentException("No targets provided");
        if(list.contains(layer)) throw new IllegalStateException("Layer already in set");

        int last = -1;
        for(var target : targets) {
            int index = list.indexOf(target);
            if(index > last){
                last = index;
            }
        }

        if(last == -1) return false;
        list.add(last+1, layer);
        return true;
    }

    /**
     * Adds your layer to the next position immediately before ALL the targets.
     * If none of the targets is found, does nothing and returns false.
     *
     * @param layer the layer you wish to add
     * @param targets the targets before which your layer will be added
     * @return true if added, false if not
     */
    public boolean addBefore(BlazeRegistry.Key<Layer> layer, Set<BlazeRegistry.Key<Layer>> targets) {
        if(layer == null) throw new IllegalArgumentException("Layer cannot be null");
        if(targets.size() == 0) throw new IllegalArgumentException("No targets provided");
        if(list.contains(layer)) throw new IllegalStateException("Layer already in set");

        int first = Integer.MAX_VALUE;
        for(var target : targets) {
            int index = list.indexOf(target);
            if(index == -1) continue;
            if(index < first){
                first = index;
            }
        }

        if(first == Integer.MAX_VALUE) return false;
        list.add(first, layer);
        return true;
    }

    /** Removes a layer from the stack */
    public boolean remove(BlazeRegistry.Key<Layer> layer) {
        if(layer == null) throw new IllegalArgumentException("Layer cannot be null");

        return list.remove(layer);
    }

    /** Replaces one layer with another */
    public boolean replace(BlazeRegistry.Key<Layer> oldLayer, BlazeRegistry.Key<Layer> newLayer) {
        if(oldLayer == null) throw new IllegalArgumentException("oldLayer cannot be null");
        if(newLayer == null) throw new IllegalArgumentException("newLayer cannot be null");
        if(list.contains(newLayer)) throw new IllegalStateException("Layer already in set");

        int index = list.indexOf(oldLayer);
        if(index == -1) return false;
        list.set(index, newLayer);
        return true;
    }

    /** Called by MapType. Calling this method additional times will do nothing useful. */
    public void update() {
        layers.clear();
        layers.addAll(list);
    }
}
