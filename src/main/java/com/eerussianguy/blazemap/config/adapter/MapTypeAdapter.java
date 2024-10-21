package com.eerussianguy.blazemap.config.adapter;

import net.minecraftforge.common.ForgeConfigSpec;

import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.maps.MapType;

public class MapTypeAdapter implements ConfigAdapter<BlazeRegistry.Key<MapType>> {
    private final ForgeConfigSpec.ConfigValue<String> target;

    public MapTypeAdapter(ForgeConfigSpec.ConfigValue<String> target) {
        this.target = target;
    }

    @Override
    public BlazeRegistry.Key<MapType> get() {
        return BlazeMapAPI.MAPTYPES.findOrCreate(target.get());
    }

    @Override
    public void set(BlazeRegistry.Key<MapType> value) {
        target.set(value.toString());
    }
}
