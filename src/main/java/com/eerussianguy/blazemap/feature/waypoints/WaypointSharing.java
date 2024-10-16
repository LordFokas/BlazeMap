package com.eerussianguy.blazemap.feature.waypoints;

import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.lib.Helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

public class WaypointSharing {

    public static void onChatReceive(ClientChatReceivedEvent event){
        ;
    }

    public static void shareWaypoint(Waypoint waypoint) {
        BlockPos pos = waypoint.getPosition();
        ResourceKey<Level> dimension = waypoint.getDimension();
        String name = waypoint.getName();
        
        String format = "[name:%1$s, x:%2$d, y:%3$d, z:%4$d, dim:%5$s]";

        String msg = String.format(format, name, pos.getX(), pos.getY(), pos.getZ(), dimension.getRegistryName());

        Helpers.getPlayer().chat(msg);
    }
}
