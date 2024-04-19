package com.eerussianguy.blazemap.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.RenderHelper;

public class Image implements Renderable {
    private final int posX, posY, width, height;
    private final ResourceLocation image;
    private int color = Colors.NO_TINT;

    public Image(ResourceLocation image, int posX, int posY, int width, int height) {
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
        this.image = image;
    }

    public Image color(int color) {
        this.color = color;
        return this;
    }

    @Override
    public void render(GuiGraphics graphics, int i0, int i1, float f0) {
        RenderHelper.drawTexturedQuad(image, color, graphics, posX, posY, width, height);
    }
}
