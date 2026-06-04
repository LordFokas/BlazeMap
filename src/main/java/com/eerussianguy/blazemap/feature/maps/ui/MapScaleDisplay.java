package com.eerussianguy.blazemap.feature.maps.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.engine.render.MapRenderer;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.gui.components.Image;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;

public class MapScaleDisplay extends Image {
    private static final ResourceLocation SCALE = BlazeMap.resource("textures/scale.png");

    private final Font font = Minecraft.getInstance().font;
    private final Window window = Minecraft.getInstance().getWindow();
    private final MapRenderer renderer;
    private final int size;

    public MapScaleDisplay(int size, MapRenderer renderer) {
        super(SCALE, size, size);
        this.renderer = renderer;
        this.size = size;
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        super.render(stack, hasMouse, mouseX, mouseY);

        double z = renderer.getZoom();
        float w = getWidth();
        float h = getHeight() - font.lineHeight;

        String zoom = z > 1 ? String.format("%.0f : 1", z) : String.format("1 : %.0f", 1 / z);
        String distance = String.format("%dm", (int) (size / renderer.getZoom()));

        stack.pushPose();
            font.draw(stack, zoom, (w-font.width(zoom))/2, h/2 - 8, Colors.NO_TINT);
            font.draw(stack, distance, (w-font.width(distance))/2, h/2 + 8, Colors.NO_TINT);
        stack.popPose();
    }

    @Override
    public int getWidth() {
        return (int) Math.ceil(super.getWidth() / window.getGuiScale());
    }

    @Override
    public int getHeight() {
        return (int) Math.ceil(super.getHeight() / window.getGuiScale());
    }
}
