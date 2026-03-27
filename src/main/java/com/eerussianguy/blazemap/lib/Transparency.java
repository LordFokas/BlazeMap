package com.eerussianguy.blazemap.lib;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.shapes.VoxelShape;

// TODO: Check if comment needs updating post refactor
/**
 * Minecraft doesn't have an easy way to tell if a block will render with transparency or not
 * based on the block data itself. There seems to be _no_ way to do this on the server side based
 * on the information Minecraft gives us alone. Thus, it's up to us to determine which blocks
 * should be considered "transparent" for mapping purposes.
 * 
 * The following 6 sets allow us, plus other mods via API, to decide which blocks should be
 * considered transparent by BM, and which should not. This can be determined based on a block's
 * class (including superclasses), its block tags, or its fluid tags. Add the class or tag to the
 * appropriate list for it to count.
 * 
 * The default transparency is quite translucent, blocking most of what's underneath but still
 * giving hints of what's below. This is for blocks like ice which aren't entirely see-through but
 * still transmit some light. For the minority of blocks that are properly clear like stained glass,
 * you'll want to also add them to the "quite transparent" lists. This will make them show a lot
 * more of what's underneath including contour shadows.
 * 
 * In practice, a block is expected to pass the `isTransparent` check if it's also expected to pass
 * the `isQuiteTransparent` check. However, each check doesn't need to pass for the same reason.
 * For example: Stained glass passes the "isTransparent" check because it subclasses `HalfTransparentBlock`.
 * But it passes the "isQuiteTransparent" check because it subclasses `AbstractGlassBlock`, which
 * is a subclass of `HalfTransparentBlock`. There is no reason to check it's a `HalfTransparentBlock`
 * again if we've already checked that it's an `AbstractGlassBlock`.
 */
public class Transparency {
    public static final float OPACITY_LOW = 0.1875f; // 3/16ths
    public static final float OPACITY_HIGH = 0.875f; // 7/8ths

    // TODO: See if I need to make these threadsafe datastructures
    private static final SortedSet<TransparencyMapping> transparentBlockTypes = initialiseTransparentBlocks();
    private static final SortedSet<TransparencyMapping> transparentFluidTypes = initialiseTransparentFluids();

    private static final Map<BlockState, BlockComposition> knownBlocks = new ConcurrentHashMap<>();

    public enum TransparencyState {
        AIR(0f),
        QUITE_TRANSPARENT(OPACITY_LOW),
        HALF_TRANSPARENT(0.5f),
        SLIGHTLY_TRANSPARENT(OPACITY_HIGH),
        OPAQUE(1f),
        ;

        /** The value used for RGB calculations.
         * 0 = fully see through, 1 = fully light blocking */
        public final float opacity;
        /** The inverse of opacity (here only as syntactic sugar) */
        public final float transparency;

        private TransparencyState(float opacity) {
            this.opacity = opacity;
            this.transparency = 1 - opacity;
        }

        public static TransparencyState max(TransparencyState t1, TransparencyState t2) {
            if (t1.opacity > t2.opacity) return t1;
            return t2;
        }

        public static TransparencyState min(TransparencyState t1, TransparencyState t2) {
            if (t1.opacity < t2.opacity) return t1;
            return t2;
        }

        public static boolean isAtLeastAsTransparentAs(TransparencyState t1, TransparencyState t2) {
            return t1.opacity <= t2.opacity;
        }
    }

    public enum CompositionState {
        BLOCK,                  // Eg: Stone, Slab (looks solid from the sky)
        NON_FULL_BLOCK,         // Eg: Torch, Iron Bar, Door
        FLUIDLOGGED_BLOCK,      // Eg: Waterlogged Enchanting Table
        FLUIDLOGGED_NON_FULL,   // Eg: Waterlogged Seagrass
        FLUID,                  // Eg: Water, Lava
        AIR,                    // Should only represent air blocks
    }


    /** 
     * Default transparency mappings for blocks + block entities
     * 
     * All default mappings have priorities < 0, so those added by API without providing a priority (and defaulting to 0)
     * will always override the defaults provided by Blaze Map
     */
    private static final SortedSet<TransparencyMapping> initialiseTransparentBlocks() {
        SortedSet<TransparencyMapping> transparentBlockTypesSet = new TreeSet<TransparencyMapping>();

        transparentBlockTypesSet.add(new TransparencyClassMapping(
            HalfTransparentBlock.class, TransparencyState.SLIGHTLY_TRANSPARENT, -2
        ));
        transparentBlockTypesSet.add(new TransparencyClassMapping(
            AbstractGlassBlock.class, TransparencyState.QUITE_TRANSPARENT, -1
        ));

        transparentBlockTypesSet.add(new TransparencyClassMapping(
            BushBlock.class, TransparencyState.HALF_TRANSPARENT, -1
        ));

        // Currently, there are no default TransparencyBlockTagMapping to add. 
        // But if there were, they would go here.

        return transparentBlockTypesSet;
    }

