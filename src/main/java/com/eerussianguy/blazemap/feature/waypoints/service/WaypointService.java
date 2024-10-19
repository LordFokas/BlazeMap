package com.eerussianguy.blazemap.feature.waypoints.service;

import java.util.*;
import java.util.function.Consumer;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import net.minecraftforge.fml.LogicalSide;

import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.api.util.StorageAccess;

public abstract class WaypointService {
    public static void init() {
        WaypointChannel.REGISTRY.register(new WaypointChannelLocal());
        WaypointChannel.REGISTRY.register(new WaypointChannelRemote());
    }

    // =================================================================================================================
    protected final StorageAccess.ServerStorage storage;
    private final HashMap<ResourceLocation, WaypointPool> pools = new HashMap<>();
    private final List<WaypointPool> view;

    protected WaypointService(LogicalSide side, StorageAccess.ServerStorage storage) {
        this.storage = storage;
        List<WaypointPool> created = new ArrayList<>();
        for(var key : WaypointChannel.REGISTRY.keys()) {
            var channel = key.value();
            created.addAll(switch(side) {
                case CLIENT -> channel.createClientPools();
                case SERVER -> channel.createServerPools();
            });
        }
        for(var pool : created) {
            pools.put(pool.id, pool);
        }
        view = Collections.unmodifiableList(created);
    }

    public void iterate(ResourceKey<Level> level, Consumer<Waypoint> consumer) {
        for(WaypointPool pool : view) {
            pool.iterate(level, consumer);
        }
    }

    public List<WaypointPool> getPools() {
        return view;
    }

    public WaypointPool getPool(ResourceLocation id) {
        return pools.get(id);
    }

    protected void shutdown() {
        save();
    }

    protected void save() {
        for(var pool : view) {
            pool.save(storage);
        }
    }

    protected void load() {
        for(var pool : view) {
            pool.load(storage);
        }
    }
}
