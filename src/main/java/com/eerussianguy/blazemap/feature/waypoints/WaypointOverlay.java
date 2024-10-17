package com.eerussianguy.blazemap.feature.waypoints;

import java.util.Collection;
import java.util.Collections;

import net.minecraft.client.multiplayer.ClientLevel;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.maps.GhostOverlay;
import com.eerussianguy.blazemap.api.maps.TileResolution;
import com.eerussianguy.blazemap.api.markers.Marker;
import com.eerussianguy.blazemap.lib.Helpers;

public class WaypointOverlay extends GhostOverlay {
    public WaypointOverlay() {
        super(
            BlazeMapReferences.Overlays.WAYPOINTS,
            Helpers.translate("blazemap.waypoints"),
            BlazeMap.resource("textures/map_icons/overlay_waypoints.png")
        );
    }

    @Override @SuppressWarnings("unchecked")
    public Collection<? extends Marker<?>> getMarkers(ClientLevel level, TileResolution resolution) {
        WaypointService waypoints = WaypointService.instance();

        if(waypoints == null) {
            return Collections.EMPTY_LIST;
        }

        return waypoints.getAll();
    }
}
