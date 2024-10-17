package com.eerussianguy.blazemap.api.maps;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.common.MinecraftForge;

import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.event.ComponentOrderingEvent.LayerOrderingEvent;

/**
 * Each available map in Blaze Map is defined by MapType.
 * These objects do no processing of any sort and merely define a structure.
 * The provided list of layers defines what layers are rendered on the screen,
 * the first layer being rendered on the bottom and the last on top.
 *
 * @author LordFokas
 */
public class MapType extends NamedMapComponent<MapType> {
    private final LinkedHashSet<BlazeRegistry.Key<Layer>> layers;
    private final Set<BlazeRegistry.Key<Layer>> layerView;
    private boolean inflated;

    @SafeVarargs
    public MapType(BlazeRegistry.Key<MapType> id, TranslatableComponent name, ResourceLocation icon, BlazeRegistry.Key<Layer>... layers) {
        super(id, name, icon);
        this.layers = new LinkedHashSet<>(Arrays.asList(layers));
        this.layerView = Collections.unmodifiableSet(this.layers);
        checkValid();
    }

    public Set<BlazeRegistry.Key<Layer>> getLayers() {
        return layerView;
    }

    /** Used by the engine to give addons a chance to contribute to an external map type, do not call this method. */
    public void inflate() {
        if(inflated) throw new IllegalStateException("MapType " + id + "already inflated");
        inflated = true;

        var event = new LayerOrderingEvent(id, layers);
        MinecraftForge.EVENT_BUS.post(event);
        event.finish();

        checkValid();
    }

    private void checkValid() {
        if(layers.size() == 0) throw new IllegalStateException("MapType "+id+" must have at least 1 layer");

        int index = 0;
        for(var key : layers) {
            var layer = key.value();
            if(index == 0) {
                if(!layer.isBottomLayer()) throw new IllegalStateException("MapType "+id+" has non-opaque layer "+layer.getID()+" at the bottom (index 0)");
            } else {
                if(layer.isBottomLayer()) throw new IllegalStateException("MapType "+id+" has opaque layer "+layer.getID()+" above the bottom (index "+index+")");
            }
            index++;
        }
    }
}
