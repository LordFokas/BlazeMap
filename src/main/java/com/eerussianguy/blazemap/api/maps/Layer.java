package com.eerussianguy.blazemap.api.maps;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.pipeline.Consumer;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.pipeline.MasterDatum;
import com.eerussianguy.blazemap.api.util.DataSource;
import com.mojang.blaze3d.platform.NativeImage;

/**
 * In Blaze Map, maps are composed of several layers superimposed on each others.
 * Layers read one or more MasterDatum objects previously collected by Collectors and use it
 * to generate a layer image for the respective chunk. These images are later stitched together
 * by the engine to generate a layer image for the whole region (LayerRegionTile).
 *
 * All operations are thread-safe by default (read data, paint image) and are executed in parallel
 * in the engine's background threads. Layers are meant exclusively to generate map tiles, for other
 * forms of data processing and analysis please use a Processor instead.
 *
 * @author LordFokas
 */
public abstract class Layer extends NamedMapComponent<Layer> implements Consumer {
    protected static final int OPAQUE = 0xFF000000;

    private final Set<Key<DataType<MasterDatum>>> inputs;
    private final boolean opaque;
    public final Type type;

    @SafeVarargs
    public Layer(Key<Layer> id, TranslatableComponent name, ResourceLocation icon, boolean opaque, Key<DataType<MasterDatum>>... inputs) {
        this(id, Type.PHYSICAL, name, icon, opaque, inputs);
    }

    @SafeVarargs
    Layer(Key<Layer> id, Type type, TranslatableComponent name, ResourceLocation icon, boolean opaque, Key<DataType<MasterDatum>>... inputs) {
        super(id, name, icon);
        this.type = type;
        if(type.isPipelined) {
            if(inputs == null || inputs.length == 0) throw new IllegalArgumentException("Pipelined Layers must have non-zero input MD list");
        } else {
            if(inputs != null && inputs.length > 0) throw new IllegalArgumentException("Non-Pipelined Layers must not have input MDs");
        }
        this.inputs = Arrays.stream(inputs).collect(Collectors.toUnmodifiableSet());
        this.opaque = opaque;
    }

    @Override
    public Set<Key<DataType<MasterDatum>>> getInputIDs() {
        return inputs;
    }

    /**
     * isOpaque (alias for isBottomLayer) refers to the fact that bottom layers are opaque, and face different restrictions.
     * 1 - opaque layers can only be the 1st layer in the map
     * 1.1 - map therefore can only have 1 opaque layer
     * 2 - layers in index >= 1 cannot be opaque
     * 3 - opaque layers cannot be disabled (they are the map background)
     */
    public final boolean isOpaque() {
        return opaque;
    }

    /** Alias for isOpaque */
    public final boolean isBottomLayer() {
        return opaque;
    }

    public abstract boolean renderTile(NativeImage tile, TileResolution resolution, DataSource data, int xGridOffset, int zGridOffset);

    /**
     * Used by the World Map (fullscreen map) to display a legend somewhere in the screen (at the layout's discretion)
     * The renderable will be asked to render at its own 0,0 and the height and width are expected to be constant.
     *
     * This currently only applies to opaque (bottom) layers, which are the first layer of the current map type,
     * however not all such layers must have one and returning null is the default action.
     */
    public Renderable getLegendWidget() {
        return null;
    }

    /**
     * Determines the capabilities and limitations of layers.
     */
    public enum Type {
        PHYSICAL(true, true),
        SYNTHETIC(false, true),
        INVISIBLE(false, false);

        /** Whether this layer runs through the pipeline and saves to disk */
        public final boolean isPipelined;

        /** Whether this layer has pixels to display */
        public final boolean isVisible;

        Type(boolean isPipelined, boolean isVisible) {
            this.isPipelined = isPipelined;
            this.isVisible = isVisible;
        }
    }


    // =================================================================================================================
    // Common helper functions for easier layer rendering

    /**
     * Allows to run code once for each pixel of the chunk tile.
     * Automatically accounts for the fact chunk tile sizes vary with resolution.
     */
    protected static void foreachPixel(TileResolution resolution, PixelConsumer consumer) {
        // Note: It's more efficient overall to run the loops this way around with x as the inner loop.
        // This is for the same reason the data format is expected to be `data[z][x]` down below.
        // Namely, that the PNG tiles are constructed with the whole first row of x values read first,
        // then the whole second row of x values, etc.
        // The CPU cache can better optimise its memory fetches if they're accessed in order.
        for(int z = 0; z < resolution.chunkWidth; z++) {
            for(int x = 0; x < resolution.chunkWidth; x++) {
                consumer.accept(x, z);
            }
        }
    }

    @FunctionalInterface
    protected interface PixelConsumer {
        void accept(int x, int y);
    }

    /**
     * When running at lower resolutions than FULL, this utility allows to collect all data points that will be rendered
     * into a single pixel. Meant to be used with ArrayAggregator or similar utilities, to aggregate the multiple data
     * points into a single value.
     *
     * Note: Data is expected to be in the format `data[z][x]` for CPU cache fetch optimisation reasons.
     */
    protected static int[] relevantData(TileResolution resolution, int x, int z, int[][] data) {
        int[] objects = new int[resolution.pixelWidth * resolution.pixelWidth];
        x *= resolution.pixelWidth;
        z *= resolution.pixelWidth;
        int idx = 0;

        for(int dz = 0; dz < resolution.pixelWidth; dz++) {
            for(int dx = 0; dx < resolution.pixelWidth; dx++) {
                objects[idx++] = data[dz + z][dx + x];
            }
        }

        return objects;
    }

    /**
     * When running at lower resolutions than FULL, this utility allows to collect all data points that will be rendered
     * into a single pixel. Meant to be used with ArrayAggregator or similar utilities, to aggregate the multiple data
     * points into a single value.
     * 
     * Note: Data is expected to be in the format `data[z][x]` for CPU cache fetch optimisation reasons.
     */
    protected static float[] relevantData(TileResolution resolution, int x, int z, float[][] data) {
        float[] objects = new float[resolution.pixelWidth * resolution.pixelWidth];
        x *= resolution.pixelWidth;
        z *= resolution.pixelWidth;
        int idx = 0;

        for(int dz = 0; dz < resolution.pixelWidth; dz++) {
            for(int dx = 0; dx < resolution.pixelWidth; dx++) {
                objects[idx++] = data[dz + z][dx + x];
            }
        }

        return objects;
    }

    /**
     * When running at lower resolutions than FULL, this utility allows to collect all data points that will be rendered
     * into a single pixel. Meant to be used with ArrayAggregator or similar utilities, to aggregate the multiple data
     * points into a single value.
     * 
     * Note: Data is expected to be in the format `data[z][x]` for CPU cache fetch optimisation reasons.
     */
    @SuppressWarnings("unchecked")
    protected static <T> T[] relevantData(TileResolution resolution, int x, int z, T[][] data, Class<T> cls) {
        T[] objects = (T[]) Array.newInstance(cls, resolution.pixelWidth * resolution.pixelWidth);
        x *= resolution.pixelWidth;
        z *= resolution.pixelWidth;
        int idx = 0;

        for(int dz = 0; dz < resolution.pixelWidth; dz++) {
            for(int dx = 0; dx < resolution.pixelWidth; dx++) {
                objects[idx++] = data[dz + z][dx + x];
            }
        }

        return objects;
    }
}
