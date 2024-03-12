package com.eerussianguy.blazemap.feature.maps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.IScreenSkipsMinimap;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.gui.BlazeGui;
import com.eerussianguy.blazemap.util.Helpers;
import com.mojang.blaze3d.vertex.PoseStack;

public class MinimapOptionsGui extends BlazeGui implements IScreenSkipsMinimap, IMapHost {
    private static final Component MAP_TYPES = Helpers.translate("blazemap.gui.minimap_options.map_types");
    private static final Component LAYERS = Helpers.translate("blazemap.gui.minimap_options.layers");
    private static final int WIDTH = 128, HEIGHT = 154;

    public static void open() {
        Minecraft.getInstance().setScreen(new MinimapOptionsGui());
    }

    private final MapRenderer mapRenderer = new MapRenderer(0, 0, Helpers.identifier("dynamic/map/minimap_preview"), MinimapRenderer.MIN_ZOOM, MinimapRenderer.MAX_ZOOM, false);
    private final MinimapConfigSynchronizer synchronizer = MinimapRenderer.INSTANCE.synchronizer;
    private final MinimapWidget minimap = new MinimapWidget(mapRenderer, synchronizer, true);

    public MinimapOptionsGui() {
        super(Helpers.translate("blazemap.gui.minimap_options.title"), WIDTH, HEIGHT);
    }

    @Override
    public boolean isLayerVisible(Key<Layer> layerID) {
        return mapRenderer.isLayerVisible(layerID);
    }

    @Override
    public void toggleLayer(Key<Layer> layerID) {
        synchronizer.toggleLayer(layerID);
    }

    @Override
    public MapType getMapType() {
        return mapRenderer.getMapType();
    }

    @Override
    public void setMapType(MapType map) {
        mapRenderer.setMapType(map);
    }

    @Override
    public void drawTooltip(PoseStack stack, Component component, int x, int y) {
        renderTooltip(stack, component, x, y);
    }

    @Override
    public Iterable<? extends GuiEventListener> getChildren() {
        return children();
    }

    @Override
    protected void init() {
        super.init();
        synchronizer.override(mapRenderer);
        ResourceKey<Level> dimension = getMinecraft().level.dimension();

        int px = 15, py = 38;
        for(Key<MapType> mapID : BlazeMapAPI.MAPTYPES.keys()) {
            if(!mapID.value().shouldRenderInDimension(dimension)) continue;
            if(px > 96) {
                px = 15;
                py += 20;
            }
            addRenderableWidget(new MapTypeButton(left + px, top + py, 18, 18, mapID, this));
            MapType map = mapID.value();
            int lpx = 15, lpy = 99;
            for(Key<Layer> layerID : map.getLayers()) {
                Layer layer = layerID.value();
                if(!layer.shouldRenderInDimension(dimension) || layer.isOpaque()) continue;
                if(lpx > 96) {
                    lpx = 15;
                    lpy += 20;
                }
                LayerButton lb = new LayerButton(left + lpx, top + lpy, 18, 18, layerID, map, this);
                lb.checkVisible();
                addRenderableWidget(lb);
                lpx += 20;
            }
            px += 20;
        }
    }

    @Override
    protected void renderComponents(PoseStack stack, MultiBufferSource buffers) {
        renderLabel(stack, buffers, MAP_TYPES, 12, 25, false);
        renderSlot(stack, buffers, 12, 36, 104, 45);

        renderLabel(stack, buffers, LAYERS, 12, 86, false);
        renderSlot(stack, buffers, 12, 97, 104, 45);
    }

    @Override
    protected void renderAbsolute(PoseStack stack, MultiBufferSource buffers, float scale) {
        minimap.render(stack, buffers);
    }

    @Override
    public void onClose() {
        super.onClose();
        mapRenderer.close();
        synchronizer.save();
        synchronizer.clear();
    }

    @Override
    public boolean mouseDragged(double cx, double cy, int button, double dx, double dy) {
        double scale = getMinecraft().getWindow().getGuiScale();
        if(minimap.mouseDragged(cx * scale, cy * scale, button, dx * scale, dy * scale)){
            return true;
        }else{
            return super.mouseDragged(cx, cy, button, dx, dy);
        }
    }
}
