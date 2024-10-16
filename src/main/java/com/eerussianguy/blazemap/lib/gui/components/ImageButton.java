package com.eerussianguy.blazemap.lib.gui.components;

import java.util.function.IntConsumer;

import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.eerussianguy.blazemap.lib.gui.core.BaseButton;
import com.mojang.blaze3d.vertex.PoseStack;

public class ImageButton extends BaseButton<ImageButton> {
    protected final ResourceLocation background;

    public ImageButton(ResourceLocation background, int width, int height, IntConsumer function) {
        super(function);
        this.background = background;
        this.setSize(width, height);
    }

    protected int getTint() {
        return Colors.NO_TINT;
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        RenderHelper.drawTexturedQuad(background, getTint(), stack, 0, 0, getWidth(), getHeight());
    }
}
