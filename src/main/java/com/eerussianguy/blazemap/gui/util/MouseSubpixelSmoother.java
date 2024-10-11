package com.eerussianguy.blazemap.gui.util;

public class MouseSubpixelSmoother {
    private double partialX, partialY;
    private int movementX, movementY;

    public void addMovement(double moveX, double moveY) {
        moveX += partialX;
        moveY += partialY;
        partialX = moveX % 1D;
        partialY = moveY % 1D;
        movementX = (int) moveX;
        movementY = (int) moveY;
    }

    public int movementX() {
        return movementX;
    }

    public int movementY() {
        return movementY;
    }
}