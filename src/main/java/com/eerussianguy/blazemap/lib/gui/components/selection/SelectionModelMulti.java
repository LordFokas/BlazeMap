package com.eerussianguy.blazemap.lib.gui.components.selection;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.eerussianguy.blazemap.lib.ArraySet;
import com.eerussianguy.blazemap.lib.Helpers;

public class SelectionModelMulti<T> extends SelectionModel<T, Set<T>> {
    protected final ArraySet<T> selection = new ArraySet<>();
    protected final Set<T> selection_view = Collections.unmodifiableSet(selection);
    private boolean ordered = false;

    public SelectionModelMulti() {}

    public SelectionModelMulti(Set<T> elements) {
        super(elements);
    }

    public SelectionModelMulti<T> ordered() {
        this.ordered = true;
        return this;
    }

    @Override
    protected void fireElementsChanged() {
        int size = selection.size();
        selection.retainAll(elements);

        super.fireElementsChanged();

        if(size < selection.size()) {
            fireSelectionChanged();
        }
    }

    @Override
    public void toggle(T item) {
        if(isSelected(item)) {
            selection.remove(item);
        }
        else {
            selection.add(item);
        }
        fireSelectionChanged();
    }

    @Override
    public boolean isSelected(T item) {
        return selection.contains(item);
    }

    @Override
    public Set<T> getSelected() {
        if(isOrdered()) { // guarantees selection is returned in order
            return elements.stream().filter(selection::contains).collect(Collectors.toSet());
        } else {
            return selection_view;
        }
    }

    public void setSelected(Set<T> selected) { // I know this check is O(n^2) shut up.
        var list = selected.stream().filter(elements::contains).toList();
        if(selection.containsAll(list) && list.containsAll(selection)) return;

        selection.clear();
        selection.addAll(list);
        fireSelectionChanged();
    }

    @Override
    public boolean isOrdered() {
        return ordered;
    }

    @Override
    public boolean canMoveDown(T item) {
        return isOrdered() && elements.indexOf(item) < (elements.size() - 1);
    }

    @Override
    public boolean canMoveUp(T item) {
        return isOrdered() && elements.indexOf(item) > 0;
    }

    @Override
    public void move(T item, int direction) {
        if(!isOrdered()) return;

        int index = elements.indexOf(item);
        if(index == -1) return;

        index = Helpers.clamp(0, index + direction, elements.size() - 1);
        elements.remove(item);
        elements.add(index, item);
    }
}
