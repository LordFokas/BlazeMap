package com.eerussianguy.blazemap.gui.trait;

import net.minecraft.client.gui.components.events.GuiEventListener;

import com.eerussianguy.blazemap.util.Colors;
import com.mojang.blaze3d.vertex.PoseStack;

public interface FocusableComponent extends BorderedComponent, GuiEventListener {
    boolean isFocused();
    void setFocused(boolean focused);

    @Override
    default boolean changeFocus(boolean focused) {
        this.setFocused(focused);
        return true;
    }

    default int getFocusColor() {
        return getFocusColor(false);
    }

    default int getFocusColor(boolean hasMouse) {
        return isEnabled() ? (isFocused() || hasMouse ? Colors.WHITE : Colors.UNFOCUSED) : Colors.DISABLED;
    }

    default void renderFocusableBackground(PoseStack stack) {
        this.renderBorderedBackground(stack, getFocusColor());
    }

    default void renderFocusableBackground(PoseStack stack, boolean hasMouse) {
        this.renderBorderedBackground(stack, getFocusColor(hasMouse), getBackgroundColor(hasMouse));
    }

    default void renderFocusableBox(PoseStack stack, float posX, float posY, int w, int h) {
        this.renderFocusableBox(stack, posX, posY, w, h, Colors.BLACK);
    }

    default void renderFocusableBox(PoseStack stack, float posX, float posY, int w, int h, int background) {
        this.renderBorderedBox(stack, posX, posY, w, h, getFocusColor(), background);
    }

    default void renderFocusableFlatBackground(PoseStack stack) {
        this.renderFlatBackground(stack, getFocusColor());
    }
}