package com.eerussianguy.blazemap.mixin;

import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.engine.client.ClientEngine;
import com.eerussianguy.blazemap.feature.MDSources;
import com.eerussianguy.blazemap.profiling.Profilers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderChunkRegion.class)
public class RenderChunkRegionMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void constructor(Level level, int centerX, int centerZ, RenderChunk[][] renderChunks, CallbackInfo ci) {
        Profilers.Client.Mixin.RENDERCHUNK_LOAD_PROFILER.hit();
        Profilers.Client.Mixin.RENDERCHUNK_TIME_PROFILER.begin();

        for(RenderChunk[] rcs : renderChunks) {
            for(RenderChunk rc : rcs) {
                ChunkPos pos = rc.wrapped.getPos();
                ClientEngine.onChunkChanged(pos, MDSources.Client.VANILLA);
            }
        }

        Profilers.Client.Mixin.RENDERCHUNK_TIME_PROFILER.end();
    }
}
