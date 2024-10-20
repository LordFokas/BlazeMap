package com.eerussianguy.blazemap.engine.server;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;

import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.event.ServerPipelineInitEvent;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.pipeline.MasterDataDispatcher;
import com.eerussianguy.blazemap.api.pipeline.MasterDatum;
import com.eerussianguy.blazemap.api.pipeline.PipelineType;
import com.eerussianguy.blazemap.engine.Pipeline;
import com.eerussianguy.blazemap.engine.PipelineProfiler;
import com.eerussianguy.blazemap.engine.storage.InternalStorage;
import com.eerussianguy.blazemap.engine.UnsafeGenerics;
import com.eerussianguy.blazemap.engine.cache.ChunkMDCache;
import com.eerussianguy.blazemap.engine.cache.ChunkMDCacheView;
import com.eerussianguy.blazemap.lib.async.AsyncChainRoot;
import com.eerussianguy.blazemap.lib.async.DebouncingThread;
import com.eerussianguy.blazemap.network.PacketChunkMDUpdate;

import static com.eerussianguy.blazemap.profiling.Profilers.Server.*;

class ServerPipeline extends Pipeline {
    private static final PipelineProfiler SERVER_PIPELINE_PROFILER = new PipelineProfiler(
        COLLECTOR_TIME_PROFILER,
        COLLECTOR_LOAD_PROFILER,
        TRANSFORMER_TIME_PROFILER,
        TRANSFORMER_LOAD_PROFILER,
        PROCESSOR_TIME_PROFILER,
        PROCESSOR_LOAD_PROFILER
    );

    private final MasterDataDispatcher dispatcher;

    public ServerPipeline(AsyncChainRoot async, DebouncingThread debouncer, ResourceKey<Level> dimension, Supplier<Level> level, InternalStorage storage) {
        super(
            async, debouncer, SERVER_PIPELINE_PROFILER, dimension, level, storage,
            BlazeMapAPI.COLLECTORS.keys().stream().filter(k -> k.value().shouldExecuteIn(dimension, PipelineType.SERVER)).collect(Collectors.toUnmodifiableSet()),
            BlazeMapAPI.TRANSFORMERS.keys().stream().filter(k -> k.value().shouldExecuteIn(dimension, PipelineType.SERVER)).collect(Collectors.toUnmodifiableSet()),
            BlazeMapAPI.PROCESSORS.keys().stream().filter(k -> k.value().shouldExecuteIn(dimension, PipelineType.SERVER)).collect(Collectors.toUnmodifiableSet())
        );

        ServerPipelineInitEvent event = new ServerPipelineInitEvent(dimension, addonStorage, this::dispatch);
        MinecraftForge.EVENT_BUS.post(event);
        this.dispatcher = event.getDispatcher();
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected void onPipelineOutput(ChunkPos pos, Set<Key<DataType>> diff, ChunkMDCacheView view, ChunkMDCache cache) {
        dispatcher.dispatch(dimension, pos, cache.data(), UnsafeGenerics.mdKeys(diff), ServerEngine.getMDSource(), level.get().getChunk(pos.x, pos.z));
    }

    private void dispatch(ResourceKey<Level> dimension, ChunkPos pos, List<MasterDatum> data, Set<BlazeRegistry.Key<DataType<MasterDatum>>> diff, String source, LevelChunk chunk) {
        new PacketChunkMDUpdate(dimension, pos, data, source).send(chunk);
    }
}
