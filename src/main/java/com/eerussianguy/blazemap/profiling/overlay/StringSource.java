package com.eerussianguy.blazemap.profiling.overlay;

import java.util.function.Supplier;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;

import com.mojang.blaze3d.vertex.PoseStack;

public class StringSource implements IDrawable {
    private static final int DEFAULT_COLOR = 0xFFFFAA;

    private final Supplier<String> main;
    private final int main_color;
    private Supplier<String> note;
    private int note_color;

    public StringSource(String string) {
        this(() -> string, DEFAULT_COLOR);
    }

    public StringSource(String string, int color) {
        this(() -> string, color);
    }

    public StringSource(Supplier<String> string) {
        this(string, DEFAULT_COLOR);
    }

    public StringSource(Supplier<String> string, int color){
        this.main = string;
        this.main_color = color;
    }

    public StringSource note(String note) {
        return note(() -> note, DEFAULT_COLOR);
    }

    public StringSource note(String note, int color) {
        return note(() -> note, color);
    }

    public StringSource note(Supplier<String> note) {
        return note(note, DEFAULT_COLOR);
    }

    public StringSource note(Supplier<String> note, int color) {
        this.note = note;
        this.note_color = color;
        return this;
    }

    @Override
    public int getHeight() {
        return 10;
    }

    @Override
    public void draw(GuiGraphics graphics, MultiBufferSource buffers, Font fontRenderer) {
        PoseStack stack = graphics.pose();
        fontRenderer.drawInBatch(main.get(), Container.Style.BLOCK.indent, 0, main_color, false, stack.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        if(note != null){
            fontRenderer.drawInBatch(note.get(), Container.PANEL_MIDDLE, 0, note_color, false, stack.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        }
    }
}
