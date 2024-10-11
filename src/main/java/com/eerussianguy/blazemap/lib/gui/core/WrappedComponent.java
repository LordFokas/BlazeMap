package com.eerussianguy.blazemap.lib.gui.core;

import java.util.Optional;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;

import com.eerussianguy.blazemap.api.maps.Renderable;
import com.mojang.blaze3d.vertex.PoseStack;

public abstract class WrappedComponent extends BaseComponent<WrappedComponent> {

    public static BaseComponent<?> ofNullable(Object content) {
        if(content == null) return null;
        return of(content);
    }

    public static Optional<BaseComponent<?>> ofOptional(Object content) {
        if(content == null) return Optional.empty();
        return Optional.of(of(content));
    }

    public static BaseComponent<?> of(Object content) {
        if(content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }

        // Already a BaseComponent from our ui lib. Just pass through, no need to wrap.
        if(content instanceof BaseComponent component) {
            return component;
        }

        // Renderable from our API. Allows addons to add components to our UI without using the whole lib.
        if(content instanceof Renderable renderable) {
            return new WrappedRenderable(renderable);
        }

        // Sometimes we just want to add "good" old vanilla components to our UI.
        // Our system already makes BaseComponent fit the vanilla implementation, with different internals.
        // This wrapper makes the vanilla implementation work seamlessly on top of those different internals.
        if(content instanceof AbstractWidget widget) {
            return new WrappedVanilla(widget);
        }

        throw new IllegalArgumentException("Object of class " + content.getClass() + " cannot be wrapped as a component");
    }

    @Override
    public WrappedComponent setSize(int w, int h) {
        throw new UnsupportedOperationException("Wrapped Components cannot be resized");
    }

    private static class WrappedRenderable extends WrappedComponent {
        private final Renderable renderable;

        private WrappedRenderable(Renderable renderable) {
            this.renderable = renderable;
        }

        @Override
        public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
            renderable.render(stack, hasMouse, mouseX, mouseY);
        }

        @Override
        public int getWidth() {
            return renderable.getWidth();
        }

        @Override
        public int getHeight() {
            return renderable.getHeight();
        }
    }

    // FIXME: widgets with tooltips are likely to render all fucked up
    public static class WrappedVanilla extends WrappedComponent implements GuiEventListener {
        private final AbstractWidget widget;
        private int padding;

        private WrappedVanilla(AbstractWidget widget) {
            this.widget = widget;
            this.setPadding(0);
        }

        @Override
        public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
            widget.render(stack, mouseX, mouseY, getPartialTick());
        }

        @Override
        public int getWidth() {
            return widget.getWidth() + padding * 2;
        }

        @Override
        public int getHeight() {
            return widget.getHeight() + padding * 2;
        }

        public WrappedComponent setPadding(int padding) {
            this.padding = widget.x = widget.y = padding;
            return this;
        }

        @Override
        public void mouseMoved(double mouseX, double mouseY) {
            widget.mouseMoved(mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return widget.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return widget.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double draggedX, double draggedY) {
            return widget.mouseDragged(mouseX, mouseY, button, draggedX, draggedY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
            return widget.mouseScrolled(mouseX, mouseY, scroll);
        }

        @Override
        public boolean keyPressed(int key, int mouseX, int mouseY) {
            return widget.keyPressed(key, mouseX, mouseY);
        }

        @Override
        public boolean keyReleased(int key, int mouseX, int mouseY) {
            return widget.keyReleased(key, mouseX, mouseY);
        }

        @Override
        public boolean charTyped(char ch, int modifier) {
            return widget.charTyped(ch, modifier);
        }

        @Override
        public boolean changeFocus(boolean focused) {
            return widget.changeFocus(focused);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return isVisible() && widget.isMouseOver(mouseX, mouseY);
        }
    }
}
