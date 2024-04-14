package com.eerussianguy.blazemap.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import com.eerussianguy.blazemap.util.Helpers;

public class BlazeNetwork {
    private static final String ENGINE_VERSION = "1";
    private static final ResourceLocation ENGINE_CHANNEL = Helpers.identifier("engine");
    private static SimpleChannel engine;

    public static SimpleChannel engine() {
        return engine;
    }

    public static void initEngine() {
        if(engine != null) return;
        engine = makeChannel(ENGINE_CHANNEL, ENGINE_VERSION);

        int id = 0;
        engine.registerMessage(id++, PacketChunkMDUpdate.class, PacketChunkMDUpdate::encode, PacketChunkMDUpdate::decode, (msg, ctx) -> msg.handle(ctx.get()));
    }

    private static SimpleChannel makeChannel(ResourceLocation channel, String version) {
        return NetworkRegistry.newSimpleChannel(channel, () -> version, version::equals, version::equals);
    }
}
