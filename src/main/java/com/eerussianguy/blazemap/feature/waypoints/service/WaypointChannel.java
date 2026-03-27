package com.eerussianguy.blazemap.feature.waypoints.service;

import java.util.Collections;
import java.util.List;

import com.eerussianguy.blazemap.api.BlazeRegistry;

public abstract class WaypointChannel implements BlazeRegistry.RegistryEntry {
    public static final BlazeRegistry<WaypointChannel> REGISTRY = new BlazeRegistry<>();

    private final BlazeRegistry.Key<WaypointChannel> id;

    protected WaypointChannel(BlazeRegistry.Key<WaypointChannel> id) {
        this.id = id;
    }

    @Override
    public BlazeRegistry.Key<WaypointChannel> getID() {
        return id;
    }

    public abstract List<WaypointPool> createClientPools();

    @SuppressWarnings("unchecked")
    public List<WaypointPool> createServerPools() {
        return Collections.EMPTY_LIST;
    }
}