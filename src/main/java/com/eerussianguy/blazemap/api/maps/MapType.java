package com.eerussianguy.blazemap.api.maps;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeRegistry;

/**
 * Each available map in Blaze Map is defined by MapType.
 * These objects do no processing of any sort and merely define a structure.
 * The provided list of layers defines what layers are rendered on the screen,
 * the first layer being rendered on the bottom and the last on top.
 *
 * @author LordFokas
 */
public abstract class MapType implements BlazeRegistry.RegistryEntry, IClientComponent {
    private final BlazeRegistry.Key<MapType> id;
    private final Set<BlazeRegistry.Key<Layer>> layers;
    private final Component name;
    private final ResourceLocation icon;

    @SafeVarargs
    public MapType(BlazeRegistry.Key<MapType> id, Component name, ResourceLocation icon, BlazeRegistry.Key<Layer>... layers) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.layers = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(layers)));
    }

    public Set<BlazeRegistry.Key<Layer>> getLayers() {
        return layers;
    }

    @Override
    public BlazeRegistry.Key<MapType> getID() {
        return id;
    }

    @Override
    public boolean shouldRenderInDimension(ResourceKey<Level> dimension) {
        return true;
    }

    @Override
    public Component getName() {
        return name;
    }

    @Override
    public ResourceLocation getIcon() {
        return icon;
    }
}