    /** 
     * Default transparency mappings for fluids.
     * 
     * All default mappings have priorities < 0, so those added by API without providing a priority (and defaulting to 0)
     * will always override the defaults provided by Blaze Map
     */
    private static final SortedSet<TransparencyMapping> initialiseTransparentFluids () {
        SortedSet<TransparencyMapping> transparentFluidTypesSet = new TreeSet<TransparencyMapping>();

        transparentFluidTypesSet.add(new TransparencyFluidTagMapping(
            FluidTags.WATER, TransparencyState.QUITE_TRANSPARENT, -1
        ));

        return transparentFluidTypesSet;
    }

    public static TransparencyState getBlockTransparencyState(BlockState testBlockState) {
        // The natural sorting of transparentBlockTypes will mean this is checked from
        // highest priority to lowest
        for (TransparencyMapping blockMapping : transparentBlockTypes) {
            if (blockMapping.appliesTo(testBlockState)) {
                return blockMapping.transparency;
            }
        }

        // No transparency mappings match, so must be considered opaque
        return TransparencyState.OPAQUE;
    }

    public static TransparencyState getFluidTransparencyState(BlockState testBlockState) {
        // The natural sorting of transparentFluidTypes will mean this is checked from
        // highest priority to lowest
        for (TransparencyMapping blockMapping : transparentFluidTypes) {
            if (blockMapping.appliesTo(testBlockState)) {
                return blockMapping.transparency;
            }
        }

        // No transparency mappings match, so must be considered opaque
        return TransparencyState.OPAQUE;
    }

    public static BlockComposition getBlockComposition(BlockState state, Level level, BlockPos pos) {
        // The Level and BlockPos shouldn't actually matter to the final result
        // but are required by Mojang to get the BlockState's shape
        return knownBlocks.computeIfAbsent(state, (s) -> {
            return new BlockComposition(s, level, pos);
        });
    }


    // External API for other mods to mark their own blocks as transparent.
    // TODO: Should probably make a custom blocktag at some point specifically for folks to apply to their
    // blocks rather than having block identifiers added to our list, but that's a future task for when people
    // outside the BME project care enough to actually proactively make their mods work better with BME

    /** Add a new block class to mark it as transparent */
    public static void addTransparentBlockClass(Class<?> block, TransparencyState transparencyLevel) { addTransparentBlockClass(block, transparencyLevel, 0); };
    public static void addTransparentBlockClass(Class<?> block, TransparencyState transparencyLevel, int priority) { 
        transparentBlockTypes.add(new TransparencyClassMapping(block, transparencyLevel, priority));
    };

    /** Add a new block tag to mark it as transparent */
    public static void addTransparentBlockTag(TagKey<Block> block, TransparencyState transparencyLevel) { addTransparentBlockTag(block, transparencyLevel, 0); };
    public static void addTransparentBlockTag(TagKey<Block> block, TransparencyState transparencyLevel, int priority) { 
        transparentBlockTypes.add(new TransparencyBlockTagMapping(block, transparencyLevel, priority));
    };

    /** Add a new fluid tag to mark it as transparent */
    public static void addTransparentFluidTag(TagKey<Fluid> fluid, TransparencyState transparencyLevel) { addTransparentFluidTag(fluid, transparencyLevel, 0); };
    public static void addTransparentFluidTag(TagKey<Fluid> fluid, TransparencyState transparencyLevel, int priority) { 
        transparentBlockTypes.add(new TransparencyFluidTagMapping(fluid, transparencyLevel, priority));
    };

    private static abstract class TransparencyMapping implements Comparable<TransparencyMapping> {
        public static int mappingCount = 0;

        public final TransparencyState transparency;
        public final int priority;
        public final int orderAdded; // Tie breaker for priority

        public TransparencyMapping(TransparencyState transparency, int priority) {
            this.transparency = transparency;
            this.priority = priority;

            this.orderAdded = TransparencyMapping.mappingCount;
            TransparencyMapping.mappingCount++;
        }

        /**
         * Check if this mapping entry applies to the provided BlockState
         */
        public abstract boolean appliesTo(BlockState testBlockState);

        // This will return the highest priority mapping first, so we can always use the first hit
        public int compareTo(TransparencyMapping other) {
            int priorityCompare = other.priority - this.priority;

            if (priorityCompare == 0) {
                // Priority the same. Use order added as a tie breaker for consistency
                // (mappings added later override those added earlier)
                return other.orderAdded - this.orderAdded;
            }

            return priorityCompare;
        }
    }

    private static class TransparencyClassMapping extends TransparencyMapping {
        public final Class<?> mappedClass;

