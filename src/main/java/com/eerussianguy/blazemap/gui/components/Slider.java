package com.eerussianguy.blazemap.gui.components;

import com.eerussianguy.blazemap.gui.lib.BaseComponent;
import com.eerussianguy.blazemap.gui.lib.Positionable;
import com.eerussianguy.blazemap.gui.trait.ComponentSounds;
import com.eerussianguy.blazemap.gui.trait.FocusableComponent;
import com.eerussianguy.blazemap.gui.trait.KeyboardControls;
import com.eerussianguy.blazemap.util.Helpers;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.floats.FloatConsumer;

public class Slider extends BaseComponent<Slider> implements FocusableComponent, ComponentSounds, KeyboardControls {
    protected FloatConsumer listener = $ -> {};
    protected float value = 0, step = 0.01F;
    private final Positionable<?> track = new Positionable<>().setPosition(1, 1);
    private final Positionable<?> handle = new Positionable<>().setPosition(0, -1);

    public Slider setValue(float value) {
        if(this.value != value) {
            this.value = value;
            listener.accept(value);
            handle.setPosition((int) (getTrackSize() * value), -1);
        }
        return this;
    }

    /** Sets the position and plays feedback sounds. To be used by internal controls. */
    protected void setValueInternal(float value, boolean playDenied) {
        if(value == this.value) {
            if(playDenied) playDeniedSound();
            return;
        }
        playPitchedClick(value > this.value ? 1.05F : 0.95F);
        setValue(value);
    }

    protected int getTrackSize() {
        return getWidth() - handle.getWidth();
    }

    public float getValue() {
        return value;
    }

    public Slider setListener(FloatConsumer listener) {
        this.listener = listener;
        listener.accept(value);
        return this;
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        renderBackground(stack, hasMouse, mouseX, mouseY);
        stack.translate(0, 0, 0.1F);
        renderForeground(stack, hasMouse, mouseX, mouseY);
    }

    protected void renderBackground(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        renderFocusableBackground(stack);
    }

    protected void renderForeground(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        renderFocusableBox(stack, handle.getPositionX(), handle.getPositionY(), handle.getWidth(), handle.getHeight(), getHandleColor());
    }

    public int getHandleColor() {
        return 0xFF404040;
    }

    @Override
    public Slider setSize(int w, int h) {
        track.setSize(w-2, h-2);
        handle.setSize(5, h+2);
        return super.setSize(w, h);
    }

    protected boolean step(int factor) {
        float value = Helpers.clamp(0, this.value + (step * factor), 1);
        setValueInternal(value, true);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(!track.mouseIntercepts(mouseX, mouseY)) return true;
        float value = (float) mouseX / track.getWidth();
        setValueInternal(value, true);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double draggedX, double draggedY) {
        if(!handle.mouseIntercepts(mouseX, mouseY)) return true;

        float value = Helpers.clamp(0F, this.value + (float) draggedX / track.getWidth(), 1);
        setValue(value);

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        return step(scroll > 0 ? -1 : 1);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if(isKeyUp(key)   || isKeyLeft(key))   return step(+1);
        if(isKeyDown(key) || isKeyRight(key))  return step(-1);
        return false;
    }
}