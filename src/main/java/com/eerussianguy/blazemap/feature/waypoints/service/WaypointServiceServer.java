package com.eerussianguy.blazemap.feature.waypoints.service;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import com.eerussianguy.blazemap.api.event.ServerEngineEvent;
import com.eerussianguy.blazemap.api.util.StorageAccess;

public class WaypointServiceServer extends WaypointService {
    private static WaypointServiceServer server = null;

    public static WaypointServiceServer instance() {
        return server;
    }

    @SubscribeEvent
    public static void onServerEngineStarting(ServerEngineEvent.EngineStartingEvent event) {
        server = new WaypointServiceServer(event.serverStorage);
    }

    @SubscribeEvent
    public static void onServerEngineStopping(ServerEngineEvent.EngineStoppingEvent event) {
        server.shutdown();
        server = null;
    }

    // =================================================================================================================

    protected WaypointServiceServer(StorageAccess.ServerStorage storage) {
        super(LogicalSide.SERVER, storage);
    }
}
