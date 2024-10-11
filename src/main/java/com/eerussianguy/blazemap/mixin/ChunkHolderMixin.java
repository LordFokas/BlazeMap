package com.eerussianguy.blazemap.mixin;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.chunk.LevelChunk;

import com.eerussianguy.blazemap.engine.server.ServerEngine;
import com.eerussianguy.blazemap.profiling.Profilers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkHolder.class)
public class ChunkHolderMixin {
    @Shadow
    private boolean hasChangedSections;

    @Inject(method = "broadcastChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;getLevel()Lnet/minecraft/world/level/Level;"))
    private void onBroadcastChangesBody(LevelChunk chunk, CallbackInfo ci) {
        if(this.hasChangedSections) {
            Profilers.Server.Mixin.CHUNKHOLDER_LOAD_PROFILER.hit();
            Profilers.Server.Mixin.CHUNKHOLDER_TIME_PROFILER.begin();

            ServerEngine.onChunkChanged(chunk.getLevel().dimension(), chunk.getPos());

            Profilers.Server.Mixin.CHUNKHOLDER_TIME_PROFILER.end();
        }
    }
}
