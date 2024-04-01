package com.eerussianguy.blazemap.profiling.overlay;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;

import com.eerussianguy.blazemap.profiling.Profiler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;

public class SubsystemProfile extends Container {
    private static final int TIME_COLOR = 0xFFFFAA;
    private static final int LOAD_COLOR = 0xAAAAFF;
    private static final int IMPACT_COLOR = 0xFFAAAA;

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
    public void draw(PoseStack stack, MultiBufferSource buffers, Font fontRenderer) {
        load.ping();

        stack.translate(0, style.margin, 0);
        for(IDrawable element : children){
            if(element.isDisabled()) continue;
            stack.pushPose();
            element.draw(stack, buffers, fontRenderer);
            stack.popPose();
            stack.translate(0, element.getHeight(), 0);
        }
        String label = metric == null ? name : String.format("%s : %s", name, metric.get());
        String roll = String.format("[ last %s ]", load.span);
        drawSubsystem(load, time, label, roll, type, style, fontRenderer, stack.last().pose(), buffers);
    }

    // =================================================================================================================
    public static void drawSubsystem(Profiler.LoadProfiler load, Profiler.TimeProfiler time, String label, String roll, String type, Style style, Font fontRenderer, Matrix4f matrix, MultiBufferSource buffers) {
        fontRenderer.drawInBatch(label, style.indent, 0, style.header, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
        fontRenderer.drawInBatch(roll, PANEL_MIDDLE, 0, style.metric, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
        drawTimeProfiler(time, 10, style, fontRenderer, matrix, buffers);
        drawLoadProfiler(load, 20, style, fontRenderer, matrix, buffers);
        drawSubsystemLoad(load, time, 30, type, style, fontRenderer, matrix, buffers);
    }

    public static void drawTimeProfiler(Profiler.TimeProfiler profiler, float y, Style style, Font fontRenderer, Matrix4f matrix, MultiBufferSource buffers) {
        double at = profiler.getAvg() / 1000D, nt = profiler.getMin() / 1000D, xt = profiler.getMax() / 1000D;
        String au = "\u03BC", nu = "\u03BC", xu = "\u03BC";
        if(at >= 1000) {
            at /= 1000D;
            au = "m";
        }
        if(nt >= 1000) {
            nt /= 1000D;
            nu = "m";
        }
        if(xt >= 1000) {
            xt /= 1000D;
            xu = "m";
        }
        String avg = String.format("%s : %.2f%ss", "\u0394", at, au);
        String dst = String.format("[ %.1f%ss - %.1f%ss ]", nt, nu, xt, xu);
        fontRenderer.drawInBatch(avg, style.indent, y, TIME_COLOR, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
        fontRenderer.drawInBatch(dst, PANEL_MIDDLE, y, TIME_COLOR, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
    }

    public static void drawLoadProfiler(Profiler.LoadProfiler profiler, float y, Style style, Font fontRenderer, Matrix4f matrix, MultiBufferSource buffers) {
        String u = profiler.unit;
        String avg = String.format("%s : %.2f\u0394/%s", "#", profiler.getAvg(), u);
        String dst = String.format("[ %.0f\u0394/%s - %.0f\u0394/%s ]", profiler.getMin(), u, profiler.getMax(), u);
        fontRenderer.drawInBatch(avg, style.indent, y, LOAD_COLOR, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
        fontRenderer.drawInBatch(dst, PANEL_MIDDLE, y, LOAD_COLOR, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
    }

    public static void drawSubsystemLoad(Profiler.LoadProfiler load, Profiler.TimeProfiler time, float y, String type, Style style, Font fontRenderer, Matrix4f matrix, MultiBufferSource buffers) {
        double l = load.getAvg();
        double t = time.getAvg() / 1000D;
        double w = l * t;
        double p = 100 * w / (load.interval * 1000);
        String u = "\u03BC";
        if(w >= 1000) {
            w /= 1000D;
            u = "m";
        }
        String con = String.format("%s : %.2f%ss/%s", "\u03C1", w, u, load.unit);
        String pct = String.format("%.3f%% %s", p, type);
        fontRenderer.drawInBatch(con, style.indent, y, IMPACT_COLOR, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
        fontRenderer.drawInBatch(pct, PANEL_MIDDLE, y, IMPACT_COLOR, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
    }
}
