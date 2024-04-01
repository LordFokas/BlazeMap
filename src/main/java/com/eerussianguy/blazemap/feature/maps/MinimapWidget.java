package com.eerussianguy.blazemap.feature.maps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;

import com.eerussianguy.blazemap.BlazeMapConfig;
import com.eerussianguy.blazemap.ClientConfig;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.Helpers;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;

public class MinimapWidget {
    private final MinimapConfigSynchronizer synchronizer;
    private final ClientConfig.MinimapConfig config = BlazeMapConfig.CLIENT.minimap;
    private final MapRenderer map;
    private final boolean editor;

    public MinimapWidget(MapRenderer map, MinimapConfigSynchronizer synchronizer, boolean editor){
        this.map = map;
        this.synchronizer = synchronizer;
        this.editor = editor;
    }

    public void render(PoseStack stack, MultiBufferSource buffers) {
        stack.translate(config.positionX.get(), config.positionY.get(), 0);

        stack.pushPose();
        stack.translate(-5, -5, 0);
        RenderHelper.fillRect(stack.last().pose(), config.width.get() + 10, config.height.get() + 10, Colors.WIDGET_BACKGROUND);
        stack.popPose();

        map.render(stack, buffers);
    }

    public boolean mouseDragged(double cx, double cy, int button, double dx, double dy) {
        int px = config.positionX.get();
        int py = config.positionY.get();
        int sx = config.width.get();
        int sy = config.height.get();

        if(cx < px || cy < py || cx > px + sx || cy > py + sy){
            return false;
        }

        Window window = Minecraft.getInstance().getWindow();

        int minX = -px, maxX = window.getWidth() - (px+sx);
        int minY = -py, maxY = window.getHeight() - (py+sy);
        int mx = Helpers.clamp(minX, (int) dx, maxX);
        int my = Helpers.clamp(minY, (int) dy, maxY);

        synchronizer.setPosition(px + mx, py + my);

        return true;
    }
}
