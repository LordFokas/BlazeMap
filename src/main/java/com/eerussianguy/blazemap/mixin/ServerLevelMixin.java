package com.eerussianguy.blazemap.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

import com.eerussianguy.blazemap.engine.server.BlazeMapServerEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "onBlockStateChange", at=@At("HEAD"))
    public void onBlockStateChange(BlockPos block, BlockState s1, BlockState s2, CallbackInfo ci) {
        // If it's stupid but it works, it's not stupid.
        // @Shadow fucks with the project because it doesn't reobf properly.
        // No one knows why, no one can / wants to help, and so no one can point fingers at us either.
        ServerLevel level = (ServerLevel) (Object) this;

        BlazeMapServerEngine.onChunkChanged(level.dimension(), new ChunkPos(block));
    }
}
