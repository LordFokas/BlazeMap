package com.eerussianguy.blazemap.feature;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.lwjgl.glfw.GLFW;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.event.BlazeRegistriesFrozenEvent;
import com.eerussianguy.blazemap.api.event.ComponentOrderingEvent.OverlayOrderingEvent;
import com.eerussianguy.blazemap.api.maps.Overlay;
import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.event.MapMenuSetupEvent;
import com.eerussianguy.blazemap.feature.mapping.*;
import com.eerussianguy.blazemap.feature.overlays.*;
import com.eerussianguy.blazemap.feature.maps.*;
import com.eerussianguy.blazemap.feature.waypoints.*;
import com.mojang.blaze3d.platform.InputConstants;

public class BlazeMapFeaturesClient {
    private static final LinkedHashSet<BlazeRegistry.Key<Overlay>> MUT_OVERLAYS = new LinkedHashSet<>();
    public static final Set<BlazeRegistry.Key<Overlay>> OVERLAYS = Collections.unmodifiableSet(MUT_OVERLAYS);

    public static final KeyMapping KEY_MAPS = new KeyMapping("blazemap.key.maps", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, BlazeMap.MOD_NAME);
    public static final KeyMapping KEY_ZOOM = new KeyMapping("blazemap.key.zoom", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, BlazeMap.MOD_NAME);
    public static final KeyMapping KEY_WAYPOINTS = new KeyMapping("blazemap.key.waypoints", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, BlazeMap.MOD_NAME);

    private static boolean mapping = false;
    private static boolean maps = false;
    private static boolean waypoints = false;
    private static boolean overlays = false;

    public static boolean hasMapping() {
        return mapping;
    }

    public static boolean hasMaps() {
        return maps;
    }

    public static boolean hasWaypoints() {
        return waypoints &&
            (BlazeMapConfig.CLIENT.clientFeatures.displayWaypointsOnMap.get() ||
                BlazeMapConfig.CLIENT.clientFeatures.renderWaypointsInWorld.get());
    }

    public static boolean hasWaypointsOnMap() {
        return waypoints && BlazeMapConfig.CLIENT.clientFeatures.displayWaypointsOnMap.get();
    }

    public static boolean hasOverlays() {
        return overlays;
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

    public static void initOverlays() {
        BlazeMapAPI.OVERLAYS.register(new GridOverlay());
        BlazeMapAPI.OVERLAYS.register(new WaypointOverlay());
        BlazeMapAPI.OVERLAYS.register(new EntityOverlay.Players());
        BlazeMapAPI.OVERLAYS.register(new EntityOverlay.Villagers());
        BlazeMapAPI.OVERLAYS.register(new EntityOverlay.Animals());
        BlazeMapAPI.OVERLAYS.register(new EntityOverlay.Enemies());
        overlays = true;
    }

    public static void initMaps() {
        ClientRegistry.registerKeyBinding(KEY_MAPS);
        ClientRegistry.registerKeyBinding(KEY_ZOOM);
        ClientRegistry.registerKeyBinding(KEY_WAYPOINTS);

        BlazeMapAPI.OBJECT_RENDERERS.register(new DefaultObjectRenderer());

        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener(MapRenderer::onDimensionChange);
        bus.addListener(MapRenderer::onMapLabelAdded);
        bus.addListener(MapRenderer::onMapLabelRemoved);
        bus.addListener(BlazeMapFeaturesClient::mapOverlays);
        bus.addListener(BlazeMapFeaturesClient::mapKeybinds);
        bus.addListener(BlazeMapFeaturesClient::mapMenu);

        maps = true;
    }

    private static void mapOverlays(BlazeRegistriesFrozenEvent evt) {
        OverlayOrderingEvent event = new OverlayOrderingEvent(MUT_OVERLAYS);
        event.add(BlazeMapReferences.Overlays.GRID);
        if(hasWaypointsOnMap()) {
            event.add(BlazeMapReferences.Overlays.WAYPOINTS);
        }
        event.add(
            BlazeMapReferences.Overlays.PLAYERS,
            BlazeMapReferences.Overlays.VILLAGERS,
            BlazeMapReferences.Overlays.ANIMALS,
            BlazeMapReferences.Overlays.ENEMIES
        );
        MinecraftForge.EVENT_BUS.post(event);
        event.finish();
        overlays = MUT_OVERLAYS.size() > 0;
    }

    private static void mapKeybinds(InputEvent.KeyInputEvent evt) {
        if(KEY_MAPS.isDown()) {
            if(Screen.hasShiftDown()) {
                MinimapOptionsGui.open();
            }
            else {
                WorldMapGui.open();
            }
        }
        if(KEY_WAYPOINTS.isDown() && hasWaypoints()) {
            if(Screen.hasShiftDown()) {
                WaypointManagerGui.open();
            }
            else {
                // WaypointEditorGui.open();
                new WaypointEditorFragment().open();
            }
        }
        if(KEY_ZOOM.isDown()) {
            if(Screen.hasShiftDown()) {
                MinimapRenderer.INSTANCE.synchronizer.zoomOut();
            }
            else {
                MinimapRenderer.INSTANCE.synchronizer.zoomIn();
            }
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

    public static void initWaypoints() {
        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener(WaypointEditorGui::onDimensionChanged);
        bus.addListener(WaypointManagerGui::onDimensionChanged);
        bus.addListener(EventPriority.HIGHEST, WaypointStore::onServerJoined);
        bus.addListener(WorldMapMenu::trackWaypointStore);
        bus.addListener(WaypointOverlay::onDimensionChange);

        WaypointRenderer.init();

        waypoints = true;
    }
}
