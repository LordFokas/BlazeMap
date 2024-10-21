package com.eerussianguy.blazemap.feature.mapping;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

public class TerrainHeightLegend extends BaseComponent<TerrainHeightLegend> {
    private static final int BORDER = 4;
    private static final int GRADIENT_WIDTH = 10;
    private static final int LABEL_STEP = 64;
    private static final float TEXT_SCALE = 0.5F;

    private static NativeImage legend;
    private static RenderType type;
    private static int min;
    private static int max;

    private static RenderType getLegend() {
        if(type == null) {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            min = level.getMinBuildHeight();
            int sea = level.getSeaLevel();
            max = level.getMaxBuildHeight();
            legend = TerrainHeightLayer.getLegend(min, sea, max);
            DynamicTexture texture = new DynamicTexture(legend);
            ResourceLocation path = BlazeMap.resource("dynamic/legend/terrain_height");
            mc.getTextureManager().register(path, texture);
            type = RenderType.text(path);
        }
        return type;
    }

    public TerrainHeightLegend() {
        if(legend == null) getLegend();
        setSize(10 + GRADIENT_WIDTH + BORDER * 2 , legend.getHeight() + BORDER * 2);
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        if(legend == null) getLegend();

        var buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        RenderHelper.fillRect(buffers, stack.last().pose(), getWidth(), getHeight(), Colors.WIDGET_BACKGROUND);

        stack.pushPose();
        stack.translate(16, BORDER, 0);
        RenderHelper.drawQuad(buffers.getBuffer(getLegend()), stack.last().pose(), GRADIENT_WIDTH, legend.getHeight());
        stack.popPose();

        var font = Minecraft.getInstance().font;
        stack.pushPose();
        stack.translate(0, 2, 0);
        stack.scale(TEXT_SCALE, TEXT_SCALE, 1);
        for(int y = max; y >= min; y -= LABEL_STEP) {
            String label = String.valueOf(y);
            stack.pushPose();
            stack.translate(getWidth() - font.width(label), 0, 0);
            font.drawInBatch(label, 0, 0, Colors.WHITE, false, stack.last().pose(), buffers, false, 0, LightTexture.FULL_BRIGHT);
            stack.popPose();
            stack.translate(0, LABEL_STEP * TEXT_SCALE, 0);
        }
        stack.popPose();

        buffers.endBatch();
    }
}
