package com.eerussianguy.blazemap.feature.maps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.gui.lib.BaseComponent;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.Helpers;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

public class WorldMapHotkey extends BaseComponent<WorldMapHotkey> {
    public static final ResourceLocation KEY = Helpers.identifier("textures/gui/key.png");
    private static final int KEY_WIDTH = 40;
    private static final int KEY_SPACE = 8;
    private static final int PADDING = 2;

    private final Component hotkey, description;
    private final Font font = Minecraft.getInstance().font;
    private final double scale = Minecraft.getInstance().getWindow().getGuiScale() / 2D;

    public WorldMapHotkey(Component hotkey, Component description) {
        this.hotkey = hotkey;
        this.description = description;

        setSize((int) ((KEY_WIDTH + KEY_SPACE + font.width(description)) / scale), (int) ((font.lineHeight + PADDING * 2) / scale));
    }

    public WorldMapHotkey(String hotkey, String description) {
        this(new TextComponent(hotkey), new TextComponent(description));
    }

    @Override
    protected void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        stack.scale(1F / (float) scale, 1F / (float) scale, 1);
        var buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        RenderHelper.drawFrame(buffers.getBuffer(RenderType.text(KEY)), stack, KEY_WIDTH, font.lineHeight + PADDING * 2, 4);
        buffers.endBatch();

        font.draw(stack, hotkey, (KEY_WIDTH - font.width(hotkey)) / 2, PADDING, Colors.NO_TINT);
        font.draw(stack, description,KEY_WIDTH + KEY_SPACE, PADDING, Colors.NO_TINT);
    }
}
