package com.eerussianguy.blazemap.engine.cache;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.pipeline.MasterDatum;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;
import com.eerussianguy.blazemap.engine.UnsafeGenerics;

public class ChunkMDCache {
    protected final Map<BlazeRegistry.Key<DataType>, MasterDatum> data = new HashMap<>(16);

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

    public List<MasterDatum> data() {
        return data.values().stream().toList();
    }

    public ChunkMDCache clear() {
        data.clear();
        return this;
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
