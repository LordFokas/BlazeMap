package com.eerussianguy.blazemap.feature.maps._deprecated;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.Overlay;
import com.eerussianguy.blazemap.feature.maps.IMapHost;
import com.eerussianguy.blazemap.integration.KnownMods;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

public class OverlayButton extends ImageButton {
    private final Key<Overlay> key;
    private final IMapHost host;
    private final Component owner;

    public OverlayButton(int px, int py, int w, int h, Key<Overlay> key, IMapHost host) {
        super(px, py, w, h, 0, 0, 0, key.value().getIcon(), w, h, button -> {
            host.toggleOverlay(key);
        }, key.value().getName());

        this.host = host;
        this.key = key;
        this.owner = new TextComponent(KnownMods.getOwnerName(key)).withStyle(ChatFormatting.BLUE);
    }

    @Override
    public void renderToolTip(PoseStack stack, int x, int y) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        host.drawTooltip(stack, x, y, key.value().getName(), owner);
    }

    @Override
    public void render(PoseStack stack, int mx, int my, float partial) {
        if(host.isOverlayVisible(key))
            RenderHelper.setShaderColor(0xFFFFDD00);
        else
            RenderHelper.setShaderColor(Colors.NO_TINT);

        super.render(stack, mx, my, partial);
        RenderHelper.setShaderColor(Colors.NO_TINT);
    }
}
