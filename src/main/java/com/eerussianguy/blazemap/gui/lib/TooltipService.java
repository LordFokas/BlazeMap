package com.eerussianguy.blazemap.gui.lib;

import java.util.Arrays;
import java.util.List;

import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.vertex.PoseStack;

public interface TooltipService {
    void drawTooltip(PoseStack stack, int x, int y, List<? extends Component> lines);

    default void drawTooltip(PoseStack stack, int x, int y, Component... lines) {
        drawTooltip(stack, x, y, Arrays.stream(lines).toList());
    }
}
