package com.eerussianguy.blazemap.feature.mapping;

import java.io.IOException;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.builtin.TerrainSlopeMD;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;

public class TerrainSlopeSerializer implements DataType<TerrainSlopeMD> {
    @Override
    public void serialize(MinecraftStreams.Output stream, TerrainSlopeMD terrain) throws IOException {
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                stream.writeFloat(terrain.slopemap[z][x]);
            }
        }
    }

    @Override
    public TerrainSlopeMD deserialize(MinecraftStreams.Input stream) throws IOException {
        float[][] slopemap = new float[16][16];

        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                slopemap[z][x] = stream.readFloat();
            }
        }

        return new TerrainSlopeMD(slopemap);
    }

    @Override
    public BlazeRegistry.Key<?> getID() {
        return BlazeMapReferences.MasterData.TERRAIN_SLOPE;
    }
}
