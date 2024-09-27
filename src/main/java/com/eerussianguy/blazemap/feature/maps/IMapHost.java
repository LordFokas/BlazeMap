package com.eerussianguy.blazemap.feature.maps;

import net.minecraft.client.gui.components.events.GuiEventListener;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.api.maps.Overlay;
import com.eerussianguy.blazemap.gui.lib.TooltipService;

public interface IMapHost extends TooltipService {
    boolean isLayerVisible(Key<Layer> layerID);
    void toggleLayer(Key<Layer> layerID);

    boolean isOverlayVisible(Key<Overlay> overlayID);
    void toggleOverlay(Key<Overlay> overlayID);

    MapType getMapType();
    void setMapType(MapType map);

    Iterable<? extends GuiEventListener> getChildren();
}