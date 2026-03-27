package com.eerussianguy.blazemap.lib.gui.trait;

import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;

public interface BorderedComponent {
    boolean isEnabled();
    int getWidth();
    int getHeight();

    default int getBackgroundColor() {
        return getBackgroundColor(false);
    }

    default int getBackgroundColor(boolean hasMouse) {
        return isEnabled() && hasMouse ? 0xFF202020 : Colors.BLACK;
    }

    default void renderBorderedBackground(PoseStack stack) {
        this.renderBorderedBackground(stack, Colors.UNFOCUSED);
    }

    default void renderBorderedBackground(PoseStack stack, int border) {
        this.renderBorderedBackground(stack, border, getBackgroundColor());
    }

    default void renderBorderedBackground(PoseStack stack, int border, int background) {
        this.renderBorderedBox(stack, 0, 0, getWidth(), getHeight(), border, background);
    }

    default void renderBorderedBox(PoseStack stack, float posX, float posY, int w, int h, int border, int background) {
        stack.pushPose();
        if(posX != 0 || posY != 0) {
            stack.translate(posX, posY, 0);
        }
        RenderHelper.fillRect(stack.last().pose(), w, h, border);
        stack.translate(1, 1, 0);
        RenderHelper.fillRect(stack.last().pose(), w - 2, h - 2, background);
        stack.popPose();
    }

    default void renderFlatBackground(PoseStack stack, int color) {
        RenderHelper.fillRect(stack.last().pose(), getWidth(), getHeight(), color);
    }
}
