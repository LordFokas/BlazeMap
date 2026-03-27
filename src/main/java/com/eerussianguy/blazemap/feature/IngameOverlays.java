package com.eerussianguy.blazemap.feature;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.IIngameOverlay;
import net.minecraftforge.client.gui.OverlayRegistry;

import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.config.ServerConfig;
import com.eerussianguy.blazemap.feature.maps.MinimapRenderer;
import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.profiling.overlay.ProfilingRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

import static com.eerussianguy.blazemap.BlazeMap.MOD_NAME;

public class IngameOverlays {
    public static final IIngameOverlay MINIMAP = OverlayRegistry.registerOverlayTop(MOD_NAME + " Minimap", IngameOverlays::renderMinimap);
    public static final IIngameOverlay PROFILER = OverlayRegistry.registerOverlayTop(MOD_NAME + " Profiler", IngameOverlays::renderProfiler);

    public static void reload() {
        OverlayRegistry.enableOverlay(MINIMAP, BlazeMapConfig.CLIENT.minimap.enabled.get());
        OverlayRegistry.enableOverlay(PROFILER, BlazeMapConfig.CLIENT.enableDebug.get());
    }

    public static void renderMinimap(ForgeIngameGui gui, PoseStack stack, float partialTicks, int width, int height) {
        if(!BlazeMapConfig.SERVER.mapItemRequirement.canPlayerAccessMap(Helpers.getPlayer(), ServerConfig.MapAccess.READ_LIVE)) return;

        stack.pushPose();
        var buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        MinimapRenderer.INSTANCE.draw(stack, buffers, gui, width, height);
        buffers.endBatch();
        stack.popPose();
    }

    public static void renderProfiler(ForgeIngameGui gui, PoseStack stack, float partialTicks, int width, int height) {
        stack.pushPose();
        var buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        ProfilingRenderer.INSTANCE.draw(stack, buffers);
        buffers.endBatch();
        stack.popPose();
    }
}
