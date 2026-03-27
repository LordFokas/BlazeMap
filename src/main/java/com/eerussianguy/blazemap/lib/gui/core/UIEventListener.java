package com.eerussianguy.blazemap.lib.gui.core;

import net.minecraft.client.gui.components.events.GuiEventListener;

/**
 * This only exists to give proper names to parameters
 * and document how to use the methods as needed.
 *
 * @author LordFokas
 */
public interface UIEventListener extends GuiEventListener {
    default void mouseMoved(double mouseX, double mouseY) {}

    default boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }

    default boolean mouseReleased(double mouseX, double mouseY, int button) { return false; }

    default boolean mouseDragged(double mouseX, double mouseY, int button, double draggedX, double draggedY) { return false; }

    default boolean mouseScrolled(double mouseX, double mouseY, double scroll) { return false; }

    default boolean keyPressed(int key, int scancode, int modifiers) { return false; }

    default boolean keyReleased(int key, int scancode, int modifiers) { return false; }

    default boolean charTyped(char ch, int modifier) { return false; }

    default boolean changeFocus(boolean focused) { return false; }

    default boolean isMouseOver(double mouseX, double mouseY) { return false; }
}
