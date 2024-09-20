package com.eerussianguy.blazemap.integration.ftbchunks;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.event.BlazeRegistryEvent.LayerRegistryEvent;
import com.eerussianguy.blazemap.api.event.MapMenuSetupEvent;
import com.eerussianguy.blazemap.api.event.MapTypeInflationEvent;
import com.eerussianguy.blazemap.integration.ModIDs;
import com.eerussianguy.blazemap.integration.ModIntegration;
import com.eerussianguy.blazemap.util.Helpers;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;

public class FTBChunksPlugin extends ModIntegration {
    private static final ResourceLocation CLAIM_ID = Helpers.identifier("map.menu.ftbchunks.claim");
    private static final ResourceLocation UNCLAIM_ID = Helpers.identifier("map.menu.ftbchunks.unclaim");
    private static final ResourceLocation CLAIMED_ID = Helpers.identifier("map.menu.ftbchunks.claimed");
    private static final TranslatableComponent CLAIM_TEXT = Helpers.translate("blazemap.gui.worldmap.menu.ftbchunks.claim");
    private static final TranslatableComponent UNCLAIM_TEXT = Helpers.translate("blazemap.gui.worldmap.menu.ftbchunks.unclaim");
    private static final TranslatableComponent CLAIMED_TEXT = Helpers.translate("blazemap.gui.worldmap.menu.ftbchunks.claimed");
    private static final MapMenuSetupEvent.MenuAction CLAIMED = new MapMenuSetupEvent.MenuAction(CLAIMED_ID, null, CLAIMED_TEXT, null);


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


        MinecraftForge.EVENT_BUS.addListener((MapMenuSetupEvent event) -> {
            if(!event.layers.contains(BlazeMapReferences.Layers.FTBCHUNKS)) return;

            final var uuid = Helpers.getPlayer().getUUID();
            final var manager = FTBChunksAPI.getManager();
            final var pos = new ChunkDimPos(event.dimension, event.chunkPosX, event.chunkPosZ);
            final var claim = manager.getChunk(pos);

            if(claim == null) {
                event.root.add(new MapMenuSetupEvent.MenuAction(CLAIM_ID, null, CLAIM_TEXT, () -> {
                    manager.registerClaim(pos, new ClaimedChunk(manager.getPersonalData(uuid), pos));
                }));
            } else if(claim.getTeamData().isTeamMember(uuid)) {
                event.root.add(new MapMenuSetupEvent.MenuAction(UNCLAIM_ID, null, UNCLAIM_TEXT, () -> {
                    manager.unregisterClaim(pos);
                }));
            } else {
                event.root.add(CLAIMED);
            }
        });
    }
}
