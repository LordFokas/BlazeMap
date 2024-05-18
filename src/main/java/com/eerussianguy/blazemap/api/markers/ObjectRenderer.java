package com.eerussianguy.blazemap.api.markers;

import net.minecraft.client.gui.GuiGraphics;

import com.eerussianguy.blazemap.api.BlazeRegistry;

public interface ObjectRenderer<T> extends BlazeRegistry.RegistryEntry {
    void render(T object, GuiGraphics graphics, double zoom, SearchTargeting search);

    BlazeRegistry.Key<ObjectRenderer<?>> getID();
}
