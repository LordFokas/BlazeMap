package com.eerussianguy.blazemap.feature.maps;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.maps.IScreenSkipsMinimap;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.engine.BlazeMapAsync;
import com.eerussianguy.blazemap.feature.BlazeMapFeaturesClient;
import com.eerussianguy.blazemap.gui.Image;
import com.eerussianguy.blazemap.gui.MouseSubpixelSmoother;
import com.eerussianguy.blazemap.profiling.overlay.ProfilingRenderer;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.Helpers;
import com.eerussianguy.blazemap.profiling.Profiler;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

public class WorldMapGui extends Screen implements IScreenSkipsMinimap, IMapHost {
    private static final TextComponent EMPTY = new TextComponent("");
    private static final ResourceLocation ICON = Helpers.identifier("textures/mod_icon.png");
    private static final ResourceLocation NAME = Helpers.identifier("textures/mod_name.png");
    public static final double MIN_ZOOM = 0.125, MAX_ZOOM = 8;
    private static final Profiler.TimeProfiler renderTime = new Profiler.TimeProfilerSync("world_map_render", 10);
    private static final Profiler.TimeProfiler uploadTime = new Profiler.TimeProfilerSync("world_map_upload", 10);
    private static boolean showWidgets = true, renderDebug = false;

    public static void open() {
        Minecraft.getInstance().setScreen(new WorldMapGui());
    }

    public static void apply(Consumer<WorldMapGui> function) {
        if(Minecraft.getInstance().screen instanceof WorldMapGui gui) {
            function.accept(gui);
        }
    }


    // =================================================================================================================


    private double zoom = 1;
    private final ResourceKey<Level> dimension;
    private final MapRenderer mapRenderer;
    private final MapConfigSynchronizer synchronizer;
    private final List<MapType> mapTypes;
    private final int layersBegin;
    private final MouseSubpixelSmoother mouse;
    private Widget legend;
    private EditBox search;
    private final Coordination coordination = new Coordination();
    private double rawMouseX = -1, rawMouseY = -1;
    private WorldMapPopup contextMenu;

    public WorldMapGui() {
        super(EMPTY);
        mapRenderer = new MapRenderer(-1, -1, Helpers.identifier("dynamic/map/worldmap"), MIN_ZOOM, MAX_ZOOM, true).setProfilers(renderTime, uploadTime);
        synchronizer = new MapConfigSynchronizer(mapRenderer, BlazeMapConfig.CLIENT.worldMap);
        dimension = Minecraft.getInstance().level.dimension();
        mapTypes = BlazeMapAPI.MAPTYPES.keys().stream().map(BlazeRegistry.Key::value).filter(m -> m.shouldRenderInDimension(dimension)).collect(Collectors.toUnmodifiableList());
        layersBegin = 50 + (mapTypes.size() * 20);
        mouse = new MouseSubpixelSmoother();
        zoom = mapRenderer.getZoom();

        mapRenderer.setSearchHost(active -> {
            if(search != null) {
                search.visible = active;
            }
        });
    }

    @Override
    public boolean isLayerVisible(BlazeRegistry.Key<Layer> layerID) {
        return mapRenderer.isLayerVisible(layerID);
    }

    @Override
    public void toggleLayer(BlazeRegistry.Key<Layer> layerID) {
        synchronizer.toggleLayer(layerID);
    }

    @Override
    public MapType getMapType() {
        return mapRenderer.getMapType();
    }

    @Override
    public void setMapType(MapType map) {
        synchronizer.setMapType(map);
        search.setValue("");
        updateLegend();
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
        double scale = getMinecraft().getWindow().getGuiScale();
        mapRenderer.resize((int) (Math.ceil(width * scale / MAX_ZOOM) * MAX_ZOOM), (int) (Math.ceil(height * scale / MAX_ZOOM) * MAX_ZOOM));

        addRenderableOnly(new Image(ICON, 5, 5, 20, 20));
        addRenderableOnly(new Image(NAME, 30, 5, 110, 20));
        int y = 20;
        for(MapType mapType : mapTypes) {
            BlazeRegistry.Key<MapType> key = mapType.getID();
            int px = 7, py = (y += 20);
            addRenderableWidget(new MapTypeButton(px, py, 16, 16, key, this));
            MapType map = key.value();
            int layerY = layersBegin;
            List<BlazeRegistry.Key<Layer>> childLayers = map.getLayers().stream().collect(Collectors.toList());
            Collections.reverse(childLayers);
            for(BlazeRegistry.Key<Layer> layer : childLayers) {
                if(layer.value().isOpaque()) continue;
                LayerButton lb = new LayerButton(px, layerY, 16, 16, layer, map, this);
                layerY += 20;
                lb.checkVisible();
                addRenderableWidget(lb);
            }
        }

        search = addRenderableWidget(new EditBox(getMinecraft().font, (width - 120) / 2, height - 15, 120, 12, EMPTY));
        search.setResponder(mapRenderer::setSearch);
        mapRenderer.pingSearchHost();

        updateLegend();
    }

