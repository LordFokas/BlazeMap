package com.eerussianguy.blazemap.feature.mapping;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.TerrainSlopeMD;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.TileResolution;
import com.eerussianguy.blazemap.api.util.ArrayAggregator;
import com.eerussianguy.blazemap.api.util.IDataSource;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.Helpers;
import com.mojang.blaze3d.platform.NativeImage;

public class TerrainSlopeLayer extends Layer {
    // Note that this range bounds 10 * ln(the slope), not the slope value itself
    private static final int SHADING_RANGE = 25;

    public TerrainSlopeLayer() {
        super(
            BlazeMapReferences.Layers.TERRAIN_SLOPE,
            Helpers.translate("blazemap.terrain_slope"),
            // This should be changed at some point to its own dedicated image
            Helpers.identifier("textures/map_icons/layer_terrain_isolines.png"),

            BlazeMapReferences.MasterData.TERRAIN_SLOPE
        );
    }

    @Override
    public boolean renderTile(NativeImage tile, TileResolution resolution, IDataSource data, int xGridOffset, int zGridOffset) {
        TerrainSlopeMD terrain = (TerrainSlopeMD) data.get(BlazeMapReferences.MasterData.TERRAIN_SLOPE);

        foreachPixel(resolution, (x, z) -> {
            float slope = ArrayAggregator.avg(relevantData(resolution, x, z, terrain.slopemap));
            paintSlope(tile, x, z, slope);
        });

        return true;
    }

    private static void paintSlope(NativeImage tile, int x, int z, float slope) {
        if (slope == 0) {
            // No slope, so nothing to change
            return;

        } else if (slope > 0) {
            int slopeLog = Helpers.clamp(0, (int)(Math.log(slope) * 10), SHADING_RANGE);
            int shadow = Colors.interpolate(0x30000000, 0, 0x70000000, SHADING_RANGE, slopeLog);
            tile.setPixelRGBA(x, z, shadow);

        } else {
            int slopeLog = Helpers.clamp(0, (int)(Math.log(-slope) * 10), SHADING_RANGE);
            int sunlight = Colors.interpolate(0x20FFFFFF, 0, 0x70FFFFFF, SHADING_RANGE, slopeLog);
            tile.setPixelRGBA(x, z, sunlight);
        }
    }
}
