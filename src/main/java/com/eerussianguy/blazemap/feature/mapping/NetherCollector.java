package com.eerussianguy.blazemap.feature.mapping;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.TerrainHeightMD;
import com.eerussianguy.blazemap.api.pipeline.Collector;
import com.eerussianguy.blazemap.api.pipeline.PipelineType;

public class NetherCollector extends Collector<TerrainHeightMD> {
    public NetherCollector() {
        super(
            BlazeMapReferences.Collectors.NETHER,
            BlazeMapReferences.MasterData.NETHER
        );
    }

    @Override
    public TerrainHeightMD collect(Level level, int minX, int minZ, int maxX, int maxZ) {
        final int[][] heightmap = new int[16][16];

        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                int height = 110;
                while(isNotAir(level, minX + x, height - 1, minZ + z)) {
                    height--;
                    if(height <= level.getMinBuildHeight()) break;
                }
                if(height > level.getMinBuildHeight()) {
                    while(isNotBaseStone(level, minX + x, height - 1, minZ + z)) {
                        height--;
                        if(height <= level.getMinBuildHeight()) break;
                    }
                }
                heightmap[z][x] = height;
            }
        }

        return new TerrainHeightMD(BlazeMapReferences.MasterData.NETHER, level.getMinBuildHeight(), level.getMaxBuildHeight(), level.getHeight(), level.getSeaLevel(), heightmap);
    }

    @Override
    public boolean shouldExecuteIn(ResourceKey<Level> dimension, PipelineType pipeline) {
        return dimension.equals(Level.NETHER) && super.shouldExecuteIn(dimension, pipeline);
    }

    private boolean isNotAir(Level level, int x, int y, int z) {
        return !level.getBlockState(POS.set(x, y, z)).isAir();
    }

    private boolean isNotBaseStone(Level level, int x, int y, int z) {
        BlockState state = level.getBlockState(POS.set(x, y, z));
        return !state.getMaterial().isSolid();
    }
}
