package com.eerussianguy.blazemap.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

import com.eerussianguy.blazemap.engine.server.BlazeMapServerEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Shadow public abstract ServerLevel getLevel();

    @Inject(method = "onBlockStateChange", at=@At("HEAD"))
    public void onBlockStateChange(BlockPos block, BlockState s1, BlockState s2, CallbackInfo ci) {
        BlazeMapServerEngine.onChunkChanged(this.getLevel().dimension(), new ChunkPos(block));
    }
}
