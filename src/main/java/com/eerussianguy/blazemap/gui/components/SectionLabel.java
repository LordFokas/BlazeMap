package com.eerussianguy.blazemap.gui.components;

import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;

public class SectionLabel extends Label {

    public SectionLabel(String text) {
        super(text);
        this.setColor(Colors.DISABLED);
    }

    public SectionLabel(Component text) {
        super(text);
        this.setColor(Colors.DISABLED);
    }

    public SectionLabel(FormattedCharSequence text) {
        super(text);
        this.setColor(Colors.DISABLED);
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        super.render(stack, hasMouse, mouseX, mouseY);
        int text = getTextWidth();

        int width = getWidth();
        if(width + 10 < text) return;

        int skip = text + 3;
        stack.translate(skip, 5, 0);
        RenderHelper.fillRect(stack.last().pose(), width - skip, 1, color);
    }

    public SectionLabel setWidth(int width) {
        setSize(width, getHeight());
        return this;
    }
}
