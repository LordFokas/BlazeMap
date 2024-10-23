package com.eerussianguy.blazemap;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.engine.RegistryController;
import com.eerussianguy.blazemap.engine.client.ClientEngine;
import com.eerussianguy.blazemap.engine.server.ServerEngine;
import com.eerussianguy.blazemap.feature.BlazeMapCommandsClient;
import com.eerussianguy.blazemap.feature.BlazeMapFeaturesClient;
import com.eerussianguy.blazemap.feature.BlazeMapFeaturesCommon;
import com.eerussianguy.blazemap.integration.KnownMods;
import com.eerussianguy.blazemap.integration.ModIntegration;
import com.eerussianguy.blazemap.integration.ftbchunks.FTBChunksPlugin;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import static com.eerussianguy.blazemap.BlazeMap.MOD_ID;

@Mod(MOD_ID)
public class BlazeMap {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String MOD_ID = "blazemap";
    public static final String MOD_NAME = "Blaze Map";

    public static final List<ModIntegration> INTEGRATIONS = List.of(
        new FTBChunksPlugin()
    );

    public static ResourceLocation resource(String name) {
        return new ResourceLocation(MOD_ID, name);
    }

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
            // Client side objects are forbidden in the dedicated server.
            // The others are frozen by the RegistryController when the time comes.
            RegistryController.freezeClientRegistries();
        }
    }

    public void setup(FMLCommonSetupEvent event) {
        KnownMods.init();

        // We are client side, enable client engine. Required on client.
        if(FMLEnvironment.dist == Dist.CLIENT) {
            ClientEngine.init();
        }

        // Regardless of side, server engine is optional.
        // This might be helpful later on, on server side, where we want the engine off but other features on.
        // So removing the mod to disable the server engine will not be an option.
        // For now, though, there are no other server features.
        if(BlazeMapConfig.COMMON.enableServerEngine.get()){
            ServerEngine.init();
        }

        // Initialize common sided features
        BlazeMapFeaturesCommon.initMapping();

        // Initialize client-only features
        if(FMLEnvironment.dist == Dist.CLIENT) {
            BlazeMapFeaturesClient.initMapping();
            BlazeMapFeaturesClient.initOverlays();
            BlazeMapFeaturesClient.initMaps();
            BlazeMapFeaturesClient.initWaypoints();
            BlazeMapCommandsClient.init();
        }

        for(var integration : INTEGRATIONS) {
            integration.init();
        }
    }
}
