package com.eerussianguy.blazemap.gui.lib;

import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.vertex.PoseStack;

public interface TooltipService {
    void drawTooltip(PoseStack stack, int x, int y, Component... lines);
}
