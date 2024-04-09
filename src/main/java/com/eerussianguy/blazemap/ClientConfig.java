package com.eerussianguy.blazemap;

import java.util.List;
import java.util.function.Function;

import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.fml.loading.FMLEnvironment;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.feature.maps.MinimapRenderer;
import com.eerussianguy.blazemap.feature.maps.WorldMapGui;
import com.eerussianguy.blazemap.util.IConfigAdapter;
import com.eerussianguy.blazemap.util.LayerListAdapter;
import com.eerussianguy.blazemap.util.MapTypeAdapter;

import static com.eerussianguy.blazemap.BlazeMap.MOD_ID;

/**
 * Forge configs happen to be a very simple way to serialize things across saves and hold data within a particular instance
 * It is not necessarily expected that the player will be editing the config
 * We are free to use key binds to allow what is essentially config editing on the fly
 */
public class ClientConfig {
    public final BooleanValue enableDebug, enableServerEngine;
    public final MapConfig worldMap;
    public final MinimapConfig minimap;

    ClientConfig(Builder innerBuilder) {
        Function<String, Builder> builder = name -> innerBuilder.translation(MOD_ID + ".config.client." + name);

        innerBuilder.push("general");
        enableDebug = builder.apply("enableDebug").comment("Enable debug mode?").define("enableDebug", !FMLEnvironment.production);
        enableServerEngine = builder.apply("enableServerEngine").comment("Enable Server Engine on the Integrated Server").define("enableServerEngine", true);
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

    public static class MinimapConfig extends MapConfig {
        public final BooleanValue enabled;
        public final IntValue positionX, positionY;
        public final IntValue width, height;

        MinimapConfig(Function<String, Builder> builder) {
            super(builder, MinimapRenderer.MIN_ZOOM, MinimapRenderer.MAX_ZOOM);
            this.enabled = builder.apply("enabled").comment("Enable the minimap?").define("enabled", true);
            this.positionX = builder.apply("positionX").comment("Minimap horizontal position on screen").defineInRange("positionX", 0, 0, 16000);
            this.positionY = builder.apply("positionY").comment("Minimap vertical position on screen").defineInRange("positionY", 0, 0, 9000);
            this.width = builder.apply("width").comment("Minimap widget width").defineInRange("width", 256, 128, 1600);
            this.height = builder.apply("height").comment("Minimap widget height").defineInRange("height", 256, 128, 1600);
        }
    }
}
