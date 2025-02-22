package com.eerussianguy.blazemap.feature.mapping;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.TerrainSlopeMD;
import com.eerussianguy.blazemap.api.pipeline.Collector;
import com.eerussianguy.blazemap.lib.Transparency;
import com.eerussianguy.blazemap.lib.Transparency.CompositionState;
import com.eerussianguy.blazemap.lib.Transparency.TransparencyState;
import com.eerussianguy.blazemap.lib.Transparency.BlockComposition;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class TerrainSlopeCollector extends Collector<TerrainSlopeMD> {
    public TerrainSlopeCollector() {
        super(
            BlazeMapReferences.Collectors.TERRAIN_SLOPE,
            BlazeMapReferences.MasterData.TERRAIN_SLOPE
        );
    }

    record Height(int highestHeight, int lowestHeight, float totalOpacity) {}

    // For when a one-off BlockPos is needed, to prevent creating more new objects than necessary
    // ThreadLocal to prevent accidental "cross-contamination"
    private static ThreadLocal<MutableBlockPos> miscReusablePos = ThreadLocal.withInitial(() -> new MutableBlockPos(0, 0, 0));
    private static float OPACITY_MIN = 0.25f;

    @Override
    public TerrainSlopeMD collect(Level level, int minX, int minZ, int maxX, int maxZ) {
        // For the 16 x 16 chunk + 2 blocks on either side
        // TODO: Once BME-198 completed, can remove calls into neighbouring chunks
        final Height[][] heightmap = new Height[20][20];
        final int[][] highestHeightmap = new int[20][20];
        final int[][] lowestHeightmap = new int[20][20];
        final float[][] opacityMap = new float[20][20];

        for(int z = -2; z < 18; z++) {
            for(int x = -2; x < 18; x++) {
                heightmap[z + 2][x + 2] = getHeight(level, minX + x, minZ + z, POS);
                highestHeightmap[z + 2][x + 2] = heightmap[z + 2][x + 2].highestHeight;
                lowestHeightmap[z + 2][x + 2] = heightmap[z + 2][x + 2].lowestHeight;
                opacityMap[z + 2][x + 2] = heightmap[z + 2][x + 2].totalOpacity;
            }
        }

        // The below should be possible to be moved off-thread to a Transformer later
        final float[][] slopemap = processSlopeData(highestHeightmap, lowestHeightmap, opacityMap, level, minX, minZ);

        return new TerrainSlopeMD(slopemap);
    }


    protected static Height getHeight(Level level, int x, int z, MutableBlockPos blockPos) {
        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
        int highestHeight = height;

        blockPos.set(x, height, z);
        boolean isTransparent = false;
        float transparency = 1;

        BlockState state = level.getBlockState(blockPos);
        BlockComposition blockComposition = Transparency.getBlockComposition(state, level, blockPos);

        while (height > level.getMinBuildHeight()
            && (
                TransparencyState.isAtLeastAsTransparentAs(
                    blockComposition.getTransparencyState(),
                    TransparencyState.SEMI_TRANSPARENT
                )
                // TODO: Make work better with bamboo
                || blockComposition.getBlockCompositionState() == CompositionState.NON_FULL_BLOCK
                )
        ) {
            isTransparent = true;
            transparency = transparency * blockComposition.getTransparencyState().transparency;

            height--;
            state = level.getBlockState(blockPos.move(Direction.DOWN));
            blockComposition = Transparency.getBlockComposition(state, level, blockPos);
        }

        float opacity = isTransparent ? 1 - transparency : 1;

        return new Height(highestHeight, height, opacity);
    }


    protected static float[][] processSlopeData(
        int[][] highestHeightmap,
        int[][] lowestHeightmap,
        float[][] opacityMap,
        Level level,
        int minX, int minZ
    ) {
        final float[][] slopemap = new float[16][16];

        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                int xOffset = x + 2;
                int zOffset = z + 2;

                float topSlope = getSlopeGradient(
                    highestHeightmap,
                    highestHeightmap,
                    opacityMap,
                    level,
                    x, z,
                    minX, minZ
                );

                if (highestHeightmap[zOffset][xOffset] != lowestHeightmap[zOffset][xOffset]) {
                    float baseSlope = getSlopeGradient(
                        lowestHeightmap,
                        lowestHeightmap,
                        null,
                        level,
                        x, z,
                        minX, minZ
                    );

                    int depth = highestHeightmap[zOffset][xOffset] - lowestHeightmap[zOffset][xOffset];

                    // Note: 1/(16^2) = 0.00390625
                    float point = Math.max(1 - 0.00390625f * (depth * depth), 0);

                    // Direct attenuation from transparent object above
                    baseSlope = baseSlope * (1 - Math.max(opacityMap[zOffset][xOffset], OPACITY_MIN));
                    // Extra attenuation from light scatter within transparent blocks
                    baseSlope = baseSlope > 0 ?
                        Math.min(Math.min(baseSlope, point), 0.25f) : // shadow
                        Math.max(Math.max(baseSlope, -(point * point)), -0.3125f); // sunlight (Note: -0.3125 = -5/16)

                    // Sunlight and shadows on a transparent surface aren't as strong as on an opaque one
                    topSlope = topSlope * Math.max(opacityMap[zOffset][xOffset], OPACITY_MIN);

                    // Combine the two cases
                    slopemap[z][x] = baseSlope + topSlope;

                } else {
                    slopemap[z][x] = topSlope;
                }

            }
        }

        return slopemap;
    }


    protected static float getSlopeGradient(
        int[][] thisHeightmap,
        int[][] adjacentHeightmap,
        float[][] opacityMap,
        Level level,
        int x, int z,
        int minX, int minZ
    ) {
        int xOffset = x + 2;
        int zOffset = z + 2;

        float nearSlopeTotal = 0;
        float nearSlopeCount = 0;
        float farSlopeTotal = 0;
        float farSlopeCount = 0;

        // Slope direction is relative to North West/top left of the map as that's the direction our
        // "sunlight" is going to be coming from. +x == East, +z == South.
        // Positive values means in shadow, negative values means in light.
        for (int dz = -2; dz <= 0; dz++) {
            for (int dx = -2; dx <= 0; dx++) {
                if (dx == 0 && dz == 0) {
                    continue;
                } else if (dx == -2 && dz == -2) {
                    continue;
                }

                if (dx >= -1 && dz >= -1) {
                    // Adjacent block
                    int nearSlope = getRelativeSlope(
                        thisHeightmap[zOffset][xOffset],
                        adjacentHeightmap[zOffset + dz][xOffset + dx],
                        adjacentHeightmap[zOffset - dz][xOffset - dx],
                        level,
                        x + minX, z + minZ,
                        dx, dz,
                        true);

                    if (nearSlope < 0) {
                        nearSlopeTotal += nearSlope;
                        nearSlopeCount += 1 - (0.4 * nearSlopeCount);
                    } else if (nearSlope > 0) {
                        // Shadows are weighted more heavily than sunlight
                        nearSlopeTotal += 4 * nearSlope * (
                            opacityMap != null ?
                                Math.max(opacityMap[zOffset + dz][xOffset + dx], OPACITY_MIN) :
                                1
                        );
                        nearSlopeCount += 4 - (0.5 * nearSlopeCount);
                    }

                } else if (dx >= -2 && dz >= -2) {
                    // Two blocks away
                    int farSlope = getRelativeSlope(
                        thisHeightmap[zOffset][xOffset],
                        adjacentHeightmap[zOffset + dz][xOffset + dx],
                        adjacentHeightmap[zOffset - dz][xOffset - dx],
                        level,
                        x + minX, z + minZ,
                        dx, dz,
                        false);

                    if (farSlope < -2) {
                        farSlopeTotal += farSlope;
                        farSlopeCount += 1 - (0.4 * farSlopeCount);
                    } else if (farSlope > 2) {
                        // Shadows are weighted more heavily than sunlight
                        farSlopeTotal += 4 * farSlope * (
                            opacityMap != null ?
                                Math.max(opacityMap[zOffset + dz][xOffset + dx], OPACITY_MIN) :
                                1
                        );
                        farSlopeCount += 4 - (0.5 * farSlopeCount);
                    }
                }
            }
        }

        return (nearSlopeCount != 0 ? nearSlopeTotal / nearSlopeCount : 0) +
                (farSlopeCount != 0 ? farSlopeTotal / farSlopeCount : 0) / 2;
    }


    protected static int getRelativeSlope(
        int blockHeight,
        int adjacentHeight,
        int oppositeHeight,
        Level level,
        int x, int z,
        int dx, int dz,
        boolean isPrimaryShadow
    ) {
        int relativeSlope = adjacentHeight - blockHeight;

        if (adjacentHeight <= level.getMinBuildHeight() && level.getBlockState(miscReusablePos.get().set(x + dx, adjacentHeight, z + dz)).isAir()) {
            // This block is in an unloaded chunk and can't be processed until BME-47 is dealt with.
            // (Alternatively, somebody's broken through to the void, but that's their own fault!)
            return 0;

        } else if (relativeSlope == 0) {
            // No shading changes
            return 0;

        } else if (relativeSlope > 0) {
            // Add shadow
            return relativeSlope;

        } else if (
            (isPrimaryShadow && oppositeHeight < blockHeight) ||
            (!isPrimaryShadow && oppositeHeight <= blockHeight)) {
            // At the top of a slope
            return 0;

        } else {
            // Add sunlight
            return relativeSlope;
        }
    }
}
