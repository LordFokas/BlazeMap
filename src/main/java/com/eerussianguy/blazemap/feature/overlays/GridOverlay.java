package com.eerussianguy.blazemap.feature.overlays;

import java.util.EnumMap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.maps.Overlay;
import com.eerussianguy.blazemap.api.maps.PixelSource;
import com.eerussianguy.blazemap.api.maps.TileResolution;
import com.eerussianguy.blazemap.api.util.RegionPos;
import com.eerussianguy.blazemap.util.Helpers;

public class GridOverlay extends Overlay {
    private static final EnumMap<TileResolution, GridPixelSource> SOURCES = new EnumMap<>(TileResolution.class);
    private static final int REGION = 512;
    private static final int CHUNK = 16;

    public GridOverlay() {
        super(
            BlazeMapReferences.Overlays.GRID,
            Helpers.translate("blazemap.grid"),
            Helpers.identifier("textures/map_icons/overlay_grid.png")
        );
    }

    @Override
    public PixelSource getPixelSource(ResourceKey<Level> dimension, RegionPos region, TileResolution resolution) {
        return SOURCES.computeIfAbsent(resolution, $ -> new GridPixelSource(resolution));
    }

    private static class GridPixelSource implements PixelSource {
        private final TileResolution resolution;

        private GridPixelSource(TileResolution resolution) {
            this.resolution = resolution;
        }

        @Override
        public int getPixel(int x, int y) {
            if(isBorder(x, REGION) || isBorder(y, REGION)) return 0x7F0000FF;
            if(resolution.pixelWidth > 2) return 0;

            if(isBorder(x, CHUNK) || isBorder(y, CHUNK)) return 0x7FC0C0C0;
            return 0;
        }

        private boolean isBorder(int param, int type) {
            return param % (type / resolution.pixelWidth) == 0;
        }

        @Override
        public int getWidth() {
            return resolution.regionWidth;
        }

        @Override
        public int getHeight() {
            return resolution.regionWidth;
        }
    }
}
