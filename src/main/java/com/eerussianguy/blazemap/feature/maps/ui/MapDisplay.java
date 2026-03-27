package com.eerussianguy.blazemap.feature.maps.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.api.maps.Overlay;
import com.eerussianguy.blazemap.engine.render.MapRenderer;
import com.eerussianguy.blazemap.feature.maps.MapConfigSynchronizer;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.eerussianguy.blazemap.profiling.Profiler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

public class MapDisplay extends BaseComponent<MapDisplay> implements MapHost {
    protected final double maxZoom;
    protected final MapRenderer renderer;
    protected MapConfigSynchronizer synchronizer;
    private Runnable onMapChange = () -> {};

    public MapDisplay(ResourceLocation mapLocation, double minZoom, double maxZoom) {
        this(0, 0, mapLocation, minZoom, maxZoom);
    }

    public MapDisplay(int width, int height, ResourceLocation mapLocation, double minZoom, double maxZoom) {
        this.renderer = new MapRenderer(width, height, mapLocation, minZoom, maxZoom);
        this.maxZoom = maxZoom;
    }

    public MapDisplay setSynchronizer(MapConfigSynchronizer synchronizer) {
        this.synchronizer = synchronizer;
        return this;
    }

    public MapDisplay setProfilers(Profiler.TimeProfiler render, Profiler.TimeProfiler upload) {
        this.renderer.setProfilers(render, upload);
        return this;
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        float scale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        stack.scale(1F / scale, 1F / scale, 1);
        var buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        renderer.render(stack, buffers);
        buffers.endBatch();
    }

    public MapRenderer getRenderer() {
        return renderer;
    }

    public void onMapChange(Runnable function) {
        this.onMapChange = function;
    }

    @Override
    public MapDisplay setSize(int w, int h) {
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        renderer.resize((int) (Math.ceil(w * scale / maxZoom) * maxZoom), (int) (Math.ceil(h * scale / maxZoom) * maxZoom));
        return super.setSize(w, h);
    }

    @Override
    public boolean isLayerVisible(BlazeRegistry.Key<Layer> layerID) {
        return renderer.isLayerVisible(layerID);
    }

    @Override
    public void toggleLayer(BlazeRegistry.Key<Layer> layerID) {
        synchronizer.toggleLayer(layerID);
    }

    @Override
    public boolean isOverlayVisible(BlazeRegistry.Key<Overlay> overlayID) {
        return renderer.isOverlayVisible(overlayID);
    }

    @Override
    public void toggleOverlay(BlazeRegistry.Key<Overlay> overlayID) {
        synchronizer.toggleOverlay(overlayID);
    }

    @Override
    public MapType getMapType() {
        return renderer.getMapType();
    }

    @Override
    public void setMapType(MapType map) {
        if(synchronizer.setMapType(map)) {
            onMapChange.run();
        }
    }
}
