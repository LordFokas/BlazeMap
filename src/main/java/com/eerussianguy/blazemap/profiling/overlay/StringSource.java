package com.eerussianguy.blazemap.profiling.overlay;

import java.util.function.Supplier;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;

import com.mojang.blaze3d.vertex.PoseStack;

public class StringSource implements IDrawable {
    private final int color;
    private final Supplier<String> string;

    public StringSource(Supplier<String> string){
        this(string, 0xFFFFAA);
    }

    public StringSource(Supplier<String> string, int color){
        this.string = string;
        this.color = color;
    }

    @Override
    public int getHeight() {
        return 10;
    }

    @Override
    public void draw(PoseStack stack, MultiBufferSource buffers, Font fontRenderer) {
        fontRenderer.drawInBatch(string.get(), Container.Style.BLOCK.indent, 0, color, false, stack.last().pose(), buffers, false, 0, LightTexture.FULL_BRIGHT);
    }
}
