package com.eerussianguy.blazemap.engine.cache;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.util.IStorageAccess;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;
import com.eerussianguy.blazemap.api.util.RegionPos;
import com.eerussianguy.blazemap.engine.BlazeMapAsync;
import com.eerussianguy.blazemap.engine.async.AsyncChainRoot;
import com.eerussianguy.blazemap.engine.async.DebouncingDomain;
import com.eerussianguy.blazemap.util.Helpers;

public class LevelMDCache {
    private static final ResourceLocation NODE = Helpers.identifier("md-cache");
    private final LoadingCache<RegionPos, RegionMDCache> regions;
    private final IStorageAccess storage;
    private final DebouncingDomain<RegionMDCache> debouncer;
    private final AsyncChainRoot asyncChain;

    public LevelMDCache(final IStorageAccess storage, AsyncChainRoot asyncChain) {
        this.storage = storage;
        this.asyncChain = asyncChain;
        this.debouncer = new DebouncingDomain<>(BlazeMapAsync.instance().debouncer, this::persist, 5_000, 30_000);

        this.regions = CacheBuilder.newBuilder()
            .maximumSize(256)
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .removalListener((RemovalNotification<RegionPos, RegionMDCache> regionCache) -> {
                // Make sure region cache on disk reflects its last known state before evicting
                debouncer.push(regionCache.getValue());
            })
            .build(new CacheLoader<>() {
                @Override
                public RegionMDCache load(RegionPos key) {
                    RegionMDCache cache = new RegionMDCache(key, debouncer);
                    String file = getFilename(key);

                    if(storage.exists(NODE, file)){
                        try(MinecraftStreams.Input stream = storage.read(NODE, file)) {
                            cache.read(stream);

                        } catch (Exception e) {
                            // Couldn't populate the cache from the existing cache file (corruption?)
                            // so behaving as if no cache file was found
                            cache = new RegionMDCache(key, debouncer);
                        }
                    }

                    return cache;
                }
            });
    }

    private void persist(RegionMDCache cache) {
        if(!cache.isDirty()) return;
        asyncChain.runOnDataThread(() -> {
            String buffer = getBufferFile(cache.pos());
            String file = getFilename(cache.pos());
            try(MinecraftStreams.Output stream = storage.write(NODE, buffer)) {
                cache.write(stream);
            }
            catch(IOException e) {
                BlazeMap.LOGGER.warn("Error writing cache buffer {}. Will try again later", buffer);
                e.printStackTrace();
                debouncer.push(cache);
            }
            try {
                storage.move(NODE, buffer, file);
            }
            catch(IOException e) {
                BlazeMap.LOGGER.warn("Error moving buffer to file {}. Will try again later", file);
                e.printStackTrace();
                debouncer.push(cache);
            }
        });
    }

    private static String getFilename(RegionPos pos) {
        return pos + ".rmd";
    }

    private static String getBufferFile(RegionPos pos) {
        return pos + ".buffer";
    }

    public void flush() {
        this.debouncer.finish();
    }

    public RegionMDCache getRegionCache(RegionPos pos) {
        try {
            return regions.get(pos);
        }
        catch(ExecutionException e) {
            // FIXME: will happen when IOExceptions occur.
            throw new RuntimeException(e);
        }
    }

    public RegionMDCache getRegionCache(ChunkPos pos) {
        return getRegionCache(new RegionPos(pos));
    }

    public ChunkMDCache getChunkCache(ChunkPos pos) {
        return getRegionCache(pos).getChunkCache(pos);
    }
}
