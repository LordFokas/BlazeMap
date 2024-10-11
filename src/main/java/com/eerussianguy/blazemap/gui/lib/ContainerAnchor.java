package com.eerussianguy.blazemap.gui.lib;

public enum ContainerAnchor {
       TOP_LEFT,    TOP_CENTER,    TOP_RIGHT,
    MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT,

    NONE(0, 1);

    public final double position_top, position_left, padding_top, padding_left;

    ContainerAnchor() {
        this.position_top  = (double)(ordinal() / 3) / 2D;
        this.position_left = (double)(ordinal() % 3) / 2D;
        this.padding_top  = - (position_top  - 0.5) * 2;
        this.padding_left = - (position_left - 0.5) * 2;
    }

    ContainerAnchor(double position, double padding) {
        position_top = position_left = position;
        padding_top = padding_left = padding;
    }

    public int getPositionX(int width, int padding) {
        return (int)(width * position_left + padding * padding_left);
    }

    public int getPositionY(int height, int padding) {
        return (int)(height * position_top + padding * padding_top);
    }
}