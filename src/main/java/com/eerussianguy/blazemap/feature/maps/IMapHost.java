package com.eerussianguy.blazemap.feature.maps;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.api.maps.Overlay;
import com.mojang.blaze3d.vertex.PoseStack;

public interface IMapHost {
    boolean isLayerVisible(Key<Layer> layerID);
    void toggleLayer(Key<Layer> layerID);

    boolean isOverlayVisible(Key<Overlay> overlayID);
    void toggleOverlay(Key<Overlay> overlayID);

    MapType getMapType();
    void setMapType(MapType map);

    void drawTooltip(PoseStack stack, int x, int y, Component... lines);
    Iterable<? extends GuiEventListener> getChildren();
}