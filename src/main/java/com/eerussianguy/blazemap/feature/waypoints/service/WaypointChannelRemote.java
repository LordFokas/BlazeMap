package com.eerussianguy.blazemap.feature.waypoints.service;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.util.StorageAccess;

public class WaypointChannelRemote extends WaypointChannel {
    public static final ResourceLocation SERVER_POOL = BlazeMap.resource("waypoints/pool/server");
    public static final ResourceLocation GROUP = BlazeMap.resource("waypoint/group/server");
    public static final Component PUBLIC_WAYPOINTS = new TextComponent("Public Waypoints").withStyle(ChatFormatting.YELLOW);

    public WaypointChannelRemote() {
        super(REGISTRY.findOrCreate(BlazeMap.resource("remote")));
        WaypointGroup.define(GROUP, () -> new WaypointGroup(GROUP, ManagementType.READONLY).setSystemName(PUBLIC_WAYPOINTS));
    }

    @Override
    public List<WaypointPool> createClientPools() {
        return List.of(new ServerPool());
    }

    private static class ServerPool extends WaypointPool {
        private static final ResourceLocation SERVER = BlazeMap.resource("server.waypoints");

        private ServerPool() {
            super(SERVER_POOL, false, BlazeMapReferences.Icons.INGOT, 0xFFFFFF00, new TextComponent("Server"));
        }

        @Override
        public List<ResourceLocation> getDefaultGroups() {
            return List.of(GROUP);
        }

        @Override
        public void save(StorageAccess.ServerStorage storage) {
            super.save(storage, SERVER);
        }

        @Override
        public void load(StorageAccess.ServerStorage storage) {
            super.load(storage, SERVER);
        }
    }
}
