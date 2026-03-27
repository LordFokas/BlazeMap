package com.eerussianguy.blazemap.api.maps;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeRegistry;

/** Superclass for map components with common traits (Layer, MapType, Overlay) */
public abstract class NamedMapComponent<T extends NamedMapComponent<T>> implements BlazeRegistry.RegistryEntry {
    protected final BlazeRegistry.Key<T> id;
    protected final TranslatableComponent name;
    protected final ResourceLocation icon;

    protected NamedMapComponent(BlazeRegistry.Key<T> id, TranslatableComponent name, ResourceLocation icon) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    public BlazeRegistry.Key<T> getID() {
        return id;
    }

    public TranslatableComponent getName() {
        return name;
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public boolean shouldRenderInDimension(ResourceKey<Level> dimension) {
        return true;
    }
}