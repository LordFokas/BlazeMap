package com.eerussianguy.blazemap.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

import com.eerussianguy.blazemap.engine.server.BlazeMapServerEngine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level {
    // This won't be called because it's a mixin, but Java still wants it to exist at compile time.
    // All values are stubbed out as we're never actually calling this.
    public ServerLevelMixin() {
        super(null, null, null, null, false, false, 0);
    }

    @Inject(method = "onBlockStateChange", at = @At("HEAD"), expect = 1)
    public void onOnBlockStateChange(BlockPos block, BlockState s1, BlockState s2, CallbackInfo ci) {
        BlazeMapServerEngine.onChunkChanged(this.dimension(), new ChunkPos(block));
    }
}
