package com.eerussianguy.blazemap.__deprecated;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.feature.maps.MapHost;
import com.eerussianguy.blazemap.integration.KnownMods;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

@Deprecated
public class LayerButton extends ImageButton {
    private final Key<Layer> key;
    private final MapType parent;
    private final MapHost host;
    private final Component owner;

    public LayerButton(int px, int py, int w, int h, Key<Layer> key, MapType parent, MapHost host) {
        super(px, py, w, h, 0, 0, 0, key.value().getIcon(), w, h, button -> {
            host.toggleLayer(key);
        }, key.value().getName());
        this.key = key;
        this.parent = parent;
        this.owner = new TextComponent(KnownMods.getOwnerName(key)).withStyle(ChatFormatting.BLUE);
        this.host = host;
        checkVisible();
    }

    @Override
    public void render(PoseStack stack, int mx, int my, float partial) {
        if(host.isLayerVisible(key))
            RenderHelper.setShaderColor(0xFFFFDD00);
        else
            RenderHelper.setShaderColor(Colors.NO_TINT);
        super.render(stack, mx, my, partial);
        RenderHelper.setShaderColor(Colors.NO_TINT);
    }

    @Override
    public void renderToolTip(PoseStack stack, int x, int y) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        host.drawTooltip(stack, x, y, key.value().getName(), owner);
    }

    public void checkVisible() {
        this.visible = host.getMapType() == parent;
    }
}
