package com.eerussianguy.blazemap.engine;

import java.util.*;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.pipeline.*;
import com.eerussianguy.blazemap.api.util.RegionPos;
import com.eerussianguy.blazemap.engine.cache.ChunkMDCache;
import com.eerussianguy.blazemap.engine.cache.ChunkMDCacheView;
import com.eerussianguy.blazemap.engine.cache.LevelMDCache;
import com.eerussianguy.blazemap.engine.storage.InternalStorage;
import com.eerussianguy.blazemap.engine.storage.PublicStorage;
import com.eerussianguy.blazemap.lib.async.AsyncChainRoot;
import com.eerussianguy.blazemap.lib.async.DebouncingDomain;
import com.eerussianguy.blazemap.lib.async.DebouncingThread;

import static com.eerussianguy.blazemap.engine.UnsafeGenerics.*;

// If you're not a fan of unchecked casts, raw generics, cheesy unsafe generics double casts through Object and so on,
// this is not a safe place for you. The code in here needs to be succinct, and we cannot afford to write a dozen type
// parameterizations every single line, it would be unreadable and hard to maintain.
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class Pipeline {
    protected final ThreadLocal<ChunkMDCacheView> CACHE_VIEWS = ThreadLocal.withInitial(ChunkMDCacheView::new);
    protected final ThreadLocal<ChunkMDCacheView> DIFF_VIEWS = ThreadLocal.withInitial(ChunkMDCacheView::new);
    protected final ThreadLocal<ChunkMDCache> PLACEHOLDER_CACHES = ThreadLocal.withInitial(ChunkMDCache::new);
    protected final ThreadLocal<ChunkMDCache> DIFFERENTIAL_CACHES = ThreadLocal.withInitial(ChunkMDCache::new);

    private final PipelineProfiler profiler;
    protected final AsyncChainRoot async;
    protected final DebouncingDomain<ChunkPos> dirtyChunks;
    public final ResourceKey<Level> dimension;
    protected final Supplier<Level> level;
    protected final Set<Key<Collector>> availableCollectors;
    protected final Set<Key<Transformer>> availableTransformers;
    protected final Set<Key<Processor>> availableProcessors;
    protected final boolean differentialExecution;
    private final Collector<MasterDatum>[] collectors;
    private final List<Transformer> transformers;
    private final List<Processor> processors;
    public final int numCollectors, numProcessors, numTransformers;
    protected final InternalStorage storage;
    public final PublicStorage addonStorage;
    protected final LevelMDCache mdCache;
    private boolean useMDCache = false;


    protected Pipeline(
        AsyncChainRoot async, DebouncingThread debouncer, PipelineProfiler profiler,
        ResourceKey<Level> dimension, Supplier<Level> level, InternalStorage storage,
        Set<Key<Collector<MasterDatum>>> availableCollectors,
        Set<Key<Transformer<MasterDatum>>> availableTransformers,
        Set<Key<Processor>> availableProcessors
    ) {
        this.async = async;
        this.dirtyChunks = new DebouncingDomain<>(debouncer, this::begin, 500, 5000, BlazeMap.LOGGER);
        this.profiler = profiler;

        this.dimension = dimension;
        this.level = level;

        this.availableCollectors = stripCollectors(availableCollectors);
        this.availableTransformers = stripTransformers(availableTransformers);
        this.availableProcessors = availableProcessors;

        collectors = availableCollectors.stream().map(Key::value).toArray(Collector[]::new);
        transformers = this.availableTransformers.stream().map(Key::value).toList();
        processors = this.availableProcessors.stream().map(Key::value).toList();

        boolean differentialExecution = false;
        for(Processor processor : processors) {
            differentialExecution = differentialExecution || processor.executionMode == ExecutionMode.DIFFERENTIAL;
        }
        this.differentialExecution = differentialExecution;

        numCollectors = collectors.length;
        numTransformers = transformers.size();
        numProcessors = processors.size();

        this.storage = storage;
        this.addonStorage = storage.addon();
        this.mdCache = new LevelMDCache(addonStorage, async);
    }

    public int getDirtyChunks() {
        return dirtyChunks.size();
    }


    // =================================================================================================================
    // Pipeline IO
    public void onChunkChanged(ChunkPos pos) {
        if(!level.get().getChunkSource().hasChunk(pos.x, pos.z)) return;
        dirtyChunks.push(pos);
    }

    public void insertMasterData(ChunkPos pos, List<MasterDatum> data) {
        async.runOnDataThread(() -> processMasterData(pos, data));
    }

    protected abstract void onPipelineOutput(ChunkPos pos, Set<Key<DataType>> diff, ChunkMDCacheView view, ChunkMDCache cache);


    // =================================================================================================================
    // Pipeline internals
    protected void begin(ChunkPos pos) {
        async.startOnGameThread($ -> this.runCollectors(pos))
            .thenOnDataThread(data -> this.processMasterData(pos, data))
            .execute();
    }

    // TODO: figure out why void gives generic errors but null Void is OK. Does it have to be an Object?
    protected Void processMasterData(ChunkPos pos, List<MasterDatum> collectedData) {
        if(collectedData.size() == 0) return null;

        ChunkMDCache cache = useMDCache ? mdCache.getChunkCache(pos) : PLACEHOLDER_CACHES.get().clear();
        ChunkMDCacheView view = CACHE_VIEWS.get().setSource(cache);
        ChunkMDCacheView old = differentialExecution ? DIFF_VIEWS.get().setSource(cache.copyInto(DIFFERENTIAL_CACHES.get())) : null;

        // Diff collected data
        Set<Key<DataType>> diff = new HashSet<>();
        diffMD(collectedData, cache, diff);

        List<MasterDatum> transformedData = this.runTransformers(diff, view);

        // Diff transformed data
        diffMD(transformedData, cache, diff);

        this.onPipelineOutput(pos, diff, view, cache);
        this.runProcessors(pos, diff, view, old);
        return null;
    }

    private static void diffMD(List<MasterDatum> data, ChunkMDCache cache, Set<Key<DataType>> diff) {
        for(MasterDatum md : data) {
            if(cache.update(md)) {
                diff.add(stripKey(md.getID()));
            }
        }
    }

    protected void useMDCache() {
        this.useMDCache = true;
    }

    public boolean isMDCached() {
        return this.useMDCache;
    }

    public LevelMDCache getMDCache() {
        return mdCache;
    }


    // =================================================================================================================
    // Pipeline execution steps
    protected List<MasterDatum> runCollectors(ChunkPos pos) {
        try {
            profiler.collectorLoad.hit();
            profiler.collectorTime.begin();
            Level level = this.level.get();
            if(!level.getChunkSource().hasChunk(pos.x, pos.z)) return Collections.EMPTY_LIST;

            int x0 = pos.getMinBlockX();
            int x1 = pos.getMaxBlockX();
            int z0 = pos.getMinBlockZ();
            int z1 = pos.getMaxBlockZ();

            List<MasterDatum> data = new ArrayList<>(32);
            for(Collector collector : collectors) {
                MasterDatum md = collector.collect(level, x0, z0, x1, z1);
                if(md != null) {
                    data.add(md);
                }
            }
            return data;
        }
        finally {
            profiler.collectorTime.end();
        }
    }

    protected List<MasterDatum> runTransformers(Set<Key<DataType>> diff, ChunkMDCacheView view) {
        Transformer[] transformers = this.transformers.stream().filter(t -> !Collections.disjoint(t.getInputIDs(), diff)).toArray(Transformer[]::new);
        if(transformers.length == 0) return Collections.EMPTY_LIST;

        try {
            profiler.transformerLoad.hit();
            profiler.transformerTime.begin();
            List<MasterDatum> data = new ArrayList<>(32);
            for(Transformer transformer : transformers) {
                view.setFilter(transformer.getInputIDs());
                data.add(transformer.transform(view));
            }
            return data;
        }
        finally {
            profiler.transformerTime.end();
        }
    }

    protected void runProcessors(ChunkPos chunk, Set<Key<DataType>> diff, ChunkMDCacheView current, ChunkMDCacheView old) {
        Processor[] processors = this.processors.stream().filter(p -> !Collections.disjoint(p.getInputIDs(), diff)).toArray(Processor[]::new);
        if(processors.length == 0) return;

        try {
            profiler.processorLoad.hit();
            profiler.processorTime.begin();
            RegionPos region = new RegionPos(chunk);
            for(Processor processor : processors) {
                Set<BlazeRegistry.Key<DataType>> keys = stripKeys(processor.getInputIDs());
                current.setFilter(keys);
                if(differentialExecution && processor.executionMode == ExecutionMode.DIFFERENTIAL) {
                    old.setFilter(keys);
                    processor.execute(dimension, region, chunk, current, old);
                } else {
                    processor.execute(dimension, region, chunk, current);
                }
            }
        }
        finally {
            profiler.processorTime.end();
        }
    }
}
