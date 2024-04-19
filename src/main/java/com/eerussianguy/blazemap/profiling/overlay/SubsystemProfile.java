package com.eerussianguy.blazemap.profiling.overlay;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;

import com.eerussianguy.blazemap.profiling.Profiler;
import com.mojang.blaze3d.vertex.PoseStack;

public class SubsystemProfile extends Container {
    private final Profiler.LoadProfiler load;
    private final Profiler.TimeProfiler time;
    private final String type;

    public SubsystemProfile(String name, Profiler.LoadProfiler load, Profiler.TimeProfiler time, String type, IDrawable... children) {
        super(name, Style.BLOCK, children);
        this.load = load;
        this.time = time;
        this.type = type;
        this.height += 30;
    }

    @Override
    public void draw(GuiGraphics graphics, MultiBufferSource buffers, Font fontRenderer) {
        load.ping();

        PoseStack stack = graphics.pose();
        stack.translate(0, style.margin, 0);
        for(IDrawable element : children){
            if(element.isDisabled()) continue;
            stack.pushPose();
            element.draw(graphics, buffers, fontRenderer);
            stack.popPose();
            stack.translate(0, element.getHeight(), 0);
        }
        String label = metric == null ? name : String.format("%s : %s", name, metric.get());
        String roll = String.format("[ last %s ]", load.span);
        ProfilingRenderer.drawSubsystem(load, time, label, roll, type, style, fontRenderer, stack.last().pose(), buffers);
    }
}
