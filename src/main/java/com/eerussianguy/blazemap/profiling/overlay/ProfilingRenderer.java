package com.eerussianguy.blazemap.profiling.overlay;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;

import com.eerussianguy.blazemap.engine.BlazeMapAsync;
import com.eerussianguy.blazemap.engine.client.ClientEngine;
import com.eerussianguy.blazemap.engine.client.LayerRegionTile;
import com.eerussianguy.blazemap.engine.server.ServerEngine;
import com.eerussianguy.blazemap.feature.maps.WorldMapGui;
import com.eerussianguy.blazemap.integration.KnownMods;
import com.eerussianguy.blazemap.integration.ModIDs;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.profiling.Profiler;
import com.eerussianguy.blazemap.profiling.Profilers;
import com.eerussianguy.blazemap.profiling.overlay.Container.Style;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;

public class ProfilingRenderer {
    private static final StringSource NO_MODS_FOUND = new StringSource("No mods found", Colors.DISABLED);
    private static final StringSource NO_BAD_MODS_FOUND = new StringSource("No troublemakers found", 0xAAFFAA);

    public static final List<Container> PANELS = Arrays.asList(
        new Container("Client Debug Info", Style.PANEL,
            new Container("Overlays", Style.SECTION,
                new Container("Debug UI", Style.BLOCK,
                    new TimeProfile(Profilers.DEBUG_TIME_PROFILER, "Render")
                ),
                new Container("Minimap", Style.BLOCK,
                    new TimeProfile(Profilers.Minimap.DRAW_TIME_PROFILER, "Render")
                )
            ),
            new Container("Client Engine", Style.SECTION,
                new StringSource(() -> String.format("MD Source: %s / %s", ClientEngine.isClientSource() ? "Client" : "Server", ClientEngine.getMDSource())),
                new StringSource(() -> String.format("Parallel Pool: %d threads", BlazeMapAsync.instance().cruncher.poolSize())),
                new StringSource(() -> {
                    double size = ((double) LayerRegionTile.getLoadedKb()) / 1024D;
                    int tiles = LayerRegionTile.getInstances();
                    String scale = "M";
                    return String.format("Layer Region Tiles: %d   [ %.2f %sB ]", tiles, size, scale);
                }),
                new SubsystemProfile("Chunk Render Mixin", Profilers.Client.Mixin.RENDERCHUNK_LOAD_PROFILER, Profilers.Client.Mixin.RENDERCHUNK_TIME_PROFILER, "tick load")
                    .enable(() -> !KnownMods.isAnyLoaded(ModIDs.SODIUM_FAMILY)),
                new SubsystemProfile("Sodium Mixin", Profilers.Client.Mixin.SODIUM_LOAD_PROFILER, Profilers.Client.Mixin.SODIUM_TIME_PROFILER, "tick load")
                    .enable(() -> KnownMods.isAnyLoaded(ModIDs.SODIUM_FAMILY))
            ),
            new Container("Client Pipeline", Style.SECTION,
                new SubsystemProfile("MD Collect", Profilers.Client.COLLECTOR_LOAD_PROFILER, Profilers.Client.COLLECTOR_TIME_PROFILER, "tick load",
                    new StringSource(() -> String.format("Dirty Chunks: %d", ClientEngine.dirtyChunks()), Style.BLOCK.header)
                ).enable(() -> ClientEngine.numCollectors() > 0).metric(() -> String.valueOf(ClientEngine.numCollectors())),
                new SubsystemProfile("MD Transform", Profilers.Client.TRANSFORMER_LOAD_PROFILER, Profilers.Client.TRANSFORMER_TIME_PROFILER, "delay")
                    .enable(() -> ClientEngine.numTransformers() > 0).metric(() -> String.valueOf(ClientEngine.numTransformers())),
                new SubsystemProfile("MD Process", Profilers.Client.PROCESSOR_LOAD_PROFILER, Profilers.Client.PROCESSOR_TIME_PROFILER, "delay")
                    .enable(() -> ClientEngine.numProcessors() > 0).metric(() -> String.valueOf(ClientEngine.numProcessors())),
                new SubsystemProfile("Layer Render", Profilers.Client.LAYER_LOAD_PROFILER, Profilers.Client.LAYER_TIME_PROFILER, "delay")
                    .enable(() -> ClientEngine.numLayers() > 0).metric(() -> String.valueOf(ClientEngine.numLayers())),
                new SubsystemProfile("Dirty Tiles", Profilers.Client.TILE_LOAD_PROFILER, Profilers.Client.TILE_TIME_PROFILER, "delay")
                    .enable(() -> ClientEngine.numLayers() > 0).metric(() -> String.valueOf(ClientEngine.dirtyTiles()))
            )
        ).metric(() -> String.format("[ %s fps ]", ClientEngine.avgFPS())),

        new Container("Server Debug Info", Style.PANEL,
            new Container("Server Engine", Style.SECTION,
                new SubsystemProfile("ChunkHolder Mixin", Profilers.Server.Mixin.CHUNKHOLDER_LOAD_PROFILER, Profilers.Server.Mixin.CHUNKHOLDER_TIME_PROFILER, "tick load"),
                new SubsystemProfile("Chunk Packet Mixins", Profilers.Server.Mixin.CHUNKPACKET_LOAD_PROFILER, Profilers.Server.Mixin.CHUNKPACKET_TIME_PROFILER, "tick load")
            ),
            new Container("Server Pipelines", Style.SECTION,
                new SubsystemProfile("MD Collect", Profilers.Server.COLLECTOR_LOAD_PROFILER, Profilers.Server.COLLECTOR_TIME_PROFILER, "tick load",
                    new StringSource(() -> String.format("Dirty Chunks: %d", ServerEngine.dirtyChunks()), Style.BLOCK.header)
                ).enable(() -> ServerEngine.numCollectors() > 0).metric(() -> String.valueOf(ServerEngine.numCollectors())),
                new SubsystemProfile("MD Transform", Profilers.Server.TRANSFORMER_LOAD_PROFILER, Profilers.Server.TRANSFORMER_TIME_PROFILER, "delay")
                    .enable(() -> ServerEngine.numTransformers() > 0).metric(() -> String.valueOf(ServerEngine.numTransformers())),
                new SubsystemProfile("MD Process", Profilers.Server.PROCESSOR_LOAD_PROFILER, Profilers.Server.PROCESSOR_TIME_PROFILER, "delay")
                    .enable(() -> ServerEngine.numProcessors() > 0).metric(() -> String.valueOf(ServerEngine.numProcessors()))
            ).metric(() -> String.valueOf(ServerEngine.numPipelines()))
        ).metric(() -> String.format("[ %d tps ]", ServerEngine.avgTPS())).enable(ServerEngine::isRunning),

        new Container("Mod Interactions", Style.PANEL,
            new Container("Core", Style.SECTION, KnownMods.getCore(IDrawable.class, mod -> new VersionString(mod.name).note(mod.version, Colors.WHITE), NO_MODS_FOUND)),
            new Container("Built-in Support", Style.SECTION, KnownMods.getCompat(IDrawable.class, mod -> new VersionString(mod.name).note(mod.version, Colors.WHITE), NO_MODS_FOUND)),
            new Container("Known Problematic", Style.SECTION, KnownMods.getProblem(IDrawable.class, mod -> new VersionString(mod.name, 0xFFAAAA).note(mod.version, 0xFFAAAA), NO_BAD_MODS_FOUND)),
            new Container("Registered API Objects", Style.SECTION, KnownMods.getAPICall(IDrawable.class, mod -> new VersionString(mod.name).note(mod.version, Colors.WHITE), NO_MODS_FOUND)),
            new Container("Announced", Style.SECTION, KnownMods.getAnnounced(IDrawable.class, mod -> new VersionString(mod.name).note(mod.version, Colors.WHITE), NO_MODS_FOUND))
        )
    );


