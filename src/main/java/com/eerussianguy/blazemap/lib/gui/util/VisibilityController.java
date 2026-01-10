package com.eerussianguy.blazemap.lib.gui.util;

import com.eerussianguy.blazemap.lib.ArraySet;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;

public class VisibilityController {
    private final ArraySet<BaseComponent<?>> components = new ArraySet<>();
    private boolean visibile = true;

    public VisibilityController(BaseComponent<?> ... children) {
        add(children);
    }

    public VisibilityController add(BaseComponent<?> ... children) {
        for(var component : children) {
            components.add(component);
            component.setVisible(visibile);
        }
        return this;
    }

    public VisibilityController clear() {
        components.clear();
        return this;
    }

    public boolean isVisibile() {
        return visibile;
    }

    public void setVisible(boolean visibile) {
        this.visibile = visibile;
        for(var component : components) {
            component.setVisible(visibile);
        }
    }

    public boolean toggleVisible() {
        this.setVisible(!visibile);
        return visibile;
    }
}
