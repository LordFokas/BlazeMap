package com.eerussianguy.blazemap.feature.maps;

import com.eerussianguy.blazemap.api.maps.Overlay;
import com.eerussianguy.blazemap.config.ClientConfig.MapConfig;
import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;

public class MapConfigSynchronizer {
    protected final MapConfig config;
    protected final MapRenderer map;
    protected MapRenderer renderer;


    public MapConfigSynchronizer(MapRenderer map, MapConfig config) {
        this.config = config;
        this.map = map;
        this.renderer = map;
        load();
    }

    public void load() {
        renderer.setMapType(config.activeMap.get().value());
        renderer.setDisabledLayers(config.disabledLayers.get());
        renderer.setZoom(config.zoom.get());
    }

    public void save() {
        config.activeMap.set(renderer.getMapType().getID());
        config.disabledLayers.set(renderer.getDisabledLayers());
        config.zoom.set(renderer.getZoom());
    }

    public boolean setMapType(MapType mapType) {
        if(!renderer.setMapType(mapType)) return false;
        config.activeMap.set(renderer.getMapType().getID());
        return true;
    }

    public boolean toggleLayer(Key<Layer> layerID) {
        if(!renderer.toggleLayer(layerID)) return false;
        config.disabledLayers.set(renderer.getDisabledLayers());
        return true;
    }

    public boolean toggleOverlay(Key<Overlay> overlayID) {
        if(!renderer.toggleOverlay(overlayID)) return false;
        config.disabledOverlays.set(renderer.getDisabledOverlays());
        return true;
    }

    public boolean setZoom(double zoom) {
        if(!renderer.setZoom(zoom)) return false;
        config.zoom.set(renderer.getZoom());
        return true;
    }

    public boolean zoomIn() {
        return setZoom(renderer.getZoom() * 2);
    }

    public boolean zoomOut() {
        return setZoom(renderer.getZoom() / 2);
    }

    public void override(MapRenderer renderer) {
        this.renderer = renderer;
        load();
    }

    public void clear() {
        this.renderer = map;
        load();
    }
}
