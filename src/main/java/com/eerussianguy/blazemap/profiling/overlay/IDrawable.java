package com.eerussianguy.blazemap.profiling.overlay;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;

public interface IDrawable {
    int getHeight();

    void draw(GuiGraphics graphics, MultiBufferSource buffers, Font fontRenderer);

    default boolean isDisabled(){
        return false;
    }
}
