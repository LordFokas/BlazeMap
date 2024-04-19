package com.eerussianguy.blazemap;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.eerussianguy.blazemap.feature.BlazeMapFeaturesClient;
import com.eerussianguy.blazemap.feature.Overlays;


public class FMLEventHandler {

    public static void init() {
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(BlazeMapFeaturesClient::onKeyBindRegister);
        bus.addListener(Overlays::onRegisterOverlays);
    }
}
