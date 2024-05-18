package com.eerussianguy.blazemap.gui.primitives;

import com.eerussianguy.blazemap.util.Colors;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;

public class Label extends GuiPrimitive {
    private static final int LABEL_HEIGHT = 11;
    private static Font font = Minecraft.getInstance().font;
    private Component text;
    private boolean shadow;

    public Label(Component text, int x, int y, int width, boolean shadow) {
        super(x, y, width, LABEL_HEIGHT);
        this.text = text;
        this.shadow = shadow;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PoseStack stack = graphics.pose();
        MultiBufferSource buffers = graphics.bufferSource();

        font.drawInBatch(text, x(), y(), shadow ? Colors.WHITE : Colors.LABEL_COLOR, shadow, stack.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
    }
}
