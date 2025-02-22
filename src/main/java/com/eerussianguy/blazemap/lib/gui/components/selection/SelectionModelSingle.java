package com.eerussianguy.blazemap.lib.gui.components.selection;

import java.util.Objects;
import java.util.Set;

public class SelectionModelSingle<T> extends SelectionModel<T, T> {
    protected T selection = null;

    public SelectionModelSingle() {}

    public SelectionModelSingle(Set<T> elements) {
        super(elements);
    }

    @Override
    protected void fireElementsChanged() {
        boolean changed = false;
        if(!elements.contains(selection)) {
            selection = null;
            changed = true;
        }

        super.fireElementsChanged();

        if(changed) {
            fireSelectionChanged();
        }
    }

    @Override
    public void toggle(T item) {
        if(Objects.equals(selection, item)) return;

        selection = item;
        fireSelectionChanged();
    }

    @Override
    public boolean isSelected(T item) {
        return Objects.equals(item, selection);
    }

    @Override
    public T getSelected() {
        return selection;
    }

    @Override
    public void setSelected(T selected) {
        if(!elements.contains(selected)) return;

        if(Objects.equals(selection, selected)) return;

        selection = selected;
        fireSelectionChanged();
    }

    public void selectFirst() {
        if(elements.size() == 0) return;
        toggle(elements.get(0));
    }
}
