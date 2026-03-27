package com.eerussianguy.blazemap.feature.waypoints.service;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.util.StorageAccess;

public class WaypointChannelLocal extends WaypointChannel {
    public static final ResourceLocation PRIVATE_POOL = BlazeMap.resource("waypoints/pool/private");
    public static final ResourceLocation GROUP_DEFAULT = BlazeMap.resource("waypoint/group/default");
    private static final ResourceLocation GROUP_DEFAULT_FIRST = BlazeMap.resource("waypoint/group/default_first");
    public static final ResourceLocation GROUP_DEATH = BlazeMap.resource("waypoint/group/death");
    public static final Component DEATHS = new TextComponent("Deaths").withStyle(ChatFormatting.YELLOW);

    public WaypointChannelLocal() {
        super(REGISTRY.findOrCreate(BlazeMap.resource("local")));
        WaypointGroup.define(GROUP_DEFAULT, () -> new WaypointGroup(GROUP_DEFAULT).setUserGivenName("New Group"));
        WaypointGroup.define(GROUP_DEFAULT_FIRST, () -> new WaypointGroup(GROUP_DEFAULT).setUserGivenName("My Waypoints"));
        WaypointGroup.define(GROUP_DEATH, () -> new WaypointGroup(GROUP_DEATH, ManagementType.INBOX).setSystemName(DEATHS));
    }

    @Override
    public List<WaypointPool> createClientPools() {
        return List.of(new PrivatePool());
    }

    private static class PrivatePool extends WaypointPool {
        private static final ResourceLocation LEGACY = BlazeMap.resource("waypoints.bin");
        private static final ResourceLocation PRIVATE = BlazeMap.resource("private.waypoints");

        private PrivatePool() {
            super(PRIVATE_POOL, true, BlazeMapReferences.Icons.WAYPOINT, 0xFF0088FF, new TextComponent("Private"));
        }

        @Override
        public List<ResourceLocation> getDefaultGroups() {
            return List.of(GROUP_DEATH, GROUP_DEFAULT_FIRST);
        }

        @Override
        public void save(StorageAccess.ServerStorage storage) {
            super.save(storage, PRIVATE);
        }

        @Override
        public void load(StorageAccess.ServerStorage storage) {
            super.load(storage, PRIVATE, LEGACY);
        }
    }
}
