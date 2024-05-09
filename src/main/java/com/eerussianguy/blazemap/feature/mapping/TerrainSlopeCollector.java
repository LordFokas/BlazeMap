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

        final float[][] slopemap = new float[16][16];

        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                slopemap[x][z] = getSlopeGradient(level, minX + x, minZ + z);
            }
        }

        return new TerrainSlopeMD(slopemap);
    }

    protected static float getSlopeGradient(Level level, int x, int z) {
        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

        float nearSlopeTotal = 0;
        float nearSlopeCount = 0;
        float farSlopeTotal = 0;
        float farSlopeCount = 0;

        // Slope direction is relative to North West/top left of the map as that's the direction our
        // "sunlight" is going to be coming from. +x == East, +z == South.
        // Positive values means in shadow, negative values means in light.
        for (int dx = -2; dx <= 0; dx++) {
            for (int dz = -2; dz <= 0; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                } else if (dx == -2 && dz == -2) {
                    continue;
                }

                if (dx >= -1 && dz >= -1) {
                    // Adjacent block
                    int nearSlope = getRelativeSlope(level, x, z, height, dx, dz, true);

                    if (nearSlope < 0) {
                        nearSlopeTotal += nearSlope;
                        nearSlopeCount += 1 - (0.4 * nearSlopeCount);
                    } else if (nearSlope > 0) {
                        // Shadows are weighted more heavily than sunlight
                        nearSlopeTotal += 4 * nearSlope;
                        nearSlopeCount += 4 - (0.5 * nearSlopeCount);
                    }

                } else if (dx >= -2 && dz >= -2) {
                    // Two blocks away
                    int farSlope = getRelativeSlope(level, x, z, height, dx, dz, false);

                    if (farSlope < -2) {
                        farSlopeTotal += farSlope;
                        farSlopeCount += 1 - (0.4 * farSlopeCount);
                    } else if (farSlope > 2) {
                        // Shadows are weighted more heavily than sunlight
                        farSlopeTotal += 4 * farSlope;
                        farSlopeCount += 4 - (0.5 * farSlopeCount);
                    }
                }
            }
        }

        return (nearSlopeCount != 0 ? nearSlopeTotal / nearSlopeCount : 0) +
                (farSlopeCount != 0 ? farSlopeTotal / farSlopeCount : 0) / 2;
    }

    protected static int getRelativeSlope(Level level, int x, int z, int height, int dx, int dz, boolean isPrimaryShadow) {
        int adjacentBlockHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x + dx, z + dz);

        int relativeSlope = adjacentBlockHeight - height;

        if (relativeSlope == 0) {
            // No shading changes
            return 0;

        } else if (relativeSlope > 0) {
            // Add shadow
            return relativeSlope;

        } else {
            int oppositeBlockHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x - dx, z - dz);

            if (
                (isPrimaryShadow && oppositeBlockHeight < height) ||
                (!isPrimaryShadow && oppositeBlockHeight <= height)) {
                // At the top of a slope
                return 0;
            }

            // Add sunlight
            return relativeSlope;
        }
    }
}
