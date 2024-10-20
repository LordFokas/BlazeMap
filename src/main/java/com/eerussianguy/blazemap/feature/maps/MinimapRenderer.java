package com.eerussianguy.blazemap.feature.maps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraftforge.client.gui.ForgeIngameGui;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.engine.render.MapRenderer;
import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.profiling.Profilers;
import com.mojang.blaze3d.vertex.PoseStack;

public class MinimapRenderer implements AutoCloseable {
    public static final MinimapRenderer INSTANCE = new MinimapRenderer();
    public static final double MIN_ZOOM = 0.5, MAX_ZOOM = 8;

    private BlockPos last = BlockPos.ZERO;
    public final MinimapConfigSynchronizer synchronizer;
    private final MapRenderer mapRenderer;
    private final MinimapWidget minimap;

    public MinimapRenderer() {
        this.mapRenderer = new MapRenderer(0, 0, BlazeMap.resource("dynamic/map/minimap"), MIN_ZOOM, MAX_ZOOM)
            .setProfilers(Profilers.Minimap.DRAW_TIME_PROFILER, Profilers.Minimap.TEXTURE_TIME_PROFILER);
        this.synchronizer = new MinimapConfigSynchronizer(mapRenderer, BlazeMapConfig.CLIENT.minimap);
        this.minimap = new MinimapWidget(mapRenderer, BlazeMapConfig.CLIENT.minimap, false);
    }

    public void draw(PoseStack stack, MultiBufferSource buffers, ForgeIngameGui gui, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if(mc.screen != null) return;

        LocalPlayer player = Helpers.getPlayer();
        if(player == null) return;
        BlockPos pos = player.blockPosition();

        if(!pos.equals(last)) {
            last = pos;
            mapRenderer.setCenter(pos.getX(), pos.getZ());
        }

        // Prepare to render minimap
        Profilers.Minimap.DRAW_TIME_PROFILER.begin();
        stack.pushPose();
        float scale = (float) (1F / mc.getWindow().getGuiScale());
        stack.scale(scale, scale, 1);
        minimap.render(stack, buffers);
        stack.popPose();
        Profilers.Minimap.DRAW_TIME_PROFILER.end();
    }

    @Override
    public void close() {
        mapRenderer.close();
        synchronizer.save();
    }
}
