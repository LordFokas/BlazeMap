package com.eerussianguy.blazemap.feature.mapping;

import java.io.IOException;

import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.builtin.TerrainSlopeMD;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.pipeline.MasterDatum;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;

public class TerrainSlopeSerializer implements DataType<TerrainSlopeMD> {
    private final BlazeRegistry.Key<?> id;

    public TerrainSlopeSerializer(BlazeRegistry.Key<DataType<MasterDatum>> id) {
        this.id = id;
    }

    @Override
    public void serialize(MinecraftStreams.Output stream, TerrainSlopeMD terrain) throws IOException {
        stream.writeKey(terrain.getID());

        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                stream.writeShort(terrain.slopemap[x][z]);
            }
        }
    }

    @Override
    public TerrainSlopeMD deserialize(MinecraftStreams.Input stream) throws IOException {
        BlazeRegistry.Key<DataType<MasterDatum>> id = stream.readKey(BlazeMapAPI.MASTER_DATA);

        int[][] slopemap = new int[16][16];

        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                slopemap[x][z] = stream.readShort();
            }
        }

        return new TerrainSlopeMD(id, slopemap);
    }

    @Override
    public BlazeRegistry.Key<?> getID() {
        return id;
    }
}
