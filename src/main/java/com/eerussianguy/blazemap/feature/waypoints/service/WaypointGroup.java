package com.eerussianguy.blazemap.feature.waypoints.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Supplier;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.api.markers.MarkerStorage;
import com.eerussianguy.blazemap.api.markers.Waypoint;

public class WaypointGroup implements MarkerStorage<Waypoint> {
    private static final HashMap<ResourceLocation, Supplier<WaypointGroup>> GROUPS = new HashMap<>();

    public static WaypointGroup make(ResourceLocation type) {
        assertDefined(type);
        return GROUPS.get(type).get();
    }

    public static void assertDefined(ResourceLocation type) {
        if(!GROUPS.containsKey(type)) {
            throw new IllegalStateException("WaypointGroup type "+type+" has not been defined");
        }
    }

    public static void define(ResourceLocation type, Supplier<WaypointGroup> factory) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(factory);
        if(GROUPS.containsKey(type)) throw new IllegalStateException("Group "+type+" already defined!");
        GROUPS.put(type, factory);
    }

    // =================================================================================================================
    public final ResourceLocation type;
    public final ManagementType management;
    protected final HashMap<ResourceLocation, Waypoint> waypoints = new HashMap<>();
    protected final HashMap<ResourceLocation, LocalState> states = new HashMap<>();
    private final LocalState state = new LocalState();
    private NameType nameType = NameType.USER_GIVEN;
    private Component name;
    private String _name;

    public WaypointGroup(ResourceLocation type) {
        this(type, ManagementType.FULL);
    }

    public WaypointGroup(ResourceLocation type, ManagementType manage) {
        assertDefined(type);
        this.type = type;
        this.management = manage;
    }

    public LocalState getState() {
        return state;
    }

    public LocalState getState(ResourceLocation marker) {
        return states.get(marker);
    }

    public boolean isUserNamed() {
        return nameType == NameType.USER_GIVEN;
    }

    public Component getName() {
        return name;
    }

    public String getNameString() {
        return switch(nameType) {
            case USER_GIVEN -> _name;
            case SYSTEM -> name.getString();
        };
    }

    public WaypointGroup setSystemName(Component name) {
        this.name = name;
        this._name = null;
        this.nameType = NameType.SYSTEM;
        return this;
    }

    public WaypointGroup setUserGivenName(String name) {
        if(nameType == NameType.SYSTEM) {
            throw new IllegalStateException("cannot name group with system name");
        }
        this._name = name;
        this.name = new TextComponent(name);
        return this;
    }

    @Override
    public Collection<Waypoint> getAll() {
        return waypoints.values();
    }

    public void add(Waypoint marker, LocalState state) {
        var key = marker.getID();
        if(waypoints.containsKey(key)) {
            throw new IllegalArgumentException("Waypoint Group already contains this waypoint");
        }
        waypoints.put(key, marker);
        state.setParent(this.state);
        states.put(key, state);
    }

    @Override
    public void add(Waypoint marker) {
        add(marker, new LocalState());
    }

    @Override
    public void remove(ResourceLocation id) {
        waypoints.remove(id);
        states.remove(id);
    }

    @Override
    public boolean has(ResourceLocation id) {
        return waypoints.containsKey(id);
    }

    private enum NameType {
        USER_GIVEN, SYSTEM
    }
}
