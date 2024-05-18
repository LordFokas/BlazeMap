package com.eerussianguy.blazemap.feature.waypoints;

import com.eerussianguy.blazemap.gui.primitives.GuiPrimitive;
import com.eerussianguy.blazemap.gui.primitives.Image;
import com.eerussianguy.blazemap.gui.primitives.Slot;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class WaypointIconPreview extends GuiPrimitive {
    private final Image waypointIcon;
    private final Slot slot;
    private final int IMAGE_SIZE = 32;

    public WaypointIconPreview (ResourceLocation texture, int x, int y, int width, int height, int color) {
        super(x, y, width, height);

        int padding = (width - IMAGE_SIZE) / 2;

        slot = new Slot(x, y, width, height, padding, this::renderSlotContents);
        waypointIcon = new Image(texture, slot.x(), slot.y(), IMAGE_SIZE, IMAGE_SIZE, color);
    }

    public void setColor(int color) {
        waypointIcon.setColor(color);
    }

    @Override
    public void setTexture(ResourceLocation texture) {
        waypointIcon.setTexture(texture);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        slot.render(graphics, mouseX, mouseY, partialTick);
    }

    public void renderSlotContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        waypointIcon.render(graphics, mouseX, mouseY, partialTick);
    }
}
