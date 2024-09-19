package com.eerussianguy.blazemap.integration.ftbchunks;

import net.minecraftforge.common.MinecraftForge;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.event.BlazeRegistryEvent.LayerRegistryEvent;
import com.eerussianguy.blazemap.api.event.MapTypeInflationEvent;
import com.eerussianguy.blazemap.integration.ModIDs;
import com.eerussianguy.blazemap.integration.ModIntegration;

public class FTBChunksPlugin extends ModIntegration {

    public FTBChunksPlugin() {
        super(
            ModIDs.FTB_CHUNKS,
            ModIDs.FTB_TEAMS, ModIDs.FTB_LIBRARY, ModIDs.ARCHITECTURY
        );
    }

    @Override
    public void setup() {
        MinecraftForge.EVENT_BUS.addListener((LayerRegistryEvent event) -> {
            event.registry.register(new ClaimedChunksLayer());
        });

        MinecraftForge.EVENT_BUS.addListener((MapTypeInflationEvent event) -> {
            event.add(BlazeMapReferences.Layers.FTBCHUNKS);
        });
    }
}
