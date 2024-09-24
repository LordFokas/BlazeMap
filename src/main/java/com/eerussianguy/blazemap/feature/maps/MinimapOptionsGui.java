package com.eerussianguy.blazemap.feature.maps;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.api.maps.Overlay;
import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.config.ClientConfig;
import com.eerussianguy.blazemap.config.MinimapConfigFacade;
import com.eerussianguy.blazemap.gui.BlazeGui;
import com.eerussianguy.blazemap.util.Helpers;
import com.mojang.blaze3d.vertex.PoseStack;

public class MinimapOptionsGui extends BlazeGui implements IMapHost {
    private static final TranslatableComponent MAP_TYPES = Helpers.translate("blazemap.gui.minimap_options.map_types");
    private static final TranslatableComponent LAYERS = Helpers.translate("blazemap.gui.minimap_options.layers");
    private static final int WIDTH = 128, HEIGHT = 154;

    public static void open() {
        Minecraft.getInstance().setScreen(new MinimapOptionsGui());
    }

    private final MapRenderer mapRenderer = new MapRenderer(0, 0, Helpers.identifier("dynamic/map/minimap_preview"), MinimapRenderer.MIN_ZOOM, MinimapRenderer.MAX_ZOOM, false);
    private final MinimapConfigSynchronizer synchronizer = MinimapRenderer.INSTANCE.synchronizer;
    private final WidgetConfigFacade configFacade = new WidgetConfigFacade(BlazeMapConfig.CLIENT.minimap, mapRenderer);
    private final MinimapWidget minimap = new MinimapWidget(mapRenderer, configFacade, true);

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
    public boolean isOverlayVisible(Key<Overlay> overlayID) {
        return mapRenderer.isOverlayVisible(overlayID);
    }

    @Override
    public void toggleOverlay(Key<Overlay> overlayID) {
        synchronizer.toggleOverlay(overlayID);
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
    public void drawTooltip(PoseStack stack, int x, int y, Component ... lines) {
        renderTooltip(stack, Arrays.stream(lines).map(Component::getVisualOrderText).toList(), x, y);
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
        configFacade.flush();
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

    public static class WidgetConfigFacade implements MinimapConfigFacade.IWidgetConfig {
        private final MinimapConfigFacade.IntFacade positionX, positionY, width, height;
        private final ClientConfig.MinimapConfig config;
        private int _positionX, _positionY, _width, _height;
        private final MapRenderer renderer;

        public WidgetConfigFacade(ClientConfig.MinimapConfig config, MapRenderer renderer) {
            this.config = config;
            this.renderer = renderer;

            _positionX = config.positionX.get();
            _positionY = config.positionY.get();
            _width = config.width.get();
            _height = config.height.get();

            positionX = new MinimapConfigFacade.IntFacade(() -> this._positionX, v -> this._positionX = v);
            positionY = new MinimapConfigFacade.IntFacade(() -> this._positionY, v -> this._positionY = v);
            width = new MinimapConfigFacade.IntFacade(() -> this._width, v -> this._width = v);
            height = new MinimapConfigFacade.IntFacade(() -> this._height, v -> this._height = v);
        }

        @Override
        public void resize(int width, int height) {
            this._width = width;
            this._height = height;
            this.renderer.resize(_width, _height);
        }

        public void flush() {
            config.positionX.set(_positionX);
            config.positionY.set(_positionY);
            config.width.set(_width);
            config.height.set(_height);
        }

        @Override
        public MinimapConfigFacade.IntFacade positionX() {
            return positionX;
        }

        @Override
        public MinimapConfigFacade.IntFacade positionY() {
            return positionY;
        }

        @Override
        public MinimapConfigFacade.IntFacade width() {
            return width;
        }

        @Override
        public MinimapConfigFacade.IntFacade height() {
            return height;
        }
    }
}
