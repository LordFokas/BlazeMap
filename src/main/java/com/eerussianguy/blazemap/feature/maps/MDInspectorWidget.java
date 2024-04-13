package com.eerussianguy.blazemap.feature.maps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import com.eerussianguy.blazemap.api.debug.MDInspectionController;
import com.eerussianguy.blazemap.api.pipeline.MasterDatum;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;

public class MDInspectorWidget<MD extends MasterDatum> implements Widget, GuiEventListener, NarratableEntry {
    private static final int WIDGET_SCALE = 2;

    protected final MD datum;
    protected final ChunkPos chunkPos;
    protected final int sizeX, sizeY;
    protected int posX, posY;
    protected final MDInspectionController<MD> controller;
    protected final Font font;
    protected Runnable dismisser;

    public MDInspectorWidget(MD datum, ChunkPos pos) {
        this.datum = datum;
        this.chunkPos = pos;
        controller = (MDInspectionController<MD>) datum.getID().value().getInspectionController();
        if(controller == null) {
            this.sizeY = 35;
        } else {
            this.sizeY = 15 + (controller.getNumLines(datum) * 10) + (controller.getNumGrids(datum) * 270);
        }
        this.sizeX = 260;
        this.font = Minecraft.getInstance().font;
        Window window = Minecraft.getInstance().getWindow();
        this.posX = (window.getWidth() - sizeX * WIDGET_SCALE) / 2;
        this.posY = (window.getHeight() - sizeY * WIDGET_SCALE) / 2;
    }

    public void setDismisser(Runnable runnable) {
        this.dismisser = runnable;
    }

    @Override
    public void render(PoseStack stack, int i0, int i1, float f0) {
        stack.pushPose();
        Window window = Minecraft.getInstance().getWindow();
        float scale = 1F / (float) window.getGuiScale();
        stack.scale(scale, scale, 1);

        stack.translate(posX, posY, 2);
        stack.scale(WIDGET_SCALE, WIDGET_SCALE, 1);
        stack.translate(-sizeX / 2, -sizeY / 2, 0);

        RenderHelper.fillRect(stack.last().pose(), sizeX, sizeY, Colors.WIDGET_BACKGROUND);
        renderTitleBar(stack);
        stack.translate(0, 15, 0);
        if(controller == null) {
            font.draw(stack, "No MDInspectionController found!", 7, 7, 0xFFFFAAAA);
        } else {
            renderMD(stack);
        }

        stack.popPose();
    }

    private void renderTitleBar(PoseStack stack) {
        stack.pushPose();
        RenderHelper.fillRect(stack.last().pose(), sizeX, 13, Colors.WIDGET_BACKGROUND);
        font.draw(stack, String.format("%s [%d, %d]", datum.getID().location, chunkPos.x, chunkPos.z), 2, 4, Colors.WHITE);
        stack.translate(sizeX - 13, 0, 0);
        RenderHelper.fillRect(stack.last().pose(), 13, 13, 0xFFFF0000);
        stack.popPose();
    }

    private void renderMD(PoseStack stack) {
        for(int line = 0; line < controller.getNumLines(datum); line++){
            String string = controller.getLine(datum, line);
            font.draw(stack, string, 2, 0, Colors.WHITE);
            stack.translate(0, 10, 0);
        }
        stack.translate(2, 0, 0);
        for(int grid = 0; grid < controller.getNumGrids(datum); grid++) {
            stack.translate(0, 12, 0);
            font.draw(stack, controller.getGridName(datum, grid), 0, -10, Colors.WHITE);
            for(int z = 0; z < 16; z++) {
                stack.pushPose();
                for(int x = 0; x < 16; x++) {
                    ResourceLocation icon = controller.getIcon(datum, grid, x, z);
                    int tint = controller.getTint(datum, grid, x, z);
                    if(icon == null) {
                        RenderHelper.fillRect(stack.last().pose(), 16, 16, tint);
                    } else {
                        RenderHelper.drawTexturedQuad(icon, tint, stack, 0, 0, 16, 16);
                    }
                    stack.translate(16, 0, 0);
                }
                stack.popPose();
                stack.translate(0, 16, 0);
            }
            stack.translate(0, 2, 0);
        }
    }

    public void dismiss() {
        if(this.dismisser != null) {
            this.dismisser.run();
        }
    }

    double mx, my;
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        mx = mouseX;
        my = mouseY;
        return false;
    }

    @Override public NarrationPriority narrationPriority() { return NarrationPriority.NONE; }
    @Override public void updateNarration(NarrationElementOutput output) {}

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dX, double dY) {
        this.posX += dX;
        this.posY += dY;
        return GuiEventListener.super.mouseDragged(mouseX, mouseY, button, dX, dY);
    }
}
