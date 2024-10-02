package com.eerussianguy.blazemap.api.event;

import net.minecraftforge.eventbus.api.Event;

import com.eerussianguy.blazemap.api.markers.MapComponentMarker;

public class MapLabelEvent extends Event {
    public final MapComponentMarker label;

    protected MapLabelEvent(MapComponentMarker label) {
        this.label = label;
    }

    public static class Created extends MapLabelEvent {
        public Created(MapComponentMarker label) {
            super(label);
        }
    }

    public static class Removed extends MapLabelEvent {
        public Removed(MapComponentMarker label) {
            super(label);
        }
    }
}
