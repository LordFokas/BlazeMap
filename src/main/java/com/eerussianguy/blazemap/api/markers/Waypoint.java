package com.eerussianguy.blazemap.api.markers;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeMapReferences;

public final class Waypoint extends Marker<Waypoint> {
    // Track render state of waypoint in world
    // TODO: Currently, nothing sets these values. Need to add both global client config options + individual options in Waypoint Editor,
    // and also incorporate with save/restore.
    // TODO: Integrate with WaypointGroup
    private boolean showWaypointOnMap = true;
    private boolean showWaypointInWorld = true;
    private boolean showBeam = true;
    private boolean showLabel = true;

    public Waypoint(ResourceLocation id, ResourceKey<Level> dimension, BlockPos position, String name) {
        this(id, dimension, position, name, BlazeMapReferences.Icons.WAYPOINT, -1);
        this.randomizeColor();
    }

    public Waypoint(ResourceLocation id, ResourceKey<Level> dimension, BlockPos position, String name, ResourceLocation icon) {
        this(id, dimension, position, name, icon, -1);
        this.randomizeColor();
    }

    public Waypoint(ResourceLocation id, ResourceKey<Level> dimension, BlockPos position, String name, ResourceLocation icon, int color) {
        super(id, dimension, position, icon);
        setName(name);
        setColor(color);
        setNameVisible(true);
    }

    public boolean shouldRenderWaypointOnMap() {
        return this.showWaypointOnMap;
    }

    public boolean shouldRenderWaypointInWorld() {
        return this.showWaypointInWorld;
    }

    public boolean shouldShowBeam() {
        return this.showWaypointInWorld && this.showBeam;
    }

    public boolean shouldShowLabel() {
        return this.showWaypointInWorld && this.showLabel;
    }


    public Waypoint setShowWaypointOnMap(boolean showWaypointOnMap) {
        this.showWaypointOnMap = showWaypointOnMap;
        return this;
    }

    // TODO: I feel there should be some logic connecting setShowWaypointInWorld and setShowBeam + setShowLabel,
    // considering the case when the former is true when the latter two are both set to false.
    // But I don't know what it should be yet, so leaving them separate for now.
    public Waypoint setShowWaypointInWorld(boolean showWaypointInWorld) {
        this.showWaypointInWorld = showWaypointInWorld;
        return this;
    }

    public Waypoint setShowBeam(boolean showBeam) {
        this.showBeam = showBeam;
        return this;
    }

    public Waypoint setShowLabel(boolean showLabel) {
        this.showLabel = showLabel;
        return this;
    }
}
