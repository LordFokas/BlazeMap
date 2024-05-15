package com.eerussianguy.blazemap.gui.primitives;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class Image extends GuiPrimitive {
    private int color = Colors.NO_TINT;

    public Image(ResourceLocation texture, int x, int y, int width, int height) {
        super(texture, x, y, width, height);
    }

    public Image(ResourceLocation texture, int x, int y, int width, int height, int color) {
        super(texture, x, y, width, height);
        this.color = color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PoseStack stack = graphics.pose();
        VertexConsumer vertices = graphics.bufferSource().getBuffer(textureRenderer);

        stack.pushPose();
        stack.translate(x(), y(), 0);
        RenderHelper.drawQuad(vertices, stack.last().pose(), width(), height(), color);
        stack.popPose();
    }
}
