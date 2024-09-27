package com.eerussianguy.blazemap.feature.maps;

import java.util.ArrayList;
import java.util.Arrays;
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

import com.eerussianguy.blazemap.api.maps.Overlay;
import com.eerussianguy.blazemap.api.maps.TileResolution;
import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.feature.BlazeMapFeaturesClient;
import com.eerussianguy.blazemap.feature.atlas.AtlasExporter;
import com.eerussianguy.blazemap.feature.atlas.AtlasTask;
import com.eerussianguy.blazemap.gui.components.Image;
import com.eerussianguy.blazemap.gui.MouseSubpixelSmoother;
import com.eerussianguy.blazemap.gui.components.NamedMapComponentButton.*;
import com.eerussianguy.blazemap.gui.lib.ContainerAxis;
import com.eerussianguy.blazemap.gui.lib.ContainerDirection;
import com.eerussianguy.blazemap.gui.components.LineContainer;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.Helpers;
import com.eerussianguy.blazemap.profiling.Profiler;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

public class WorldMapGui extends Screen implements IMapHost {
    private static final TextComponent EMPTY = new TextComponent("");
    private static final ResourceLocation ICON = Helpers.identifier("textures/mod_icon.png");
    private static final ResourceLocation NAME = Helpers.identifier("textures/mod_name.png");
    private static final ResourceLocation SCALE = Helpers.identifier("textures/scale.png");
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
    private final List<Overlay> overlays;
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
        overlays = BlazeMapFeaturesClient.OVERLAYS.stream().map(BlazeRegistry.Key::value).filter(o -> o.shouldRenderInDimension(dimension)).collect(Collectors.toUnmodifiableList());
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
    public boolean isOverlayVisible(BlazeRegistry.Key<Overlay> overlayID) {
        return mapRenderer.isOverlayVisible(overlayID);
    }

    @Override
    public void toggleOverlay(BlazeRegistry.Key<Overlay> overlayID) {
        synchronizer.toggleOverlay(overlayID);
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
    public void drawTooltip(PoseStack stack, int x, int y, Component... lines) {
        renderTooltip(stack, Arrays.stream(lines).map(Component::getVisualOrderText).toList(), x, y);
    }

    @Override
    public Iterable<? extends GuiEventListener> getChildren() {
        return children();
    }

    @Override
    protected void init() {
        double scale = getMinecraft().getWindow().getGuiScale();
        mapRenderer.resize((int) (Math.ceil(width * scale / MAX_ZOOM) * MAX_ZOOM), (int) (Math.ceil(height * scale / MAX_ZOOM) * MAX_ZOOM));

        addRenderableOnly(new Image(ICON, 20, 20).setPosition(5, 5));
        addRenderableOnly(new Image(NAME, 110, 20).setPosition(width / 2 - 55, 5));

        LineContainer maps = addRenderableWidget(new LineContainer(ContainerAxis.HORIZONTAL, ContainerDirection.POSITIVE, 2).setPosition(35, 5)).withBackground();
        List<LineContainer> layerSets = new ArrayList<>();
        for(var mapType : mapTypes) {
            LineContainer layerSet = addRenderableWidget(new LineContainer(ContainerAxis.VERTICAL, ContainerDirection.NEGATIVE, 2).setPosition(5, 35)).withBackground();
            layerSets.add(layerSet);
            maps.add(new MapTypeButton(mapType.getID(), this, layerSets, layerSet));

            layerSet.setVisible(getMapType().getID().equals(mapType.getID()));
            for(var layer : mapType.getLayers()) {
                if(layer.value().isOpaque()) continue; // TODO: fix this crap
                layerSet.add(new LayerButton(layer, this));
            }
        }

        LineContainer overlaySet = addRenderableWidget(new LineContainer(ContainerAxis.HORIZONTAL, ContainerDirection.POSITIVE, 2).setPosition(5, height - 25).withBackground());
        for(var overlay : overlays) {
            overlaySet.add(new OverlayButton(overlay.getID(), this));
        }

        addRenderableOnly(new LineContainer(ContainerAxis.VERTICAL, ContainerDirection.POSITIVE, 3)
            .withBackground().setPosition(5, height - 30).with(
                new WorldMapHotkey("LMB", "Drag to pan the map"),
                new WorldMapHotkey("RMB", "Open context menu"),
                new WorldMapHotkey("Scroll", "Zoom in / out"),
                new WorldMapHotkey("F1", "Toggle map UI"),
                new WorldMapHotkey("F3", "Toggle debug info"),
                new WorldMapHotkey("F12", "Export atlas"),
                new WorldMapHotkey("W A S D", "Pan the map by 1 chunk")
            ).shiftPositionY()
        );

        search = addRenderableWidget(new EditBox(getMinecraft().font, (width - 120) / 2, height - 15, 120, 12, EMPTY));
        search.setResponder(mapRenderer::setSearch);
        mapRenderer.pingSearchHost();

        addRenderableOnly(new WorldMapDebug(mapRenderer.debug, coordination, renderTime, uploadTime, () -> renderDebug).setPosition(35, 35));

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
            contextMenu = new WorldMapPopup(coordination, width * scale, height * scale, mapRenderer.getVisibleLayers(), mapRenderer.getVisibleOverlays());
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
            renderAtlasExportProgress(stack, scale);

            stack.pushPose();
            super.render(stack, i0, i1, f0);
            stack.popPose();
        }
    }

    private void renderAtlasExportProgress(PoseStack stack, float scale) {
        AtlasTask task = AtlasExporter.getTask();
        if(task == null) return;
        Font font = Minecraft.getInstance().font;
        stack.pushPose();

        stack.translate(width - 205, 5, 0); // Go to corner
        RenderHelper.fillRect(stack.last().pose(), 200, 30, Colors.WIDGET_BACKGROUND); // draw background

        // Process flashing "animation"
        int textColor = Colors.WHITE;
        long flashUntil = ((long)task.getFlashUntil()) * 1000L;
        long now = System.currentTimeMillis();
        if(task.isErrored() || (flashUntil >= now && now % 333 < 166)) {
            textColor = 0xFFFF0000;
        }

        // Render progress text
        int total = task.getTilesTotal();
        int current = task.getTilesCurrent();
        font.draw(stack, String.format("Exporting  1:%d", task.resolution.pixelWidth), 5, 5, textColor);
        String operation = switch(task.getStage()){
            case QUEUED -> "queued";
            case CALCULATING -> "calculating";
            case STITCHING -> String.format("stitching %d / %d tiles", current, total);
            case SAVING -> "saving";
        };
        font.draw(stack, operation, 195 - font.width(operation), 5, textColor);

        // Render progress bar
        double progress = ((double)current) / ((double)total);
        stack.translate(5, 17, 0);
        RenderHelper.fillRect(stack.last().pose(), 190, 10, Colors.LABEL_COLOR);
        RenderHelper.fillRect(stack.last().pose(), (int)(190*progress), 10, textColor);

        stack.popPose();
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

        if(key == GLFW.GLFW_KEY_F12) {
            AtlasExporter.exportAsync(new AtlasTask(this.dimension, this.getMapType().getID(), this.mapRenderer.getVisibleLayers(), TileResolution.FULL, this.mapRenderer.getCenterRegion()));
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
