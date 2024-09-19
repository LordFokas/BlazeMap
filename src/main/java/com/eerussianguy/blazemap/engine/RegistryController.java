package com.eerussianguy.blazemap.engine;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLEnvironment;

import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.event.BlazeRegistryEvent;
import com.eerussianguy.blazemap.integration.KnownMods;

public class RegistryController {
    private static boolean frozenRegistries = false;

    public static synchronized void ensureRegistriesReady() {
        if(!frozenRegistries) {
            IEventBus bus = MinecraftForge.EVENT_BUS;
            bus.post(new BlazeRegistryEvent.MasterDataRegistryEvent());
            BlazeMapAPI.MASTER_DATA.freeze();

            bus.post(new BlazeRegistryEvent.CollectorRegistryEvent());
            BlazeMapAPI.COLLECTORS.freeze();

            bus.post(new BlazeRegistryEvent.TransformerRegistryEvent());
            BlazeMapAPI.TRANSFORMERS.freeze();

            bus.post(new BlazeRegistryEvent.ProcessorRegistryEvent());
            BlazeMapAPI.PROCESSORS.freeze();

            if(FMLEnvironment.dist == Dist.CLIENT) {
                ensureClientRegistriesReady();
            }
            frozenRegistries = true;

            KnownMods.addRegistry(BlazeMapAPI.MASTER_DATA);
            KnownMods.addRegistry(BlazeMapAPI.COLLECTORS);
            KnownMods.addRegistry(BlazeMapAPI.TRANSFORMERS);
            KnownMods.addRegistry(BlazeMapAPI.PROCESSORS);
        }
    }

    private static void ensureClientRegistriesReady() {
        IEventBus bus = MinecraftForge.EVENT_BUS;

        bus.post(new BlazeRegistryEvent.LayerRegistryEvent());
        BlazeMapAPI.LAYERS.freeze();

        bus.post(new BlazeRegistryEvent.MapTypeRegistryEvent());
        BlazeMapAPI.MAPTYPES.freeze();

        bus.post(new BlazeRegistryEvent.ObjectRendererRegistryEvent());
        BlazeMapAPI.OBJECT_RENDERERS.freeze();

        for(var key : BlazeMapAPI.MAPTYPES.keys()) {
            key.value().inflate();
        }

        KnownMods.addRegistry(BlazeMapAPI.LAYERS);
        KnownMods.addRegistry(BlazeMapAPI.MAPTYPES);
        KnownMods.addRegistry(BlazeMapAPI.OBJECT_RENDERERS);
    }
}
