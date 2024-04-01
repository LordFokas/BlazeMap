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
    private static final int HANDLE_SIZE = 30;

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
        int width = config.width.get();
        int height = config.height.get();
        stack.translate(config.positionX.get(), config.positionY.get(), 0);

        stack.pushPose();
        stack.translate(-5, -5, 0);
        RenderHelper.fillRect(stack.last().pose(), width + 10, height + 10, Colors.WIDGET_BACKGROUND);
        stack.popPose();

        map.render(stack, buffers);

        if(editor){
            stack.translate(width - HANDLE_SIZE, height - HANDLE_SIZE, 0);
            RenderHelper.fillRect(buffers, stack.last().pose(), HANDLE_SIZE, HANDLE_SIZE, 0xFFFF0000);
        }
    }

    public boolean mouseDragged(double cx, double cy, int button, double dx, double dy) {
        int px = config.positionX.get();
        int py = config.positionY.get();
        int sx = config.width.get();
        int sy = config.height.get();

        // Check if mouse is within minimap bounds
        if(cx < px || cy < py || cx > px + sx || cy > py + sy){
            return false;
        }

        // Calculate window relative bounds
        Window window = Minecraft.getInstance().getWindow();
        int maxX = window.getWidth() - (px+sx);
        int maxY = window.getHeight() - (py+sy);

        // Check if outside handle bounds
        if(cx < px + sx - HANDLE_SIZE || cy < py + sy - HANDLE_SIZE){
            // Move map
            int mx = Helpers.clamp(-px, (int) dx, maxX);
            int my = Helpers.clamp(-py, (int) dy, maxY);
            synchronizer.setPosition(px + mx, py + my);
        }else{
            // Resize map
            int rx = Helpers.clamp(-sx, (int) dx, maxX);
            int ry = Helpers.clamp(-sy, (int) dy, maxY);
            synchronizer.setSize(sx + rx, sy + ry);
        }
        return true;
    }
}
