package com.eerussianguy.blazemap.gui.lib;

public class EdgeReference extends Positionable<EdgeReference> {
    public EdgeReference(BaseComponent<?> parent, ContainerAnchor anchor){
        withParent(parent).setAnchor(anchor);
    }

    @Override
    public int getPositionX() {
        return getAnchor().getPositionX(getParent().getWidth() - getWidth(), super.getPositionX());
    }

    @Override
    public int getPositionY() {
        return getAnchor().getPositionY(getParent().getHeight() - getHeight(), super.getPositionY());
    }

    @Override
    protected ReferenceFrame getReferenceFrame() {
        return ReferenceFrame.GLOBAL;
    }
}