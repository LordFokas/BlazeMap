package com.eerussianguy.blazemap.feature.mapping;

import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.WaterLevelMD;
import com.eerussianguy.blazemap.api.pipeline.Collector;

public class WaterLevelCollector extends Collector<WaterLevelMD> {

    public WaterLevelCollector() {
        super(
            BlazeMapReferences.Collectors.WATER_LEVEL,
            BlazeMapReferences.MasterData.WATER_LEVEL
        );
    }


    @Override
    public WaterLevelMD collect(Level level, int minX, int minZ, int maxX, int maxZ) {

        int blockX;
        int blockZ;
        final int[][] water = new int[16][16];

        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                blockX = x + minX;
                blockZ = z + minZ;

                int depth = 0;
                int height = findSurfaceBelowVegetation(level, blockX, blockZ, true);

                while(isWater(level, blockX, height - depth, blockZ)) {
                    depth++;
                    if(height - depth < level.getMinBuildHeight()) break;
                }

                water[z][x] = depth;
            }
        }

        return new WaterLevelMD(level.getSeaLevel(), water);
    }
}
