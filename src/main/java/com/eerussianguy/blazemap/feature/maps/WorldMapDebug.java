package com.eerussianguy.blazemap.feature.maps;

import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;

import com.eerussianguy.blazemap.engine.BlazeMapAsync;
import com.eerussianguy.blazemap.engine.render.MapRenderer;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.eerussianguy.blazemap.profiling.Profiler;
import com.eerussianguy.blazemap.profiling.overlay.ProfilingRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

public class WorldMapDebug extends BaseComponent<WorldMapDebug> {
    private final MapRenderer.DebugInfo debug;
    private final Coordination coordination;
    private final Profiler.TimeProfiler renderTime, uploadTime;
    private final BooleanSupplier visibility;

    public WorldMapDebug(MapRenderer.DebugInfo debug, Coordination coordination, Profiler.TimeProfiler renderTime, Profiler.TimeProfiler uploadTime, BooleanSupplier visibility) {
        this.debug = debug;
        this.coordination = coordination;
        this.renderTime = renderTime;
        this.uploadTime = uploadTime;
        this.visibility = visibility;

        this.setSize(135, 135);
    }

    @Override
    public boolean isVisible() {
        return visibility.getAsBoolean();
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        RenderHelper.fillRect(stack.last().pose(), getWidth(), getHeight(), Colors.WIDGET_BACKGROUND);

        font.draw(stack, "Debug Info", 5, 5, 0xFFFF0000);
        stack.translate(5, 20, 0);
        stack.scale(0.5F, 0.5F, 1);

        font.draw(stack, "Atlas Time Profiling:", 0, 0, -1);
        var buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        ProfilingRenderer.drawTimeProfiler(renderTime, 12, "Render", font, stack.last().pose(), buffers);
        ProfilingRenderer.drawTimeProfiler(uploadTime, 24, "Upload", font, stack.last().pose(), buffers);
        buffers.endBatch();

        int y = 30;
        font.draw(stack, String.format("Renderer Size: %d x %d", debug.rw, debug.rh), 0, y += 12, -1);
        font.draw(stack, String.format("Renderer Zoom: %sx", debug.zoom), 0, y += 12, -1);
        font.draw(stack, String.format("Atlas Size: %d x %d", debug.mw, debug.mh), 0, y += 12, -1);
        font.draw(stack, String.format("Atlas Frustum: [%d , %d] to [%d , %d]", debug.bx, debug.bz, debug.ex, debug.ez), 0, y += 12, -1);

        font.draw(stack, String.format("Region Matrix: %d x %d", debug.ox, debug.oz), 0, y += 18, -1);
        font.draw(stack, String.format("Active Layers: %d", debug.layers), 0, y += 12, -1);
        font.draw(stack, String.format("Active Overlays: %d", debug.overlays), 0, y += 12, -1);
        font.draw(stack, String.format("Stitching: %s", debug.stitching), 0, y += 12, 0xFF0088FF);
        font.draw(stack, String.format("Parallel Pool: %d", BlazeMapAsync.instance().cruncher.poolSize()), 0, y += 12, 0xFFFFFF00);

        font.draw(stack, String.format("Addon Labels: %d", debug.labels), 0, y += 18, -1);
        font.draw(stack, String.format("Player Waypoints: %d", debug.waypoints), 0, y += 12, -1);

        String region = String.format("Rg %d %d  |  px: %d %d", coordination.regionX, coordination.regionZ, coordination.regionPixelX, coordination.regionPixelY);
        font.draw(stack, region, 0, y += 18, 0x6666FF);
        String chunk = String.format("Ch %d %d  |  px: %d %d", coordination.chunkX, coordination.chunkZ, coordination.chunkPixelX, coordination.chunkPixelY);
        font.draw(stack, chunk, 0, y += 12, 0x66FF66);
        String block = String.format("Bl %d %d  |  px: %d %d", coordination.blockX, coordination.blockZ, coordination.blockPixelX, coordination.blockPixelY);
        font.draw(stack, block, 0, y += 12, 0xFF6666);
    }
}
