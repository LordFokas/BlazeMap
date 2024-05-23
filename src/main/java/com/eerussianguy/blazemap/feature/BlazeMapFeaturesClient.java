package com.eerussianguy.blazemap.feature;

import org.lwjgl.glfw.GLFW;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.event.MapMenuSetupEvent;
import com.eerussianguy.blazemap.feature.mapping.*;
import com.eerussianguy.blazemap.feature.maps.*;
import com.eerussianguy.blazemap.feature.waypoints.WaypointEditorGui;
import com.eerussianguy.blazemap.feature.waypoints.WaypointManagerGui;
import com.eerussianguy.blazemap.feature.waypoints.WaypointRenderer;
import com.eerussianguy.blazemap.feature.waypoints.WaypointStore;
import com.mojang.blaze3d.platform.InputConstants;

public class BlazeMapFeaturesClient {
    public static final Lazy<KeyMapping> KEY_MAPS = Lazy.of(() -> new KeyMapping("blazemap.key.maps", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, BlazeMap.MOD_NAME));
    public static final Lazy<KeyMapping> KEY_ZOOM_IN = Lazy.of(() -> new KeyMapping("blazemap.key.zoom_in", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, BlazeMap.MOD_NAME));
    public static final Lazy<KeyMapping> KEY_ZOOM_OUT = Lazy.of(() -> new KeyMapping("blazemap.key.zoom_out", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, BlazeMap.MOD_NAME));
    public static final Lazy<KeyMapping> KEY_WAYPOINTS = Lazy.of(() -> new KeyMapping("blazemap.key.waypoints", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, BlazeMap.MOD_NAME));

    private static boolean mapping = false;
    private static boolean maps = false;
    private static boolean waypoints = false;
    
    public static void onKeyBindRegister(RegisterKeyMappingsEvent event) {
        event.register(KEY_MAPS.get());
        event.register(KEY_ZOOM_IN.get());
        event.register(KEY_ZOOM_OUT.get());
        event.register(KEY_WAYPOINTS.get());
    }

    public static boolean hasMapping() {
        return mapping;
    }

    public static void initMapping() {
        BlazeMapAPI.LAYERS.register(new TerrainHeightLayer());
        BlazeMapAPI.LAYERS.register(new TerrainSlopeLayer());
        BlazeMapAPI.LAYERS.register(new WaterLevelLayer());
        BlazeMapAPI.LAYERS.register(new TerrainIsolinesLayer());
        BlazeMapAPI.LAYERS.register(new BlockColorLayer());
        BlazeMapAPI.LAYERS.register(new NetherLayer());

        BlazeMapAPI.MAPTYPES.register(new AerialViewMapType());
        BlazeMapAPI.MAPTYPES.register(new TopographyMapType());
        BlazeMapAPI.MAPTYPES.register(new NetherMapType());

        mapping = true;
    }

    public static boolean hasMaps() {
        return maps;
    }

    public static void initMaps() {
        BlazeMapAPI.OBJECT_RENDERERS.register(new DefaultObjectRenderer());

        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener(MapRenderer::onDimensionChange);
        bus.addListener(MapRenderer::onMapLabelAdded);
        bus.addListener(MapRenderer::onMapLabelRemoved);
        bus.addListener(BlazeMapFeaturesClient::mapKeybinds);
        bus.addListener(BlazeMapFeaturesClient::mapMenu);

        maps = true;
    }

    private static void mapKeybinds(InputEvent.Key evt) {
        if(KEY_MAPS.get().isDown()) {
            if(Screen.hasShiftDown()) {
                MinimapOptionsGui.open();
            }
            else {
                WorldMapGui.open();
            }
        }
        if(KEY_WAYPOINTS.get().isDown() && hasWaypoints()) {
            if(Screen.hasShiftDown()) {
                WaypointManagerGui.open();
            }
            else {
                WaypointEditorGui.open();
            }
        }
        if(KEY_ZOOM_IN.get().isDown()) {
            MinimapRenderer.INSTANCE.synchronizer.zoomIn();
        }
        if(KEY_ZOOM_OUT.get().isDown()) {
            MinimapRenderer.INSTANCE.synchronizer.zoomOut();
        }
    }

    private static void mapMenu(MapMenuSetupEvent evt) {
        if(hasWaypoints()){
            evt.root.add(WorldMapMenu.waypoints(evt.blockPosX, evt.blockPosZ));
        }
        if(BlazeMapConfig.CLIENT.enableDebug.get()) {
            evt.root.add(WorldMapMenu.debug(evt.blockPosX, evt.blockPosZ, evt.chunkPosX, evt.chunkPosZ, evt.regionPosX, evt.regionPosZ));
        }
    }

    public static boolean hasWaypoints() {
        return waypoints &&
            (BlazeMapConfig.CLIENT.clientFeatures.displayWaypointsOnMap.get() ||
             BlazeMapConfig.CLIENT.clientFeatures.renderWaypointsInWorld.get());
    }

    public static void initWaypoints() {
        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener(WaypointEditorGui::onDimensionChanged);
        bus.addListener(WaypointManagerGui::onDimensionChanged);
        bus.addListener(EventPriority.HIGHEST, WaypointStore::onServerJoined);
        bus.addListener(MapRenderer::onWaypointAdded);
        bus.addListener(MapRenderer::onWaypointRemoved);
        bus.addListener(WorldMapMenu::trackWaypointStore);

        WaypointRenderer.init();

        waypoints = true;
    }
}
