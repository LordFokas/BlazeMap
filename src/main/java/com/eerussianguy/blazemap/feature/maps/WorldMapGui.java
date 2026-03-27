package com.eerussianguy.blazemap.feature.maps;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.builtin.TerrainHeightMD;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.api.maps.Overlay;
import com.eerussianguy.blazemap.api.maps.TileResolution;
import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.engine.UnsafeGenerics;
import com.eerussianguy.blazemap.engine.cache.ChunkMDCache;
import com.eerussianguy.blazemap.engine.client.ClientEngine;
import com.eerussianguy.blazemap.feature.BlazeMapFeaturesClient;
import com.eerussianguy.blazemap.feature.atlas.AtlasExportProgress;
import com.eerussianguy.blazemap.feature.atlas.AtlasExporter;
import com.eerussianguy.blazemap.feature.atlas.AtlasTask;
import com.eerussianguy.blazemap.feature.maps.ui.InteractiveMapDisplay;
import com.eerussianguy.blazemap.feature.maps.ui.MapScaleDisplay;
import com.eerussianguy.blazemap.feature.maps.ui.NamedMapComponentButton.LayerButton;
import com.eerussianguy.blazemap.feature.maps.ui.NamedMapComponentButton.MapTypeButton;
import com.eerussianguy.blazemap.feature.maps.ui.NamedMapComponentButton.OverlayButton;
import com.eerussianguy.blazemap.feature.maps.ui.WorldMapDebug;
import com.eerussianguy.blazemap.feature.maps.ui.WorldMapHotkey;
import com.eerussianguy.blazemap.feature.waypoints.WaypointEditorFragment;
import com.eerussianguy.blazemap.lib.ObjHolder;
import com.eerussianguy.blazemap.lib.gui.components.Image;
import com.eerussianguy.blazemap.lib.gui.components.LineContainer;
import com.eerussianguy.blazemap.lib.gui.components.Placeholder;
import com.eerussianguy.blazemap.lib.gui.components.VanillaComponents;
import com.eerussianguy.blazemap.lib.gui.core.*;
import com.eerussianguy.blazemap.lib.gui.fragment.BaseFragment;
import com.eerussianguy.blazemap.lib.gui.fragment.FragmentHost;
import com.eerussianguy.blazemap.lib.gui.fragment.HostWindowComponent;
import com.eerussianguy.blazemap.lib.gui.util.VisibilityController;
import com.eerussianguy.blazemap.profiling.Profiler;
import com.mojang.blaze3d.vertex.PoseStack;

public class WorldMapGui extends Screen implements FragmentHost, TooltipService {
    private static final ResourceLocation HEADER_MAPS = BlazeMap.resource("textures/map_icons/header_maps.png");
    private static final ResourceLocation HEADER_LAYERS = BlazeMap.resource("textures/map_icons/header_layers.png");
    private static final ResourceLocation HEADER_OVERLAYS = BlazeMap.resource("textures/map_icons/header_overlays.png");
    private static final ResourceLocation BLAZEMAP_ICON = BlazeMap.resource("textures/mod_icon.png");
    private static final ResourceLocation BLAZEMAP_NAME = BlazeMap.resource("textures/mod_name.png");
    public static final double MIN_ZOOM = 0.125, MAX_ZOOM = 8;
    private static final int MARGIN = 5;
    private static final Profiler.TimeProfiler renderTime = new Profiler.TimeProfilerSync("world_map_render", 10);
    private static final Profiler.TimeProfiler uploadTime = new Profiler.TimeProfilerSync("world_map_upload", 10);
    private static final VisibilityController visibilityController = new VisibilityController();
    private static boolean renderDebug = false;

    public static void open() {
        Minecraft.getInstance().setScreen(new WorldMapGui());
    }

    public static void apply(Consumer<WorldMapGui> function) {
        if(Minecraft.getInstance().screen instanceof WorldMapGui gui) {
            function.accept(gui);
        }
    }

    private static final WorldMapHotkey[] HOTKEYS = new WorldMapHotkey[] {
        new WorldMapHotkey("LMB", "Drag to pan the map"),
        new WorldMapHotkey("RMB", "Open context menu"),
        new WorldMapHotkey("Scroll", "Zoom in / out"),
        new WorldMapHotkey(BlazeMapFeaturesClient.KEY_WAYPOINTS.getKey().getDisplayName().getString().toUpperCase(), "Create waypoint at cursor"),
        new WorldMapHotkey("F1", "Toggle map UI"),
        new WorldMapHotkey("F3", "Toggle debug info"),
        new WorldMapHotkey("F12", "Export atlas"),
        new WorldMapHotkey("W A S D", "Pan the map")
    };


