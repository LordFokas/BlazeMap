package com.eerussianguy.blazemap.gui.trait;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public interface ComponentSounds {
    default void playOkSound() {
        playPitchedClick(1.0F);
    }

    default void playDeniedSound() {
        playPitchedClick(0.8F);
    }

    default void playPitchedClick(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }
}
