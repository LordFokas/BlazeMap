package com.eerussianguy.blazemap.profiling.overlay;

import java.util.function.Supplier;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;

import com.eerussianguy.blazemap.lib.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;

public class Container implements IDrawable {
    public static final float PANEL_WIDTH = 250;
    public static final float PANEL_MIDDLE = 120;

    public final String name;
    public final Style style;

    protected final IDrawable[] children;
    protected int height;
    protected Supplier<String> metric = null;
    protected Supplier<Boolean> enable = () -> true;

    public Container(String name, Style style, IDrawable... children){
        this.name = name;
        this.style = style;
        this.children = children;

        int height = 10 + (style.margin * (style.isRoot ? 2 : 1));
        for(IDrawable element : children){
            if(element.isDisabled()) continue;
            height += element.getHeight();
        }
        this.height = height;
    }

    public Container metric(Supplier<String> metric) {
        this.metric = metric;
        return this;
    }

    public Container enable(Supplier<Boolean> enable) {
        this.enable = enable;
        return this;
    }

    @Override
    public boolean isDisabled() {
        return !enable.get();
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void draw(PoseStack stack, MultiBufferSource buffers, Font fontRenderer) {
        Matrix4f matrix = stack.last().pose();

        if(style.isRoot){
            RenderHelper.fillRect(buffers, matrix, PANEL_WIDTH, getHeight(), 0xA0000000);
        }

        drawHead(matrix, buffers, fontRenderer);

        stack.translate(0, 10 + style.margin, 0);
        for(IDrawable element : children){
            if(element.isDisabled()) continue;
            stack.pushPose();
            element.draw(stack, buffers, fontRenderer);
            stack.popPose();
            stack.translate(0, element.getHeight(), 0);
        }
    }

    protected void drawHead(Matrix4f matrix, MultiBufferSource buffers, Font fontRenderer){
        if(style.isRoot){
            fontRenderer.drawInBatch(name, style.indent, style.margin, style.header, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
            if(metric != null){
                fontRenderer.drawInBatch(metric.get(), PANEL_MIDDLE, style.margin, style.metric, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
            }
        }else{
            if(metric == null){
                fontRenderer.drawInBatch(name, style.indent, style.margin, style.header, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
            }else{
                fontRenderer.drawInBatch(String.format("%s : %s", name, metric.get()), style.indent, style.margin, style.header, false, matrix, buffers, false, 0, LightTexture.FULL_BRIGHT);
            }
        }
    }

    enum Style {
        PANEL(0xFF0000, 0xFFAAAA, 5, 5, true),
        SECTION(0x0088FF, 0x0088FF, 5, 10, false),
        BLOCK(0xCCCCCC, 0xCCCCCC, 15, 5, false);

        public final int header, metric;
        public final int indent, margin;
        public final boolean isRoot;

        Style(int header, int metric, int indent, int margin, boolean isRoot){
            this.header = header;
            this.metric = metric;
            this.indent = indent;
            this.margin = margin;
            this.isRoot = isRoot;
        }
    }
}
