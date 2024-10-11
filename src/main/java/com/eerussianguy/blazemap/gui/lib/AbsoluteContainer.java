package com.eerussianguy.blazemap.gui.lib;

public class AbsoluteContainer extends BaseContainer<AbsoluteContainer> {
    protected final int padding;

    public AbsoluteContainer(int padding) {
        this.padding = padding;
    }

    public void add(BaseComponent<?> child, ContainerAnchor anchor) {
        int positionX = anchor.getPositionX(getWidth() - child.getWidth(), padding);
        int positionY = anchor.getPositionY(getHeight() - child.getHeight(), padding);
        this.add(child.setAnchor(anchor), positionX, positionY);
    }

    public void add(BaseComponent<?> child, int positionX, int positionY) {
        super.add(child.setPosition(positionX, positionY));
    }
}
