package com.eerussianguy.blazemap.feature.mapping;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.lib.Helpers;

public class AerialViewMapType extends MapType {

    public AerialViewMapType() {
        super(
            BlazeMapReferences.MapTypes.AERIAL_VIEW,
            Helpers.translate("blazemap.aerial_view"),
            BlazeMap.resource("textures/map_icons/map_aerial.png"),

            BlazeMapReferences.Layers.BLOCK_COLOR,
            BlazeMapReferences.Layers.TERRAIN_SLOPE
        );
    }
}
