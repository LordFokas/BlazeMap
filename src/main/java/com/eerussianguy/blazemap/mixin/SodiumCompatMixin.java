package com.eerussianguy.blazemap.mixin;

import com.eerussianguy.blazemap.engine.client.BlazeMapClientEngine;
import com.eerussianguy.blazemap.feature.MDSources;
import com.eerussianguy.blazemap.profiling.Profilers;
import net.minecraft.world.level.ChunkPos;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SodiumWorldRenderer.class)
public class SodiumCompatMixin {
    @Inject(method = "scheduleRebuildForChunk", at = @At("HEAD"), remap = false)
    void onScheduleRebuildForChunk(int x, int y, int z, boolean important, CallbackInfo ci) {
        Profilers.Client.Mixin.SODIUM_LOAD_PROFILER.hit();
        Profilers.Client.Mixin.SODIUM_TIME_PROFILER.begin();

        BlazeMapClientEngine.onChunkChanged(new ChunkPos(x, z), MDSources.Client.SODIUM);

        Profilers.Client.Mixin.SODIUM_TIME_PROFILER.end();
    }
}