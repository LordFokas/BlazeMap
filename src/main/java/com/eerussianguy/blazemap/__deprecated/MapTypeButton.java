package com.eerussianguy.blazemap.__deprecated;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.feature.maps.MapHost;
import com.eerussianguy.blazemap.integration.KnownMods;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

@Deprecated
public class MapTypeButton extends ImageButton {
    private final Key<MapType> key;
    private final MapHost host;
    private final Component owner;

    public MapTypeButton(int px, int py, int w, int h, Key<MapType> key, MapHost host) {
        super(px, py, w, h, 0, 0, 0, key.value().getIcon(), w, h, button -> {
            host.setMapType(key.value());
            for(GuiEventListener widget : host.getChildren()) {
                if(widget instanceof LayerButton lb) {
                    lb.checkVisible();
                }
            }
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
        if(key.equals(host.getMapType().getID()))
            RenderHelper.setShaderColor(0xFFFFDD00);
        else
            RenderHelper.setShaderColor(Colors.NO_TINT);

        super.render(stack, mx, my, partial);
        RenderHelper.setShaderColor(Colors.NO_TINT);
    }
}
