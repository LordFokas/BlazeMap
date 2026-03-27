package com.eerussianguy.blazemap.config;

import java.util.List;
import java.util.function.Function;

import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.fml.loading.FMLEnvironment;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.api.maps.Overlay;
import com.eerussianguy.blazemap.feature.maps.MinimapRenderer;
import com.eerussianguy.blazemap.feature.maps.WorldMapGui;
import com.eerussianguy.blazemap.config.adapter.ConfigAdapter;
import com.eerussianguy.blazemap.config.adapter.NamedMapComponentListAdapter;
import com.eerussianguy.blazemap.config.adapter.MapTypeAdapter;

/**
 * Forge configs happen to be a very simple way to serialize things across saves and hold data within a particular instance
 * It is not necessarily expected that the player will be editing the config
 * We are free to use key binds to allow what is essentially config editing on the fly
 */
public class ClientConfig {
    public final BooleanValue enableDebug;
    public final IntValue atlasMaxSize;
    public final FeaturesConfig clientFeatures;
    public final MapConfig worldMap;
    public final MinimapConfig minimap;

    ClientConfig(Builder innerBuilder) {
        Function<String, Builder> builder = name -> innerBuilder.translation(BlazeMap.MOD_ID + ".config.client." + name);

        innerBuilder.push("general");
        enableDebug = builder.apply("enableDebug").comment("Enable debug mode?").define("enableDebug", !FMLEnvironment.production);
        atlasMaxSize = builder.apply("atlasMaxSize").comment("Max memory allocation for atlas export, in Megabytes").defineInRange("atlasMaxSize", 1024, 256, 16384);
        innerBuilder.pop();

        innerBuilder.comment("Enable or disable (un)desired features");
        innerBuilder.push("clientFeatures");
        clientFeatures = new FeaturesConfig(builder);
        innerBuilder.pop();

        innerBuilder.push("worldmap");
        worldMap = new MapConfig(builder, WorldMapGui.MIN_ZOOM, WorldMapGui.MAX_ZOOM);
        innerBuilder.pop();

        innerBuilder.push("minimap");
        minimap = new MinimapConfig(builder);
        innerBuilder.pop();
    }

    public static class FeaturesConfig {
        public final BooleanValue displayCoords;
        public final BooleanValue displayFriendlyMobs;
        public final BooleanValue displayHostileMobs;
        public final BooleanValue displayOtherPlayers;
        public final BooleanValue displayWaypointsOnMap;
        public final BooleanValue renderWaypointsInWorld;
        public final BooleanValue deathWaypoints;

        FeaturesConfig(Function<String, Builder> builder) {
            this.displayCoords = builder.apply("displayCoords")
                .comment("Enables current coordinates to render under minimap")
                .define("displayCoords", true);

            this.displayFriendlyMobs = builder.apply("displayFriendlyMobs")
                .comment("Enables markers showing the location of nearby friendly mobs")
                .define("displayFriendlyMobs", true);
            this.displayHostileMobs = builder.apply("displayHostileMobs")
                .comment("Enables markers showing the location of nearby hostile mobs")
                .define("displayHostileMobs", true);
            this.displayOtherPlayers = builder.apply("displayOtherPlayers")
                .comment("Enables markers showing the location of other players")
                .define("displayOtherPlayers", true);

            this.displayWaypointsOnMap = builder.apply("displayWaypointsOnMap")
                .comment("Enables waypoints to be shown on the map itself")
                .define("displayWaypointsOnMap", true);
            this.renderWaypointsInWorld = builder.apply("renderWaypointsInWorld")
                .comment("Enables waypoints to be rendered in the world")
                .define("renderWaypointsInWorld", false);
            this.deathWaypoints = builder.apply("deathWaypoints")
                .comment("Automatically create special waypoints at the place of your death")
                .define("deathWaypoints", true);
        }
    }

    public static class MapConfig {
        public final ConfigAdapter<Key<MapType>> activeMap;
        public final ConfigAdapter<List<Key<Layer>>> disabledLayers;
        public final ConfigAdapter<List<Key<Overlay>>> disabledOverlays;
        public final DoubleValue zoom;

        MapConfig(Function<String, Builder> builder, double minZoom, double maxZoom) {
            ConfigValue<String> _activeMap = builder.apply("activeMap").comment("List of disabled Layers, comma separated").define("activeMap", BlazeMapReferences.MapTypes.AERIAL_VIEW.toString());
            ConfigValue<List<? extends String>> _disabledLayers = builder.apply("disabledLayers").comment("List of disabled Layers, comma separated").defineList("disabledLayers", List::of, o -> o instanceof String);
            ConfigValue<List<? extends String>> _disabledOverlays = builder.apply("disabledOverlays").comment("List of disabled Overlays, comma separated").defineList("disabledOverlays", List::of, o -> o instanceof String);
            this.zoom = builder.apply("zoom").comment("Zoom level. Must be a power of 2").defineInRange("zoom", 1.0, minZoom, maxZoom);

            this.activeMap = new MapTypeAdapter(_activeMap);
            this.disabledLayers = new NamedMapComponentListAdapter<>(_disabledLayers, BlazeMapAPI.LAYERS);
            this.disabledOverlays = new NamedMapComponentListAdapter<>(_disabledOverlays, BlazeMapAPI.OVERLAYS);
        }
    }

    public static class MinimapConfig extends MapConfig implements MinimapConfigFacade.IWidgetConfig {
        public final BooleanValue enabled;
        public final IntValue positionX, positionY;
        public final IntValue width, height;

        private final MinimapConfigFacade.IntFacade _positionX, _positionY, _width, _height;

        MinimapConfig(Function<String, Builder> builder) {
            super(builder, MinimapRenderer.MIN_ZOOM, MinimapRenderer.MAX_ZOOM);
            this.enabled = builder.apply("enabled").comment("Enable the minimap?").define("enabled", true);
            this.positionX = builder.apply("positionX").comment("Minimap horizontal position on screen").defineInRange("positionX", 15, 0, 16000);
            this.positionY = builder.apply("positionY").comment("Minimap vertical position on screen").defineInRange("positionY", 15, 0, 9000);
            this.width = builder.apply("width").comment("Minimap widget width").defineInRange("width", 256, 128, 1600);
            this.height = builder.apply("height").comment("Minimap widget height").defineInRange("height", 256, 128, 1600);

            // Facade stuff for BME-54
            this._positionX = new MinimapConfigFacade.IntFacade(positionX);
            this._positionY = new MinimapConfigFacade.IntFacade(positionY);
            this._width = new MinimapConfigFacade.IntFacade(width);
            this._height = new MinimapConfigFacade.IntFacade(height);
        }

        @Override
        public MinimapConfigFacade.IntFacade positionX() {
            return _positionX;
        }

        @Override
        public MinimapConfigFacade.IntFacade positionY() {
            return _positionY;
        }

        @Override
        public MinimapConfigFacade.IntFacade width() {
            return _width;
        }

        @Override
        public MinimapConfigFacade.IntFacade height() {
            return _height;
        }
    }
}
