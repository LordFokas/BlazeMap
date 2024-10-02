package com.eerussianguy.blazemap.api.event;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraftforge.eventbus.api.Event;

import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.api.maps.NamedMapComponent;
import com.eerussianguy.blazemap.api.maps.Overlay;

/**
 * A generic base event to handle manipulation of NamedMapComponent sets.
 * Once constructed with a set of components, it gives subscribers the chance to alter the list contents
 * as well as the order of its elements.
 * Refer to this event's subclasses for intended usage.
 *
 * @param <T> the type of NamedMapComponent
 *
 * @author LordFokas
 */
public abstract class ComponentOrderingEvent<T extends NamedMapComponent<T>> extends Event {
    private LinkedHashSet<BlazeRegistry.Key<T>> orderedSet;
    private ArrayList<BlazeRegistry.Key<T>> list;
    private final String type;

    protected ComponentOrderingEvent(LinkedHashSet<BlazeRegistry.Key<T>> orderedSet, String type) {
        this.orderedSet = orderedSet;
        this.list = new ArrayList<>(orderedSet);
        this.type = type;
    }

    /**
     * Fired after registry freeze to expand MapTypes.
     * This event is a chance for addons to contribute to or modify the layer stack in maps they do not own.
     * Can also be used as a loosely coupled way to conditionally add integration layers to your own maps.
     */
    public static class LayerOrderingEvent extends ComponentOrderingEvent<Layer> {
        public final BlazeRegistry.Key<MapType> id;


        public LayerOrderingEvent(BlazeRegistry.Key<MapType> id, LinkedHashSet<BlazeRegistry.Key<Layer>> layers) {
            super(layers, "Layer");
            this.id = id;
        }
    }

    /**
     * Fired after registry freeze to add Overlays to the maps.
     * Even though they have been registered, Overlays will not be available in the maps without being added here.
     * This even is also a chance for addons to modify the overlay set or the rendering order.
     */
    public static class OverlayOrderingEvent extends ComponentOrderingEvent<Overlay> {
        public OverlayOrderingEvent(LinkedHashSet<BlazeRegistry.Key<Overlay>> orderedSet) {
            super(orderedSet, "Overlay");
        }
    }

    /** Adds your component to the top of the stack */
    public void add(BlazeRegistry.Key<T> component) {
        if(component == null) throw new IllegalArgumentException(type + " cannot be null");
        if(list.contains(component)) throw new IllegalStateException(type + " already in set");

        list.add(component);
    }

    /** Adds multiple components, in order, to the top of the stack */
    @SafeVarargs
    public final void add(BlazeRegistry.Key<T>... components) {
        if(components == null || components.length == 0) throw new IllegalArgumentException("components must not be null or zero-length");

        for(var component : components) {
            add(component);
        }
    }

    /**
     * Adds your component to the next position immediately after ALL the targets.
     * If none of the targets is found, does nothing and returns false.
     *
     * @param component the component you wish to add
     * @param targets the targets after which your component will be added
     * @return true if added, false if not
     */
    public boolean addAfter(BlazeRegistry.Key<T> component, Set<BlazeRegistry.Key<T>> targets) {
        if(component == null) throw new IllegalArgumentException(type + " cannot be null");
        if(targets.size() == 0) throw new IllegalArgumentException("No targets provided");
        if(list.contains(component)) throw new IllegalStateException(type + " already in set");

        int last = -1;
        for(var target : targets) {
            int index = list.indexOf(target);
            if(index > last){
                last = index;
            }
        }

        if(last == -1) return false;
        list.add(last+1, component);
        return true;
    }

    /**
     * Adds your component to the next position immediately before ALL the targets.
     * If none of the targets is found, does nothing and returns false.
     *
     * @param component the component you wish to add
     * @param targets the targets before which your component will be added
     * @return true if added, false if not
     */
    public boolean addBefore(BlazeRegistry.Key<T> component, Set<BlazeRegistry.Key<T>> targets) {
        if(component == null) throw new IllegalArgumentException(type + " cannot be null");
        if(targets.size() == 0) throw new IllegalArgumentException("No targets provided");
        if(list.contains(component)) throw new IllegalStateException(type + " already in set");

        int first = Integer.MAX_VALUE;
        for(var target : targets) {
            int index = list.indexOf(target);
            if(index == -1) continue;
            if(index < first){
                first = index;
            }
        }

        if(first == Integer.MAX_VALUE) return false;
        list.add(first, component);
        return true;
    }

    /** Removes a component from the stack */
    public boolean remove(BlazeRegistry.Key<T> component) {
        if(component == null) throw new IllegalArgumentException(type + " cannot be null");

        return list.remove(component);
    }

    /** Replaces one component with another */
    public boolean replace(BlazeRegistry.Key<T> oldComponent, BlazeRegistry.Key<T> newComponent) {
        if(oldComponent == null) throw new IllegalArgumentException("Old " + type + " cannot be null");
        if(newComponent == null) throw new IllegalArgumentException("New " + type + " cannot be null");
        if(list.contains(newComponent)) throw new IllegalStateException(type + " already in set");

        int index = list.indexOf(oldComponent);
        if(index == -1) return false;
        list.set(index, newComponent);
        return true;
    }

    /** Called internally by the publisher. Do not call this method yourself. */
    public void finish() {
        orderedSet.clear();
        orderedSet.addAll(list);
        orderedSet = null;
        list = null;
    }
}