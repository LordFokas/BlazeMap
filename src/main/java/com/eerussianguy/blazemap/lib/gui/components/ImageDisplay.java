package com.eerussianguy.blazemap.lib.gui.components;

import java.util.function.IntSupplier;

import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.lib.RenderHelper;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.eerussianguy.blazemap.lib.gui.trait.BorderedComponent;
import com.mojang.blaze3d.vertex.PoseStack;

public class ImageDisplay extends BaseComponent<ImageDisplay> implements BorderedComponent {
    private ResourceLocation image;
    private int imageWidth, imageHeight;
    private IntSupplier color;

    public ImageDisplay setImageSize(int w, int h) {
        this.imageWidth = w;
        this.imageHeight = h;
        return this;
    }

    public ImageDisplay setColor(int color) {
        return setColor(() -> color);
    }

    public ImageDisplay setColor(IntSupplier color) {
        this.color = color;
        return this;
    }

    public ImageDisplay setImage(ResourceLocation image) {
        this.image = image;
        return this;
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        this.renderBorderedBackground(stack);

        if(image == null) return;

        RenderHelper.drawTexturedQuad(image, color.getAsInt(), stack, (getWidth() - imageWidth) / 2, (getHeight() - imageHeight) / 2, imageWidth, imageHeight);
    }
}