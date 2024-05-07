package com.eerussianguy.blazemap.api.builtin;

import java.util.Arrays;

import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.pipeline.MasterDatum;

public class TerrainSlopeMD extends MasterDatum {
    private final BlazeRegistry.Key<DataType<MasterDatum>> id;
    public final int[][] slopemap;

    public TerrainSlopeMD(BlazeRegistry.Key<DataType<MasterDatum>> id, int[][] slopemap) {
        this.id = id;
        this.slopemap = slopemap;
    }

    @Override
    public BlazeRegistry.Key<DataType<MasterDatum>> getID() {
        return id;
    }

    @Override
    public boolean equalsMD(MasterDatum md) {
        TerrainSlopeMD other = (TerrainSlopeMD) md;
        return Arrays.equals(this.slopemap, other.slopemap, Arrays::compare);
    }
}
