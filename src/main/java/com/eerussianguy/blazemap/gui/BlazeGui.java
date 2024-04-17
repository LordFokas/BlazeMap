package com.eerussianguy.blazemap.gui;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.Helpers;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

public abstract class BlazeGui extends Screen {
    private static final Component EMPTY = Component.literal("");
    public static final ResourceLocation SLOT = Helpers.identifier("textures/gui/slot.png");
    public static final ResourceLocation GUI = Helpers.identifier("textures/gui/gui.png");

    protected final RenderType background, slot;
    protected int guiWidth, guiHeight;
    protected int left, top;

    protected BlazeGui(int guiWidth, int guiHeight) {
        this(EMPTY, guiWidth, guiHeight);
    }

    protected BlazeGui(@Nonnull Component title, int guiWidth, int guiHeight) {
        super(title);
        this.minecraft = Minecraft.getInstance();
        this.guiWidth = guiWidth;
        this.guiHeight = guiHeight;
        this.background = RenderType.text(GUI);
        this.slot = RenderType.text(SLOT);
    }

    @Override
    protected void init() {
        super.init();
        this.left = (width - guiWidth) / 2;
        this.top = (height - guiHeight) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int i0, int i1, float f0) {
        PoseStack stack = graphics.pose();
        
        renderBackground(graphics);

        var buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        renderFrame(graphics, buffers);

        stack.pushPose();
        stack.translate(left, top, 0.05F);
        if(title != EMPTY) {
            renderLabel(graphics, buffers, title, 12, 12, true);
        }
        renderComponents(graphics, buffers);
        stack.popPose();
        buffers.endBatch();

        stack.pushPose();
        stack.translate(0, 0, 0.1F);
        super.render(graphics, i0, i1, f0);
        stack.popPose();

        stack.pushPose();
        float scale = (float) getMinecraft().getWindow().getGuiScale();
        float unscale = 1F / scale;
        stack.scale(unscale, unscale, 1);
        stack.translate(0, 0, 0.5F);
        buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        renderAbsolute(graphics, buffers, scale);
        buffers.endBatch();
        stack.popPose();
    }

    protected void renderAbsolute(GuiGraphics graphics, MultiBufferSource buffers, float scale){}

    protected void renderFrame(GuiGraphics graphics, MultiBufferSource buffers) {
        PoseStack stack = graphics.pose();

        stack.pushPose();
        stack.translate(left, top, 0);
        RenderHelper.drawFrame(buffers.getBuffer(background), graphics, guiWidth, guiHeight, 8);
        stack.popPose();
    }

    protected abstract void renderComponents(GuiGraphics graphics, MultiBufferSource buffers);

    protected void renderLabel(GuiGraphics graphics, MultiBufferSource buffers, Component text, int x, int y, boolean shadow) {
        PoseStack stack = graphics.pose();
        this.font.drawInBatch(text, x, y, shadow ? Colors.WHITE : Colors.LABEL_COLOR, shadow, stack.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
    }

    protected void renderSlot(GuiGraphics graphics, MultiBufferSource buffers, int x, int y, int width, int height) {
        PoseStack stack = graphics.pose();

        stack.pushPose();
        stack.translate(x, y, 0);
        RenderHelper.drawFrame(buffers.getBuffer(slot), graphics, width, height, 1);
        stack.popPose();
    }
}
