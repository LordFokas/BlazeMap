package com.eerussianguy.blazemap.lib.gui.components;

import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;

public class HueSlider extends Slider {
    int color = 0xFF404040;

    {step = 1F / 72F;} // not something you see every day, is it?

    protected void renderBackground(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        stack.pushPose();
        renderFocusableFlatBackground(stack);
        stack.translate(1, 1, 0);
        RenderHelper.renderChromaticGradient(stack, getWidth() - 2, getHeight() - 2);
        stack.popPose();
    }

    @Override // dynamic handle color
    public int getHandleColor() {
        return color;
    }

    public Slider setValue(float value) {
        color = Colors.HSB2RGB(value, 1, 1);
        return super.setValue(value);
    }
}
