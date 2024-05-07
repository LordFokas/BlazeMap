package com.eerussianguy.blazemap.feature.mapping;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.TerrainSlopeMD;
import com.eerussianguy.blazemap.api.pipeline.Collector;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

public class TerrainSlopeCollector extends Collector<TerrainSlopeMD> {

    public TerrainSlopeCollector() {
        super(
            BlazeMapReferences.Collectors.TERRAIN_SLOPE,
            BlazeMapReferences.MasterData.TERRAIN_SLOPE
        );
    }

    @Override
    public TerrainSlopeMD collect(Level level, int minX, int minZ, int maxX, int maxZ) {

        final int[][] slopemap = new int[16][16];

        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                slopemap[x][z] = getSlopeGradient(level, minX + x, minZ + z);
            }
        }

        return new TerrainSlopeMD(BlazeMapReferences.MasterData.TERRAIN_SLOPE, slopemap);
    }

    protected static int getSlopeGradient(Level level, int x, int z) {
        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

        // Positive values means in shadow, negative values means in light
        int slope = 0;

        // Slope direction is relative to North West/top left of the map as that's the direction our
        // "sunlight" is going to be coming from. +x == East, +z == South.
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                int adjacentBlockHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x + dx, z + dz);

                if (adjacentBlockHeight > height) {
                    // Shadows are being cast by blocks on the negative side
                    slope = Math.max(slope, adjacentBlockHeight - height);

                } else if (adjacentBlockHeight < height) {
                    int oppositeBlockHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x - dx, z - dz);

                    if (oppositeBlockHeight < height) {
                        // At a peak with drops on both sides
                        slope = 0;
                    } else {
                        // Sunlight shining on the higher block
                        slope = Math.min(slope, adjacentBlockHeight - height);
                    }
                }
            }
        }

        return slope;
    }
}
