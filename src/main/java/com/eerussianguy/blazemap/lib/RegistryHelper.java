package com.eerussianguy.blazemap.lib;

import java.util.HashMap;
import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class RegistryHelper {
    private static final HashMap<ResourceLocation, ResourceKey<Level>> DIMENSION_MAP = new HashMap<>();
    private static final ArraySet<ResourceKey<Level>> DIMENSION_SET = new ArraySet<>();

    static {
        addDimension(Level.OVERWORLD);
        addDimension(Level.NETHER);
        addDimension(Level.END);
    }

    private static void addDimension(ResourceKey<Level> dimension) {
        DIMENSION_MAP.put(dimension.location(), dimension);
        DIMENSION_SET.add(dimension);
    }

    public static ResourceKey<Level> getDimension(ResourceLocation dimension) {
        return DIMENSION_MAP.computeIfAbsent(dimension, d -> ResourceKey.create(Registry.DIMENSION_REGISTRY, d));
    }

    public static Set<ResourceKey<Level>> getAllDimensions() {
        if(DIMENSION_SET.size() < DIMENSION_MAP.size()) {
            DIMENSION_SET.addAll(DIMENSION_MAP.values());
        }
        return DIMENSION_SET;
    }
}
