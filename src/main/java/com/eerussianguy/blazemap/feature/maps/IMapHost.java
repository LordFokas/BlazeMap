package com.eerussianguy.blazemap.feature.maps;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.mojang.blaze3d.vertex.PoseStack;

public interface IMapHost {
    void drawTooltip(PoseStack stack, Component component, int x, int y);

    boolean isLayerVisible(Key<Layer> layerID);

    void toggleLayer(Key<Layer> layerID);

    MapType getMapType();

    void setMapType(MapType map);

    Iterable<? extends GuiEventListener> getChildren();
}
