package com.eerussianguy.blazemap.profiling.overlay;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;

import com.eerussianguy.blazemap.profiling.Profiler;
import com.mojang.blaze3d.vertex.PoseStack;

public class TimeProfile implements IDrawable {
    private final Profiler.TimeProfiler time;
    private final String label;

    public TimeProfile(Profiler.TimeProfiler time, String label){
        this.time = time;
        this.label = label;
    }

    @Override
    public int getHeight() {
        return 10;
    }

    @Override
    public void draw(GuiGraphics graphics, MultiBufferSource buffers, Font fontRenderer) {
        PoseStack stack = graphics.pose();
        ProfilingRenderer.drawTimeProfiler(time, 0, label, Container.Style.BLOCK, fontRenderer, stack.last().pose(), buffers);
    }
}
