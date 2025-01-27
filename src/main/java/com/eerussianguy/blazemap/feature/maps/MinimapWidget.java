package com.eerussianguy.blazemap.feature.maps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.config.MinimapConfigFacade.IWidgetConfig;
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
    private static final int COORDS_BORDER = 3; // size of the background padding around the player coordinates

    private final IWidgetConfig config;
    private final MapRenderer map;
    private final boolean editor;
    private final MouseSubpixelSmoother mouse;

    public MinimapWidget(MapRenderer map, IWidgetConfig config, boolean editor){
        this.map = map;
        this.config = config;
        this.editor = editor;
        this.mouse = editor ? new MouseSubpixelSmoother() : null;
    }

    public void render(GuiGraphics graphics) {
        Window window = Minecraft.getInstance().getWindow();
        int mcWidth = window.getWidth();
        int mcHeight = window.getHeight();

        int width = config.width().get();
        int height = config.height().get();
        int posX = mcWidth - width - config.positionX().get(); // BME-63 Invert X: x coord starts from right side
        int posY = config.positionY().get();

        // BME-64: Keep minimap inside screen
        posX = Helpers.clamp(0, posX, mcWidth - width);
        posY = Helpers.clamp(0, posY, mcHeight - height);

        PoseStack stack = graphics.pose();
        MultiBufferSource buffers = graphics.bufferSource();

        stack.translate(posX, posY, 0);

        stack.pushPose();
        stack.translate(-BORDER_SIZE, -BORDER_SIZE, 0);
        RenderHelper.fillRect(stack.last().pose(), width + BORDER_SIZE*2, height + BORDER_SIZE*2, Colors.WIDGET_BACKGROUND);
        stack.popPose();

        map.render(graphics);

        if(editor){
            stack.pushPose();
            stack.translate(0, height - HANDLE_SIZE - BORDER_SIZE, 0);
            RenderHelper.fillRect(buffers, stack.last().pose(), HANDLE_SIZE + BORDER_SIZE, HANDLE_SIZE + BORDER_SIZE, Colors.WIDGET_BACKGROUND);
            stack.translate(0, BORDER_SIZE, 0.1);
            RenderHelper.fillRect(buffers, stack.last().pose(), HANDLE_SIZE, HANDLE_SIZE, 0xFFFF0000);
            stack.popPose();
        }

        if (BlazeMapConfig.CLIENT.clientFeatures.displayCoords.get()) {
            BlockPos pos = Helpers.getPlayer().blockPosition();
            String coords = String.format("[ %d | %d | %d ]", pos.getX(), pos.getY(), pos.getZ());
            Font font = Minecraft.getInstance().font;
            int length = font.width(coords);

            stack.pushPose();
            stack.translate(width / 2, height + BORDER_SIZE * 2, 0);
            stack.scale(2, 2, 1);
            stack.translate(-COORDS_BORDER - ((float)length) / 2F, 0, 0);

            RenderHelper.fillRect(buffers, stack.last().pose(), length + COORDS_BORDER*2, font.lineHeight - 2 + COORDS_BORDER*2, Colors.WIDGET_BACKGROUND);
            font.drawInBatch(coords, COORDS_BORDER, COORDS_BORDER, Colors.WHITE, false, stack.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            stack.popPose();
        }
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double draggedX, double draggedY) {
        Window window = Minecraft.getInstance().getWindow();

        int mapPosX = config.positionX().get();
        int mapPosY = config.positionY().get();
        int mapSizeX = config.width().get();
        int mapSizeY = config.height().get();
        int mapEndX = mapPosX + mapSizeX;
        int mapEndY = mapPosY + mapSizeY;

        // Make bounds checks against _previous_ mouse positions to avoid issues when mouse moves fast (BME-62)
        mouseX -= draggedX;
        mouseY -= draggedY;
        mouseX = window.getWidth() - mouseX; // Invert X origin coordinate for BME-63

        // Check if the mouse is outside the minimap bounds, and ignore input if so.
        if(mouseX < mapPosX || mouseY < mapPosY || mouseX > mapEndX || mouseY > mapEndY) {
            return false; // We did not handle this input.
        }

        // Calculate window "end" distance from "map end"
        int maxX = window.getWidth() - mapEndX;
        int maxY = window.getHeight() - mapEndY;

        // Used to smooth out subpixel movements
        mouse.addMovement(-draggedX, draggedY); // draggedX negated for BME-63 Invert X

        try{
            // Check if outside the resize handle (red square) bounds
            if(mouseX < mapEndX - HANDLE_SIZE || mouseY < mapEndY - HANDLE_SIZE) {
                // Move map
                int moveX = Helpers.clamp(-mapPosX, mouse.movementX(), maxX);
                int moveY = Helpers.clamp(-mapPosY, mouse.movementY(), maxY);
                config.positionX().set(mapPosX + moveX);
                config.positionY().set(mapPosY + moveY);
            } else {
                // Resize map
                int resizeX = Helpers.clamp(-mapSizeX, mouse.movementX(), maxX);
                int resizeY = Helpers.clamp(-mapSizeY, mouse.movementY(), maxY);
                config.resize(mapSizeX + resizeX, mapSizeY + resizeY);
            }
        }
        catch(WritingException we){ // FIXME: BME-54   The proper thing to do here is not to catch but debounce saving.
            BlazeMap.LOGGER.error("Config exception while saving minimap config", we);
        }

        return true; // Input was handled, take no further action.
    }
}
