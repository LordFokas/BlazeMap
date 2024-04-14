package com.eerussianguy.blazemap.config;

import java.util.List;
import java.util.function.Function;

import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.fml.loading.FMLEnvironment;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.feature.maps.MinimapRenderer;
import com.eerussianguy.blazemap.feature.maps.WorldMapGui;
import com.eerussianguy.blazemap.util.IConfigAdapter;
import com.eerussianguy.blazemap.util.LayerListAdapter;
import com.eerussianguy.blazemap.util.MapTypeAdapter;

/**
 * Forge configs happen to be a very simple way to serialize things across saves and hold data within a particular instance
 * It is not necessarily expected that the player will be editing the config
 * We are free to use key binds to allow what is essentially config editing on the fly
 */
public class ClientConfig {
    public final BooleanValue enableDebug;
    public final MapConfig worldMap;
    public final MinimapConfig minimap;

    ClientConfig(Builder innerBuilder) {
        Function<String, Builder> builder = name -> innerBuilder.translation(BlazeMap.MOD_ID + ".config.client." + name);

        innerBuilder.push("general");
        enableDebug = builder.apply("enableDebug").comment("Enable debug mode?").define("enableDebug", !FMLEnvironment.production);
        innerBuilder.pop();

        innerBuilder.push("worldmap");
        worldMap = new MapConfig(builder, WorldMapGui.MIN_ZOOM, WorldMapGui.MAX_ZOOM);
        innerBuilder.pop();

        innerBuilder.push("minimap");
        minimap = new MinimapConfig(builder);
        innerBuilder.pop();
    }

    public static class MapConfig {
        public final IConfigAdapter<Key<MapType>> activeMap;
        public final IConfigAdapter<List<Key<Layer>>> disabledLayers;
        public final DoubleValue zoom;

        MapConfig(Function<String, Builder> builder, double minZoom, double maxZoom) {
            ConfigValue<String> _activeMap = builder.apply("activeMap").comment("List of disabled Layers, comma separated").define("activeMap", BlazeMapReferences.MapTypes.AERIAL_VIEW.toString());
            ConfigValue<List<? extends String>> _disabledLayers = builder.apply("disabledLayers").comment("List of disabled Layers, comma separated").defineList("disabledLayers", List::of, o -> o instanceof String);
            this.zoom = builder.apply("zoom").comment("Zoom level. Must be a power of 2").defineInRange("zoom", 1.0, minZoom, maxZoom);

            this.activeMap = new MapTypeAdapter(_activeMap);
            this.disabledLayers = new LayerListAdapter(_disabledLayers);
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
