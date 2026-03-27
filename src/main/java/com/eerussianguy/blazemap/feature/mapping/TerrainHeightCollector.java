package com.eerussianguy.blazemap.feature.mapping;

import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.TerrainHeightMD;
import com.eerussianguy.blazemap.api.pipeline.Collector;

public class TerrainHeightCollector extends Collector<TerrainHeightMD> {

    public TerrainHeightCollector() {
        super(
            BlazeMapReferences.Collectors.TERRAIN_HEIGHT,
            BlazeMapReferences.MasterData.TERRAIN_HEIGHT
        );
    }

    @Override
    public TerrainHeightMD collect(Level level, int minX, int minZ, int maxX, int maxZ) {
        final int[][] heightmapTerrain = new int[16][16];
        // final int[][] heightmapSurface = new int[16][16];
        // final int[][] heightmapOpaque = new int[16][16];
        // final float[][] heightmapAttenuation = new float[16][16];

        final int minBuildHeight = level.getMinBuildHeight();

        int blockX;
        int blockZ;

        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                blockX = x + minX;
                blockZ = z + minZ;
                /** 
                 * Collect heights of the highest and lowest block that can be seen.
                 * (Primarily for shadow implementation)
                 */
                // TODO: Waiting until Transformer improvements have been made in BME-198 before implementing
                
                /** 
                 * Now collect base terrain height.
                 * This ignores non-terrain blocks such as trees and other plantlife
                 */
                int height = findSurfaceBelowVegetation(level, blockX, blockZ, false);

                // Note: The + 1 here is for legacy reasons. Will make it somebody else's decision
                // wether or not to remove it and possibly make other visual adjustments instead.
                heightmapTerrain[z][x] = height + 1;
            }
        }


        return new TerrainHeightMD(
            BlazeMapReferences.MasterData.TERRAIN_HEIGHT,
            minBuildHeight,
            level.getMaxBuildHeight(),
            level.getHeight(),
            level.getSeaLevel(),
            heightmapTerrain
        );
    }
}
