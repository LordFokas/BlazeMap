package com.eerussianguy.blazemap.engine.cache;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;


import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.pipeline.MasterDatum;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;
import com.eerussianguy.blazemap.engine.UnsafeGenerics;

public class ChunkMDCache {
    protected final Map<BlazeRegistry.Key<DataType>, MasterDatum> data = new TreeMap<>(Comparator.comparing(o -> o.location));
    protected volatile boolean dirty = false;

    protected final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    protected final ReentrantReadWriteLock.ReadLock rLock = cacheLock.readLock();
    protected final ReentrantReadWriteLock.WriteLock wLock = cacheLock.writeLock();


    public MasterDatum get(BlazeRegistry.Key<DataType> key) {
        rLock.lock();
        try {
            return data.get(key);

        } finally {
            rLock.unlock();
        }
    }

    public boolean update(MasterDatum datum) {
        BlazeRegistry.Key<DataType> key = UnsafeGenerics.stripKey(datum.getID());

        wLock.lock();
        try {
            MasterDatum old = data.get(key);

            if(!datum.equals(old)) {
                setDirty(true);
                data.put(key, datum);

                return true;
            }
            return false;

        } finally {
            wLock.unlock();
        }
    }

    public boolean isEmpty() {
        rLock.lock();
        try {
            return data.isEmpty();

        } finally {
            rLock.unlock();
        }
    }

    public Set<BlazeRegistry.Key<DataType>> keys() {
        rLock.lock();
        try {
            return data.keySet();

        } finally {
            rLock.unlock();
        }
    }

    public List<MasterDatum> data() {
        rLock.lock();
        try {
            return data.values().stream().toList();

        } finally {
            rLock.unlock();
        }
    }

    public ChunkMDCache clear() {
        wLock.lock();
        try {
            setDirty(true);
            data.clear();

            return this;

        } finally {
            wLock.unlock();
        }
    }

    public ChunkMDCache copy() {
        return copyInto(new ChunkMDCache());
    }

    public ChunkMDCache copyInto(ChunkMDCache clone) {
        rLock.lock();
        clone.wLock.lock();

        try {
            clone.setDirty(true);

            clone.data.clear();
            clone.data.putAll(data);
            return clone;

        } finally {
            clone.wLock.unlock();
            rLock.unlock();
        }
    }

    /**
     * Mark this chunk as either "dirty" or "not dirty" (ie "clean").
     *
     * A chunk can only be marked dirty if already holding the write lock, however can be marked as
     * clean from either the read or write lock (eg if cache has been read out to file to save).
     *
     * Note: As isDirty() is _not_ protected by the cache lock (though this.dirty is marked volatile to
     * synchronise its value between threads), placement of calls to setDirty() need to be considered in the
     * context of providing the most useful value to other threads checking this chunks current cleanliness status
     * (knowing they won't then be able to act further on this chunk without first acquiring its lock)
     */
    protected void setDirty(boolean dirty) {
        rLock.lock();
        try {
            if (dirty && !cacheLock.isWriteLockedByCurrentThread()) {
                throw new IllegalThreadStateException(
                    "Trying to mark ChunkMDCache as dirty without holding write lock first. You must hold the write lock before changing the ChunkMDCache."
                );
            }

            this.dirty = dirty;

        } finally {
            rLock.unlock();
        }
    }

    /**
     * Returns if this cache is dirty or not.
     *
     * Note that this function is _not_ protected by the chunk lock to improve throughput when checking region cleanliness,
     * though the underlying value itself is volatile to keep it synchronised across threads
     */
    public boolean isDirty() {
        return dirty;
    }

    static class Persisted extends ChunkMDCache {
        private final RegionMDCache parent;

        public Persisted(RegionMDCache parent) {
            this.parent = parent;
        }

        /**
         * Read from stream into cache
         */
        public void read(MinecraftStreams.Input stream) throws IOException {
            int entries = stream.readInt();

            // Yes I see the irony that the read() function needs a write lock and the write() function needs a read lock
            wLock.lock();
            try {
                for(int i = 0; i < entries; i++) { // Why can't I just write Key<DataType<>> and have the compiler trust me? ffs.
                    BlazeRegistry.Key<DataType> key = (BlazeRegistry.Key<DataType>) (Object) BlazeMapAPI.MASTER_DATA.findOrCreate(stream.readResourceLocation());

                    if (key.value() == null) {
                        BlazeMap.LOGGER.warn("Unrecognised DataType in MD cache. Did you uninstall a Blaze Map extension? Skipping cache read");
                        throw new IOException("Cannot deserialize unregistered MD DataType");
                    }

                    data.put(key, key.value().deserialize(stream));
                }

                // Setting "clean" now the cache matches what's on disk
                setDirty(false);

            } finally {
                wLock.unlock();
            }
        }
        /**
         * Write from cache into stream
         */
        public void write(MinecraftStreams.Output stream) throws IOException {
            rLock.lock();
            try {
                stream.writeInt(data.size());

                for(Map.Entry<BlazeRegistry.Key<DataType>, MasterDatum> entry : data.entrySet()) {
                    BlazeRegistry.Key<DataType> key = entry.getKey();

                    stream.writeResourceLocation(key.location);
                    key.value().serialize(stream, entry.getValue());
                }

                // Only set "clean" after everything's been read in case exceptions are thrown while doing so
                setDirty(false);

            } finally {
                rLock.unlock();
            }
        }

        @Override
        protected void setDirty(boolean dirty) {
            super.setDirty(dirty);
            parent.requestSave();
        }
    }
}
