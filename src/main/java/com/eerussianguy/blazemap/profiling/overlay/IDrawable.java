package com.eerussianguy.blazemap.profiling.overlay;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public interface IDrawable {
    int getHeight();

    void draw(GuiGraphics graphics, Font fontRenderer);

    default boolean isDisabled(){
        return false;
    }
}
