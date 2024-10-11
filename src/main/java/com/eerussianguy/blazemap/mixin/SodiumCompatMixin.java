package com.eerussianguy.blazemap.mixin;

import com.eerussianguy.blazemap.engine.client.ClientEngine;
import com.eerussianguy.blazemap.feature.MDSources;
import com.eerussianguy.blazemap.profiling.Profilers;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkRenderList.class)
public class SodiumCompatMixin {

    @Inject(method = "add", at = @At("HEAD"), remap = false)
    void onAdd(RenderSection render, CallbackInfo ci) {
        Profilers.Client.Mixin.SODIUM_LOAD_PROFILER.hit();
        Profilers.Client.Mixin.SODIUM_TIME_PROFILER.begin();

        ClientEngine.onChunkChanged(render.getChunkPos().chunk(), MDSources.Client.SODIUM);

        Profilers.Client.Mixin.SODIUM_TIME_PROFILER.end();
    }
}