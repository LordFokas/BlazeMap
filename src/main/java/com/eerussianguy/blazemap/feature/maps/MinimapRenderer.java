package com.eerussianguy.blazemap.feature.maps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.api.maps.IScreenSkipsMinimap;
import com.eerussianguy.blazemap.util.Helpers;
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
        this.mapRenderer = new MapRenderer(0, 0, Helpers.identifier("dynamic/map/minimap"), MIN_ZOOM, MAX_ZOOM, true)
            .setProfilers(Profilers.Minimap.DRAW_TIME_PROFILER, Profilers.Minimap.TEXTURE_TIME_PROFILER);
        this.synchronizer = new MinimapConfigSynchronizer(mapRenderer, BlazeMapConfig.CLIENT.minimap);
        this.minimap = new MinimapWidget(mapRenderer, BlazeMapConfig.CLIENT.minimap, false);
    }

    public void draw(GuiGraphics graphics, ForgeGui gui, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if(mc.screen instanceof IScreenSkipsMinimap) return;

        LocalPlayer player = Helpers.getPlayer();
        if(player == null) return;
        BlockPos pos = player.blockPosition();

        if(!pos.equals(last)) {
            last = pos;
            mapRenderer.setCenter(pos.getX(), pos.getZ());
        }

        // Prepare to render minimap
        Profilers.Minimap.DRAW_TIME_PROFILER.begin();
        PoseStack stack = graphics.pose();
        stack.pushPose();
        float scale = (float) (1F / mc.getWindow().getGuiScale());
        stack.scale(scale, scale, 1);
        minimap.render(graphics);
        stack.popPose();
        Profilers.Minimap.DRAW_TIME_PROFILER.end();
    }

    @Override
    public void close() {
        mapRenderer.close();
        synchronizer.save();
    }
}
