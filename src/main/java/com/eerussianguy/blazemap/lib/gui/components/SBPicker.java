package com.eerussianguy.blazemap.lib.gui.components;

import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.eerussianguy.blazemap.lib.gui.core.Positionable;
import com.eerussianguy.blazemap.lib.gui.trait.ComponentSounds;
import com.eerussianguy.blazemap.lib.gui.trait.FocusableComponent;
import com.eerussianguy.blazemap.lib.gui.trait.KeyboardControls;
import com.mojang.blaze3d.vertex.PoseStack;

public class SBPicker extends BaseComponent<SBPicker> implements FocusableComponent, ComponentSounds, KeyboardControls {
    private SBConsumer listener = (s, b) -> {};
    private float posX = 1, posY = 0, stepX, stepY;
    private float hue;
    private int fullColor, handleColor;
    private final Positionable<?> gradient = new Positionable<>().setPosition(1,1);
    private final Positionable<?> handle = new Positionable<>().setSize(5,5);

    public SBPicker setHue(float hue) {
        this.hue = hue;
        this.fullColor = Colors.HSB2RGB(hue, 1, 1);
        updateHandleColor();
        return this;
    }

    public float getSaturation() {
        return posX;
    }

    public float getBrightness() {
        return 1 - posY;
    }

    @FunctionalInterface
    public interface SBConsumer {
        void accept(float saturation, float brightness);
    }

    public SBPicker setListener(SBConsumer listener) {
        this.listener = listener;
        listener.accept(getSaturation(), getBrightness());
        return this;
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        renderFocusableFlatBackground(stack);

        stack.pushPose();
        stack.translate(1, 1, 0);
        RenderHelper.renderGradient(stack, getWidth() - 2, getHeight() - 2, Colors.WHITE, fullColor, Colors.BLACK, Colors.BLACK);
        stack.popPose();

        stack.translate(0, 0, 0.1F);
        renderBorderedBox(stack, handle.getPositionX(), handle.getPositionY(), handle.getWidth(), handle.getHeight(), 0xFF808080, handleColor);
    }

    @Override
    public SBPicker setSize(int w, int h) {
        super.setSize(w, h);
        gradient.setSize(h-2, h-2);
        updateHandlePosition();
        stepX = 1F / getTrackX();
        stepY = 1F / getTrackY();
        return this;
    }

    private int getTrackX() {
        return getWidth() - handle.getWidth();
    }

    private int getTrackY() {
        return getHeight() - handle.getHeight();
    }

    public SBPicker setValue(float saturation, float brightness) {
        brightness = Helpers.clamp(0, brightness, 1);
        saturation = Helpers.clamp(0, saturation, 1);
        return setValueInternal(saturation, 1 - brightness);
    }

    private SBPicker setValueInternal(float posX, float posY) {
        if(this.posX == posX && this.posY == posY) return this;

        this.posX = posX;
        this.posY = posY;
        listener.accept(getSaturation(), getBrightness());
        updateHandleColor();
        updateHandlePosition();

        return this;
    }

    private void updateHandleColor() {
        handleColor = Colors.HSB2RGB(hue, getSaturation(), getBrightness());
    }

    private void updateHandlePosition() {
        handle.setPosition((int) (getTrackX() * posX), (int)(getTrackY() * posY));
    }

    private void setValueInternal(float posX, float posY, boolean playDenied) {
        if(this.posX == posX && this.posY == posY){
            if(playDenied) playDeniedSound();
            return;
        }

        playOkSound();

        setValueInternal(posX, posY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(!gradient.mouseIntercepts(mouseX, mouseY)) return true;

        float posX = (float) --mouseX / gradient.getWidth();
        float posY = (float) --mouseY / gradient.getHeight();

        setValueInternal(posX, posY, true);

        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double draggedX, double draggedY) {
        if(!handle.mouseIntercepts(mouseX, mouseY)) return true;

        float posX = Helpers.clamp(0F, this.posX + (float) draggedX / getTrackX(), 1);
        float posY = Helpers.clamp(0F, this.posY + (float) draggedY / getTrackY(), 1);

        setValueInternal(posX, posY);

        return true;
    }

    private boolean next(float x, float y) {
        float posX = Helpers.clamp(0, this.posX + x, 1);
        float posY = Helpers.clamp(0, this.posY + y, 1);
        setValueInternal(posX, posY, true);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if(isKeyUp(key))     return next(0, - stepY);
        if(isKeyDown(key))   return next(0, + stepY);
        if(isKeyLeft(key))   return next(- stepX, 0);
        if(isKeyRight(key))  return next(+ stepX, 0);
        return false;
    }
}
