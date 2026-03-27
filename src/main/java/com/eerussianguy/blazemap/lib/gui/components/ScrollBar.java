package com.eerussianguy.blazemap.lib.gui.components;

import java.util.function.IntSupplier;

import com.eerussianguy.blazemap.lib.RenderHelper;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.mojang.blaze3d.vertex.PoseStack;

public class ScrollBar extends BaseComponent<ScrollBar> {
    private final IntSupplier track, view, pos;
    private final int border, width;
    private boolean alwaysVisible = false;

    public ScrollBar(int border, int width, IntSupplier track, IntSupplier view, IntSupplier pos) {
        this.border = border;
        this.width = width;
        this.track = track;
        this.view = view;
        this.pos = pos;
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        RenderHelper.fillRect(stack.last().pose(), width, getHeight(), 0xFF242424);
        if(needsTrack()) {
            float view = this.view.getAsInt(), track = this.track.getAsInt();
            stack.scale(1, view / (view + track), 1);
            stack.translate(0, pos.getAsInt(), 0);
            RenderHelper.fillRect(stack.last().pose(), width, getHeight(), 0xFF444444);
        }
    }

    public void setAlwaysVisible(boolean always) {
        this.alwaysVisible = always;
    }

    @Override
    public boolean isVisible() { // If there is no y-overflow, do not render
        return super.isVisible() && (alwaysVisible || needsTrack());
    }

    public boolean needsTrack() {
        return track.getAsInt() > 0;
    }

    // Force component to be full height and attach to the right side of parent // =====================================
    @Override public int getWidth()     { return width; }
    @Override public int getHeight()    { return getParent().getHeight() - border * 2; }
    @Override public int getPositionX() { return getParent().getWidth() - width - border; }
    @Override public int getPositionY() { return border; }
    // =================================================================================================================
}
