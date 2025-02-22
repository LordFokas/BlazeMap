package com.eerussianguy.blazemap.feature.mapping;

import java.util.function.IntFunction;

import java.util.Queue;
import java.util.LinkedList;
import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.KelpPlantBlock;
import net.minecraft.world.level.block.SeagrassBlock;
import net.minecraft.world.level.block.TallSeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MaterialColor;

import net.minecraftforge.client.model.data.EmptyModelData;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.BlockColorMD;
import com.eerussianguy.blazemap.api.pipeline.ClientOnlyCollector;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.Transparency;
import com.eerussianguy.blazemap.lib.Transparency.TransparencyState;
import com.eerussianguy.blazemap.lib.Transparency.BlockComposition;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;

public class BlockColorCollector extends ClientOnlyCollector<BlockColorMD> {
    private static final int TINTED_FLAG = 0xFA000000;
    private static final Object2IntArrayMap<BlockState> colors = new Object2IntArrayMap<>();
    protected static final HashMap<Integer, Float> darknessPointCache = new HashMap<Integer, Float>();

    public BlockColorCollector() {
        super(
            BlazeMapReferences.Collectors.BLOCK_COLOR,
            BlazeMapReferences.MasterData.BLOCK_COLOR
        );
    }

    @Override
    public BlockColorMD collect(Level level, int minX, int minZ, int maxX, int maxZ) {
        final int[][] colors = new int[16][16];
        final BlockColors blockColors = Minecraft.getInstance().getBlockColors();
        final MutableBlockPos blockPos = new MutableBlockPos();

        // Reusable arrays
        float[] arr1 = new float[4];
        float[] arr2 = new float[4];
        
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                int color = getColorAtMapPixel(level, blockColors, blockPos, minX + x, minZ + z, arr1, arr2);
                
                if(color > 0) {
                    colors[z][x] = color;
                }
            }
        }

        return new BlockColorMD(colors);
    }


    protected int getColorAtMapPixel(Level level, BlockColors blockColors, MutableBlockPos blockPos, int x, int z, float[] arr1, float[] arr2) {
        int color = 0;
        float[] argb = arr1;
        // Extra arrays to reuse the same memory addresses for GC's sake
        float[] spareArray = arr2;
        float[] tmpArray;

        Queue<BlockColor> transparentBlocks = new LinkedList<BlockColor>();

        for (int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                y > level.getMinBuildHeight();
                y--) {
            blockPos.set(x, y, z);
            final BlockState state = level.getBlockState(blockPos);

            if (state.isAir()) {
                continue;
            }

            BlockColor processedBlock = new BlockColor(state, level, blockPos, blockColors, transparentBlocks.isEmpty(), argb, spareArray);

            // TODO: See if this inequality is the cause of the transparency bug
            if (processedBlock.totalColor <= 0) {
                continue;
            }

            if (processedBlock.getTransparencyState() != TransparencyState.OPAQUE) {
                transparentBlocks.add(processedBlock);
                continue;
            }

            // Hasn't met any of the conditions to continue checking the blocks under it, so finalise and break
            color = processedBlock.totalColor;
            break;
        }

        if (transparentBlocks.size() > 0) {
            // TODO: Forgot to change depth while iterating through block column.
            // Will require re-tuning colours after fixing.
            int depth = transparentBlocks.size();
            argb = transparentBlocks.poll().argb(argb);

            // Top layer must be minimum this colour as it should be the easiest to see.
            // Represents extra sunlight reflecting off the surface
            argb[0] = Math.max(0.5f, argb[0]);

            while (transparentBlocks.size() > 0) {
                BlockColor transparentBlock = transparentBlocks.poll();
                tmpArray = Colors.filterARGB(argb, transparentBlock.argb(spareArray), depth);
                spareArray = argb;
                argb = tmpArray;
            }

            // The shade on the solid block at the bottom of the ocean
            color = Colors.recomposeRGBA(Colors.filterARGB(new float[] {0,0,0,0}, Colors.decomposeRGBA(color, spareArray), depth));

            int finalColor = Colors.recomposeRGBA(argb);
            color = Colors.interpolate(
                color, 0, 
                finalColor, 1, 
                Math.max(0.75f, argb[0]) // It looks silly on thinner layers if the final opacity < 0.75
            ) & 0x00FFFFFF;
        }

        return color;
    }


    /**
     * These blocks don't return accurate colours using the other methods,
     * so unfortunately need to set a colour manually
     */
    protected static int handleSpecialCases(BlockState state) {
        var block = state.getBlock();

        // By default, the colour returned for seagrass is purple, so replacing with a green picked from
        // its texture 
        if (block instanceof SeagrassBlock || block instanceof TallSeagrassBlock || block instanceof KelpPlantBlock || block instanceof KelpBlock) {
            return 0x215800;
        }

        return 0;
    }

    protected static int getColorAtPos(Level level, BlockColors blockColors, BlockState state, BlockPos blockPos) {
        // int color = handleSpecialCases(state);
        int color;

        // Get color from texture
        if(state.is(BlockTags.FLOWERS)) {
            color = getBestTexturePixel(level, state, null, BlockColorCollector::avoidGreen);
        } else {
            color = getAverageTextureColor(level, state, Direction.UP);
        }

        if((color & Colors.ALPHA) == TINTED_FLAG) {
            color = Colors.multiplyRGB(color, blockColors.getColor(state, level, blockPos, 0));
        }

        // Fallback 1: get block tint
        if(color == 0) {
            color = blockColors.getColor(state, level, blockPos, 0);
        }

        // Fallback 2: get block map color
        if(color <= 0) {
            MaterialColor mapColor = state.getMapColor(level, blockPos);
            if(mapColor != MaterialColor.NONE) {
                color = mapColor.col;
            }
        }

        return color;
    }

    private static int getAverageTextureColor(Level level, BlockState state, Direction direction) {
        return colors.computeIfAbsent(state, $ -> {
            var mc = Minecraft.getInstance();
            var model = mc.getModelManager().getModel(BlockModelShaper.stateToModelLocation(state));
            var quads = model.getQuads(state, direction, level.getRandom(), EmptyModelData.INSTANCE);

            int flag = 0;
            int r = 0, g = 0, b = 0, total = 0;

            for(var quad : quads) {
                if(quad.isTinted()) {
                    flag = TINTED_FLAG;
                }
                var texture = quad.getSprite();
                int w = texture.getWidth(), h = texture.getHeight();
                for(int x = 0; x < w; x++) {
                    for(int y = 0; y < h; y++) {
                        var pixel = Colors.decomposeRGBA(texture.getPixelRGBA(0, x, y));
                        float alpha = pixel[0];
                        if(alpha < 0.05F) continue;
                        r += (255 * pixel[3] * alpha);
                        g += (255 * pixel[2] * alpha);
                        b += (255 * pixel[1] * alpha);
                        total++;
                    }
                }
            }

            // prevent dumb math and division by zero early
            if(total == 0) return 0;

            r /= total;
            g /= total;
            b /= total;

            return flag | (r << 16) | (g << 8) | b;
        });
    }

    private static int getBestTexturePixel(Level level, BlockState state, Direction direction, IntFunction<Integer> fitness) {
        return colors.computeIfAbsent(state, $ -> {
            var mc = Minecraft.getInstance();
            var model = mc.getModelManager().getModel(BlockModelShaper.stateToModelLocation(state));
            var quads = model.getQuads(state, direction, level.getRandom(), EmptyModelData.INSTANCE);
            int pixel = 0, best = Integer.MIN_VALUE;

            for(var quad : quads) {
                var texture = quad.getSprite();
                int w = texture.getWidth(), h = texture.getHeight();
                for(int x = 0; x < w; x++) {
                    for(int y = 0; y < h; y++) {
                        int color = Colors.abgr(texture.getPixelRGBA(0, x, y));
                        int score = fitness.apply(color);
                        if(score > best) {
                            best = score;
                            pixel = color;
                        }
                    }
                }
            }

            return pixel;
        });
    }

    private static int avoidGreen(int pixel) {
        var channels = Colors.decomposeRGBA(pixel);
        float a = channels[0];
        int r = (int) (channels[1] * 1000);
        int g = (int) (channels[2] * 1000);
        int b = (int) (channels[3] * 1000);
        return (int) (a * (r + 1000-g + b));
    }


    public static class BlockColor {
        private final BlockComposition blockComposition;
        protected final int totalColor;

        private BlockColor(BlockState state, Level level, BlockPos pos, BlockColors blockColors, boolean isSurfaceBlock) {
            this(state, level, pos, blockColors, isSurfaceBlock, null, null);
        }

        private BlockColor(BlockState state, Level level, BlockPos pos, BlockColors blockColors, boolean isSurfaceBlock, float[] arr1, float[] arr2) {
            this.blockComposition = Transparency.getBlockComposition(state, level, pos);

            // Check if we have reusable arrays and create new ones if not
            if (arr1 == null || arr2 == null) {
                arr1 = new float[4];
                arr2 = new float[4];
            }

            // Get and mix the colours based on the appropriate mixing scheme
            switch (blockComposition.compositionState) {
                case BLOCK:
                case NON_FULL_BLOCK:
                    // Normal block conditions
                    this.totalColor = getColorAtPos(level, blockColors, state, pos);
                    break;

                case FLUID:
                    // Just a fluid
                    this.totalColor = getColorAtPos(level, blockColors, state, pos);
                    break;

                case FLUIDLOGGED_BLOCK:
                case FLUIDLOGGED_NON_FULL:
                    // Fluidlogged block
                    int blockColor = getColorAtPos(level, blockColors, state, pos);

                    BlockState equivalentFluidBlock = state.getFluidState().createLegacyBlock();
                    int fluidColor = getColorAtPos(level, blockColors, equivalentFluidBlock, pos);

                    float[] blockArgb = argb(blockColor, blockComposition.blockTransparencyLevel.opacity, arr1);
                    float[] fluidArgb = argb(fluidColor, blockComposition.fluidTransparencyLevel.opacity, arr2);

                    if (isSurfaceBlock) {
                        // Baseline opacity so thin fluids can still be seen
                        // Represents extra sunlight reflecting off the surface
                        fluidArgb[0] = Math.max(0.5f, fluidArgb[0]);
                    }

                    float[] totalArgb = Colors.filterARGB(fluidArgb, blockArgb, 0);
                    this.totalColor = Colors.recomposeRGBA(totalArgb) & 0x00FFFFFF;
                    break;

                default:
                    // Zero opacity
                    this.totalColor = 0x00000000;
            }
        }

        public float[] argb() {
            return argb(totalColor, blockComposition.totalTransparencyLevel.opacity);
        }
        public float[] argb(float[] arr) {
            return argb(totalColor, blockComposition.totalTransparencyLevel.opacity, arr);
        }

        protected float[] argb(int color, float opacity) {
            float[] argb = Colors.decomposeRGBA(color);
            argb[0] = opacity;
            return argb;
        }
        protected float[] argb(int color, float opacity, float[] arr) {
            float[] argb = Colors.decomposeRGBA(color, arr);
            argb[0] = opacity;
            return argb;
        }

        public TransparencyState getTransparencyState() {
            return blockComposition.getTransparencyState();
        }
    }
}
