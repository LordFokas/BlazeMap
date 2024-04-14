package com.eerussianguy.blazemap;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.engine.client.BlazeMapClientEngine;
import com.eerussianguy.blazemap.engine.server.BlazeMapServerEngine;
import com.eerussianguy.blazemap.feature.BlazeMapCommandsClient;
import com.eerussianguy.blazemap.feature.BlazeMapFeaturesClient;
import com.eerussianguy.blazemap.feature.BlazeMapFeaturesCommon;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import static com.eerussianguy.blazemap.BlazeMap.MOD_ID;

@Mod(MOD_ID)
public class BlazeMap {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String MOD_ID = "blazemap";
    public static final String MOD_NAME = "Blaze Map";

    public BlazeMap() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "Nothing", (remote, isServer) -> true));
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::setup);

        BlazeMapConfig.init();

        if(FMLEnvironment.dist == Dist.CLIENT) {
            FMLEventHandler.init();

            // Enabling this will log when certain events happen in the world, allowing you
            // to crossreference what's happening on screen with what's happening in the logs.
            // DebuggingEventHandler.init();
        }
        else {
            // These are forbidden in the dedicated server.
            // The others are frozen by the RegistryController when the time comes.
            BlazeMapAPI.LAYERS.freeze();
            BlazeMapAPI.MAPTYPES.freeze();
            BlazeMapAPI.OBJECT_RENDERERS.freeze();
        }
    }

    public void setup(FMLCommonSetupEvent event) {
        // We are client side, enable client engine. Required on client.
        if(FMLEnvironment.dist == Dist.CLIENT) {
            BlazeMapClientEngine.init();
        }

        // Regardless of side, server engine is optional.
        // This might be helpful later on, on server side, where we want the engine off but other features on.
        // So removing the mod to disable the server engine will not be an option.
        // For now, though, there are no other server features.
        if(BlazeMapConfig.COMMON.enableServerEngine.get()){
            BlazeMapServerEngine.init();
        }

        // Initialize common sided features
        BlazeMapFeaturesCommon.initMapping();

        // Initialize client-only features
        if(FMLEnvironment.dist == Dist.CLIENT) {
            BlazeMapFeaturesClient.initMapping();
            BlazeMapFeaturesClient.initMaps();
            BlazeMapFeaturesClient.initWaypoints();
            BlazeMapCommandsClient.init();
        }
    }
}
