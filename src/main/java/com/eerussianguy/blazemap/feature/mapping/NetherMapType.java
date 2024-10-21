package com.eerussianguy.blazemap.feature.mapping;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.lib.Helpers;

public class NetherMapType extends MapType {

    public NetherMapType() {
        super(BlazeMapReferences.MapTypes.NETHER, Helpers.translate("blazemap.nether"), BlazeMap.resource("textures/map_icons/map_nether.png"), BlazeMapReferences.Layers.NETHER);
    }

    @Override
    public boolean shouldRenderInDimension(ResourceKey<Level> dimension) {
        return dimension.equals(Level.NETHER);
    }
}
