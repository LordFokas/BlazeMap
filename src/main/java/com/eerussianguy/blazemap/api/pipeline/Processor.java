package com.eerussianguy.blazemap.api.pipeline;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.BlazeRegistry.RegistryEntry;
import com.eerussianguy.blazemap.api.util.IDataSource;
import com.eerussianguy.blazemap.api.util.RegionPos;
import org.jetbrains.annotations.NotNull;

/**
 * Like Layers, Processors consume one or more MasterDatum objects for a given chunk, however
 * instead of producing an image processors are free to use the data however they see fit.
 *
 * Processors are not part of the main map rendering pipeline and are instead available for addons
 * to implement more advanced features.
 *
 * Processors operate in asynchronously and in parallel in the engine's background threads. Given its
 * operations are determined by addon developers, care must be taken to ensure all external interactions
 * are made thread-safe.
 *
 * @author LordFokas
 */
public abstract class Processor implements RegistryEntry, PipelineComponent, Consumer {
    private final Key<Processor> id;
    private final Set<Key<DataType<MasterDatum>>> inputs;

    @SafeVarargs
    public Processor(Key<Processor> id, Key<DataType<MasterDatum>>... inputs) {
        this.id = id;
        this.inputs = Arrays.stream(inputs).collect(Collectors.toUnmodifiableSet());
    }

    public Key<Processor> getID() {
        return id;
    }

    @Override
    public Set<Key<DataType<MasterDatum>>> getInputIDs() {
        return inputs;
    }

    @Nonnull
    public abstract ExecutionMode getExecutionMode();

    /** Called for ExecutionMode.DIRECT, more performant */
    public abstract boolean execute(ResourceKey<Level> dimension, RegionPos region, ChunkPos chunk, IDataSource data);

    /** Called for ExecutionMode.DIFFERENTIAL, more powerful */
    public abstract boolean execute(ResourceKey<Level> dimension, RegionPos region, ChunkPos chunk, IDataSource current, IDataSource old);


    /**
     * Processor subclass set up to run in Direct mode.
     * When in doubt this is the Processor type you should extend.
     */
    public static abstract class Direct extends Processor {

        @SafeVarargs
        public Direct(Key<Processor> id, Key<DataType<MasterDatum>>... inputs) {
            super(id, inputs);
        }

        @Override @NotNull
        public final ExecutionMode getExecutionMode() {
            return ExecutionMode.DIRECT;
        }

        @Override
        public final boolean execute(ResourceKey<Level> dimension, RegionPos region, ChunkPos chunk, IDataSource current, IDataSource old) {
            return false;
        }
    }


    /**
     * Processor subclass set up to run in Differential mode.
     * This is more powerful but inflicts some performance penalties.
     * Unless you are building some very advanced features, you should never need these.
     */
    public static abstract class Differential extends Processor {

        @SafeVarargs
        public Differential(Key<Processor> id, Key<DataType<MasterDatum>>... inputs) {
            super(id, inputs);
        }

        @Override @NotNull
        public final ExecutionMode getExecutionMode() {
            return ExecutionMode.DIFFERENTIAL;
        }

        @Override
        public final boolean execute(ResourceKey<Level> dimension, RegionPos region, ChunkPos chunk, IDataSource data) {
            return false;
        }
    }
}
