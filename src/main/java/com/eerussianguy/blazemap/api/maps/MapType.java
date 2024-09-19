package com.eerussianguy.blazemap.api.maps;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import net.minecraftforge.common.MinecraftForge;

import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.event.MapTypeInflationEvent;

/**
 * Each available map in Blaze Map is defined by MapType.
 * These objects do no processing of any sort and merely define a structure.
 * The provided list of layers defines what layers are rendered on the screen,
 * the first layer being rendered on the bottom and the last on top.
 *
 * @author LordFokas
 */
public class MapType implements BlazeRegistry.RegistryEntry {
    private final BlazeRegistry.Key<MapType> id;
    private final LinkedHashSet<BlazeRegistry.Key<Layer>> layers;
    private final Set<BlazeRegistry.Key<Layer>> layerView;
    private final TranslatableComponent name;
    private final ResourceLocation icon;
    private boolean inflated;

    @SafeVarargs
    public MapType(BlazeRegistry.Key<MapType> id, TranslatableComponent name, ResourceLocation icon, BlazeRegistry.Key<Layer>... layers) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.layers = new LinkedHashSet<>(Arrays.asList(layers));
        this.layerView = Collections.unmodifiableSet(this.layers);
    }

    public Set<BlazeRegistry.Key<Layer>> getLayers() {
        return layerView;
    }

    @Override
    public BlazeRegistry.Key<MapType> getID() {
        return id;
    }

    public boolean shouldRenderInDimension(ResourceKey<Level> dimension) {
        return true;
    }

    public TranslatableComponent getName() {
        return name;
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    /** Used by the engine to give addons a chance to contribute to an external map type, do not call this method. */
    public void inflate() {
        if(inflated) throw new IllegalStateException("MapType " + id + "already inflated");
        inflated = true;

        var event = new MapTypeInflationEvent(id, layers);
        MinecraftForge.EVENT_BUS.post(event);
        event.update();
    }
}