    private void updateLegend() {
        legend = mapRenderer.getMapType().getLayers().iterator().next().value().getLegendWidget();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double draggedX, double draggedY) {
        setMouse(mouseX, mouseY);
        if(button == GLFW.GLFW_MOUSE_BUTTON_1) {
            double scale = getMinecraft().getWindow().getGuiScale();
            mouse.addMovement(draggedX * scale / zoom, draggedY * scale / zoom);
            mapRenderer.moveCenter(-mouse.movementX(), -mouse.movementY());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, draggedX, draggedY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        setMouse(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        boolean zoomed;
        if(scroll > 0) {
            zoomed = synchronizer.zoomIn();
        }
        else {
            zoomed = synchronizer.zoomOut();
        }
        zoom = mapRenderer.getZoom();
        setMouse(mouseX, mouseY);
        return zoomed;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        setMouse(mouseX, mouseY);

        if(contextMenu != null){ // On existing menu, pass into
            var result = contextMenu.onClick((int) rawMouseX, (int) rawMouseY, button);
            if(result.shouldDismiss) {
                contextMenu = null;
            }
            if(result.wasHandled) {
                return true;
            }
        }

        if(super.mouseClicked(mouseX, mouseY, button)) { // If super handled, exit
            return true;
        }

        if(button == GLFW.GLFW_MOUSE_BUTTON_2) { // If right click open new menu
            int scale = (int) getMinecraft().getWindow().getGuiScale();
            contextMenu = new WorldMapPopup(coordination, width * scale, height * scale, mapRenderer.getVisibleLayers());
            return true;
        }

        return false;
    }

    private void setMouse(double mouseX, double mouseY) {
        double scale = getMinecraft().getWindow().getGuiScale();
        this.rawMouseX = mouseX * scale;
        this.rawMouseY = mouseY * scale;
        coordination.calculate((int) this.rawMouseX, (int) this.rawMouseY, mapRenderer.getBeginX(), mapRenderer.getBeginZ(), mapRenderer.getZoom());
        if(contextMenu != null) {
            contextMenu.setMouse(coordination.mousePixelX, coordination.mousePixelY);
        }
    }

    @Override
    public void render(PoseStack stack, int i0, int i1, float f0) {
        float scale = (float) getMinecraft().getWindow().getGuiScale();
        fillGradient(stack, 0, 0, this.width, this.height, 0xFF333333, 0xFF333333);

        stack.pushPose();
        stack.scale(1F / scale, 1F / scale, 1);
        var buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        mapRenderer.render(stack, buffers);
        buffers.endBatch();
        if(contextMenu != null){
            contextMenu.render(stack, i0, i1, f0);
        }
        stack.popPose();

        if(legend != null) {
            stack.pushPose();
            stack.translate(width - 5, height - 5, 0);
            legend.render(stack, -1, -1, 0);
            stack.popPose();
        }

        if(showWidgets) {
            int maps = mapTypes.size();
            if(maps > 0) {
                stack.pushPose();
                stack.translate(5, 38, 0);
                RenderHelper.fillRect(stack.last().pose(), 20, maps * 20, Colors.WIDGET_BACKGROUND);
                stack.popPose();
            }
            long layers = mapRenderer.getMapType().getLayers().stream().map(k -> k.value()).filter(l -> !l.isOpaque() && l.shouldRenderInDimension(dimension)).count();
            if(layers > 0) {
                stack.pushPose();
                stack.translate(5, layersBegin - 2, 0);
                RenderHelper.fillRect(stack.last().pose(), 20, layers * 20, Colors.WIDGET_BACKGROUND);
                stack.popPose();
            }
            stack.pushPose();
            super.render(stack, i0, i1, f0);
            stack.popPose();
        }

        if(renderDebug) {
            stack.pushPose();
            renderDebug(stack);
            stack.popPose();

            stack.pushPose();
            stack.scale(1F / scale, 1F / scale, 1);
            renderCoordination(stack, scale);
            stack.popPose();
        }
    }

    private void renderCoordination(PoseStack stack, float scale){
        if(rawMouseX == -1 || rawMouseY == -1) return;

        stack.pushPose();
        stack.translate(coordination.regionPixelX, coordination.regionPixelY, 0.1);
        RenderHelper.fillRect(stack.last().pose(), coordination.regionPixels, coordination.regionPixels, 0x400000FF);
        stack.popPose();

        stack.pushPose();
        stack.translate(coordination.chunkPixelX, coordination.chunkPixelY, 0.2);
        RenderHelper.fillRect(stack.last().pose(), coordination.chunkPixels, coordination.chunkPixels, 0x6000FF00);
        stack.popPose();

        stack.pushPose();
        stack.translate(coordination.blockPixelX, coordination.blockPixelY, 0.3);
        RenderHelper.fillRect(stack.last().pose(), coordination.blockPixels, coordination.blockPixels, 0x80FF0000);
        stack.popPose();

        stack.pushPose();
        stack.translate(width * scale / 2, 10, 1);
        stack.scale(3, 3, 0);
        Font font = getMinecraft().font;
        String region = String.format("Rg %d %d  |  px: %d %d", coordination.regionX, coordination.regionZ, coordination.regionPixelX, coordination.regionPixelY);
        font.draw(stack, region, 0, 0, 0x0000FF);
        String chunk = String.format("Ch %d %d  |  px: %d %d", coordination.chunkX, coordination.chunkZ, coordination.chunkPixelX, coordination.chunkPixelY);
        font.draw(stack, chunk, 0, 10, 0x00FF00);
        String block = String.format("Bl %d %d  |  px: %d %d", coordination.blockX, coordination.blockZ, coordination.blockPixelX, coordination.blockPixelY);
        font.draw(stack, block, 0, 20, 0xFF0000);
        stack.popPose();
    }

    private void renderDebug(PoseStack stack) {
        stack.translate(32, 25, 0);
        RenderHelper.fillRect(stack.last().pose(), 135, 110, 0x80000000);
        font.draw(stack, "Debug Info", 5, 5, 0xFFFF0000);
        stack.translate(5, 20, 0);
        stack.scale(0.5F, 0.5F, 1);

        font.draw(stack, "Atlas Time Profiling:", 0, 0, -1);
        var buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        ProfilingRenderer.drawTimeProfiler(renderTime, 12, "Render", font, stack.last().pose(), buffers);
        ProfilingRenderer.drawTimeProfiler(uploadTime, 24, "Upload", font, stack.last().pose(), buffers);
        buffers.endBatch();

        MapRenderer.DebugInfo debug = mapRenderer.debug;
        int y = 30;
        font.draw(stack, String.format("Renderer Size: %d x %d", debug.rw, debug.rh), 0, y += 12, -1);
        font.draw(stack, String.format("Renderer Zoom: %sx", debug.zoom), 0, y += 12, -1);
        font.draw(stack, String.format("Atlas Size: %d x %d", debug.mw, debug.mh), 0, y += 12, -1);
        font.draw(stack, String.format("Atlas Frustum: [%d , %d] to [%d , %d]", debug.bx, debug.bz, debug.ex, debug.ez), 0, y += 12, -1);

        font.draw(stack, String.format("Region Matrix: %d x %d", debug.ox, debug.oz), 0, y += 18, -1);
        font.draw(stack, String.format("Active Layers: %d", debug.layers), 0, y += 12, -1);
        font.draw(stack, String.format("Stitching: %s", debug.stitching), 0, y += 12, 0xFF0088FF);
        font.draw(stack, String.format("Parallel Pool: %d", BlazeMapAsync.instance().cruncher.poolSize()), 0, y += 12, 0xFFFFFF00);

        font.draw(stack, String.format("Addon Labels: %d", debug.labels), 0, y += 18, -1);
        font.draw(stack, String.format("Player Waypoints: %d", debug.waypoints), 0, y += 12, -1);
    }

    @Override
    public void onClose() {
        mapRenderer.close();
        synchronizer.save();
        super.onClose();
    }

    @Override
    public boolean keyPressed(int key, int x, int y) {
        if(key == GLFW.GLFW_KEY_F1) {
            showWidgets = !showWidgets;
            return true;
        }

        if(key == GLFW.GLFW_KEY_F3) {
            renderDebug = !renderDebug;
            return true;
        }

        if(!search.isFocused()) {
            if(key == BlazeMapFeaturesClient.KEY_MAPS.getKey().getValue()) {
                this.onClose();
                return true;
            }

            int dx = 0;
            int dz = 0;
            if(key == GLFW.GLFW_KEY_W) {
                dz -= 16;
            }
            if(key == GLFW.GLFW_KEY_S) {
                dz += 16;
            }
            if(key == GLFW.GLFW_KEY_D) {
                dx += 16;
            }
            if(key == GLFW.GLFW_KEY_A) {
                dx -= 16;
            }
            if(dx != 0 || dz != 0) {
                mapRenderer.moveCenter(dx, dz);
                return true;
            }
        }
        return super.keyPressed(key, x, y);
    }

    @Override
    public Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

    public void addInspector(MDInspectorWidget<?> widget) {
        this.addRenderableWidget(widget);
        widget.setDismisser(() -> this.removeWidget(widget));
    }
}
