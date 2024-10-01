package com.eerussianguy.blazemap.gui.components;

import java.util.Objects;

import com.eerussianguy.blazemap.gui.lib.BaseComponent;
import com.eerussianguy.blazemap.gui.lib.BaseContainer;
import com.eerussianguy.blazemap.gui.lib.ContainerAxis;
import com.eerussianguy.blazemap.gui.lib.ContainerDirection;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;


public class LineContainer extends BaseContainer<LineContainer> {
    private int background = 0;
    protected final ContainerAxis axis;
    protected final ContainerDirection direction;
    protected final int spacing;

    public LineContainer(ContainerAxis axis, ContainerDirection direction, int spacing) {
        this.axis = Objects.requireNonNull(axis);
        this.direction = Objects.requireNonNull(direction);
        this.spacing = spacing;
    }

    @Override
    public void add(BaseComponent<?> child) {
        if(direction == ContainerDirection.POSITIVE) {
            child.setPosition(nextChildX(), nextChildY());
            super.add(child);
        } else {
            switch(axis) {
                case HORIZONTAL -> {
                    int width = child.getWidth() + spacing;
                    forEach(c -> c.addPositionX(width));
                }
                case VERTICAL -> {
                    int height = child.getHeight() + spacing;
                    forEach(c -> c.addPositionY(height));
                }
            }
            child.setPosition(spacing, spacing);
            super.add(child);
        }
    }

    public LineContainer with(BaseComponent<?> ... children) {
        for(var child : children) {
            add(child);
        }
        return this;
    }

    public LineContainer addSpacer() {
        var spacer = new Spacer(spacing, Colors.DISABLED);
        switch(axis) {
            case HORIZONTAL -> spacer.setSize(1 + spacing*2, getHeight() - spacing*2);
            case VERTICAL -> spacer.setSize(getWidth() - spacing*2, 1 + spacing*2);
        }
        add(spacer);
        return this;
    }

    @Override
    protected void renderBackground(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        if(size() == 0) return;

        RenderHelper.fillRect(stack.last().pose(), getWidth(), getHeight(), background);
    }

    public LineContainer withBackground(int color) {
        this.background = color;
        return this;
    }

    public LineContainer withBackground() {
        return withBackground(Colors.WIDGET_BACKGROUND);
    }

    @Override
    public int getWidth() {
        if(size() == 0) return 0;

        return switch(axis) {
            case HORIZONTAL -> sum(BaseComponent::getWidth) + (size()+1) * spacing;
            case VERTICAL -> max(BaseComponent::getWidth) + 2 * spacing;
        };
    }

    @Override
    public int getHeight() {
        if(size() == 0) return 0;

        return switch(axis) {
            case HORIZONTAL -> max(BaseComponent::getHeight) + 2 * spacing;
            case VERTICAL -> sum(BaseComponent::getHeight) + (size()+1) * spacing;
        };
    }

    public int nextChildX() {
        return switch(axis) {
            case HORIZONTAL -> size() == 0 ? spacing : getWidth();
            case VERTICAL -> spacing;
        };
    }

    public int nextChildY() {
        return switch(axis) {
            case HORIZONTAL -> spacing;
            case VERTICAL -> size() == 0 ? spacing : getHeight();
        };
    }

    static class Spacer extends BaseComponent<Spacer> {
        private final int spacing, color;

        Spacer(int spacing, int color) {
            this.spacing = spacing;
            this.color = (color & Colors.ALPHA) == 0 ? Colors.ALPHA | color : color;
        }

        @Override
        public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
            stack.translate(spacing, spacing, 0.1);
            RenderHelper.fillRect(stack.last().pose(), getWidth() - spacing*2, getHeight() - spacing*2, color);
        }
    }
}
