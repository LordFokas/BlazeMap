package com.eerussianguy.blazemap.lib.gui.components;

import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.eerussianguy.blazemap.lib.gui.core.BaseContainer;

public class Placeholder extends BaseContainer<Placeholder> {

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public void add(BaseComponent<?> child) {
        clear();
        super.add(child);
        this.setSize(child.getWidth(), child.getHeight());
    }

    @Override
    public boolean isVisible() {
        return super.isVisible() && size() > 0;
    }
}
