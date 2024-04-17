package com.eerussianguy.blazemap.api.builtin;

import java.util.Arrays;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.pipeline.MasterDatum;

public class WaterLevelMD extends MasterDatum {
    public final int sea;
    public final int[][] level;

    public WaterLevelMD(int sea, int[][] level) {
        this.sea = sea;
        this.level = level;
    }

    @Override
    public BlazeRegistry.Key<DataType<MasterDatum>> getID() {
        return BlazeMapReferences.MasterData.WATER_LEVEL;
    }

    @Override
    public boolean equalsMD(MasterDatum md) {
        WaterLevelMD other = (WaterLevelMD) md;
        return Arrays.equals(this.level, other.level, Arrays::compare);
    }
}
