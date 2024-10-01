package com.eerussianguy.blazemap.gui.lib;

import java.util.Optional;

import net.minecraft.client.gui.components.AbstractWidget;

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
            throw new IllegalArgumentException("content cannot be null");
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

        throw new IllegalArgumentException("Component of class " + content.getClass() + " cannot be wrapped as a component");
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

    // TODO: implement GuiEventListener and passthrough.
    // FIXME: widgets with tooltips are likely to render all fucked up
    private static class WrappedVanilla extends WrappedComponent {
        private final AbstractWidget widget;

        private WrappedVanilla(AbstractWidget widget) {
            this.widget = widget;
            widget.x = widget.y = 0;
        }

        @Override
        public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
            widget.render(stack, mouseX, mouseY, 0F);
        }

        @Override
        public int getWidth() {
            return widget.getWidth();
        }

        @Override
        public int getHeight() {
            return widget.getHeight();
        }
    }
}
