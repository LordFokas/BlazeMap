package com.eerussianguy.blazemap.gui.lib;

public class Positionable<T extends Positionable<T>> {
    private ReferenceFrame referenceFrame = ReferenceFrame.GLOBAL;
    private int positionX, positionY;
    private int width, height;

    protected T withParentReferenceFrame() {
        this.referenceFrame = ReferenceFrame.PARENT;
        return (T) this;
    }

    protected ReferenceFrame getReferenceFrame() {
        return referenceFrame;
    }

    public T setPosition(int x, int y) {
        this.positionX = x;
        this.positionY = y;
        return (T) this;
    }

    public T addPositionX(int x) {
        this.positionX += x;
        return (T) this;
    }

    public T addPositionY(int y) {
        this.positionY += y;
        return (T) this;
    }

    public T shiftPositionX() {
        return addPositionX( - getWidth());
    }

    public T shiftPositionY() {
        return addPositionY( - getHeight());
    }

    public int getPositionX() {
        return positionX;
    }

    public int getPositionY() {
        return positionY;
    }

    public T setSize(int w, int h) {
        this.width = w;
        this.height = h;
        return (T) this;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
