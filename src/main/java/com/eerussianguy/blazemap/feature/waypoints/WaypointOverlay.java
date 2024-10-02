package com.eerussianguy.blazemap.feature.waypoints;

import java.util.Collection;
import java.util.Collections;

import net.minecraft.client.multiplayer.ClientLevel;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.event.DimensionChangedEvent;
import com.eerussianguy.blazemap.api.maps.GhostOverlay;
import com.eerussianguy.blazemap.api.maps.TileResolution;
import com.eerussianguy.blazemap.api.markers.MarkerStorage;
import com.eerussianguy.blazemap.api.markers.Marker;
import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.util.Helpers;

public class WaypointOverlay extends GhostOverlay {
    private static MarkerStorage<Waypoint> waypointStorage;

    public static void onDimensionChange(DimensionChangedEvent evt) {
        waypointStorage = evt.waypoints;
    }

    public WaypointOverlay() {
        super(
            BlazeMapReferences.Overlays.WAYPOINTS,
            Helpers.translate("blazemap.waypoints"),
            BlazeMapReferences.Icons.WAYPOINT
        );
    }

    @Override @SuppressWarnings("unchecked")
    public Collection<? extends Marker<?>> getMarkers(ClientLevel level, TileResolution resolution) {
        if(waypointStorage == null) {
            return Collections.EMPTY_LIST;
        }
        return waypointStorage.getAll();
    }
}
