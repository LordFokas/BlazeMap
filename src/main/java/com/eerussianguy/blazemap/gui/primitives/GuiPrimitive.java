package com.eerussianguy.blazemap.gui.primitives;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public abstract class GuiPrimitive implements Renderable {
    private final int padding;
    private int x;
    private int y;
    private int width;
    private int height;

    protected boolean focused;
    protected ResourceLocation texture;
    protected RenderType textureRenderer;
    protected RenderFunction renderChildren;


    public GuiPrimitive(int x, int y, int width, int height) {
        this(null, x, y, width, height, 0, null);
    }

    public GuiPrimitive(int x, int y, int width, int height, RenderFunction renderChildren) {
        this(null, x, y, width, height, 0, renderChildren);
    }

    public GuiPrimitive(int x, int y, int width, int height, int padding, RenderFunction renderChildren) {
        this(null, x, y, width, height, padding, renderChildren);
    }

    public GuiPrimitive(ResourceLocation texture, int x, int y, int width, int height) {
        this(texture, x, y, width, height, 0, null);
    }

    public GuiPrimitive(ResourceLocation texture, int x, int y, int width, int height, RenderFunction renderChildren) {
        this(texture, x, y, width, height, 0, renderChildren);
    }

    public GuiPrimitive(ResourceLocation texture, int x, int y, int width, int height, int padding, RenderFunction renderChildren) {
        this.padding = padding;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.texture = texture;
        if (texture != null) {
            // Cache the RenderType to save a little work each frame
            this.textureRenderer = RenderType.text(texture);
        }

        this.renderChildren = renderChildren;
    }

    /**
     * Accessing a components position should always be done via these methods
     * to preserve composability.
     *
     * The exact position of a component shouldn't need to be written in code.
     * Everything can be defined relative to its parent component, except for
     * the outermost component for obvious reasons.
     */

    public int outerX()         { return this.x; }
    public int x()              { return this.x + this.padding; }
    public int x(int offset)    { return this.x + this.padding + offset; }
    public void moveX(int offset) { this.x += offset; }

    public int outerY()         { return this.y; }
    public int y()              { return this.y + this.padding; }
    public int y(int offset)    { return this.y + this.padding + offset; }
    public void moveY(int offset) { this.y += offset; }

    public int width()          { return width; }
    public int internalWidth()  { return width - 2 * padding; }
    public int height()         { return height; }
    public int internalHeight() { return height - 2 * padding; }

    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
        if (texture != null) {
            this.textureRenderer = RenderType.text(texture);
        } else {
            this.textureRenderer = null;
        }
    }

    /**
     * This functional interface defines a lambda expression that can be passed from
     * parent objects to tell this object what to render inside itself
     */
    @FunctionalInterface
    public interface RenderFunction {
        void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
    }

    /**
     * Finally, the actual render method to render this object and all of its children!
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (renderChildren != null) {
            renderChildren.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    /**
     * These are required for compat with GuiEventListener, a required interface for
     * interactable widgets.
     *
     * Child classes should override if they want different behaviour.
     */

    public void setFocused(boolean focused) {
        this.focused = focused;
    };
    public boolean isFocused() {
        return this.focused;
    }
}
