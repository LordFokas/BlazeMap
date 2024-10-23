package com.eerussianguy.blazemap.config;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class BlazeMapConfig {
    public static final ClientConfig CLIENT = register(ClientConfig::new, ModConfig.Type.CLIENT);
    public static final CommonConfig COMMON = register(CommonConfig::new, ModConfig.Type.COMMON);
    public static final ServerConfig SERVER = register(ServerConfig::new, ModConfig.Type.SERVER);

    public static void init() {}

    private static <C> C register(Function<ForgeConfigSpec.Builder, C> factory, ModConfig.Type type) {
        Pair<C, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(factory);
        ModLoadingContext.get().registerConfig(type, specPair.getRight());
        return specPair.getLeft();
    }
}
