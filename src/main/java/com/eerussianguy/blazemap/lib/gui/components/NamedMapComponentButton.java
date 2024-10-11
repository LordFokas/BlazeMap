package com.eerussianguy.blazemap.lib.gui.components;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.api.maps.NamedMapComponent;
import com.eerussianguy.blazemap.api.maps.Overlay;
import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.config.ServerConfig;
import com.eerussianguy.blazemap.feature.maps.MapHost;
import com.eerussianguy.blazemap.integration.KnownMods;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.eerussianguy.blazemap.lib.gui.core.TooltipService;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

public abstract class NamedMapComponentButton<T extends NamedMapComponent<T>> extends ImageButton {
    protected final BlazeRegistry.Key<T> key;
    protected final MapHost host;
    protected final Component name, disabled, owner;
    protected final BooleanSupplier active;
    protected final ServerConfig.NamedMapComponentPermissions<T> permissions;
    protected final ArrayList<Component> tooltip = new ArrayList<>();

    public NamedMapComponentButton(BlazeRegistry.Key<T> key, String type, MapHost host, IntConsumer function, BooleanSupplier active, ServerConfig.NamedMapComponentPermissions<T> permissions) {
        super(key.value().getIcon(), 16, 16, function);
        this.key = key;
        this.host = host;
        this.name = key.value().getName().plainCopy();
        this.disabled = Helpers.translate("blazemap.gui.common."+type+".disabled").withStyle(ChatFormatting.DARK_GRAY);
        this.owner = new TextComponent(KnownMods.getOwnerName(key)).withStyle(ChatFormatting.BLUE);
        this.active = active;
        this.permissions = permissions;
    }

    @Override
    public boolean isEnabled() {
        return permissions.isAllowed(key);
    }

    @Override
    protected void renderTooltip(PoseStack stack, int mouseX, int mouseY, TooltipService service) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        tooltip.clear();
        populateTooltip();
        tooltip.add(0, name);
        tooltip.add(owner);
        host.drawTooltip(stack, mouseX, mouseY, tooltip);
    }

    protected void populateTooltip() {
        if(!permissions.isAllowed(key)) {
            tooltip.add(disabled);
        }
    }

    @Override
    protected int getTint() {
        if(!isEnabled()) return Colors.DISABLED;
        return active.getAsBoolean() ? 0xFFFFDD00 : Colors.NO_TINT;
    }

    public static class MapTypeButton extends NamedMapComponentButton<MapType> {
        public MapTypeButton(BlazeRegistry.Key<MapType> key, MapHost host, List<? extends BaseComponent<?>> others, BaseComponent<?> own) {
            super(key, "map", host, button -> {
                host.setMapType(key.value());
                for(var other : others) {
                    other.setVisible(false);
                }
                own.setVisible(true);
            }, () -> host.getMapType().getID().equals(key), BlazeMapConfig.SERVER.mapPermissions);
        }

        @Override
        protected boolean onClick(int button) {
            if(active.getAsBoolean()){
                playDeniedSound();
                return true;
            }
            return super.onClick(button);
        }
    }

    public static class LayerButton extends NamedMapComponentButton<Layer> {
        private final Component bottom = Helpers.translate("blazemap.gui.common.layer.bottom").withStyle(ChatFormatting.DARK_GRAY);
        private final boolean isBottom;

        public LayerButton(BlazeRegistry.Key<Layer> key, MapHost host) {
            super(key, "layer", host, button -> host.toggleLayer(key), () -> host.isLayerVisible(key), BlazeMapConfig.SERVER.layerPermissions);
            isBottom = key.value().isBottomLayer();
        }

        @Override
        protected void populateTooltip() {
            super.populateTooltip();
            if(isBottom) tooltip.add(bottom);
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled() && !isBottom;
        }
    }

    public static class OverlayButton extends NamedMapComponentButton<Overlay> {
        public OverlayButton(BlazeRegistry.Key<Overlay> key, MapHost host) {
            super(key, "overlay", host, button -> host.toggleOverlay(key), () -> host.isOverlayVisible(key), BlazeMapConfig.SERVER.overlayPermissions);
        }
    }
}
