package com.eerussianguy.blazemap.util;

import java.util.List;
import java.util.stream.Collectors;

import net.minecraftforge.common.ForgeConfigSpec;

import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.Overlay;

public class OverlayListAdapter implements IConfigAdapter<List<Key<Overlay>>> {
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> target;

    public OverlayListAdapter(ForgeConfigSpec.ConfigValue<List<? extends String>> target) {
        this.target = target;
    }

    @Override
    public List<Key<Overlay>> get() {
        return target.get().stream().map(BlazeMapAPI.OVERLAYS::findOrCreate).collect(Collectors.toList());
    }

    @Override
    public void set(List<Key<Overlay>> value) {
        target.set(value.stream().map(Key::toString).collect(Collectors.toList()));
    }
}
