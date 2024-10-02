package com.eerussianguy.blazemap.feature.maps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.markers.Marker;
import com.eerussianguy.blazemap.api.markers.ObjectRenderer;
import com.eerussianguy.blazemap.api.markers.SearchTargeting;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;

public class DefaultObjectRenderer implements ObjectRenderer<Marker<?>> {

    @Override
    public BlazeRegistry.Key<ObjectRenderer<?>> getID() {
        return BlazeMapReferences.ObjectRenderers.DEFAULT;
    }

    @Override
    public void render(Marker<?> marker, PoseStack stack, MultiBufferSource buffers, double zoom, SearchTargeting search) {
        // Set appropriate scale for the current zoom level
        if(!marker.getUsesZoom()) {
            stack.scale(1F / (float) zoom, 1F / (float) zoom, 1);
        }

        // Get common marker properties
        String name = marker.getName();
        int width = marker.getWidth();
        int height = marker.getHeight();
        int color = marker.getColor();

        // Render marker name
        if(marker.isNameVisible() && name != null) {
            float scale = 2;
            Minecraft mc = Minecraft.getInstance();

            stack.pushPose();
            stack.translate(-mc.font.width(name), (10 + (height / scale)), 0);
            stack.scale(scale, scale, 0);
            mc.font.drawInBatch(name, 0, 0, search.color(color), true, stack.last().pose(), buffers, false, 0, LightTexture.FULL_BRIGHT);
            stack.popPose();
        }

        // Render marker icon / texture
        stack.mulPose(Vector3f.ZP.rotationDegrees(marker.getRotation()));
        stack.translate(-width / 2, -height / 2, 0);
        VertexConsumer vertices = buffers.getBuffer(RenderType.text(marker.getIcon()));
        RenderHelper.drawQuad(vertices, stack.last().pose(), (float) width, (float) height, search.color(color));
    }
}
