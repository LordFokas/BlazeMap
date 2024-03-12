package com.eerussianguy.blazemap.feature;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.GuiOverlayManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.*;

import com.eerussianguy.blazemap.BlazeMap;

import com.eerussianguy.blazemap.BlazeMapConfig;
import com.eerussianguy.blazemap.feature.maps.MinimapRenderer;
import com.eerussianguy.blazemap.profiling.overlay.ProfilingRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

import static com.eerussianguy.blazemap.BlazeMap.MOD_NAME;

@Mod.EventBusSubscriber(modid = MOD_NAME, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class Overlays {
    public static final IGuiOverlay MINIMAP = 
        (ForgeGui gui, PoseStack stack, float partialTicks, int width, int height) -> renderMinimap(gui, stack, partialTicks, width, height);
    public static final IGuiOverlay PROFILER = 
        (ForgeGui gui, PoseStack stack, float partialTicks, int width, int height) -> renderProfiler(gui, stack, partialTicks, width, height);

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        System.out.println("Ran onRegisterOverlays");

        event.registerAboveAll(MOD_NAME + " Minimap", MINIMAP);
        event.registerAboveAll(MOD_NAME + " Profiler", PROFILER);
    }

    // @SubscribeEvent
    // public static void onRenderOverlays(RenderGuiOverlayEvent.Pre event) {

    //     BlazeMap.LOGGER.warn(event.getOverlay().id().toString());

    //     if ((MOD_NAME + " Minimap").equals(event.getOverlay().id()) && !BlazeMapConfig.CLIENT.minimap.enabled.get()) {
    //         event.setCanceled(true);
    //     }

    //     if ((MOD_NAME + " Profiler").equals(event.getOverlay().id()) && !BlazeMapConfig.CLIENT.enableDebug.get()) {
    //         event.setCanceled(true);
    //     }
    // }

    // public static void reload() {
    //     GuiOverlayManager.enableOverlay(MINIMAP, BlazeMapConfig.CLIENT.minimap.enabled.get());
    //     GuiOverlayManager.enableOverlay(PROFILER, BlazeMapConfig.CLIENT.enableDebug.get());
    // }

    public static void renderMinimap(ForgeGui gui, PoseStack stack, float partialTicks, int width, int height) {
        stack.pushPose();
        var buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        MinimapRenderer.INSTANCE.draw(stack, buffers, gui, width, height);
        buffers.endBatch();
        stack.popPose();
    }

    public static void renderProfiler(ForgeGui gui, PoseStack stack, float partialTicks, int width, int height) {
        stack.pushPose();
        var buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        ProfilingRenderer.INSTANCE.draw(stack, buffers);
        buffers.endBatch();
        stack.popPose();
    }
}
