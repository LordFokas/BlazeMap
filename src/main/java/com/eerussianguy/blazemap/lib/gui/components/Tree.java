package com.eerussianguy.blazemap.lib.gui.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.eerussianguy.blazemap.lib.gui.core.BaseScrollable;

public class Tree extends BaseScrollable<Tree> {
    private final List<TreeItem> items = new ArrayList<>();

    public Tree() {
        this(10);
    }

    public Tree(int step) {
        super(new TreeContainer(1, step));
    }

    public <T extends BaseComponent<?> & TreeItem> void addItem(T item) {
        items.add(item);
        recalculate();
    }

    public void clearItems() {
        items.clear();
        recalculate();
    }

    @Override
    public void recalculate() {
        items.removeIf(TreeItem::wasDeleted);
        super.recalculate();
    }

    // =================================================================================================================
    public interface TreeItem {
        @SuppressWarnings("unchecked")
        default List<TreeItem> getChildren() {
            return Collections.EMPTY_LIST;
        }

        default void setUpdater(Runnable function){ }

        default boolean wasDeleted() {
            return false;
        }
    }

    // =================================================================================================================
    protected static class TreeContainer extends ScrollableContainer<Tree> {
        protected TreeContainer(int spacing, int step) {
            super(spacing, step);
        }

        @Override
        protected void recalculate() {
            var items = parent().items;
            this.clear();
            if(items.size() == 0) return;
            int y = deepAdd(items, 0) - spacing;
            this.setSize(0, y);

            super.recalculate();
        }

        protected int deepAdd(List<TreeItem> items, int y) {
            Tree tree = parent();
            for(var item : items) {
                item.setUpdater(tree::recalculate);
                var component = (BaseComponent<?>) item;
                add(component);
                y += component.getHeight() + spacing;
                y = deepAdd(item.getChildren(), y);
            }
            return y;
        }
    }
}