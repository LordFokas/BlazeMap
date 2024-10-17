package com.eerussianguy.blazemap.config.adapter;

import java.util.List;
import java.util.stream.Collectors;

import net.minecraftforge.common.ForgeConfigSpec;

import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.NamedMapComponent;

public class NamedMapComponentListAdapter<C extends NamedMapComponent<C>> implements ConfigAdapter<List<Key<C>>> {
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> target;
    private final BlazeRegistry<C> registry;

    public NamedMapComponentListAdapter(ForgeConfigSpec.ConfigValue<List<? extends String>> target, BlazeRegistry<C> registry) {
        this.target = target;
        this.registry = registry;
    }

    @Override
    public List<Key<C>> get() {
        return target.get().stream().map(registry::findOrCreate).collect(Collectors.toList());
    }

    @Override
    public void set(List<Key<C>> value) {
        target.set(value.stream().map(Key::toString).collect(Collectors.toList()));
    }
}
