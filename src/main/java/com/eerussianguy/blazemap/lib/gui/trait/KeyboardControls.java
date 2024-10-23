package com.eerussianguy.blazemap.lib.gui.trait;

import org.lwjgl.glfw.GLFW;

public interface KeyboardControls {
    default boolean isKeyUp(int key) {
        return key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_W;
    }

    default boolean isKeyDown(int key) {
        return key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_S;
    }

    default boolean isKeyLeft(int key) {
        return key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_A;
    }

    default boolean isKeyRight(int key) {
        return key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_D;
    }

    default boolean isKeySubmit(int key) {
        return key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE;
    }
}
