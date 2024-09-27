package com.eerussianguy.blazemap.gui.lib;

import java.util.function.IntConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public abstract class BaseButton<T extends BaseButton<T>> extends BaseComponent<T> implements GuiEventListener {
    private final IntConsumer function;

    public BaseButton(IntConsumer function) {
        this.function = function;
    }

    protected boolean onClick(int button) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        function.accept(button);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(!isEnabled()) return false;
        return onClick(button);
    }
}