    // =================================================================================================================


    private final ResourceKey<Level> dimension = Minecraft.getInstance().level.dimension();
    private final List<MapType> mapTypes = BlazeMapAPI.MAPTYPES.keys().stream().map(BlazeRegistry.Key::value).filter(m -> m.shouldRenderInDimension(dimension)).toList();
    private final List<Overlay> overlays = BlazeMapFeaturesClient.OVERLAYS.stream().map(BlazeRegistry.Key::value).filter(o -> o.shouldRenderInDimension(dimension)).toList();
    private final MapConfigSynchronizer synchronizer;
    private final InteractiveMapDisplay map;
    private AbsoluteContainer windows, components;
    private VolatileContainer volatiles;
    private MetaContainer root;
    private Placeholder legend;

    public WorldMapGui() {
        super(TextComponent.EMPTY);

        map = new InteractiveMapDisplay(BlazeMap.resource("dynamic/map/worldmap"), MIN_ZOOM, MAX_ZOOM);
        var renderer = map.getRenderer();
        synchronizer = new MapConfigSynchronizer(renderer, BlazeMapConfig.CLIENT.worldMap);
        map.setSynchronizer(synchronizer).setProfilers(renderTime, uploadTime).onMapChange(this::updateLegend);
    }

    @Override
    public void drawTooltip(PoseStack stack, int x, int y, List<? extends Component> lines) {
        renderTooltip(stack, lines.stream().map(Component::getVisualOrderText).collect(Collectors.toList()), x, y);
    }

    @Override
    protected void init() {
        // UI LAYERS
        volatiles = new VolatileContainer(0);
        windows = new AbsoluteContainer(0);
        components = new AbsoluteContainer(MARGIN);
        AbsoluteContainer background = new AbsoluteContainer(0);
        background.add(map.setVolatiles(volatiles).setSize(width, height), 0, 0);
        root = addRenderableWidget(new MetaContainer(width, height).add(background, components, windows, volatiles).setInputConsumer(map));
        visibilityController.clear().add(components, windows, volatiles);

        // BRANDING
        var brand_icon = new Image(BLAZEMAP_ICON, 20, 20);
        var brand_logo = new Image(BLAZEMAP_NAME, 110, 20);
        components.add(brand_icon, ContainerAnchor.TOP_LEFT);
        components.add(brand_logo, ContainerAnchor.TOP_CENTER);

        // MAPS AND LAYERS
        LineContainer maps = new LineContainer(ContainerAxis.HORIZONTAL, ContainerDirection.POSITIVE, 2).withBackground();
        components.anchor(maps, brand_icon, ContainerAxis.HORIZONTAL, ContainerDirection.POSITIVE);
        maps.add(new Image(HEADER_MAPS, 16, 16).tooltip(new TextComponent("Maps")));
        maps.addSpacer();
        List<LineContainer> layerSets = new ArrayList<>();
        for(var mapType : mapTypes) {
            LineContainer layerSet = new LineContainer(ContainerAxis.VERTICAL, ContainerDirection.NEGATIVE, 2).withBackground();
            components.anchor(layerSet, brand_icon, ContainerAxis.VERTICAL, ContainerDirection.POSITIVE);
            layerSets.add(layerSet);
            maps.add(new MapTypeButton(mapType.getID(), map, layerSets, layerSet));

            layerSet.setVisible(map.getMapType().getID().equals(mapType.getID()));
            for(var layer : mapType.getLayers()) {
                layerSet.add(new LayerButton(layer, map));
            }
            layerSet.addSpacer().add(new Image(HEADER_LAYERS, 16, 16).tooltip(new TextComponent("Layers")));
        }

        // OVERLAYS
        LineContainer overlaySet = new LineContainer(ContainerAxis.HORIZONTAL, ContainerDirection.POSITIVE, 2).withBackground();
        overlaySet.add(new Image(HEADER_OVERLAYS, 16, 16).tooltip(new TextComponent("Overlays")));
        overlaySet.addSpacer();
        for(var overlay : overlays) {
            overlaySet.add(new OverlayButton(overlay.getID(), map));
        }
        components.add(overlaySet, ContainerAnchor.BOTTOM_LEFT);

        // SEARCH
        ObjHolder<String> text = new ObjHolder<>();
        BaseComponent<?> search = VanillaComponents.makeTextField(getMinecraft().font, 120, 15, text);
        components.add(search, ContainerAnchor.BOTTOM_CENTER);
        text.setResponder(map.getRenderer()::setSearch);
        var renderer = map.getRenderer();
        renderer.setSearchHost(search::setVisible);
        renderer.pingSearchHost();

        // HOTKEYS
        var hotkeys = new LineContainer(ContainerAxis.VERTICAL, ContainerDirection.POSITIVE, 3).withBackground().with(HOTKEYS);
        components.anchor(hotkeys, overlaySet, ContainerAxis.VERTICAL, ContainerDirection.NEGATIVE);

        // LEGEND
        legend = new Placeholder();
        components.add(legend, ContainerAnchor.BOTTOM_RIGHT);
        updateLegend();

        // SCALE
        MapScaleDisplay scale = new MapScaleDisplay(256, map.getRenderer());
        components.anchor(scale, legend, ContainerAxis.HORIZONTAL, ContainerDirection.NEGATIVE);

        components.add(new AtlasExportProgress(), ContainerAnchor.TOP_RIGHT);
        components.anchor(new WorldMapDebug(map.getRenderer().debug, map.getCoordination(), renderTime, uploadTime, () -> renderDebug), maps, ContainerAxis.VERTICAL, ContainerDirection.POSITIVE);
    }

