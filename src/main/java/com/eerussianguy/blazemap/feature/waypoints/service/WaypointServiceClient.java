package com.eerussianguy.blazemap.feature.waypoints.service;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.event.ClientEngineEvent;
import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.api.util.StorageAccess;
import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.lib.Helpers;

public class WaypointServiceClient extends WaypointService {
    private static final ResourceLocation DEATH = BlazeMap.resource("textures/waypoints/special/death.png");
    private static WaypointServiceClient client = null;

    public static WaypointServiceClient instance() {
        return client;
    }

    @SubscribeEvent
    public static void onClientEngineStarting(ClientEngineEvent.EngineStartingEvent event) {
        client = new WaypointServiceClient(event.serverStorage);
    }

    @SubscribeEvent
    public static void onClientEngineStopping(ClientEngineEvent.EngineStoppingEvent event) {
        client.shutdown();
        client = null;
    }

    @SubscribeEvent
    public static void onDeath(EntityLeaveWorldEvent event) {
        if(!BlazeMapConfig.CLIENT.clientFeatures.deathWaypoints.get()) return;
        var entity = event.getEntity();
        if(!entity.level.isClientSide) return;
        if(entity instanceof LocalPlayer player) {
            if(!player.isDeadOrDying()) return;

            var dimension = player.level.dimension();
            Waypoint waypoint = new Waypoint(BlazeMap.resource("waypoint/death/"+System.nanoTime()), dimension, player.blockPosition(), Helpers.getISO8601('-', ' ', ':'), DEATH);
            client.getPool(WaypointChannelLocal.PRIVATE_POOL).getGroups(dimension)
                .stream().filter(g -> g.type == WaypointChannelLocal.GROUP_DEATH).findFirst()
                .ifPresent(g -> g.add(waypoint));
        }
    }

    // =================================================================================================================
    public WaypointServiceClient(StorageAccess.ServerStorage storage) {
        super(LogicalSide.CLIENT, storage);
        load();
    }

    public void iterate(Consumer<Waypoint> consumer) {
        iterate(Helpers.levelOrThrow().dimension(), consumer);
    }

    public void iterate(BiConsumer<Waypoint, WaypointGroup> consumer) {
        iterate(Helpers.levelOrThrow().dimension(), consumer);
    }
}
