package com.eerussianguy.blazemap.lib.gui.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.lib.gui.core.AbsoluteContainer;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.eerussianguy.blazemap.lib.gui.core.BaseContainer;
import com.eerussianguy.blazemap.lib.gui.trait.ComponentSounds;
import com.eerussianguy.blazemap.lib.gui.trait.FocusableComponent;
import com.eerussianguy.blazemap.lib.gui.trait.KeyboardControls;
import com.mojang.blaze3d.vertex.PoseStack;

public class TreeContainer extends BaseContainer<TreeContainer> implements FocusableComponent, ComponentSounds, KeyboardControls {
    private static final int SCROLLBAR_WIDTH = 5;
    private final List<TreeItem> items = new ArrayList<>();
    private final ScrollBar scroll;
    private final TreeList tree;

    public TreeContainer() {
        this(10);
    }

    public TreeContainer(int step) {
        this.tree = new TreeList(1, step, SCROLLBAR_WIDTH);
        super.add(tree);

        this.scroll = new ScrollBar(1, SCROLLBAR_WIDTH, () -> tree.trackSize, () -> getHeight() - 4, () -> tree.trackPos);
        super.add(scroll);
    }

    public <T extends BaseComponent<?> & TreeItem> void addItem(T item) {
        items.add(item);
        item.setUpdater(this::recalculate);
        recalculate();
    }

    public ScrollBar getScrollBar() {
        return scroll;
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        renderFocusableBackground(stack);
        renderWithScissor(1, 1, getWidth() - 2, getHeight() - 2,
            () -> super.render(stack, hasMouse, mouseX, mouseY)
        );
    }

    @Override
    public TreeContainer setSize(int w, int h) {
        super.setSize(w, h);
        recalculate();
        return this;
    }

    public void recalculate() {
        tree.recalculate();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        tree.step(scroll > 0 ? -1 : 1);
        return true;
    }

    // =================================================================================================================
    public interface TreeItem {
        @SuppressWarnings("unchecked")
        default List<TreeItem> getChildren() {
            return Collections.EMPTY_LIST;
        }

        default void setUpdater(Runnable function){ }
    }

    // =================================================================================================================
    private class TreeList extends AbsoluteContainer implements ComponentSounds {
        private final int offset, step, scroll;
        private int trackSize = 0, trackPos = 0;

        private TreeList(int padding, int step, int scroll) {
            super(padding);
            this.step = step;
            this.scroll = scroll;
            offset = padding + 1;
        }

        @Override
        public int getWidth() { // shrink to accommodate scrollbar
            return super.getWidth() - (TreeContainer.this.scroll.isVisible() ? scroll : 0);
        }

        private void recalculate() {
            this.clear();
            if(items.size() == 0) return;
            int y = deepAdd(items, 0) - padding;
            this.setSize(TreeContainer.this.getWidth() - offset*2, y);

            int parent = (TreeContainer.this.getHeight() - offset * 2);
            int self = getHeight();
            if(parent < self) {
                trackSize = self - parent;
                setTrackPos(trackPos, false);
            } else {
                trackSize = 0;
                setTrackPos(0, false);
            }
        }

        private int deepAdd(List<TreeItem> items, int y) {
            for(var item : items) {
                var component = (BaseComponent<?>) item;
                add(component, 0, y);
                y += component.getHeight() + padding;
                y = deepAdd(item.getChildren(), y);
            }
            return y;
        }

        private void setTrackPos(int nextPos, boolean sound) {
            int previous = this.trackPos; // for sound

            // set track pos
            this.trackPos = Helpers.clamp(0, nextPos, trackSize);
            this.setPosition(offset, offset - this.trackPos);

            if(sound) { // play feedback sound
                if(this.trackPos < previous) {
                    playUpSound();
                } else if (this.trackPos > previous) {
                    playDownSound();
                } else {
                    playDeniedSound();
                }
            }
        }

        private void step(int dir) {
            if(trackSize == 0) return;
            setTrackPos(trackPos + step * dir, true);
        }
    }
}