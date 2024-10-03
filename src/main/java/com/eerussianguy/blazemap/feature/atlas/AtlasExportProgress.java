package com.eerussianguy.blazemap.feature.atlas;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import com.eerussianguy.blazemap.gui.lib.BaseComponent;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;

public class AtlasExportProgress extends BaseComponent<AtlasExportProgress> {
    private final float scale = (float) Minecraft.getInstance().getWindow().getGuiScale() / 2F;
    private final Font font = Minecraft.getInstance().font;

    public AtlasExportProgress() {
        setSize((int)(200 / scale), (int)(30 / scale));
    }

    @Override
    public boolean isVisible() {
        return AtlasExporter.getTask() != null;
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        AtlasTask task = AtlasExporter.getTask();
        if(task == null) return;

        // draw background at regular scale to match size
        RenderHelper.fillRect(stack.last().pose(), getWidth(), getHeight(), Colors.WIDGET_BACKGROUND);

        // scale contents down
        stack.scale(1F / scale, 1F / scale, 1F);

        // Process flashing "animation"
        int textColor = Colors.WHITE;
        long flashUntil = ((long)task.getFlashUntil()) * 1000L;
        long now = System.currentTimeMillis();
        if(task.isErrored() || (flashUntil >= now && now % 333 < 166)) {
            textColor = 0xFFFF0000;
        }

        // Render progress text
        int total = task.getTilesTotal();
        int current = task.getTilesCurrent();
        font.draw(stack, String.format("Exporting  1:%d", task.resolution.pixelWidth), 5, 5, textColor);
        String operation = switch(task.getStage()){
            case QUEUED -> "queued";
            case CALCULATING -> "calculating";
            case STITCHING -> String.format("stitching %d / %d tiles", current, total);
            case SAVING -> "saving";
            case COMPLETE -> "complete";
        };
        font.draw(stack, operation, 195 - font.width(operation), 5, textColor);

        // Render progress bar
        double progress = ((double)current) / ((double)total);
        stack.translate(5, 17, 0);
        RenderHelper.fillRect(stack.last().pose(), 190, 10, Colors.LABEL_COLOR);
        RenderHelper.fillRect(stack.last().pose(), (int)(190*progress), 10, textColor);
    }
}
