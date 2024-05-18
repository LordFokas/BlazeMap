package com.eerussianguy.blazemap.feature.maps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.annotation.Nullable;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.IClientComponent;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.gui.primitives.GuiPrimitive;
import com.eerussianguy.blazemap.gui.primitives.Label;
import com.eerussianguy.blazemap.gui.primitives.Slot;
import com.eerussianguy.blazemap.util.Helpers;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public abstract class MinimapConfigurator<T extends IClientComponent> extends GuiPrimitive implements ContainerEventHandler, NarratableEntry {
    protected final ResourceKey<Level> dimension;
    protected final IMapHost host;
    protected final Label label;
    protected final Slot configPanel;

    protected GuiEventListener focused;

    protected int BUTTON_SIZE = 18;
    protected int BUTTON_GAP = 2;

    public MinimapConfigurator (Component labelText, ResourceKey<Level> dimension, IMapHost host, int x, int y) {
        super(x, y, 104, 45);

        this.host = host;
        this.dimension = dimension;
        this.label = new Label(labelText, x, y, width(), false);
        this.configPanel = new Slot(x, y + label.height(), width(), height(), 3, this::renderButtons);
    }

    protected ArrayList<ImageButton> generateButtonsList(Collection<Key<T>> maps, @Nullable MapType parent) {
        int buttonX = 0;
        int buttonY = 0;

        ArrayList<ImageButton> buttons = new ArrayList<ImageButton>();

        for (Key<T> mapID : maps) {
            if (mapID.value().shouldRenderInDimension(dimension)) {
                ImageButton button = makeButton(configPanel.x(buttonX), configPanel.y(buttonY), mapID, host, parent);

                // If the button shouldn't be created for some map-specific reason,
                // don't advance the button position counters
                if (button == null) continue;

                buttons.add(button);
                buttonX += BUTTON_SIZE + BUTTON_GAP;

                if (buttonX + BUTTON_SIZE > configPanel.internalWidth()) {
                    buttonX = 0;
                    buttonY += BUTTON_SIZE + BUTTON_GAP;
                }
            }
        }
        return buttons;
    }

    protected abstract ImageButton makeButton(int buttonX, int buttonY, Key<T> mapID, IMapHost host, @Nullable MapType parent);
    protected abstract ArrayList<ImageButton> getButtons();

    protected void renderButtons(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        getButtons().forEach((button) -> button.render(graphics, mouseX, mouseY, partialTick));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        label.render(graphics, mouseX, mouseY, partialTick);
        configPanel.render(graphics, mouseX, mouseY, partialTick);
    }


    // === ContainerEventHandler Methods ===
    @Override
    public ArrayList<? extends GuiEventListener> children() {
        return getButtons();
    }

    @Override
    @Nullable
    public GuiEventListener getFocused() {
        return focused;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        this.focused = focused;
    }

    @Override
    public boolean isDragging() { return false; }

    @Override
    public void setDragging(boolean p_94720_) {}

    // === NarratableEntry Methods ===
    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationOutput) {}


    // =============================================================================================

    /**
     * Configurator for selecting which overall map object to render
     */
    public static class MapTypeConfigurator extends MinimapConfigurator<MapType> {
        private static final Component MAP_TYPES = Helpers.translate("blazemap.gui.minimap_options.map_types");
        protected ArrayList<ImageButton> mapButtons;

        public MapTypeConfigurator (Collection<Key<MapType>> maps, ResourceKey<Level> dimension, IMapHost host, int x, int y) {
            super(MAP_TYPES, dimension, host, x, y);
            this.mapButtons = generateButtonsList(maps, null);
        }

        @Override
        protected ImageButton makeButton(int buttonX, int buttonY, Key<MapType> mapID, IMapHost host, @Nullable MapType parent) {
            return new MapTypeButton(buttonX, buttonY, BUTTON_SIZE, BUTTON_SIZE, mapID, host);
        }

        @Override
        protected ArrayList<ImageButton> getButtons() {
            return this.mapButtons;
        }
    }

    /**
     * Configurator for selecting which layers to render for the selected map type
     */
    public static class LayerConfigurator extends MinimapConfigurator<Layer> {
        private static final Component LAYERS = Helpers.translate("blazemap.gui.minimap_options.layers");
        private final HashMap<MapType, ArrayList<ImageButton>> layerButtonsMap;

        public LayerConfigurator (Collection<Key<MapType>> maps, ResourceKey<Level> dimension, IMapHost host, int x, int y) {
            super(LAYERS, dimension, host, x, y);
            this.layerButtonsMap = new HashMap<MapType, ArrayList<ImageButton>>();

            for (Key<MapType> mapID : maps) {
                layerButtonsMap.put(mapID.value(), generateButtonsList(mapID.value().getLayers(), mapID.value()));
            }

        }

        @Override
        protected ImageButton makeButton(int buttonX, int buttonY, Key<Layer> layerID, IMapHost host, MapType parent) {
            if (layerID.value().isOpaque()) {
                return null;
            } else {
                LayerButton layerButton = new LayerButton(buttonX, buttonY, BUTTON_SIZE, BUTTON_SIZE, layerID, parent, host);
                // Visibility will be controlled by the configurator so each button needn't worry about it
                layerButton.forceVisible();
                return layerButton;
            }
        }

        @Override
        protected ArrayList<ImageButton> getButtons() {
            MapType currentMap = host.getMapType();
            return layerButtonsMap.get(currentMap);
        }

    }
}
