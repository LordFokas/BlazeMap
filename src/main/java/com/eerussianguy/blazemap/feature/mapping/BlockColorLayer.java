package com.eerussianguy.blazemap.feature.mapping;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.BlockColorMD;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.TileResolution;
import com.eerussianguy.blazemap.api.util.ArrayAggregator;
import com.eerussianguy.blazemap.api.util.DataSource;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.Helpers;
import com.mojang.blaze3d.platform.NativeImage;

public class BlockColorLayer extends Layer {

    public BlockColorLayer() {
        super(
            BlazeMapReferences.Layers.BLOCK_COLOR,
            Helpers.translate("blazemap.block_color"),
            BlazeMap.resource("textures/map_icons/layer_block_color.png"),
            true,

            BlazeMapReferences.MasterData.BLOCK_COLOR
        );
    }

    @Override
    public boolean renderTile(NativeImage tile, TileResolution resolution, DataSource data, int xGridOffset, int zGridOffset) {
        BlockColorMD blocks = (BlockColorMD) data.get(BlazeMapReferences.MasterData.BLOCK_COLOR);
        if(blocks == null) return false;

        int[][] blockColors = blocks.colors;

        foreachPixel(resolution, (x, z) -> {
            int color = ArrayAggregator.avgColor(relevantData(resolution, x, z, blockColors));
            tile.setPixelRGBA(x, z, Colors.abgr(OPAQUE | color));
        });

        return true;
    }
}