    private void updateLegend() {
        var legend = WrappedComponent.ofNullable(map.getMapType().getLayers().iterator().next().value().getLegendWidget());

        if(legend == null) {
            this.legend.clear();
        } else {
            this.legend.add(legend);
        }
    }

    @Override
    public boolean consumeFragment(BaseFragment fragment) {
        HostWindowComponent window = new HostWindowComponent(fragment, volatiles).setCloser(windows::remove);
        windows.add(window, ContainerAnchor.MIDDLE_CENTER);
        return true;
    }

    @Override
    public boolean mouseDragged(double p_94699_, double p_94700_, int p_94701_, double p_94702_, double p_94703_) {
        return super.mouseDragged(p_94699_, p_94700_, p_94701_, p_94702_, p_94703_);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if(key == GLFW.GLFW_KEY_F1) {
            visibilityController.toggleVisible();
            return true;
        }

        if(key == GLFW.GLFW_KEY_F12) {
            AtlasExporter.exportAsync(new AtlasTask(this.dimension, map.getMapType().getID(), map.getRenderer().getVisibleLayers(), TileResolution.FULL, map.getRenderer().getCenterRegion()));
            return true;
        }

        if(key == GLFW.GLFW_KEY_F3) {
            renderDebug = !renderDebug;
            return true;
        }

        if(root.keyPressed(key, scancode, modifiers)) return true;
        if(super.keyPressed(key, scancode, modifiers)) return true;

        if(key == BlazeMapFeaturesClient.KEY_MAPS.getKey().getValue()) {
            this.onClose();
            return true;
        }

        if(key == BlazeMapFeaturesClient.KEY_WAYPOINTS.getKey().getValue()) {
            var position = getCursorBlockPos();
            new WaypointEditorFragment(position).open();
            return true;
        }

        return false;
    }

    @Override
    public void onClose() {
        map.getRenderer().close();
        synchronizer.save();
        visibilityController.clear();
        super.onClose();
    }

    public void addInspector(MDInspectorWidget<?> widget) {
        this.addRenderableWidget(widget);
        widget.setDismisser(() -> this.removeWidget(widget));
    }

    public BlockPos getCursorBlockPos() {
        Level level = Minecraft.getInstance().level;
        int posY = level == null ? 65 : level.getSeaLevel();
        Coordination coordination = map.getCoordination();
        BlockPos position = new BlockPos(coordination.blockX, posY, coordination.blockZ);
        ChunkPos chunkPos = new ChunkPos(position);

        // Attempt to get y actual level from MDCache
        ChunkMDCache mdCache = ClientEngine.getMDCache(chunkPos);
        if(mdCache != null) {
            TerrainHeightMD heightMD = (TerrainHeightMD) mdCache.get(
                UnsafeGenerics.stripKey(BlazeMapReferences.MasterData.TERRAIN_HEIGHT)
            );

            if(heightMD != null) {
                int chunkX = SectionPos.sectionRelative(position.getX());
                int chunkZ = SectionPos.sectionRelative(position.getZ());
                posY = heightMD.heightmap[chunkX][chunkZ];
                position = position.atY(posY);
            }
        }

        return position;
    }
}
