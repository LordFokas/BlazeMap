package com.eerussianguy.blazemap.api.markers;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;

import com.eerussianguy.blazemap.api.BlazeRegistry;

public interface ObjectRenderer<T> extends BlazeRegistry.RegistryEntry {
    void render(T object, GuiGraphics graphics, MultiBufferSource buffers, double zoom, SearchTargeting search);

    BlazeRegistry.Key<ObjectRenderer<?>> getID();
}
