package com.eerussianguy.blazemap.feature.maps;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.RenderHelper;

public class MapTypeButton extends ImageButton {
    private final Key<MapType> key;
    private final IMapHost host;

    public MapTypeButton(int px, int py, int w, int h, Key<MapType> key, IMapHost host) {
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

        setTooltip(Tooltip.create(key.value().getName()));
    }

    @Override
    public void render(GuiGraphics graphics, int mx, int my, float partial) {
        if(key.equals(host.getMapType().getID()))
            RenderHelper.setShaderColor(0xFFFFDD00);
        else
            RenderHelper.setShaderColor(Colors.NO_TINT);

        super.render(graphics, mx, my, partial);
        RenderHelper.setShaderColor(Colors.NO_TINT);
    }
}