        public TransparencyClassMapping(Class<?> mappedClass, TransparencyState transparency, int priority) {
            super(transparency, priority);
            this.mappedClass = mappedClass;
        }

        public boolean appliesTo(BlockState testBlockState) {
            return this.mappedClass.isAssignableFrom(testBlockState.getBlock().getClass());
        }
    }

    private static class TransparencyBlockTagMapping extends TransparencyMapping {
        public final TagKey<Block> blockTag;

        public TransparencyBlockTagMapping(TagKey<Block> blockTag, TransparencyState transparency, int priority) {
            super(transparency, priority);
            this.blockTag = blockTag;
        }

        public boolean appliesTo(BlockState testBlockState) {
            return testBlockState.is(blockTag);
        }
    }

    private static class TransparencyFluidTagMapping extends TransparencyMapping {
        public final TagKey<Fluid> fluidTag;

        public TransparencyFluidTagMapping(TagKey<Fluid> fluidTag, TransparencyState transparency, int priority) {
            super(transparency, priority);
            this.fluidTag = fluidTag;
        }

        public boolean appliesTo(BlockState testBlockState) {
            return testBlockState.getFluidState().is(fluidTag);
        }
    }

    public static class BlockComposition {
        public final TransparencyState totalTransparencyLevel;
        public final TransparencyState blockTransparencyLevel;
        public final TransparencyState fluidTransparencyLevel;
        public final CompositionState compositionState;

        public BlockComposition(BlockState state, Level level, BlockPos pos) {
            // Short circuit on air block.
            if (state.isAir()) {
                this.blockTransparencyLevel = TransparencyState.AIR;
                this.fluidTransparencyLevel = TransparencyState.AIR;
                this.totalTransparencyLevel = TransparencyState.AIR;
                this.compositionState = CompositionState.AIR;
                return;
            }

            // Get shape of block to know what it occludes
            VoxelShape blockShape = state.getOcclusionShape(level, pos);
            boolean isBlockEmpty = blockShape.isEmpty();

            // Set base transparency levels
            if (isBlockEmpty) {
                this.blockTransparencyLevel = TransparencyState.AIR;
            } else {
                this.blockTransparencyLevel = getBlockTransparencyState(state);
            }

            if (state.getFluidState().isEmpty()) {
                this.fluidTransparencyLevel = TransparencyState.AIR;
            } else {
                this.fluidTransparencyLevel = getFluidTransparencyState(state);
            }

            // Find overall transparency state based on block shape
            if (isBlockEmpty) {
                // Block contains only fluid
                this.compositionState = CompositionState.FLUID;
                this.totalTransparencyLevel = fluidTransparencyLevel;

            } else if (Block.isFaceFull(blockShape, Direction.UP)) {
                // Normal block transparency rules
                this.compositionState = CompositionState.BLOCK;
                this.totalTransparencyLevel = blockTransparencyLevel;
    
            } else if (Block.isFaceFull(blockShape, Direction.DOWN)) {
                if (fluidTransparencyLevel == TransparencyState.AIR) {
                    // Normal block transparency rules
                    this.compositionState = CompositionState.BLOCK;
                    this.totalTransparencyLevel = blockTransparencyLevel;

                } else {
                    // Filter block color through fluid colour based on fluid transparency rules.
                    this.compositionState = CompositionState.FLUIDLOGGED_BLOCK;
    
                    // Total opacity based on highest opacity between fluid and solid.
                    this.totalTransparencyLevel = TransparencyState.max(blockTransparencyLevel, fluidTransparencyLevel);
                }

            } else { 
                // Not a solid block

                if (fluidTransparencyLevel == TransparencyState.AIR) {
                    // Normal block transparency rules, but can be at most semi-transparent due to
                    // light traveling through the gaps
                    this.compositionState = CompositionState.NON_FULL_BLOCK;
                    this.totalTransparencyLevel = TransparencyState.min(blockTransparencyLevel, TransparencyState.SLIGHTLY_TRANSPARENT);

                } else {
                    // Filter block color through fluid colour based on fluid transparency rules.
                    this.compositionState = CompositionState.FLUIDLOGGED_NON_FULL;
        
                    // Total opacity based on fluid opacity.
                    // Can only cross "quite transparent" threshold if block also quite transparent
                    // (otherwise partial block is considering "blocking too much of the light")
                    this.totalTransparencyLevel = TransparencyState.max(
                        fluidTransparencyLevel, 
                        TransparencyState.min(blockTransparencyLevel, TransparencyState.SLIGHTLY_TRANSPARENT)
                    );
                }
            }
        }

        public TransparencyState getTransparencyState() { return totalTransparencyLevel; }
        public CompositionState getBlockCompositionState() { return compositionState; }
    }

}
