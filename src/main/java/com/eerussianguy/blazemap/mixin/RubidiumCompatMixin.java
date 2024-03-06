package com.eerussianguy.blazemap.mixin;

import com.eerussianguy.blazemap.engine.client.BlazeMapClientEngine;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = ChunkRenderList.class, remap = false)
public class RubidiumCompatMixin {

    @Inject(method = "add", at = @At("HEAD"))
    void onAdd(RenderSection render, CallbackInfo ci) {
        BlazeMapClientEngine.onChunkChanged(render.getChunkPos().chunk(), "Rubidium Chunk Hook");
    }
}