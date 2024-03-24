package com.eerussianguy.blazemap.mixin;

import java.util.List;
import java.util.concurrent.Executor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;

import com.eerussianguy.blazemap.engine.server.BlazeMapServerEngine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level {
    // This won't be called because it's a mixin, but Java still wants it to exist at compile time,
    // so directly copying from ServerLevel
    public ServerLevelMixin(MinecraftServer p_203762_, Executor p_203763_, LevelStorageSource.LevelStorageAccess p_203764_, ServerLevelData p_203765_, ResourceKey<Level> p_203766_, Holder<DimensionType> p_203767_, ChunkProgressListener p_203768_, ChunkGenerator p_203769_, boolean p_203770_, long p_203771_, List<CustomSpawner> p_203772_, boolean p_203773_) {
      super(p_203765_, p_203766_, p_203767_, p_203762_::getProfiler, false, p_203770_, p_203771_);
    }

    @Shadow
    public abstract void onBlockStateChange(BlockPos block, BlockState s1, BlockState s2);

    @Inject(method = "onBlockStateChange", at = @At("HEAD"), expect = 1)
    public void onOnBlockStateChange(BlockPos block, BlockState s1, BlockState s2, CallbackInfo ci) {
        BlazeMapServerEngine.onChunkChanged(super.dimension(), new ChunkPos(block));
    }
}
