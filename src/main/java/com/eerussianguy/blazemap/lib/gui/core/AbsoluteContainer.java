package com.eerussianguy.blazemap.lib.gui.core;

public class AbsoluteContainer extends BaseContainer<AbsoluteContainer> {
    protected final int padding;

    public AbsoluteContainer(int padding) {
        this.padding = padding;
    }

    public void add(BaseComponent<?> child, ContainerAnchor anchor) {
        int positionX = anchor.getPositionX(getWidth(), padding);
        int positionY = anchor.getPositionY(getHeight(), padding);
        this.add(child.setAnchor(anchor), positionX, positionY);
    }

    public void add(BaseComponent<?> child, int positionX, int positionY) {
        super.add(child.setPosition(positionX, positionY));
    }

    public void anchor(BaseComponent<?> child, BaseComponent<?> target, ContainerAxis axis, ContainerDirection direction) {
        anchor(child, target, axis, direction, padding);
    }

    public void anchor(BaseComponent<?> child, BaseComponent<?> target, ContainerAxis axis, ContainerDirection direction, int spacing) {
        super.add(child.anchorTo(target, axis, direction, spacing));
    }
}
