package com.eerussianguy.blazemap.lib.gui.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;

import com.eerussianguy.blazemap.api.maps.Renderable;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;

public abstract class BaseComponent<T extends BaseComponent<T>> extends Positionable<T> implements Renderable, Widget, NarratableEntry {
    private static float partial = 0F;
    private boolean enabled = true, visible = true, focused = false;
    private ContainerAxis axis;
    private ContainerDirection direction;
    private BaseComponent<?> target;
    private int spacing;

    public static float getPartialTick() {
        return partial;
    }

    @Override
    public abstract void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY);
    protected void renderTooltip(PoseStack stack, int mouseX, int mouseY, TooltipService service){}

    protected final void renderTooltipAsChild(PoseStack stack, int mouseX, int mouseY, TooltipService service){
        if(!isVisible()) return;
        renderTooltip(stack, mouseX, mouseY, service);
    }

    protected final void renderAsChild(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        if(!isVisible()) return;
        render(stack, hasMouse, mouseX, mouseY);
    }

    @Override
    public final void render(PoseStack stack, int mouseX, int mouseY, float partial) {
        if(!isVisible()) return;

        BaseComponent.partial = partial;
        boolean hasMouse = mouseIntercepts(mouseX, mouseY);
        int positionX = getPositionX(), positionY = getPositionY();

        stack.pushPose();
            if(getReferenceFrame() == ReferenceFrame.GLOBAL) {
                stack.translate(positionX, positionY, 0);
                mouseX -= positionX;
                mouseY -= positionY;
            }

            this.render(stack, hasMouse, mouseX, mouseY);

            stack.translate(0, 0, 100);
            if(hasMouse && Minecraft.getInstance().screen instanceof TooltipService service) {
                this.renderTooltip(stack, mouseX, mouseY, service);
            }

        stack.popPose();
    }

    protected void renderWithScissor(int x, int y, int w, int h, Runnable function) {
        x += getGlobalPositionX();
        y += getGlobalPositionY();
        RenderHelper.renderWithScissorScaled(x, y, w, h, function);
    }

    @Override
    public boolean mouseIntercepts(double mouseX, double mouseY) {
        if(!isVisible()) return false;
        return super.mouseIntercepts(mouseX, mouseY);
    }

    protected T anchorTo(BaseComponent<?> target, ContainerAxis axis, ContainerDirection direction, int spacing) {
        this.target = target;
        this.axis = axis;
        this.direction = direction;
        this.spacing = spacing;
        return this.setAnchor(target.getAnchor());
    }

    public int getAnchorX() {
        if(target == null) return super.getPositionX();
        if(axis == ContainerAxis.VERTICAL) return target.getAnchorX();

        int tx = target.getAnchorX();
        if(!target.isVisible()) return tx;

        int offset = target.getWidth() + spacing;
        return switch(direction) {
            case POSITIVE -> tx + offset;
            case NEGATIVE -> tx - offset;
        };
    }

    public int getAnchorY() {
        if(target == null) return super.getPositionY();
        if(axis == ContainerAxis.HORIZONTAL) return target.getAnchorY();

        int ty = target.getAnchorY();
        if(!target.isVisible()) return ty;

        int offset = target.getHeight() + spacing;
        return switch(direction) {
            case POSITIVE -> ty + offset;
            case NEGATIVE -> ty - offset;
        };
    }

    @Override
    public int getPositionX() {
        return getAnchorX() - getAnchor().getPositionX(getWidth(), 0);
    }

    @Override
    public int getPositionY() {
        return getAnchorY() - getAnchor().getPositionY(getHeight(), 0);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override //TODO: maybe, just MAYBE, one day do this
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override //TODO: maybe, just MAYBE, one day do this
    public void updateNarration(NarrationElementOutput p_169152_) {}

    /** Transforms an input into its equivalent component representation */
    @FunctionalInterface
    public interface Materializer<T> {
        BaseComponent<?> transform(T input);
    }
}