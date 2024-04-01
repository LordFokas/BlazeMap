package com.eerussianguy.blazemap.profiling.overlay;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;

import com.mojang.blaze3d.vertex.PoseStack;

public interface IDrawable {
    int getHeight();

    void draw(PoseStack stack, MultiBufferSource buffers, Font fontRenderer);

    default boolean isDisabled(){
        return false;
    }
}
