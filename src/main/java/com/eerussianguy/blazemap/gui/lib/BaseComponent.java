package com.eerussianguy.blazemap.gui.lib;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;

import com.eerussianguy.blazemap.api.maps.Renderable;
import com.mojang.blaze3d.vertex.PoseStack;

public abstract class BaseComponent<T extends BaseComponent<T>> extends Positionable<T> implements Renderable, Widget, NarratableEntry {
    private boolean enabled = true, visible = true;

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

    public boolean mouseIntercepts(double mouseX, double mouseY) {
        if(!isVisible()) return false;
        if(getReferenceFrame() == ReferenceFrame.GLOBAL) {
            mouseX -= getPositionX();
            mouseY -= getPositionY();
        }
        return mouseX >= 0
            && mouseX <  getWidth()
            && mouseY >= 0
            && mouseY <  getHeight();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override //TODO: maybe, just MAYBE, one day do this
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override //TODO: maybe, just MAYBE, one day do this
    public void updateNarration(NarrationElementOutput p_169152_) {}
}