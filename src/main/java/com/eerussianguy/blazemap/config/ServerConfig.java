package com.eerussianguy.blazemap.config;

import java.util.List;
import java.util.function.Function;

import net.minecraftforge.common.ForgeConfigSpec.*;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.api.maps.NamedMapComponent;
import com.eerussianguy.blazemap.api.maps.Overlay;
import com.eerussianguy.blazemap.config.adapter.ConfigAdapter;
import com.eerussianguy.blazemap.config.adapter.NamedMapComponentListAdapter;

public class ServerConfig {
    public final NamedMapComponentPermissions<MapType> mapPermissions;
    public final NamedMapComponentPermissions<Layer> layerPermissions;
    public final NamedMapComponentPermissions<Overlay> overlayPermissions;

    ServerConfig(Builder innerBuilder) {
        Function<String, Builder> builder = name -> innerBuilder.translation(BlazeMap.MOD_ID + ".config.server." + name);

        innerBuilder.push("permissions");

        innerBuilder.comment("Restrict which maps the players may use");
        innerBuilder.push("maps");
        mapPermissions = new NamedMapComponentPermissions<>(builder, "maps", BlazeMapAPI.MAPTYPES);
        innerBuilder.pop();

        innerBuilder.comment("Restrict which layers the players may use");
        innerBuilder.push("layers");
        layerPermissions = new NamedMapComponentPermissions<>(builder, "layers", BlazeMapAPI.LAYERS);
        innerBuilder.pop();

        innerBuilder.comment("Restrict which overlays the players may use");
        innerBuilder.push("overlays");
        overlayPermissions = new NamedMapComponentPermissions<>(builder, "overlays", BlazeMapAPI.OVERLAYS);
        innerBuilder.pop();

        innerBuilder.pop();
    }

    private enum ListMode {
        WHITELIST, BLACKLIST;

        boolean allows(boolean found) {
            return switch(this) {
                case BLACKLIST -> !found;
                case WHITELIST -> found;
            };
        }
    }

    public static final class NamedMapComponentPermissions<C extends NamedMapComponent<C>> {
        private final EnumValue<ListMode> listMode;
        private final ConfigAdapter<List<BlazeRegistry.Key<C>>> list;

        NamedMapComponentPermissions(Function<String, Builder> builder, String key, BlazeRegistry<C> registry){
            listMode = builder.apply("mode").defineEnum("mode", ListMode.BLACKLIST);
            ConfigValue<List<? extends String>> _list = builder.apply(key).comment("List of "+key+", comma separated").defineList(key, List::of, o -> o instanceof String);
            list = new NamedMapComponentListAdapter<>(_list, registry);
        }

        public boolean isAllowed(BlazeRegistry.Key<C> key) {
            return listMode.get().allows(list.get().contains(key));
        }
    }
}
