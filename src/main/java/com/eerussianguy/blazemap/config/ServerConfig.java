package com.eerussianguy.blazemap.config;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.registries.ForgeRegistries;

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
    public final MapItemRequirement mapItemRequirement;

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

        innerBuilder.comment("Configurations for requiring an item to map the world");
        innerBuilder.push("required_item");
        mapItemRequirement = new MapItemRequirement(builder);
        innerBuilder.pop();
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

    private enum ListMode {
        WHITELIST, BLACKLIST;

        boolean allows(boolean found) {
            return switch(this) {
                case BLACKLIST -> !found;
                case WHITELIST -> found;
            };
        }
    }

    public static final class MapItemRequirement {
        private final EnumValue<RequirementMode> write_mode, read_mode;
        private final ConfigValue<String> item;
        private final HashMap<String, Predicate<ItemStack>> predicates = new HashMap<>();

        MapItemRequirement(Function<String, Builder> builder) {
            write_mode = builder.apply("write_mode").comment("Restriction level to update the player's map").defineEnum("write_mode", RequirementMode.UNRESTRICTED);
            read_mode = builder.apply("read_mode")
                .comment("Restriction level to show the players minimap.\nIf not unrestricted, the restriction level for the fullscreen map is always ITEM_IN_INVENTORY, for convenience.")
                .defineEnum("read_mode", RequirementMode.UNRESTRICTED);
            item = builder.apply("map_item").comment("Required item, if not unrestricted. Item ID or Tag. Tags must begin with '#'").define("map_item", "minecraft:map");
        }

        public boolean canPlayerAccessMap(Player player, MapAccess access) {
            RequirementMode mode = (access == MapAccess.WRITE ? write_mode : read_mode).get();
            if(mode == RequirementMode.UNRESTRICTED) return true;
            if(access == MapAccess.READ_STATIC) mode = RequirementMode.ITEM_IN_INVENTORY;

            Predicate<ItemStack> predicate = predicates.computeIfAbsent(item.get(), string -> {
                if(string.startsWith("#")) {
                    var manager = ForgeRegistries.ITEMS.tags();
                    var tagKey = manager.createTagKey(new ResourceLocation(string.replace("#", "")));
                    return (ItemStack stack) -> stack.is(tagKey);
                } else {
                    var id = new ResourceLocation(string);
                    return (ItemStack stack) -> stack.getItem().getRegistryName().equals(id);
                }
            });

            var inventory = player.getInventory();
            switch(mode) {
                case ITEM_IN_INVENTORY:
                    for(int slot = 9; slot < 36; slot++) {
                        if(predicate.test(inventory.getItem(slot))) return true;
                    }
                case ITEM_IN_HOTBAR:
                    for(int slot = 0; slot < 9; slot++) {
                        if(predicate.test(inventory.getItem(slot))) return true;
                    }
                case ITEM_IN_HAND:
                    return predicate.test(player.getItemInHand(InteractionHand.MAIN_HAND))
                        || predicate.test(player.getItemInHand(InteractionHand.OFF_HAND));
            }

            BlazeMap.LOGGER.error("Execution should never be able to reach this point: Unable to calculate player mapping restriction");
            return false;
        }
    }

    public enum MapAccess {
        WRITE, // Update the map
        READ_LIVE, // Read the map live, like with minimaps
        READ_STATIC // Read the map in a more static context, like stopping to open the full map
    }

    private enum RequirementMode {
        UNRESTRICTED,
        ITEM_IN_INVENTORY,
        ITEM_IN_HOTBAR,
        ITEM_IN_HAND
    }
}
