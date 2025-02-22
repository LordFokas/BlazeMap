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

    public static float getPartialTick() {
        return partial;
    }

    @Override
    public abstract void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY);
    protected void renderTooltip(PoseStack stack, int mouseX, int mouseY, TooltipService service){}

    protected final void renderInternal(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        if(!isVisible()) return;
        render(stack, hasMouse, mouseX, mouseY);
    }

    @Override
    public final void render(PoseStack stack, int mouseX, int mouseY, float partial) {
        if(!isVisible()) return;
        BaseComponent.partial = partial;
        stack.pushPose();

        boolean hasMouse = mouseIntercepts(mouseX, mouseY);

        int positionX = getPositionX(), positionY = getPositionY();
        if(getReferenceFrame() == ReferenceFrame.GLOBAL) {
            stack.translate(positionX, positionY, 0);
            mouseX -= positionX;
            mouseY -= positionY;
        }

        this.renderInternal(stack, hasMouse, mouseX, mouseY);

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