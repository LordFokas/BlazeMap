package com.eerussianguy.blazemap.api.maps;

import com.eerussianguy.blazemap.api.BlazeRegistry;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * To unify the rendering behaviour of widgets that can take either a MapType or a Layer
 */
public interface IClientComponent {
    public BlazeRegistry.Key<? extends IClientComponent> getID();

    public boolean shouldRenderInDimension(ResourceKey<Level> dimension);

    public Component getName();

    public ResourceLocation getIcon();
}