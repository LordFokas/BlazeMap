package com.eerussianguy.blazemap.lib.gui.components;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.eerussianguy.blazemap.lib.gui.core.Positionable;
import com.eerussianguy.blazemap.lib.gui.core.TooltipService;
import com.eerussianguy.blazemap.lib.gui.trait.BorderedComponent;
import com.eerussianguy.blazemap.lib.gui.trait.ComponentSounds;
import com.mojang.blaze3d.vertex.PoseStack;

public class IconTabs extends BaseComponent<IconTabs> implements BorderedComponent, ComponentSounds, GuiEventListener {
    private final ArrayList<Tab> tabs = new ArrayList<>();
    private final int spacing, offset;
    private int size, grain, track;
    private int begin, end;
    private Tab active = null;

    public IconTabs(int spacing) {
        this.spacing = spacing;
        this.offset = spacing + 1;
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        // render background
        renderBorderedBackground(stack);

        // render tabs
        for(var tab : tabs) {
            stack.pushPose();
            stack.translate(tab.getPositionX(), tab.getPositionY(), 0);
            var item = tab.component;

            // render active tab & hover extras
            if(tab == active) {
                Minecraft.getInstance().font.draw(stack, item.getName(), size + 2, size / 2f - 4, Colors.WHITE);
            }
            else if(hasMouse && tab.mouseIntercepts(mouseX, mouseY)) {
                RenderHelper.fillRect(stack.last().pose(), tab.getWidth(), tab.getHeight(), 0xFF222222); // render hover
            }

            // render icon
            RenderHelper.drawTexturedQuad(item.getIcon(), item.getIconTint(), stack, 0, 0, size, size);

            stack.popPose();
        }
    }

    @Override
    protected void renderTooltip(PoseStack stack, int mouseX, int mouseY, TooltipService service) {
        for(var tab : tabs) {
            if(tab.mouseIntercepts(mouseX, mouseY)) {
                service.drawTooltip(stack, mouseX, mouseY, tab.component.getTooltip());
                return;
            }
        }
    }

    @Override
    public IconTabs setSize(int w, int h) {
        super.setSize(w, h);
        size = h - offset * 2;
        grain = size + spacing;
        track = w - offset * 2;
        recalculate();
        return this;
    }

    private void recalculate() {
        if(tabs.size() == 0) return;
        int open = track - (tabs.size() - 1) * grain;
        int x = offset;
        for(var tab : tabs) {
            tab.setSize(tab == active ? open : size, size);
            tab.setPosition(x, offset);
            x += tab.getWidth() + spacing;
        }
    }

    public IconTabs setLine(int begin, int end) {
        this.begin = begin;
        this.end = end;
        return this;
    }

    public void add(TabComponent component) {
        Tab tab = new Tab(component);
        tabs.add(tab);
        if(active == null) {
            active = tab;
        }
        component.setVisible(active == tab);
        recalculate();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for(var tab : tabs) {
            if(tab.mouseIntercepts(mouseX, mouseY)) {
                if(tab == active) {
                    playDeniedSound();
                }
                else {
                    setActive(tab);
                    playOkSound();
                    recalculate();
                }
                break;
            }
        }
        return true;
    }

    private void setActive(Tab tab) {
        if(active != null) active.component.setVisible(false);
        tab.component.setVisible(true);
        active = tab;
    }

    public interface TabComponent {
        void setVisible(boolean visible);
        ResourceLocation getIcon();
        int getIconTint();
        Component getName();
        List<Component> getTooltip();
    }

    public static class Tab extends Positionable<Tab> {
        private final TabComponent component;

        public Tab(TabComponent component) {
            this.component = component;
        }
    }
}