    // =================================================================================================================
    public static final ProfilingRenderer INSTANCE = new ProfilingRenderer();
    private static final double DEBUG_SCALE = 2D;

    private ProfilingRenderer() {}

    public void draw(PoseStack stack, MultiBufferSource buffers) {
        Minecraft mc = Minecraft.getInstance();
        if(mc.screen instanceof WorldMapGui) return;
        if(Helpers.getPlayer() == null) return;

        Profilers.DEBUG_TIME_PROFILER.begin();

        stack.pushPose();
        double guiScale = mc.getWindow().getGuiScale();
        if(guiScale > DEBUG_SCALE){
            stack.scale((float)(DEBUG_SCALE / guiScale), (float)(DEBUG_SCALE / guiScale), 1);
        }
        stack.translate(5, 5, 0);
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

    // =================================================================================================================
    public static final int TIME_COLOR = 0xFFFFAA;
    public static final int LOAD_COLOR = 0xAAAAFF;
    public static final int IMPACT_COLOR = 0xFFAAAA;
    public static void drawSubsystem(Profiler.LoadProfiler load, Profiler.TimeProfiler time, String label, String roll, String type, Style style, Font fontRenderer, Matrix4f matrix, MultiBufferSource buffers) {
        fontRenderer.drawInBatch(label, style.indent, 0, style.header, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
        fontRenderer.drawInBatch(roll, Container.PANEL_MIDDLE, 0, style.metric, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
        drawTimeProfiler(time, 10, "\u0394 ", style, fontRenderer, matrix, buffers);
        drawLoadProfiler(load, 20, "# ", style, fontRenderer, matrix, buffers);
        drawSubsystemLoad(load, time, 30, "\u03C1 ", type, style, fontRenderer, matrix, buffers);
    }

    /** Used by WorldMapGui as Container.Style should not leave the profiling package. */
    public static void drawTimeProfiler(Profiler.TimeProfiler profiler, float y, String label, Font fontRenderer, Matrix4f matrix, MultiBufferSource buffers) {
        drawTimeProfiler(profiler, y, label, Style.BLOCK, fontRenderer, matrix, buffers);
    }

    public static void drawTimeProfiler(Profiler.TimeProfiler profiler, float y, String label, Style style, Font fontRenderer, Matrix4f matrix, MultiBufferSource buffers) {
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
        String avg = String.format("%s : %.2f%ss", label, at, au);
        String dst = String.format("[ %.1f%ss - %.1f%ss ]", nt, nu, xt, xu);
        fontRenderer.drawInBatch(avg, style.indent, y, TIME_COLOR, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
        fontRenderer.drawInBatch(dst, Container.PANEL_MIDDLE, y, TIME_COLOR, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
    }

    public static void drawLoadProfiler(Profiler.LoadProfiler profiler, float y, String label, Style style, Font fontRenderer, Matrix4f matrix, MultiBufferSource buffers) {
        String u = profiler.unit;
        String avg = String.format("%s : %.2f\u0394/%s", label, profiler.getAvg(), u);
        String dst = String.format("[ %.0f\u0394/%s - %.0f\u0394/%s ]", profiler.getMin(), u, profiler.getMax(), u);
        fontRenderer.drawInBatch(avg, style.indent, y, LOAD_COLOR, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
        fontRenderer.drawInBatch(dst, Container.PANEL_MIDDLE, y, LOAD_COLOR, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
    }

    public static void drawSubsystemLoad(Profiler.LoadProfiler load, Profiler.TimeProfiler time, float y, String label, String type, Style style, Font fontRenderer, Matrix4f matrix, MultiBufferSource buffers) {
        double l = load.getAvg();
        double t = time.getAvg() / 1000D;
        double w = l * t;
        double p = 100 * w / (load.interval * 1000);
        String u = "\u03BC";
        if(w >= 1000) {
            w /= 1000D;
            u = "m";
        }
        String con = String.format("%s : %.2f%ss/%s", label, w, u, load.unit);
        String pct = String.format("%.3f%% %s", p, type);
        fontRenderer.drawInBatch(con, style.indent, y, IMPACT_COLOR, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
        fontRenderer.drawInBatch(pct, Container.PANEL_MIDDLE, y, IMPACT_COLOR, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
    }
}
