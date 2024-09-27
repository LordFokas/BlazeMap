package com.eerussianguy.blazemap.gui.components;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.gui.lib.BaseComponent;
import com.eerussianguy.blazemap.gui.lib.TooltipService;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;

public class Image extends BaseComponent<Image> {
    private final ResourceLocation image;
    private int color = Colors.NO_TINT;
    private Component tooltip;

    public Image(ResourceLocation image, int width, int height) {
        this.setSize(width, height);
        this.image = image;
    }

    public Image color(int color) {
        this.color = color;
        return this;
    }

    public Image tooltip(Component tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    @Override
    protected void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        RenderHelper.drawTexturedQuad(image, color, stack, 0, 0, getWidth(), getHeight());
    }

    @Override
    protected void renderTooltip(PoseStack stack, int mouseX, int mouseY, TooltipService service) {
        if(tooltip != null) {
            service.drawTooltip(stack, mouseX, mouseY, tooltip);
        }
    }
}
