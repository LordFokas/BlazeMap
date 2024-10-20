package com.eerussianguy.blazemap.lib.gui.core;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.client.gui.components.events.GuiEventListener;

import com.mojang.blaze3d.vertex.PoseStack;

public class MetaContainer extends BaseContainer<MetaContainer> {
    private final ArrayList<AbsoluteContainer> containers = new ArrayList<>();
    private final ArrayList<AbsoluteContainer> renderables = new ArrayList<>();

    public MetaContainer(int width, int height) {
        this.setSize(width, height);
    }

    public void add(AbsoluteContainer ... children) {
        for(var child : children) add(child);
    }

    public void add(AbsoluteContainer child) {
        child.setSize(getWidth(), getHeight()).withParent(this);
        containers.add(0, child);
        renderables.add(child);
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        stack.pushPose();
        for(var layer : renderables) {
            stack.translate(0, 0, 25);
            stack.pushPose();
            layer.render(stack, hasMouse, mouseX, mouseY);
            stack.popPose();
        }
        stack.pushPose();
    }

    @Override
    protected boolean passthrough(double x, double y, Predicate<GuiEventListener> function, boolean fallback) {
        if(!isVisible()) return fallback;

        for(var container : containers) {
            if(function.test(container)) return true;
        }

        return fallback;
    }

    @Override
    public Optional<BaseComponent<?>> getComponentAt(double x, double y, ReferenceFrame reference) {
        for(var container : containers) {
            var component = container.getComponentAt(x, y, reference);
            if(component.isPresent()) {
                return component;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<GuiEventListener> getLeafListenerAt(double x, double y) {
        for(var container : containers) {
            var listener = container.getLeafListenerAt(x, y);
            if(listener.isPresent()) {
                return listener;
            }
        }
        return Optional.empty();
    }

    @Override
    public MetaContainer setSize(int w, int h) {
        for(var container : containers) {
            container.setSize(w, h);
        }
        return super.setSize(w, h);
    }
}