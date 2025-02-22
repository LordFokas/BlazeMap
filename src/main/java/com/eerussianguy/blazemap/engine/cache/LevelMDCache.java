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
import com.eerussianguy.blazemap.api.util.StorageAccess;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;
import com.eerussianguy.blazemap.api.util.RegionPos;
import com.eerussianguy.blazemap.engine.BlazeMapAsync;
import com.eerussianguy.blazemap.lib.async.AsyncChainRoot;
import com.eerussianguy.blazemap.lib.async.DebouncingDomain;

public class LevelMDCache {
    private static final ResourceLocation NODE = BlazeMap.resource("md-cache");
    private final LoadingCache<RegionPos, RegionMDCache> regions;
    private final StorageAccess storage;
    private final DebouncingDomain<RegionMDCache> debouncer;
    private final AsyncChainRoot asyncChain;

    public LevelMDCache(final StorageAccess storage, AsyncChainRoot asyncChain) {
        this.storage = storage;
        this.asyncChain = asyncChain;
        this.debouncer = new DebouncingDomain<>(BlazeMapAsync.instance().debouncer, this::persist, 5_000, 30_000, BlazeMap.LOGGER);

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
                        // Try to greedily acquire file lock
                        if (!cache.fileLock.readLock().tryLock()) cache.fileLock.readLock().lock();

                        try(MinecraftStreams.Input stream = storage.read(NODE, file)) {
                            cache.read(stream);
                        }
                        catch (Exception e) {
                            // Unlocking old cache before we discard cache
                            // TODO: There must be a better way of doing this?
                            cache.fileLock.readLock().unlock();

                            // Couldn't populate the cache from the existing cache file (corruption?)
                            // so behaving as if no cache file was found
                            cache = new RegionMDCache(key, debouncer);
                        }
                        finally {
                            if (cache.fileLock.getReadHoldCount() > 0) {
                                cache.fileLock.readLock().unlock();
                            }
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

            cache.bufferLock.lock();
            try {
                try(MinecraftStreams.Output stream = storage.write(NODE, buffer)) {
                    cache.write(stream);
                }
                catch(IOException e) {
                    BlazeMap.LOGGER.warn("Error writing cache buffer {}. Will try again later", buffer);
                    e.printStackTrace();
                    debouncer.push(cache);
                    return;
                }

                cache.fileLock.writeLock().lock();
                try {
                    storage.move(NODE, buffer, file);
                }
                catch(IOException e) {
                    BlazeMap.LOGGER.warn("Error moving buffer to file {}. Will try again later", file);
                    e.printStackTrace();
                    debouncer.push(cache);
                }
                finally {
                    cache.fileLock.writeLock().unlock();
            }
            }
            finally {
                cache.bufferLock.unlock();
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
