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
    private int size, grain, track;
    private int begin, end;
    private Tab active = null;

    public IconTabs() {
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        //render bottom line
        stack.pushPose();
        stack.translate(-begin, getHeight() - 1, 0);
        RenderHelper.fillRect(stack.last().pose(), getWidth() + begin + end, 1, Colors.UNFOCUSED);
        stack.popPose();

        // render tabs
        for(var tab : tabs) {
            stack.pushPose();
            stack.translate(tab.getPositionX(), tab.getPositionY(), 0);
            var item = tab.component;

            if(tab == active) { // render active tab
                renderBorderedBox(stack, -1, -1, tab.getWidth()+2, tab.getHeight()+2, Colors.UNFOCUSED, Colors.BLACK);
                stack.pushPose();
                stack.translate(0, tab.getHeight(), 0);
                RenderHelper.fillRect(stack.last().pose(), tab.getWidth(), 1, 0xFF303030);
                stack.popPose();
                Minecraft.getInstance().font.draw(stack, item.getName(), size + 2, size / 2f - 4, Colors.WHITE);
            }
            else { // render inactive tabs
                renderBorderedBox(stack, -1, 0, tab.getWidth()+2, tab.getHeight()+1, Colors.UNFOCUSED, Colors.BLACK);

                // hover only for inactive tabs
                if(hasMouse && tab.mouseIntercepts(mouseX, mouseY)) {
                    stack.pushPose();
                    stack.translate(0, 1, 0);
                    RenderHelper.fillRect(stack.last().pose(), tab.getWidth(), tab.getHeight()-1, 0xFF222222); // render hover
                    stack.popPose();
                }
            }

            // render icon
            RenderHelper.drawTexturedQuad(item.getIcon(), item.getIconTint(), stack, 1, 1, size-2, size-2);

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
        size = h - 2;
        grain = h - 1;
        track = w - 2;
        recalculate();
        return this;
    }

    private void recalculate() {
        if(tabs.size() == 0) return;
        int open = track - (tabs.size() - 1) * grain;
        int x = 1;
        for(var tab : tabs) {
            tab.setSize(tab == active ? open : size, size);
            tab.setPosition(x, 1);
            x += tab.getWidth()+1;
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
