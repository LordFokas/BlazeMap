package com.eerussianguy.blazemap.profiling.overlay;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;

import com.eerussianguy.blazemap.profiling.Profiler;
import com.mojang.blaze3d.vertex.PoseStack;

public class TimeProfile implements IDrawable {
    private final Profiler.TimeProfiler time;

    public TimeProfile(Profiler.TimeProfiler time){
        this.time = time;
    }

    @Override
    public int getHeight() {
        return 10;
    }

    @Override
    public void draw(PoseStack stack, MultiBufferSource buffers, Font fontRenderer) {
        SubsystemProfile.drawTimeProfiler(time, 0, Container.Style.BLOCK, fontRenderer, stack.last().pose(), buffers);
    }
}
