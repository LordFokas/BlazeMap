package com.eerussianguy.blazemap.feature.waypoints;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.event.ServerJoinedEvent;
import com.eerussianguy.blazemap.api.event.WaypointEvent;
import com.eerussianguy.blazemap.api.markers.MarkerStorage;
import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.api.util.StorageAccess;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;

public class WaypointService implements MarkerStorage<Waypoint> {
    private static final ResourceLocation DEATH = BlazeMap.resource("textures/waypoints/special/death.png");
    private static WaypointService instance = null;

    public static WaypointService instance() {
        return instance;
    }

    public static void onServerJoined(ServerJoinedEvent event) {
        instance = new WaypointService(event.serverStorage);
    }

    public static void onDeath(EntityLeaveWorldEvent event) {
        var entity = event.getEntity();
        if(!entity.level.isClientSide) return;
        if(entity instanceof LocalPlayer player) {
            if(!player.isDeadOrDying()) return;

            Waypoint waypoint = new Waypoint(BlazeMap.resource("waypoint/death/"+System.nanoTime()), player.level.dimension(), player.blockPosition(), "Death", DEATH);
            instance().add(waypoint);
        }
    }


    // =================================================================================================================
    private static final ResourceLocation WAYPOINTS = BlazeMap.resource("waypoints");
    private static final ResourceLocation LEGACY_BIN = BlazeMap.resource("waypoints.bin");
    private final StorageAccess.ServerStorage storage;
    private final Map<ResourceLocation, Waypoint> store = new HashMap<>();
    private final Collection<Waypoint> view = Collections.unmodifiableCollection(store.values());

    public WaypointService(StorageAccess.ServerStorage storage) {
        this.storage = storage;
        load();
    }

    protected void save() {
        try(MinecraftStreams.Output output = storage.write(WAYPOINTS)) {
            WaypointSerialization.FORMAT.write(store, output);
        }
        catch(IOException e) {
            // TODO: Should add some form of retry mechanism to minimise loss of folks' data
            BlazeMap.LOGGER.error("Error while saving waypoints. Not all waypoints could be saved. Aborting");
            e.printStackTrace();
        }
    }

    protected void load() {
        store.clear();

        // Load standard format
        if(storage.exists(WAYPOINTS)) {
            try(MinecraftStreams.Input input = storage.read(WAYPOINTS)) {
                store.putAll(WaypointSerialization.FORMAT.read(input));
            }
            catch(IOException e) {
                BlazeMap.LOGGER.error("Error loading waypoints. Waypoints could not be loaded.", e);
            }
            return; // standard file exists, don't fall back to legacy loading
        }

        // Attempt to load legacy format
        storage.forEachLevel((key, storage) -> {
            if(!storage.exists(LEGACY_BIN)) return;
            try(MinecraftStreams.Input input = storage.read(LEGACY_BIN)) {
                store.putAll(WaypointSerialization.FORMAT.readLegacy(input));
            }
            catch(IOException e) {
                BlazeMap.LOGGER.error("Error loading legacy waypoints for level \""+key+"\". Not all waypoints could be loaded", e);
            }
        });

        save(); // Legacy waypoints were loaded. Saving immediately forces standard file to exist from the start.
    }


    // =================================================================================================================
    @Override
    public Collection<Waypoint> getAll() {
        return view;
    }

    @Override
    public void add(Waypoint waypoint) {
        if(store.containsKey(waypoint.getID()))
            throw new IllegalStateException("The waypoint is already registered!");
        store.put(waypoint.getID(), waypoint);
        MinecraftForge.EVENT_BUS.post(new WaypointEvent.Created(waypoint));
        save();
    }

    @Override
    public void remove(ResourceLocation id) {
        if(store.containsKey(id)) {
            Waypoint waypoint = store.remove(id);
            MinecraftForge.EVENT_BUS.post(new WaypointEvent.Removed(waypoint));
            save();
        }
    }

    @Override
    public boolean has(ResourceLocation id) {
        return store.containsKey(id);
    }
}
