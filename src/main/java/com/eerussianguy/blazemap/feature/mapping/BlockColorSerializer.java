package com.eerussianguy.blazemap.feature.mapping;

import java.io.IOException;

import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.builtin.BlockColorMD;
import com.eerussianguy.blazemap.api.debug.MDInspectionController;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;

public class BlockColorSerializer implements DataType<BlockColorMD> {
    @Override
    public BlazeRegistry.Key<?> getID() {
        return BlazeMapReferences.MasterData.BLOCK_COLOR;
    }

    @Override
    public void serialize(MinecraftStreams.Output stream, BlockColorMD datum) throws IOException {
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                stream.writeInt(datum.colors[z][x]);
            }
        }
    }

    @Override
    public BlockColorMD deserialize(MinecraftStreams.Input stream) throws IOException {
        int[][] colors = new int[16][16];

        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                colors[z][x] = stream.readInt();
            }
        }

        return new BlockColorMD(colors);
    }

    @Override
    public MDInspectionController<BlockColorMD> getInspectionController() {
        return new MDInspectionController<BlockColorMD>() {
            @Override
            public int getNumLines(BlockColorMD datum) {
                return 0;
            }

            @Override
            public String getLine(BlockColorMD datum, int line) {
                return null;
            }

            @Override
            public int getNumGrids(BlockColorMD datum) {
                return 1;
            }

            @Override
            public String getGridName(BlockColorMD datum, int grid) {
                return "colors";
            }

            @Override
            public ResourceLocation getIcon(BlockColorMD datum, int grid, int x, int z) {
                return null;
            }

            @Override
            public int getTint(BlockColorMD datum, int grid, int x, int z) {
                return 0xFF000000 | datum.colors[x][z];
            }

            @Override
            public String getTooltip(BlockColorMD datum, int grid, int x, int z) {
                return Integer.toHexString(getTint(datum, grid, x, z));
            }
        };
    }
}
