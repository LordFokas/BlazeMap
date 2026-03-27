package com.eerussianguy.blazemap.feature.maps.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.feature.maps.Coordination;
import com.eerussianguy.blazemap.feature.maps.MapConfigSynchronizer;
import com.eerussianguy.blazemap.lib.gui.core.UIEventListener;
import com.eerussianguy.blazemap.lib.gui.core.VolatileContainer;
import com.eerussianguy.blazemap.lib.gui.trait.KeyboardControls;
import com.eerussianguy.blazemap.lib.gui.util.MouseSubpixelSmoother;
import com.mojang.blaze3d.platform.Window;

public class InteractiveMapDisplay extends MapDisplay implements UIEventListener, KeyboardControls {
    private final Window window = Minecraft.getInstance().getWindow();
    private final MouseSubpixelSmoother mouse = new MouseSubpixelSmoother();
    private final Coordination coordination = new Coordination();
    private VolatileContainer volatiles;
    private double zoom;
    int lastMouseX;
    int lastMouseY;

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        super.render(stack, hasMouse, mouseX, mouseY);
    }

    public InteractiveMapDisplay(ResourceLocation mapLocation, double minZoom, double maxZoom) {
        this(0, 0, mapLocation, minZoom, maxZoom);
    }

    public InteractiveMapDisplay(int width, int height, ResourceLocation mapLocation, double minZoom, double maxZoom) {
        super(width, height, mapLocation, minZoom, maxZoom);
        zoom = renderer.getZoom();
    }

    @Override
    public MapDisplay setSynchronizer(MapConfigSynchronizer synchronizer) {
        zoom = renderer.getZoom(); // Needed to get the correct zoom level when initializing
        return super.setSynchronizer(synchronizer);
    }

    public InteractiveMapDisplay setVolatiles(VolatileContainer volatiles) {
        this.volatiles = volatiles;
        return this;
    }

    public Coordination getCoordination() {
        return coordination;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double draggedX, double draggedY) {
        setMouse(mouseX, mouseY);
        if(button == GLFW.GLFW_MOUSE_BUTTON_1) {
            double scale = window.getGuiScale();
            mouse.addMovement(draggedX * scale / zoom, draggedY * scale / zoom);
            renderer.moveCenter(-mouse.movementX(), -mouse.movementY());
            return true;
        }
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        setMouse(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        boolean zoomed;
        if(scroll > 0) {
            zoomed = synchronizer.zoomIn();
        }
        else {
            zoomed = synchronizer.zoomOut();
        }
        zoom = renderer.getZoom();
        setMouse(mouseX, mouseY);
        return zoomed;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        setMouse(mouseX, mouseY);

        if(button == GLFW.GLFW_MOUSE_BUTTON_2) {
            // TODO: create popup into volatiles.
            return true;
        }

        return false;
    }

    private void setMouse(double mouseX, double mouseY) {
        double scale = window.getGuiScale();
        coordination.calculate((int)(mouseX * scale), (int)(mouseY * scale), renderer.getBeginX(), renderer.getBeginZ(), renderer.getZoom());
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        setMouse(lastMouseX, lastMouseY);
        int dx = 0, dz = 0;

        if( isKeyUp    (key) ){ dz -= 16; }
        if( isKeyDown  (key) ){ dz += 16; }
        if( isKeyRight (key) ){ dx += 16; }
        if( isKeyLeft  (key) ){ dx -= 16; }

        if(dx != 0 || dz != 0) {
            renderer.moveCenter((int)(dx / zoom), (int)(dz / zoom));
            return true;
        }
        return false;
    }
}
