package com.eerussianguy.blazemap.profiling.overlay;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;

import com.eerussianguy.blazemap.BlazeMapConfig;
import com.eerussianguy.blazemap.engine.client.BlazeMapClientEngine;
import com.eerussianguy.blazemap.engine.client.LayerRegionTile;
import com.eerussianguy.blazemap.engine.server.BlazeMapServerEngine;
import com.eerussianguy.blazemap.feature.MDSources;
import com.eerussianguy.blazemap.feature.maps.WorldMapGui;
import com.eerussianguy.blazemap.profiling.Profilers;
import com.eerussianguy.blazemap.profiling.overlay.Container.Style;
import com.eerussianguy.blazemap.util.Helpers;
import com.mojang.blaze3d.vertex.PoseStack;

public class ProfilingRenderer {
    public static final List<Container> PANELS = Arrays.asList(
        new Container("Client Debug Info", Style.PANEL,
            new Container("Overlays", Style.SECTION,
                new Container("Debug UI", Style.BLOCK, new TimeProfile(Profilers.DEBUG_TIME_PROFILER)),
                new SubsystemProfile("Minimap", Profilers.Minimap.TEXTURE_LOAD_PROFILER, Profilers.Minimap.TEXTURE_TIME_PROFILER, "frame load")
                    .enable(BlazeMapConfig.CLIENT.minimap.enabled)
            ),
            new Container("Client Engine", Style.SECTION,
                new StringSource(() -> String.format("MD Source: %s / %s", BlazeMapClientEngine.isClientSource() ? "Client" : "Server", BlazeMapClientEngine.getMDSource())),
                new StringSource(() -> String.format("Parallel Pool: %d threads", BlazeMapClientEngine.cruncher().poolSize())),
                new StringSource(() -> {
                    double size = ((double) LayerRegionTile.getLoadedKb()) / 1024D;
                    int tiles = LayerRegionTile.getInstances();
                    String scale = "M";
                    return String.format("Layer Region Tiles: %d   [ %.2f %sB ]", tiles, size, scale);
                }),
                new SubsystemProfile("Chunk Render Mixin", Profilers.Client.Mixin.RENDERCHUNK_LOAD_PROFILER, Profilers.Client.Mixin.RENDERCHUNK_TIME_PROFILER, "tick load")
                    .enable(() -> BlazeMapClientEngine.getMDSource().equals(MDSources.Client.VANILLA)),
                new SubsystemProfile("Rubidium Mixin", Profilers.Client.Mixin.RUBIDIUM_LOAD_PROFILER, Profilers.Client.Mixin.RUBIDIUM_TIME_PROFILER, "tick load")
                    .enable(() -> BlazeMapClientEngine.getMDSource().equals(MDSources.Client.RUBIDIUM))
            ),
            new Container("Client Pipeline", Style.SECTION,
                new SubsystemProfile("MD Collect", Profilers.Client.COLLECTOR_LOAD_PROFILER, Profilers.Client.COLLECTOR_TIME_PROFILER, "tick load",
                    new StringSource(() -> String.format("Dirty Chunks: %d", BlazeMapClientEngine.dirtyChunks()), Style.BLOCK.header)
                ).enable(() -> BlazeMapClientEngine.numCollectors() > 0).metric(() -> String.valueOf(BlazeMapClientEngine.numCollectors())),
                new SubsystemProfile("MD Transform", Profilers.Client.TRANSFORMER_LOAD_PROFILER, Profilers.Client.TRANSFORMER_TIME_PROFILER, "delay")
                    .enable(() -> BlazeMapClientEngine.numTransformers() > 0).metric(() -> String.valueOf(BlazeMapClientEngine.numTransformers())),
                new SubsystemProfile("MD Process", Profilers.Client.PROCESSOR_LOAD_PROFILER, Profilers.Client.PROCESSOR_TIME_PROFILER, "delay")
                    .enable(() -> BlazeMapClientEngine.numProcessors() > 0).metric(() -> String.valueOf(BlazeMapClientEngine.numProcessors())),
                new SubsystemProfile("Layer Render", Profilers.Client.LAYER_LOAD_PROFILER, Profilers.Client.LAYER_TIME_PROFILER, "delay")
                    .enable(() -> BlazeMapClientEngine.numLayers() > 0).metric(() -> String.valueOf(BlazeMapClientEngine.numLayers())),
                new SubsystemProfile("Dirty Tiles", Profilers.Client.TILE_LOAD_PROFILER, Profilers.Client.TILE_TIME_PROFILER, "delay")
                    .enable(() -> BlazeMapClientEngine.numLayers() > 0).metric(() -> String.valueOf(BlazeMapClientEngine.dirtyTiles()))
            )
        ).metric(() -> String.format("[ %s fps ]", BlazeMapClientEngine.avgFPS())),

        new Container("Server Debug Info", Style.PANEL,
            new Container("Server Engine", Style.SECTION,
                new SubsystemProfile("ChunkHolder Mixin", Profilers.Server.Mixin.CHUNKHOLDER_LOAD_PROFILER, Profilers.Server.Mixin.CHUNKHOLDER_TIME_PROFILER, "tick load"),
                new SubsystemProfile("Chunk Packet Mixins", Profilers.Server.Mixin.CHUNKPACKET_LOAD_PROFILER, Profilers.Server.Mixin.CHUNKPACKET_TIME_PROFILER, "tick load")
            ),
            new Container("Server Pipelines", Style.SECTION,
                new SubsystemProfile("MD Collect", Profilers.Server.COLLECTOR_LOAD_PROFILER, Profilers.Server.COLLECTOR_TIME_PROFILER, "tick load",
                    new StringSource(() -> String.format("Dirty Chunks: %d", BlazeMapServerEngine.dirtyChunks()), Style.BLOCK.header)
                ).enable(() -> BlazeMapServerEngine.numCollectors() > 0).metric(() -> String.valueOf(BlazeMapServerEngine.numCollectors())),
                new SubsystemProfile("MD Transform", Profilers.Server.TRANSFORMER_LOAD_PROFILER, Profilers.Server.TRANSFORMER_TIME_PROFILER, "delay")
                    .enable(() -> BlazeMapServerEngine.numTransformers() > 0).metric(() -> String.valueOf(BlazeMapServerEngine.numTransformers())),
                new SubsystemProfile("MD Process", Profilers.Server.PROCESSOR_LOAD_PROFILER, Profilers.Server.PROCESSOR_TIME_PROFILER, "delay")
                    .enable(() -> BlazeMapServerEngine.numProcessors() > 0).metric(() -> String.valueOf(BlazeMapServerEngine.numProcessors()))
            ).metric(() -> String.valueOf(BlazeMapServerEngine.numPipelines()))
        ).metric(() -> String.format("[ %d tps ]", BlazeMapServerEngine.avgTPS())).enable(BlazeMapServerEngine::isRunning)
    );


    // =================================================================================================================
    public static final ProfilingRenderer INSTANCE = new ProfilingRenderer();
    private static final double DEBUG_SCALE = 4D;

    private ProfilingRenderer() {}

    public void draw(PoseStack stack, MultiBufferSource buffers) {
        Minecraft mc = Minecraft.getInstance();
        if(mc.screen instanceof WorldMapGui) return;
        if(Helpers.getPlayer() == null) return;

        Profilers.DEBUG_TIME_PROFILER.begin();

        stack.pushPose();
        stack.translate(5, 5, 0);
        double guiScale = mc.getWindow().getGuiScale();
        stack.scale((float)(DEBUG_SCALE / guiScale), (float)(DEBUG_SCALE / guiScale), 1);
        drawPanels(stack, buffers, mc.font);
        stack.popPose();

        Profilers.DEBUG_TIME_PROFILER.end();
    }

    private void drawPanels(PoseStack stack, MultiBufferSource buffers, Font fontRenderer) {
        for(Container panel : PANELS){
            if(panel.isDisabled()) continue;
            stack.pushPose();
            panel.draw(stack, buffers, fontRenderer);
            stack.popPose();
            stack.translate(Container.PANEL_WIDTH + 5, 0, 0);
        }
    }
}
