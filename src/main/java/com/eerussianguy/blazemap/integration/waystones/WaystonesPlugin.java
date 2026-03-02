package com.eerussianguy.blazemap.integration.waystones;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointChannelLocal;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointGroup;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointServiceClient;
import com.eerussianguy.blazemap.integration.ModIDs;
import com.eerussianguy.blazemap.integration.ModIntegration;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.api.KnownWaystonesEvent;

public class WaystonesPlugin extends ModIntegration {
    public static final ResourceLocation WAYSTONE = BlazeMap.resource("textures/waypoints/special/waystone.png");
    private static final int WHITE = 0xFFFFFFFF;

    public WaystonesPlugin() {
        super(ModIDs.WAYSTONES, ModIDs.BALM);
    }

    @Override
    public void setup() {
        MinecraftForge.EVENT_BUS.addListener(this::onKnownWaystonesEvent);
    }

    /**
     * Event handler for adding waypoints for waystones. The event gets fired on the client
     * every time a waystone is added/updated and contains a list of all the current waystones.
     */
    private void onKnownWaystonesEvent(KnownWaystonesEvent event) {
        var waypointClient = WaypointServiceClient.instance();
        var waystones = event.getWaystones();

        removeDeletedWaystoneWaypoints(waystones);

        for(var waystone : waystones) {
            var dimension = waystone.getDimension();
            var waypoint = new Waypoint(
                BlazeMap.resource("waypoint/waystone/" + waystone.getWaystoneUid()),
                dimension,
                waystone.getPos(),
                waystone.getName(),
                WAYSTONE,
                WHITE
            );
            var waypointGroups = waypointClient.getPool(WaypointChannelLocal.PRIVATE_POOL).getGroups(dimension);
            waypointGroups.stream()
                .filter(g -> g.type == WaypointChannelLocal.GROUP_WAYSTONE)
                .findFirst()
                .ifPresentOrElse(g -> updateWaypoint(g, waypoint), () -> {
                    // Add the waystone group if it didn't exist before
                    // This is so it doesn't need to be a default group (if waystones isn't installed) and
                    // this is the easiest place to add it
                    var group = WaypointGroup.make(WaypointChannelLocal.GROUP_WAYSTONE);
                    waypointGroups.add(group);
                    updateWaypoint(group, waypoint);
                });
        }
    }

    /**
     * Helper to add or updates waypoints to a waypoint group.
     *
     * @param group       The waypoint group to add to.
     * @param newWaypoint The waypoint to add/update.
     */
    private void updateWaypoint(WaypointGroup group, Waypoint newWaypoint) {
        if(!group.has(newWaypoint)) {
            group.add(newWaypoint);
            return;
        }

        // If it already exists update the existing waypoint instead
        group.getAll().stream()
            .filter(waypoint -> waypoint.getID().equals(newWaypoint.getID()))
            .findFirst()
            .ifPresent(waypoint -> {
                waypoint.setName(newWaypoint.getName());
                // I don't know if these change can change but updating them in case it's possible to move waystones
                waypoint.setDimension(newWaypoint.getDimension());
                waypoint.setPosition(newWaypoint.getPosition());
            });
    }

    /**
     * Removes waystone waypoints that are not in the list of known waystones.
     *
     * @param waystones The list of all known waystones.
     */
    private void removeDeletedWaystoneWaypoints(List<IWaystone> waystones) {
        var waypointClient = WaypointServiceClient.instance();
        var waystoneIds = waystones.stream().map(waystone -> waystone.getWaystoneUid().toString()).toList();

        Map<Waypoint, WaypointGroup> waypointsToRemove = new HashMap<>();

        waypointClient.getPool(WaypointChannelLocal.PRIVATE_POOL).iterateAll((waypoint, group) -> {
            if(group.type != WaypointChannelLocal.GROUP_WAYSTONE) return;

            var idParts = waypoint.getID().getPath().split("/");
            var id = idParts[idParts.length - 1]; // UUID of Waystone associated with waypoint
            if(!waystoneIds.contains(id)) {
                waypointsToRemove.put(waypoint, group);
            }
        });

        waypointsToRemove.forEach((waypoint, group) -> group.remove(waypoint));
    }
}
