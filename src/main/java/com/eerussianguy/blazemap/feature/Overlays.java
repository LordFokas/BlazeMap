package com.eerussianguy.blazemap.feature;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.feature.maps.MinimapRenderer;
import com.eerussianguy.blazemap.profiling.overlay.ProfilingRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

import static com.eerussianguy.blazemap.BlazeMap.MOD_NAME;

public class Overlays {
    private static final String NORMALISED_MOD_NAME = MOD_NAME.replaceAll("\\s", "_").toLowerCase();
    private static final String MINIMAP_ID = String.format("%s_minimap", NORMALISED_MOD_NAME);
    private static final String PROFILER_ID = String.format("%s_profiler", NORMALISED_MOD_NAME);

    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll(MINIMAP_ID, Overlays::renderMinimap);
        event.registerAboveAll(PROFILER_ID, Overlays::renderProfiler);
    }

    public static void renderMinimap(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height) {
        if (BlazeMapConfig.CLIENT.minimap.enabled.get()) {
            PoseStack stack = graphics.pose();
            stack.pushPose();
            var buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            MinimapRenderer.INSTANCE.draw(graphics, buffers, gui, width, height);
            buffers.endBatch();
            stack.popPose();
        }
    }

    public static void renderProfiler(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height) {
        if (BlazeMapConfig.CLIENT.enableDebug.get()) {
            PoseStack stack = graphics.pose();
            stack.pushPose();
            var buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            ProfilingRenderer.INSTANCE.draw(graphics, buffers);
            buffers.endBatch();
            stack.popPose();
        }
    }
}
