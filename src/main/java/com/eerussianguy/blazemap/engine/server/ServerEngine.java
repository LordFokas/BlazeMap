package com.eerussianguy.blazemap.engine.server;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.engine.BlazeMapAsync;
import com.eerussianguy.blazemap.engine.Pipeline;
import com.eerussianguy.blazemap.engine.RegistryController;
import com.eerussianguy.blazemap.engine.StorageAccess;
import com.eerussianguy.blazemap.network.BlazeNetwork;
import com.eerussianguy.blazemap.profiling.Profiler;

public class ServerEngine {
    private static final Map<ResourceKey<Level>, ServerPipeline> PIPELINES = new HashMap<>();
    private static MinecraftServer server;
    private static boolean isRunning;
    private static StorageAccess.Internal storage;
    private static int numCollectors = 0, numProcessors = 0, numTransformers = 0;

    public static void init() {
        BlazeNetwork.initEngine();
        MinecraftForge.EVENT_BUS.register(ServerEngine.class);
    }

    public static void submit(Runnable task) {
        if(server == null) return;
        server.submit(task);
    }

    @SubscribeEvent
    public static void onServerStart(ServerStartingEvent event) {
        RegistryController.ensureRegistriesReady();
        isRunning = true;
        server = event.getServer();
        storage = new StorageAccess.Internal(server.getWorldPath(LevelResource.ROOT).toFile(), "blazemap-server");
        Profiler.setServerInstance(server);
    }

    @SubscribeEvent
    public static void onServerStop(ServerStoppedEvent event) {
        isRunning = false;
        server = null;
        storage = null;
        PIPELINES.clear();
        Profiler.setServerInstance(null);
    }

    public static void onChunkChanged(ResourceKey<Level> dim, ChunkPos pos) {
        if(!isRunning) return;
        getPipeline(dim).onChunkChanged(pos);
    }

    private static ServerPipeline getPipeline(ResourceKey<Level> dimension) {
        BlazeMapAsync async = BlazeMapAsync.instance();
        return PIPELINES.computeIfAbsent(dimension, d -> {
            BlazeMapAPI.COLLECTORS.keys();
            ServerPipeline pipeline = new ServerPipeline(async.serverChain, async.debouncer, d, () -> server.getLevel(d), storage.internal(d.location()));
            numCollectors = Math.max(numCollectors, pipeline.numCollectors);
            numTransformers = Math.max(numTransformers, pipeline.numTransformers);
            numProcessors = Math.max(numProcessors, pipeline.numProcessors);
            return pipeline;
        });
    }

    public static boolean isRunning() {
        return isRunning;
    }


    // =================================================================================================================
    // Debug Info Access
    public static String getMDSource() {
        return "Default";
    }

    public static int numPipelines() {
        return PIPELINES.size();
    }

    public static int avgTPS() {
        return (int) Math.min(20, 1000F / server.getAverageTickTime());
    }

    public static int numCollectors() {
        return numCollectors;
    }

    public static int numProcessors() {
        return numProcessors;
    }

    public static int numTransformers() {
        return numTransformers;
    }

    public static int dirtyChunks() {
        int dirty = 0;
        for(Pipeline pipeline : PIPELINES.values()) {
            dirty += pipeline.getDirtyChunks();
        }
        return dirty;
    }
}