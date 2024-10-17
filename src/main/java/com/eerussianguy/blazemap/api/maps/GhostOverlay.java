package com.eerussianguy.blazemap.api.maps;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.util.RegionPos;

/**
 * A GhostOverlay is an overlay type that does not render pixels in the map.
 * Its use case is specifically to present markers (labels, waypoints, mobs) on the map.
 *
 * @author LordFokas
 */
public class GhostOverlay extends Overlay {
    protected GhostOverlay(BlazeRegistry.Key<Overlay> id, TranslatableComponent name, ResourceLocation icon) {
        super(id, Type.INVISIBLE, name, icon);
    }

    @Override // GhostOverlays do not have map pixels
    public final PixelSource getPixelSource(ResourceKey<Level> dimension, RegionPos region, TileResolution resolution) {
        return null;
    }
}