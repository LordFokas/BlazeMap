package com.eerussianguy.blazemap.engine.cache;

import java.io.IOException;
import java.util.*;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.pipeline.MasterDatum;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;
import com.eerussianguy.blazemap.engine.UnsafeGenerics;

public class ChunkMDCache {
    protected final Map<BlazeRegistry.Key<DataType>, MasterDatum> data = new TreeMap<>(Comparator.comparing(o -> o.location));

    public MasterDatum get(BlazeRegistry.Key<DataType> key) {
        return data.get(key);
    }

    public boolean update(MasterDatum datum) {
        BlazeRegistry.Key<DataType> key = UnsafeGenerics.stripKey(datum.getID());
        MasterDatum old = data.get(key);
        if(!datum.equals(old)) {
            data.put(key, datum);
            setDirty();
            return true;
        }
        return false;
    }

    protected void setDirty() {}

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public Set<BlazeRegistry.Key<DataType>> keys() {
        return data.keySet();
    }

    public List<MasterDatum> data() {
        return data.values().stream().toList();
    }

    public ChunkMDCache clear() {
        data.clear();
        return this;
    }

    public ChunkMDCache copy() {
        return copyInto(new ChunkMDCache());
    }

    public ChunkMDCache copyInto(ChunkMDCache clone) {
        clone.data.clear();
        clone.data.putAll(data);
        return clone;
    }

    static class Persisted extends ChunkMDCache {
        private final RegionMDCache parent;

        public Persisted(RegionMDCache parent) {
            this.parent = parent;
        }

        public void read(MinecraftStreams.Input stream) throws IOException {
            int entries = stream.readInt();

            for(int i = 0; i < entries; i++) { // Why can't I just write Key<DataType<>> and have the compiler trust me? ffs.
                BlazeRegistry.Key<DataType> key = (BlazeRegistry.Key<DataType>) (Object) BlazeMapAPI.MASTER_DATA.findOrCreate(stream.readResourceLocation());

                if (key.value() == null) {
                    BlazeMap.LOGGER.warn("Unrecognised DataType in MD cache. Did you uninstall a Blaze Map extension? Skipping cache read");
                    throw new IOException("Cannot deserialize unregistered MD DataType");
                }

                data.put(key, key.value().deserialize(stream));
            }
        }

        public void write(MinecraftStreams.Output stream) throws IOException {
            stream.writeInt(data.size());
            for(Map.Entry<BlazeRegistry.Key<DataType>, MasterDatum> entry : data.entrySet()) {
                BlazeRegistry.Key<DataType> key = entry.getKey();
                stream.writeResourceLocation(key.location);
                key.value().serialize(stream, entry.getValue());
            }
        }

        @Override
        public boolean update(MasterDatum datum) {
            try {
                parent.lock().lock();
                return super.update(datum);
            }
            finally {
                parent.lock().unlock();
            }
        }

        @Override
        protected void setDirty() {
            parent.setDirty();
        }
    }
}
