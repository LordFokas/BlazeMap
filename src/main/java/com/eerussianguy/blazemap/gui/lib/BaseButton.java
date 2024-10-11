package com.eerussianguy.blazemap.gui.lib;

import java.util.function.IntConsumer;

import net.minecraft.client.gui.components.events.GuiEventListener;

import com.eerussianguy.blazemap.gui.trait.ComponentSounds;

public abstract class BaseButton<T extends BaseButton<T>> extends BaseComponent<T> implements ComponentSounds, GuiEventListener {
    private final IntConsumer function;

    public BaseButton(IntConsumer function) {
        this.function = function;
    }

    protected boolean onClick(int button) {
        playOkSound();
        function.accept(button);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(!isEnabled()){
            playDeniedSound();
            return true;
        }
        return onClick(button);
    }
}
