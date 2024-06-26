package com.eerussianguy.blazemap.feature.maps;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.markers.MapLabel;
import com.eerussianguy.blazemap.api.markers.ObjectRenderer;
import com.eerussianguy.blazemap.api.markers.SearchTargeting;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

public class DefaultObjectRenderer implements ObjectRenderer<MapLabel> {

    @Override
    public BlazeRegistry.Key<ObjectRenderer<?>> getID() {
        return BlazeMapReferences.ObjectRenderers.DEFAULT;
    }

    @Override
    public void render(MapLabel label, GuiGraphics graphics, double zoom, SearchTargeting search) {
        PoseStack stack = graphics.pose();
        MultiBufferSource buffers = graphics.bufferSource();

        if(!label.getUsesZoom()) {
            stack.scale(1F / (float) zoom, 1F / (float) zoom, 1);
        }

        stack.mulPose(Axis.ZP.rotationDegrees(label.getRotation()));
        int width = label.getWidth();
        int height = label.getHeight();
        stack.translate(-width / 2, -height / 2, 0);

        VertexConsumer vertices = buffers.getBuffer(RenderType.text(label.getIcon()));
        RenderHelper.drawQuad(vertices, stack.last().pose(), (float) width, (float) height, search.color(label.getColor()));
    }
}
