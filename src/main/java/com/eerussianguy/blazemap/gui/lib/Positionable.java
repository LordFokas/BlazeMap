package com.eerussianguy.blazemap.gui.lib;

public class Positionable<T extends Positionable<T>> {
    private ReferenceFrame referenceFrame = ReferenceFrame.GLOBAL;
    private ContainerAnchor anchor = ContainerAnchor.NONE;
    private BaseComponent<?> parent;
    private int positionX, positionY;
    private int width, height;

    protected T withParent(BaseComponent<?> parent) {
        this.parent = parent;
        this.referenceFrame = ReferenceFrame.PARENT;
        return (T) this;
    }

    protected BaseComponent<?> getParent() {
        return parent;
    }

    protected ReferenceFrame getReferenceFrame() {
        return referenceFrame;
    }

    protected T setAnchor(ContainerAnchor anchor) {
        this.anchor = anchor;
        return (T) this;
    }

    protected ContainerAnchor getAnchor() {
        return anchor;
    }

    public T setPosition(int x, int y) {
        this.positionX = x;
        this.positionY = y;
        return (T) this;
    }

    public T move(int x, int y) {
        this.positionX += x;
        this.positionY += y;
        return (T) this;
    }

    public T moveX(int x) {
        this.positionX += x;
        return (T) this;
    }

    public T moveY(int y) {
        this.positionY += y;
        return (T) this;
    }

    @Deprecated
    public T shiftPositionX() {
        return moveX( - getWidth());
    }

    @Deprecated
    public T shiftPositionY() {
        return moveY( - getHeight());
    }

    public int getPositionX() {
        return positionX;
    }

    public int getPositionY() {
        return positionY;
    }

    public int getGlobalPositionX() {
        return getPositionX() + (parent == null ? 0 : parent.getGlobalPositionX());
    }

    public int getGlobalPositionY() {
        return getPositionY() + (parent == null ? 0 : parent.getGlobalPositionY());
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

    /**
     * Called by containers that stretch with content size
     * Must be overridden by components that stretch with parent container size
     * Failure to do so will cause Stack Overflows
     */
    public int getIndependentWidth() {
        return getWidth();
    }

    /**
     * Called by containers that stretch with content size
     * Must be overridden by components that stretch with parent container size
     * Failure to do so will cause Stack Overflows
     */
    public int getIndependentHeight() {
        return getHeight();
    }

    public boolean mouseIntercepts(double mouseX, double mouseY) {
        if(getReferenceFrame() == ReferenceFrame.GLOBAL) {
            mouseX -= getPositionX();
            mouseY -= getPositionY();
        }
        return mouseX >= 0
            && mouseX <  getWidth()
            && mouseY >= 0
            && mouseY <  getHeight();
    }
}
