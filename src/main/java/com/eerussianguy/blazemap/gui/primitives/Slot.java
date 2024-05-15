package com.eerussianguy.blazemap.gui.primitives;

import com.eerussianguy.blazemap.util.Helpers;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Appears as an inset region that can itself hold content
 */
public class Slot extends GuiPrimitive {
    public Slot(int x, int y, int width, int height, GuiPrimitive.RenderFunction renderChildren) {
        super(Helpers.identifier("textures/gui/slot.png"), x, y, width, height, renderChildren);
    }

    public Slot(int x, int y, int width, int height, int padding, GuiPrimitive.RenderFunction renderChildren) {
        super(Helpers.identifier("textures/gui/slot.png"), x, y, width, height, padding, renderChildren);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PoseStack stack = graphics.pose();
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();

        // Draw slot
        stack.pushPose();
        stack.translate(outerX(), outerY(), 0);
        RenderHelper.drawFrame(buffers.getBuffer(textureRenderer), graphics, width(), height(), 1);
        stack.popPose();

        // Draw contents
        renderChildren.render(graphics, mouseX, mouseY, partialTick);
    };
}