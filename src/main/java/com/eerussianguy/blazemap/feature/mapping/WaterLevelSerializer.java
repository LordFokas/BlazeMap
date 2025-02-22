package com.eerussianguy.blazemap.feature.mapping;

import java.io.IOException;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.builtin.WaterLevelMD;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;

public class WaterLevelSerializer implements DataType<WaterLevelMD> {
    @Override
    public BlazeRegistry.Key<?> getID() {
        return BlazeMapReferences.MasterData.WATER_LEVEL;
    }

    @Override
    public void serialize(MinecraftStreams.Output stream, WaterLevelMD water) throws IOException {
        stream.writeShort(water.sea);

        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                stream.writeShort(water.level[z][x]);
            }
        }
    }

    @Override
    public WaterLevelMD deserialize(MinecraftStreams.Input stream) throws IOException {
        short sea = stream.readShort();

        int[][] level = new int[16][16];

        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                level[z][x] = stream.readShort();
            }
        }

        return new WaterLevelMD(sea, level);
    }
}
