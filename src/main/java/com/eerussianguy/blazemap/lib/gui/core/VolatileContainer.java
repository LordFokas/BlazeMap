package com.eerussianguy.blazemap.lib.gui.core;

import com.eerussianguy.blazemap.lib.ArraySet;
import com.mojang.blaze3d.vertex.PoseStack;

public class VolatileContainer extends AbsoluteContainer {
    private final ArraySet<BaseComponent<?>> markedForRemoval = new ArraySet<>();

    public VolatileContainer(int padding) {
        super(padding);
    }

    /** Delays removal of components to allow for input event lifecycles to fully process */
    public void markForRemoval(BaseComponent<?> component) {
        markedForRemoval.add(component);
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        if(markedForRemoval.size() > 0) {
            for(var component : markedForRemoval) {
                remove(component);
            }
            markedForRemoval.clear();
        }

        super.render(stack, hasMouse, mouseX, mouseY);
    }
}
