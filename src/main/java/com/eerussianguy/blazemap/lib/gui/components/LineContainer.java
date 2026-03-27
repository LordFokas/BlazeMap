package com.eerussianguy.blazemap.lib.gui.components;

import java.util.Objects;

import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.eerussianguy.blazemap.lib.gui.core.BaseContainer;
import com.eerussianguy.blazemap.lib.gui.core.ContainerAxis;
import com.eerussianguy.blazemap.lib.gui.core.ContainerDirection;
import com.mojang.blaze3d.vertex.PoseStack;


public class LineContainer extends BaseContainer<LineContainer> {
    private int background = 0;
    protected final ContainerAxis axis;
    protected final ContainerDirection direction;
    protected final int padding, spacing;

    public LineContainer(ContainerAxis axis, ContainerDirection direction, int padding) {
        this(axis, direction, padding, padding);
    }

    public LineContainer(ContainerAxis axis, ContainerDirection direction, int padding, int spacing) {
        this.axis = Objects.requireNonNull(axis);
        this.direction = Objects.requireNonNull(direction);
        this.padding = padding;
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
                    forEach(c -> c.moveX(width));
                }
                case VERTICAL -> {
                    int height = child.getHeight() + spacing;
                    forEach(c -> c.moveY(height));
                }
            }
            child.setPosition(padding, padding);
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
        var spacer = new Spacer(padding, Colors.DISABLED);
        switch(axis) {
            case HORIZONTAL -> spacer.setSize(1 + padding *2, getHeight() - padding *2);
            case VERTICAL -> spacer.setSize(getWidth() - padding *2, 1 + padding *2);
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
            case HORIZONTAL -> sum(BaseComponent::getIndependentWidth) + (size()-1) * spacing + padding * 2;
            case VERTICAL -> max(BaseComponent::getIndependentWidth) + 2 * padding;
        };
    }

    @Override
    public int getHeight() {
        if(size() == 0) return 0;

        return switch(axis) {
            case HORIZONTAL -> max(BaseComponent::getIndependentHeight) + 2 * padding;
            case VERTICAL -> sum(BaseComponent::getIndependentHeight) + (size()-1) * spacing + padding * 2;
        };
    }

    public int nextChildX() {
        return switch(axis) {
            case HORIZONTAL -> size() == 0 ? padding : getWidth() - padding + spacing;
            case VERTICAL -> padding;
        };
    }

    public int nextChildY() {
        return switch(axis) {
            case HORIZONTAL -> padding;
            case VERTICAL -> size() == 0 ? padding : getHeight() - padding + spacing;
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
