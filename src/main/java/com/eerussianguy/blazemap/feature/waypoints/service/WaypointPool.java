package com.eerussianguy.blazemap.feature.waypoints.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;
import com.eerussianguy.blazemap.api.util.StorageAccess;

public abstract class WaypointPool extends ManagedContainer {
    protected final HashMap<ResourceKey<Level>, List<WaypointGroup>> groups = new HashMap<>();
    public final ResourceLocation id, icon;
    public final int tint;
    private final Component name;

    public WaypointPool(ResourceLocation id, ManagementType manage, ResourceLocation icon, int tint, Component name) {
        super(manage);
        this.id = id;
        this.icon = icon;
        this.tint = tint;
        this.name = name;
    }

    public Component getName() {
        return name;
    }

    public void iterate(ResourceKey<Level> dimension, Consumer<Waypoint> consumer) {
        if(!groups.containsKey(dimension)) return;

        for(WaypointGroup group : groups.get(dimension)) {
            group.getAll().forEach(consumer);
        }
    }

    public void iterate(ResourceKey<Level> dimension, BiConsumer<Waypoint, WaypointGroup> consumer) {
        if(!groups.containsKey(dimension)) return;

        for(WaypointGroup group : groups.get(dimension)) {
            group.getAll().forEach(waypoint -> {
                consumer.accept(waypoint, group);
            });
        }
    }

    public List<WaypointGroup> getGroups(ResourceKey<Level> dimension) {
        return groups.computeIfAbsent(dimension, $ -> makeDefaultGroups());
    }

    private List<WaypointGroup> makeDefaultGroups() {
        return new ArrayList<>(
            getDefaultGroups().stream().map(WaypointGroup::make).toList()
        );
    }

    @SuppressWarnings("unchecked")
    public List<ResourceLocation> getDefaultGroups() {
        return Collections.EMPTY_LIST;
    }

    public abstract void save(StorageAccess.ServerStorage storage);
    public abstract void load(StorageAccess.ServerStorage storage);

    protected void save(StorageAccess.ServerStorage storage, ResourceLocation file) {
        try(MinecraftStreams.Output output = storage.write(file)) {
            WaypointSerialization.FORMAT.write(groups, output);
        }
        catch(IOException e) {
            // TODO: Should add some form of retry mechanism to minimise loss of folks' data
            BlazeMap.LOGGER.error("Error while saving waypoints. Not all waypoints could be saved. Aborting");
            e.printStackTrace();
        }
    }

    protected void load(StorageAccess.ServerStorage storage, ResourceLocation file) {
        this.load(storage, file, null);
    }

    protected void load(StorageAccess.ServerStorage storage, ResourceLocation file, @Nullable ResourceLocation legacy) {
        groups.clear();

        // Load standard format
        if(storage.exists(file)) {
            try(MinecraftStreams.Input input = storage.read(file)) {
                groups.putAll(WaypointSerialization.FORMAT.read(input));
            }
            catch(IOException e) {
                BlazeMap.LOGGER.error("Error loading waypoints. Waypoints could not be loaded.", e);
            }
            return; // standard file exists, don't fall back to legacy loading
        }

        // Attempt to load legacy format, if provided
        if(legacy == null) return;
        storage.forEachLevel((key, _storage) -> {
            if(!_storage.exists(legacy)) return;
            try(MinecraftStreams.Input input = _storage.read(legacy)) {
                groups.putAll(WaypointSerialization.FORMAT.readLegacy(input));
            }
            catch(IOException e) {
                BlazeMap.LOGGER.error("Error loading legacy waypoints for level \""+key+"\". Not all waypoints could be loaded", e);
            }
        });

        save(storage, file); // Legacy waypoints were loaded. Saving immediately forces standard file to exist from the start.
    }
}
