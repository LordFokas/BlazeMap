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
        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                stream.writeFloat(terrain.slopemap[x][z]);
            }
        }
    }

    @Override
    public TerrainSlopeMD deserialize(MinecraftStreams.Input stream) throws IOException {
        float[][] slopemap = new float[16][16];

        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                slopemap[x][z] = stream.readShort();
            }
        }

        return new TerrainSlopeMD(slopemap);
    }

    @Override
    public BlazeRegistry.Key<?> getID() {
        return BlazeMapReferences.MasterData.TERRAIN_SLOPE;
    }
}
