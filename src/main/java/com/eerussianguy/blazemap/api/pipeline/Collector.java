package com.eerussianguy.blazemap.api.pipeline;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.BlazeRegistry.RegistryEntry;
import com.eerussianguy.blazemap.lib.Transparency;
import com.eerussianguy.blazemap.lib.Transparency.CompositionState;

/**
 * Collectors collect MasterData from chunks that need updating to be processed later.
 * This operation is executed synchronously in the main game thread.
 *
 * MasterData is consumed by Layers and Processors asynchronously in the data crunching threads.
 *
 * @author LordFokas
 */
public abstract class Collector<T extends MasterDatum> implements RegistryEntry, PipelineComponent, Producer {
    protected static final BlockPos.MutableBlockPos POS = new BlockPos.MutableBlockPos();
    protected final Key<Collector<MasterDatum>> id;
    protected final Key<DataType<MasterDatum>> output;

    public Collector(Key<Collector<MasterDatum>> id, Key<DataType<MasterDatum>> output) {
        this.id = id;
        this.output = output;
    }

    public Key<Collector<MasterDatum>> getID() {
        return id;
    }

    public abstract T collect(Level level, int minX, int minZ, int maxX, int maxZ);

    @Override
    public Key<DataType<MasterDatum>> getOutputID() {
        return output;
    }

    protected static boolean isSolid(Level level, int x, int y, int z) {
        BlockState state = level.getBlockState(POS.set(x, y, z));
        return isSolid(level, state);
    }
    protected static boolean isSolid(Level level, BlockState state) {
        CompositionState composition = Transparency.getBlockComposition(state, level, POS).getBlockCompositionState();
        return !state.getMaterial().isReplaceable() && (composition == CompositionState.BLOCK || composition == CompositionState.FLUIDLOGGED_BLOCK);
    }

    protected static boolean isWater(Level level, int x, int y, int z) {
        BlockState state = level.getBlockState(POS.set(x, y, z));
        return isWater(state);
    }
    protected static boolean isWater(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER);
    }

    protected static boolean isFluid(Level level, int x, int y, int z) {
        BlockState state = level.getBlockState(POS.set(x, y, z));
        return isFluid(state);
    }
    protected static boolean isFluid(BlockState state) {
        return !state.getFluidState().isEmpty();
    }

    protected static boolean isLeavesOrReplaceable(Level level, int x, int y, int z) {
        BlockState state = level.getBlockState(POS.set(x, y, z));
        return isLeavesOrReplaceable(state);
    }
    protected static boolean isLeavesOrReplaceable(BlockState state) {
        // TODO: For 1.20, re-add: state.getBlock() instanceof MangroveRootsBlock || state.canBeReplaced()
        return state.isAir() || state.is(BlockTags.LEAVES) || state.getMaterial().isReplaceable() || state.canBeReplaced(Fluids.WATER) ;
    }

    protected static boolean isSkippableAfterLeaves(Level level, int x, int y, int z) {
        BlockState state = level.getBlockState(POS.set(x, y, z));
        return isSkippableAfterLeaves(state);
    }
    protected static boolean isSkippableAfterLeaves(BlockState state) {
        return state.is(BlockTags.LOGS) || isLeavesOrReplaceable(state);
    }

    protected static int findSurfaceBelowVegetation(Level level, int x, int z, boolean stopAtFluid) {
        final int minBuildHeight = level.getMinBuildHeight();

        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
        BlockState state = level.getBlockState(POS.set(x, height, z));

        boolean foundLeaves = false;

        if (stopAtFluid) {
            while(isLeavesOrReplaceable(state) && !isFluid(state)) {
                height--;
                if(height <= minBuildHeight) break;
                state = level.getBlockState(POS.set(x, height, z));

                foundLeaves = true;
            }

            while(foundLeaves && isSkippableAfterLeaves(state) && !isFluid(state)) {
                height--;
                if(height <= minBuildHeight) break;
                state = level.getBlockState(POS.set(x, height, z));
            }

            while (!isSolid(level, state) && !isFluid(state)) {
                height--;
                if(height <= minBuildHeight) break;
                state = level.getBlockState(POS.set(x, height, z));
            }

        } else {
            while(isLeavesOrReplaceable(state)) {
                height--;
                if(height <= minBuildHeight) break;
                state = level.getBlockState(POS.set(x, height, z));

                foundLeaves = true;
            }

            while(foundLeaves && isSkippableAfterLeaves(state)) {
                height--;
                if(height <= minBuildHeight) break;
                state = level.getBlockState(POS.set(x, height, z));
            }

            while (!isSolid(level, state)) {
                height--;
                if(height <= minBuildHeight) break;
                state = level.getBlockState(POS.set(x, height, z));
            }
        }

        return height;
    }
}
