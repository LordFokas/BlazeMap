package com.eerussianguy.blazemap.feature.maps.ui;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.api.maps.Overlay;

public interface MapHost {
    boolean isLayerVisible(Key<Layer> layerID);
    void toggleLayer(Key<Layer> layerID);

    boolean isOverlayVisible(Key<Overlay> overlayID);
    void toggleOverlay(Key<Overlay> overlayID);

    MapType getMapType();
    void setMapType(MapType map);
}