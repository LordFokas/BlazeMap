package com.eerussianguy.blazemap.gui;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
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

public abstract class BlazeGui extends Screen {
    private static final Component EMPTY = Component.literal("");
    public static final ResourceLocation SLOT = Helpers.identifier("textures/gui/slot.png");
    public static final ResourceLocation GUI = Helpers.identifier("textures/gui/gui.png");

    protected final RenderType background, slot;
    protected final BlazeWidgetFrame widgetFrame;
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

        this.widgetFrame = new BlazeWidgetFrame();
    }

    @Override
    protected void init() {
        super.init();
        this.left = (width - guiWidth) / 2;
        this.top = (height - guiHeight) / 2;

        addRenderableOnly(this.widgetFrame);
    }

    @Override
    public void render(GuiGraphics graphics, int i0, int i1, float f0) {
        PoseStack stack = graphics.pose();

        // Render the dark overlay over the game
        renderBackground(graphics);

        // This will render all the Renderable widgets declared in the init function
        stack.pushPose();
        stack.translate(0, 0, 0.1F);
        super.render(graphics, i0, i1, f0);
        stack.popPose();

        // For things outside the GUI frame such as the editable minimap
        stack.pushPose();
        float scale = (float) getMinecraft().getWindow().getGuiScale();
        float unscale = 1F / scale;

        stack.scale(unscale, unscale, 1);
        stack.translate(0, 0, 0.5F);

        renderAbsolute(graphics, scale);
        stack.popPose();
    }

    protected void renderAbsolute(GuiGraphics graphics, float scale){}

    protected void renderLabel(GuiGraphics graphics, Component text, int x, int y, boolean shadow) {
        PoseStack stack = graphics.pose();
        MultiBufferSource buffers = graphics.bufferSource();
        this.font.drawInBatch(text, x, y, shadow ? Colors.WHITE : Colors.LABEL_COLOR, shadow, stack.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
    }

    /**
     * This is the shared frame that forms the background of all GUI widgets
     */
    protected class BlazeWidgetFrame implements Renderable {

        public void render(GuiGraphics graphics, int i0, int i1, float f0) {
            PoseStack stack = graphics.pose();

            stack.pushPose();
            renderFrame(graphics);
            stack.popPose();

            stack.pushPose();
            stack.translate(left, top, 0.05F);
            if(title != EMPTY) {
                renderLabel(graphics, title, 12, 12, true);
            }
            stack.popPose();
        }

        protected void renderFrame(GuiGraphics graphics) {
            PoseStack stack = graphics.pose();
            MultiBufferSource.BufferSource buffers = graphics.bufferSource();

            stack.pushPose();
            stack.translate(left, top, 0);
            RenderHelper.drawFrame(buffers.getBuffer(background), graphics, guiWidth, guiHeight, 8);
            stack.popPose();
        }
    }
}
