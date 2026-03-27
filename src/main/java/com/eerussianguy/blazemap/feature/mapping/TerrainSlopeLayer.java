package com.eerussianguy.blazemap.feature.mapping;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.TerrainSlopeMD;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.TileResolution;
import com.eerussianguy.blazemap.api.util.ArrayAggregator;
import com.eerussianguy.blazemap.api.util.DataSource;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.Helpers;
import com.mojang.blaze3d.platform.NativeImage;

public class TerrainSlopeLayer extends Layer {
    // Note that this range bounds 10 * ln(the slope), not the slope value itself
    private static final int SHADING_RANGE = 30;

    public TerrainSlopeLayer() {
        super(
            BlazeMapReferences.Layers.TERRAIN_SLOPE,
            Helpers.translate("blazemap.terrain_slope"),
            // This should be changed at some point to its own dedicated image
            BlazeMap.resource("textures/map_icons/layer_terrain_isolines.png"),
            false,

            BlazeMapReferences.MasterData.TERRAIN_SLOPE
        );
    }

    @Override
    public boolean renderTile(NativeImage tile, TileResolution resolution, DataSource data, int xGridOffset, int zGridOffset) {
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
            float slopeLog = (float)Helpers.clamp(-SHADING_RANGE * 0.75f, (Math.log(slope) * 10), SHADING_RANGE);
            int shadow = Colors.interpolate(0x00000000, -SHADING_RANGE * 0.75f, 0x70000000, SHADING_RANGE, slopeLog);
            tile.setPixelRGBA(x, z, shadow);

        } else {
            float slopeLog = (float)Helpers.clamp(-SHADING_RANGE * 0.5f, (Math.log(-slope) * 10), SHADING_RANGE);
            int sunlight = Colors.interpolate(0x00FFFFFF, -SHADING_RANGE * 0.5f, 0x60FFFFFF, SHADING_RANGE, slopeLog);
            tile.setPixelRGBA(x, z, sunlight);
        }
    }
}
