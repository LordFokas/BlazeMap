package com.eerussianguy.blazemap.feature.maps;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.api.maps.Overlay;
import com.eerussianguy.blazemap.api.maps.TileResolution;
import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.engine.render.MapRenderer;
import com.eerussianguy.blazemap.feature.BlazeMapFeaturesClient;
import com.eerussianguy.blazemap.feature.atlas.AtlasExportProgress;
import com.eerussianguy.blazemap.feature.atlas.AtlasExporter;
import com.eerussianguy.blazemap.feature.atlas.AtlasTask;
import com.eerussianguy.blazemap.feature.maps.NamedMapComponentButton.*;
import com.eerussianguy.blazemap.lib.gui.components.Image;
import com.eerussianguy.blazemap.lib.gui.components.LineContainer;
import com.eerussianguy.blazemap.lib.gui.core.*;
import com.eerussianguy.blazemap.lib.gui.fragment.BaseFragment;
import com.eerussianguy.blazemap.lib.gui.fragment.FragmentHost;
import com.eerussianguy.blazemap.lib.gui.fragment.HostWindowComponent;
import com.eerussianguy.blazemap.lib.gui.util.MouseSubpixelSmoother;
import com.eerussianguy.blazemap.profiling.Profiler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

public class WorldMapGui extends Screen implements MapHost, FragmentHost {
    private static final ResourceLocation HEADER_MAPS = BlazeMap.resource("textures/map_icons/header_maps.png");
    private static final ResourceLocation HEADER_LAYERS = BlazeMap.resource("textures/map_icons/header_layers.png");
    private static final ResourceLocation HEADER_OVERLAYS = BlazeMap.resource("textures/map_icons/header_overlays.png");
    private static final ResourceLocation ICON = BlazeMap.resource("textures/mod_icon.png");
    private static final ResourceLocation NAME = BlazeMap.resource("textures/mod_name.png");
    private static final ResourceLocation SCALE = BlazeMap.resource("textures/scale.png");
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
    private final AbsoluteContainer windows = new AbsoluteContainer(0);
    private final VolatileContainer volatiles = new VolatileContainer(0);
    private BaseComponent<?> legend;
    private EditBox search;
    private final Coordination coordination = new Coordination();
    private double rawMouseX = -1, rawMouseY = -1;
    private WorldMapPopup contextMenu;

    public WorldMapGui() {
        super(TextComponent.EMPTY);
        mapRenderer = new MapRenderer(-1, -1, BlazeMap.resource("dynamic/map/worldmap"), MIN_ZOOM, MAX_ZOOM).setProfilers(renderTime, uploadTime);
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
    public void drawTooltip(PoseStack stack, int x, int y, List<? extends Component> lines) {
        renderTooltip(stack, lines.stream().map(Component::getVisualOrderText).collect(Collectors.toList()), x, y);
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
        maps.add(new Image(HEADER_MAPS, 16, 16).tooltip(new TextComponent("Maps")));
        maps.addSpacer();
        List<LineContainer> layerSets = new ArrayList<>();
        for(var mapType : mapTypes) {
            LineContainer layerSet = addRenderableWidget(new LineContainer(ContainerAxis.VERTICAL, ContainerDirection.NEGATIVE, 2).setPosition(5, 35)).withBackground();
            layerSets.add(layerSet);
            maps.add(new MapTypeButton(mapType.getID(), this, layerSets, layerSet));

            layerSet.setVisible(getMapType().getID().equals(mapType.getID()));
            for(var layer : mapType.getLayers()) {
                layerSet.add(new LayerButton(layer, this));
            }
            layerSet.addSpacer().add(new Image(HEADER_LAYERS, 16, 16).tooltip(new TextComponent("Layers")));
        }

        LineContainer overlaySet = addRenderableWidget(new LineContainer(ContainerAxis.HORIZONTAL, ContainerDirection.POSITIVE, 2).setPosition(5, height - 25).withBackground());
        overlaySet.add(new Image(HEADER_OVERLAYS, 16, 16).tooltip(new TextComponent("Overlays")));
        overlaySet.addSpacer();
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

        addRenderableOnly(new AtlasExportProgress().setPosition(width - 5, 5).shiftPositionX());

        search = addRenderableWidget(new EditBox(getMinecraft().font, (width - 120) / 2, height - 15, 120, 12, TextComponent.EMPTY));
        search.setResponder(mapRenderer::setSearch);
        mapRenderer.pingSearchHost();

        addRenderableOnly(new WorldMapDebug(mapRenderer.debug, coordination, renderTime, uploadTime, () -> renderDebug).setPosition(35, 35));

        updateLegend();

        windows.setSize(width, height);
        addRenderableWidget(windows);

        volatiles.setSize(width, height);
        addRenderableWidget(volatiles);
    }

    @Override
    public boolean consumeFragment(BaseFragment fragment) {
        HostWindowComponent window = new HostWindowComponent(fragment, volatiles).setCloser(windows::remove);
        windows.add(window, ContainerAnchor.MIDDLE_CENTER);
        return true;
    }

    private void updateLegend() {
        if(legend != null) {
            legend.setVisible(false);
            renderables.remove(legend); // TODO: will not work when we switch to multi layer containers
        }

        legend = WrappedComponent.ofNullable(mapRenderer.getMapType().getLayers().iterator().next().value().getLegendWidget());

        if(legend != null) {
            legend.setPosition(width - 5, height - 5);
            legend.shiftPositionX().shiftPositionY();
            addRenderableOnly(legend);
        }
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

        if(showWidgets) {
            stack.pushPose();
            super.render(stack, i0, i1, f0);
            stack.popPose();
        }
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
