package com.eerussianguy.blazemap.api.builtin;

import java.util.Arrays;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.pipeline.MasterDatum;

public class TerrainSlopeMD extends MasterDatum {
    public final int[][] slopemap;

    public TerrainSlopeMD(int[][] slopemap) {
        this.slopemap = slopemap;
    }

    @Override
    public BlazeRegistry.Key<DataType<MasterDatum>> getID() {
        return BlazeMapReferences.MasterData.TERRAIN_SLOPE;
    }

    @Override
    public boolean equalsMD(MasterDatum md) {
        TerrainSlopeMD other = (TerrainSlopeMD) md;
        return Arrays.equals(this.slopemap, other.slopemap, Arrays::compare);
    }
}
