package com.eerussianguy.blazemap.engine.cache;

import java.util.Set;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.pipeline.MasterDatum;
import com.eerussianguy.blazemap.api.util.DataSource;
import com.eerussianguy.blazemap.engine.UnsafeGenerics;

@SuppressWarnings("rawtypes")
public class ChunkMDCacheView implements DataSource {
    private ChunkMDCache source;
    private Set<Key<DataType>> filter;

    public ChunkMDCacheView() {}

    public ChunkMDCacheView setSource(ChunkMDCache source) {
        this.source = source;
        return this;
    }

    public void setFilter(Set<Key<DataType>> filter) {
        this.filter = filter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends MasterDatum> T get(Key<DataType<T>> key) {
        if(!filter.contains(key)) return null;
        return (T) source.get(UnsafeGenerics.stripKey(key));
    }
}
