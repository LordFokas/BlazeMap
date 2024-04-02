package com.eerussianguy.blazemap.feature.maps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.BlazeMapConfig;
import com.eerussianguy.blazemap.ClientConfig;
import com.eerussianguy.blazemap.gui.MouseSubpixelSmoother;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.Helpers;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.electronwill.nightconfig.core.io.WritingException;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;

public class MinimapWidget {
    private static final int HANDLE_SIZE = 30; // size of the resizing handle (red square)
    private static final int BORDER_SIZE = 5; // size of the translucent black minimap border

    private final MinimapConfigSynchronizer synchronizer;
    private final ClientConfig.MinimapConfig config = BlazeMapConfig.CLIENT.minimap;
    private final MapRenderer map;
    private final boolean editor;
    private final MouseSubpixelSmoother mouse;

    public MinimapWidget(MapRenderer map, MinimapConfigSynchronizer synchronizer, boolean editor){
        this.map = map;
        this.synchronizer = synchronizer;
        this.editor = editor;
        this.mouse = editor ? new MouseSubpixelSmoother() : null;
    }

    public void render(PoseStack stack, MultiBufferSource buffers) {
        int width = config.width.get();
        int height = config.height.get();
        stack.translate(config.positionX.get(), config.positionY.get(), 0);

        stack.pushPose();
        stack.translate(-BORDER_SIZE, -BORDER_SIZE, 0);
        RenderHelper.fillRect(stack.last().pose(), width + BORDER_SIZE*2, height + BORDER_SIZE*2, Colors.WIDGET_BACKGROUND);
        stack.popPose();

        map.render(stack, buffers);

        if(editor){
            stack.translate(width - HANDLE_SIZE, height - HANDLE_SIZE, 0);
            RenderHelper.fillRect(buffers, stack.last().pose(), HANDLE_SIZE, HANDLE_SIZE, 0xFFFF0000);
        }
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double draggedX, double draggedY) {
        int mapPosX = config.positionX.get();
        int mapPosY = config.positionY.get();
        int mapSizeX = config.width.get();
        int mapSizeY = config.height.get();
        int mapEndX = mapPosX + mapSizeX;
        int mapEndY = mapPosY + mapSizeY;

        // Check if the mouse is outside the minimap bounds, and ignore input if so.
        if(mouseX < mapPosX || mouseY < mapPosY || mouseX > mapEndX || mouseY > mapEndY) {
            return false; // We did not handle this input.
        }

        // Calculate window "end" distance from "map end"
        Window window = Minecraft.getInstance().getWindow();
        int maxX = window.getWidth() - mapEndX;
        int maxY = window.getHeight() - mapEndY;

        // Used to smooth out subpixel movements
        mouse.addMovement(draggedX, draggedY);

        try{
            // Check if outside the resize handle (red square) bounds
            if(mouseX < mapEndX - HANDLE_SIZE || mouseY < mapEndY - HANDLE_SIZE) {
                // Move map
                int moveX = Helpers.clamp(-mapPosX, mouse.movementX(), maxX);
                int moveY = Helpers.clamp(-mapPosY, mouse.movementY(), maxY);
                synchronizer.move(moveX, moveY);
            } else {
                // Resize map
                int resizeX = Helpers.clamp(-mapSizeX, mouse.movementX(), maxX);
                int resizeY = Helpers.clamp(-mapSizeY, mouse.movementY(), maxY);
                synchronizer.resize(resizeX, resizeY);
            }
        }
        catch(WritingException we){ // FIXME: BME-54   The proper thing to do here is not to catch but debounce saving.
            BlazeMap.LOGGER.error("Config exception while saving minimap config", we);
        }

        return true; // Input was handled, take no further action.
    }
}
