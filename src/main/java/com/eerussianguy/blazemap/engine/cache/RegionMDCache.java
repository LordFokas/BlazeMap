package com.eerussianguy.blazemap.engine.cache;

import java.io.IOException;

import net.minecraft.world.level.ChunkPos;

import com.eerussianguy.blazemap.api.util.MinecraftStreams;
import com.eerussianguy.blazemap.api.util.RegionPos;
import com.eerussianguy.blazemap.engine.async.DebouncingDomain;
import com.eerussianguy.blazemap.engine.async.PriorityLock;

public class RegionMDCache {
    private static final int CHUNKS = 0x20;
    private static final byte VOID = 0x00, DATA = 0x01;

    private final ChunkMDCache.Persisted[] chunks = new ChunkMDCache.Persisted[CHUNKS * CHUNKS];
    private final DebouncingDomain<RegionMDCache> debouncer;
    private final PriorityLock lock = new PriorityLock();
    private final RegionPos pos;
    private boolean dirty;

    public RegionMDCache(RegionPos pos, DebouncingDomain<RegionMDCache> debouncer) {
        this.pos = pos;
        this.debouncer = debouncer;
    }

    PriorityLock lock() {
        return lock;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void read(MinecraftStreams.Input stream) throws IOException {
        try {
            lock.lock();
            for(int index = 0; index < chunks.length; index++) {
                byte b = stream.readByte();
                switch(b) {
                    case VOID: continue;
                    case DATA:
                        ChunkMDCache.Persisted chunk = chunks[index] = new ChunkMDCache.Persisted(this);
                        chunk.read(stream);
                        break;
                    default: throw new RuntimeException("Unexpected byte flag: " + b);
                }
            }
            dirty = false;
        }
        finally {
            lock.unlock();
        }
    }

    public void write(MinecraftStreams.Output stream) throws IOException {
        try {
            lock.lockPriority();
            for(ChunkMDCache.Persisted chunk : chunks) {
                if(chunk == null || chunk.isEmpty()) {
                    stream.writeByte(VOID);
                } else {
                    stream.writeByte(DATA);
                    chunk.write(stream);
                }
            }
            dirty = false;
        }
        finally {
            lock.unlock();
        }
    }

    public final ChunkMDCache.Persisted getChunkCache(ChunkPos chunk) {
        int x = chunk.getRegionLocalX();
        int z = chunk.getRegionLocalZ();
        int index = x * CHUNKS + z;
        try {
            lock.lock();
            synchronized(chunks) {
                if(chunks[index] == null) {
                    chunks[index] = new ChunkMDCache.Persisted(this);
                }
            }
        }
        finally {
            lock.unlock();
        }
        return chunks[index];
    }

    public RegionPos pos() {
        return pos;
    }

    void setDirty(){
        this.dirty = true;
        debouncer.push(this);
    }
}
