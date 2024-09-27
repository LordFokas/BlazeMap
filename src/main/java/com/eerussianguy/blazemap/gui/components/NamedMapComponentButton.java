package com.eerussianguy.blazemap.gui.components;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.sounds.SoundEvents;

import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.api.maps.NamedMapComponent;
import com.eerussianguy.blazemap.api.maps.Overlay;
import com.eerussianguy.blazemap.feature.maps.IMapHost;
import com.eerussianguy.blazemap.gui.lib.BaseComponent;
import com.eerussianguy.blazemap.gui.lib.TooltipService;
import com.eerussianguy.blazemap.integration.KnownMods;
import com.eerussianguy.blazemap.util.Colors;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

public abstract class NamedMapComponentButton<T extends NamedMapComponent<T>> extends ImageButton {
    protected final BlazeRegistry.Key<T> key;
    protected final IMapHost host;
    protected final Component name, owner;
    protected final BooleanSupplier active;

    public NamedMapComponentButton(BlazeRegistry.Key<T> key, IMapHost host, IntConsumer function, BooleanSupplier active) {
        super(key.value().getIcon(), 16, 16, function);
        this.key = key;
        this.host = host;
        this.name = key.value().getName();
        this.owner = new TextComponent(KnownMods.getOwnerName(key)).withStyle(ChatFormatting.BLUE);
        this.active = active;
    }

    @Override
    protected void renderTooltip(PoseStack stack, int mouseX, int mouseY, TooltipService service) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        host.drawTooltip(stack, mouseX, mouseY, name, owner);
    }

    @Override
    protected int getTint() {
        if(!isEnabled()) return Colors.DISABLED;
        return active.getAsBoolean() ? 0xFFFFDD00 : Colors.NO_TINT;
    }

    public static class MapTypeButton extends NamedMapComponentButton<MapType> {
        public MapTypeButton(BlazeRegistry.Key<MapType> key, IMapHost host, List<? extends BaseComponent<?>> others, BaseComponent<?> own) {
            super(key, host, button -> {
                host.setMapType(key.value());
                for(var other : others) {
                    other.setVisible(false);
                }
                own.setVisible(true);
            }, () -> host.getMapType().getID().equals(key));
        }

        @Override
        protected boolean onClick(int button) {
            if(active.getAsBoolean()){
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.8F));
                return true;
            }
            return super.onClick(button);
        }
    }

    public static class LayerButton extends NamedMapComponentButton<Layer> {
        public LayerButton(BlazeRegistry.Key<Layer> key, IMapHost host) {
            super(key, host, button -> host.toggleLayer(key), () -> host.isLayerVisible(key));
        }
    }

    public static class OverlayButton extends NamedMapComponentButton<Overlay> {
        public OverlayButton(BlazeRegistry.Key<Overlay> key, IMapHost host) {
            super(key, host, button -> host.toggleOverlay(key), () -> host.isOverlayVisible(key));
        }
    }
}
