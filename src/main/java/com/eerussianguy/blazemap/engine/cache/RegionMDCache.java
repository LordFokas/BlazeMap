package com.eerussianguy.blazemap.engine.cache;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Iterator;


import net.minecraft.world.level.ChunkPos;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;
import com.eerussianguy.blazemap.api.util.RegionPos;
import com.eerussianguy.blazemap.engine.async.DebouncingDomain;
import com.eerussianguy.blazemap.profiling.Profilers;

public class RegionMDCache {
    private static final int CHUNKS = 0x20;
    private static final byte VOID = 0x00, DATA = 0x01;

    private final ChunkCacheStore chunks = new ChunkCacheStore(CHUNKS * CHUNKS);
    private final DebouncingDomain<RegionMDCache> debouncer;
    private final RegionPos pos;

    // These are both invoked at the LevelMDCache level, due to the movement between buffer and file
    public final ReentrantReadWriteLock fileLock = new ReentrantReadWriteLock();
    public final ReentrantLock bufferLock = new ReentrantLock();

    public RegionMDCache(RegionPos pos, DebouncingDomain<RegionMDCache> debouncer) {
        this.pos = pos;
        this.debouncer = debouncer;
    }

    public boolean isDirty() {
        for (ChunkMDCache.Persisted chunk : chunks) {
            if (chunk != null && chunk.isDirty()) {
                return true;
            }
        }

        return false;
    }

    public void read(MinecraftStreams.Input stream) {
        Profilers.FileOps.CACHE_READ_TIME_PROFILER.begin();

        try {
            for(int index = 0; index < chunks.length; index++) {
                byte b = stream.readByte();

                switch(b) {
                    case VOID: continue;
                    case DATA:
                        ChunkMDCache.Persisted chunk = chunks.get(index);
                        chunk.read(stream);
                        break;

                    default: throw new RuntimeException("Unexpected byte flag: " + b);
                }
            }
        } catch (IOException e) {
            BlazeMap.LOGGER.error("Could not read region MD cache file. Skipping.", e);

        } finally {
            Profilers.FileOps.CACHE_READ_TIME_PROFILER.end();
        }
    }

    public void write(MinecraftStreams.Output stream) throws IOException {
        Profilers.FileOps.CACHE_WRITE_TIME_PROFILER.begin();

        try {
            for(ChunkMDCache.Persisted chunk : chunks) {
                if(chunk == null || chunk.isEmpty()) {
                    stream.writeByte(VOID);
                } else {
                    stream.writeByte(DATA);
                    chunk.write(stream);
                }
            }
        }
        finally {
            Profilers.FileOps.CACHE_WRITE_TIME_PROFILER.end();
        }
    }

    public final ChunkMDCache getChunkCache(ChunkPos chunk) {
        int x = chunk.getRegionLocalX();
        int z = chunk.getRegionLocalZ();
        int index = x * CHUNKS + z;

        return chunks.get(index);
    }

    public RegionPos pos() {
        return pos;
    }

    void requestSave(){
        debouncer.push(this);
    }

    /**
     * This is a wrapper for the list of chunk caches to make sure it's threadsafe.
     *
     * This is done by not allowing any external set opperations, so each index will only ever return
     * its originally assigned ChunkMDCache reference. This means read/write safety can be performed on
     * the individual chunk cache level, avoiding having to lock the whole list.
     */
    private class ChunkCacheStore implements Iterable<ChunkMDCache.Persisted> {
        private final AtomicReferenceArray<ChunkMDCache.Persisted> chunksAtomicArray;
        public final int length;

        public ChunkCacheStore (int size) {
            this.chunksAtomicArray = new AtomicReferenceArray<ChunkMDCache.Persisted>(size);
            this.length = size;
        }

        public ChunkMDCache.Persisted get(int index) {
            // The if statement doesn't provide any thread safety, but the compareAndSet does.
            // The if is only here to minimise the amount of objects unnecessarily created, since the cache
            // will already exist most of the time, thus minimising GC
            if (chunksAtomicArray.get(index) == null) {
                chunksAtomicArray.compareAndSet(index, null, new ChunkMDCache.Persisted(RegionMDCache.this));
            }

            return chunksAtomicArray.get(index);
        }

        public Iterator<ChunkMDCache.Persisted> iterator() {
            return new ChunkCacheIterator();
        }

        private class ChunkCacheIterator implements Iterator<ChunkMDCache.Persisted> {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < length;
            }

            @Override
            public ChunkMDCache.Persisted next() {
                return chunksAtomicArray.get(index++);
            }
        }
    }
}
