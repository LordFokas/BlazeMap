package com.eerussianguy.blazemap.lib.gui.core;

import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.lib.gui.components.LineContainer;
import com.eerussianguy.blazemap.lib.gui.components.ScrollBar;
import com.eerussianguy.blazemap.lib.gui.trait.ComponentSounds;
import com.eerussianguy.blazemap.lib.gui.trait.FocusableComponent;
import com.mojang.blaze3d.vertex.PoseStack;

public abstract class BaseScrollable<T extends BaseScrollable<T>> extends BaseContainer<T> implements FocusableComponent, ComponentSounds {
    protected static final int SCROLLBAR_WIDTH = 5;
    protected final ScrollBar scroll;
    protected final ScrollableContainer<?> container;

    public BaseScrollable(ScrollableContainer<?> container) {
        this.container = container;
        super.add(container);

        this.scroll = new ScrollBar(1, SCROLLBAR_WIDTH, () -> container.trackSize, () -> getHeight() - 4, () -> container.trackPos);
        super.add(scroll);
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

    @Override @SuppressWarnings("unchecked")
    public T setSize(int w, int h) {
        super.setSize(w, h);
        recalculate();
        return (T) this;
    }

    public void recalculate() {
        container.recalculate();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        container.step(scroll > 0 ? -1 : 1);
        return true;
    }

    // =================================================================================================================
    protected static class ScrollableContainer<T extends BaseScrollable<?>> extends LineContainer implements ComponentSounds {
        protected final int step;
        private int trackSize = 0;
        private int trackPos = 0;
        private T parent;

        public ScrollableContainer(int spacing) {
            this(spacing, 10);
        }

        public ScrollableContainer(int spacing, int step) {
            super(ContainerAxis.VERTICAL, ContainerDirection.POSITIVE, spacing);
            this.setPosition(1, 1);
            this.step = step;
        }

        @Override @SuppressWarnings("unchecked")
        protected LineContainer withParent(BaseComponent<?> parent) {
            this.parent = (T) parent;
            return super.withParent(parent);
        }

        protected T parent() {
            return parent;
        }

        protected void addItem(BaseComponent<?> child) {
            super.add(child);
            recalculate();
        }

        @Override // Dynamically stick to parent width very carefully to not cause Stack Overflows
        public int getWidth() {
            return parent().getWidth() - (parent().scroll.isVisible() ? SCROLLBAR_WIDTH : 0) - (spacing+1) * 2;
        }

        protected void recalculate() {
            var parent = parent();
            if(parent == null) return;

            int viewport = (parent.getHeight() - spacing * 2);
            int self = getHeight();
            if(viewport < self) {
                trackSize = self - viewport;
                setTrackPos(trackPos, false);
            } else {
                trackSize = 0;
                setTrackPos(0, false);
            }
        }

        protected void setTrackPos(int nextPos, boolean sound) {
            int previous = this.trackPos; // for sound

            // set track pos
            this.trackPos = Helpers.clamp(0, nextPos, trackSize);
            this.setPosition(spacing, spacing - this.trackPos);

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

        protected void step(int dir) {
            if(trackSize == 0) return;
            setTrackPos(trackPos + step * dir, true);
        }
    }
}
