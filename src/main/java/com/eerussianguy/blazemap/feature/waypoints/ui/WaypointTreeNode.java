package com.eerussianguy.blazemap.feature.waypoints.ui;

import org.lwjgl.glfw.GLFW;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.feature.waypoints.service.LocalState;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.lib.InheritedBoolean;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.eerussianguy.blazemap.lib.gui.components.Label;
import com.eerussianguy.blazemap.lib.gui.components.Tree;
import com.eerussianguy.blazemap.lib.gui.core.ContainerAnchor;
import com.eerussianguy.blazemap.lib.gui.core.EdgeReference;
import com.eerussianguy.blazemap.lib.gui.core.TooltipService;
import com.eerussianguy.blazemap.lib.gui.trait.BorderedComponent;
import com.eerussianguy.blazemap.lib.gui.trait.ComponentSounds;
import com.mojang.blaze3d.vertex.PoseStack;

public abstract class WaypointTreeNode extends Label implements Tree.TreeItem, BorderedComponent, ComponentSounds, GuiEventListener {
    protected static final ResourceLocation ADD = BlazeMap.resource("textures/gui/add.png");
    protected static final ResourceLocation REMOVE = BlazeMap.resource("textures/gui/remove.png");
    protected static final ResourceLocation EDIT = BlazeMap.resource("textures/gui/edit.png");
    private static final ResourceLocation ON_OVERRIDE = BlazeMap.resource("textures/gui/on.png");
    private static final ResourceLocation ON_INHERITED = BlazeMap.resource("textures/gui/on_inherited.png");
    private static final ResourceLocation OFF_OVERRIDE = BlazeMap.resource("textures/gui/off.png");
    private static final ResourceLocation OFF_INHERITED = BlazeMap.resource("textures/gui/off_inherited.png");

    protected final EdgeReference visibility = new EdgeReference(this, ContainerAnchor.TOP_RIGHT).setSize(8, 8).setPosition(2, 2);
    protected final EdgeReference delete = new EdgeReference(this, ContainerAnchor.TOP_RIGHT).setSize(8, 8).setPosition(12, 2);
    protected final EdgeReference edit = new EdgeReference(this, ContainerAnchor.TOP_RIGHT).setSize(8, 8).setPosition(22, 2);
    protected final LocalState state;
    protected final Runnable onDelete;
    protected final String editTooltip;
    protected Runnable updater = () -> {};
    private boolean wasDeleted = false;

    protected WaypointTreeNode(Component text, LocalState state, Runnable delete, String editTooltip) {
        super(text);
        this.state = state;
        this.onDelete = delete;
        this.editTooltip = editTooltip;
    }

    protected WaypointTreeNode(String text, LocalState state, Runnable delete, String editTooltip) {
        super(text);
        this.state = state;
        this.onDelete = delete;
        this.editTooltip = editTooltip;
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        ResourceLocation texture = switch(state.getVisibility()) {
            case TRUE -> ON_OVERRIDE;
            case FALSE -> OFF_OVERRIDE;
            case DEFAULT -> state.isVisible() ? ON_INHERITED : OFF_INHERITED;
        };
        RenderHelper.drawTexturedQuad(texture, Colors.NO_TINT, stack, visibility.getPositionX(), visibility.getPositionY(), visibility.getWidth(), visibility.getHeight());
        if(isDeletable()) {
            RenderHelper.drawTexturedQuad(REMOVE, Screen.hasShiftDown() ? Colors.NO_TINT : Colors.DISABLED, stack, delete.getPositionX(), delete.getPositionY(), delete.getWidth(), delete.getHeight());
        }
        if(isEditable()) {
            RenderHelper.drawTexturedQuad(EDIT, Colors.NO_TINT, stack, edit.getPositionX(), edit.getPositionY(), edit.getWidth(), edit.getHeight());
        }

        stack.translate(getHeight(), getHeight() / 2F - 4, 0);
        super.render(stack, hasMouse, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(PoseStack stack, int mouseX, int mouseY, TooltipService service) {
        if(visibility.mouseIntercepts(mouseX, mouseY)) {
            InheritedBoolean visible = state.getVisibility();
            var tooltip = Helpers.translate(switch(visible) {
                case TRUE -> "blazemap.gui.button.visibility_show";
                case FALSE -> "blazemap.gui.button.visibility_hide";
                case DEFAULT -> "blazemap.gui.button.visibility_default";
            });
            service.drawTooltip(stack, mouseX, mouseY, tooltip);
            return;
        }

        if(isDeletable() && delete.mouseIntercepts(mouseX, mouseY)) {
            var tooltip = Helpers.translate("blazemap.gui.button.delete");
            if(!Screen.hasShiftDown()) {
                service.drawTooltip(stack, mouseX, mouseY, tooltip, Helpers.translate("blazemap.gui.tooltip.confirm_delete").withStyle(ChatFormatting.YELLOW));
            }
            else {
                service.drawTooltip(stack, mouseX, mouseY, tooltip.withStyle(ChatFormatting.RED));
            }
            return;
        }

        if(isEditable() && edit.mouseIntercepts(mouseX, mouseY)) {
            service.drawTooltip(stack, mouseX, mouseY, Helpers.translate(editTooltip));
        }
    }

    protected boolean isDeletable() {
        return true;
    }

    protected boolean isEditable() {
        return true;
    }

    @Override
    public int getColor() {
        return state.isVisible() ? Colors.WHITE : Colors.DISABLED;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(visibility.mouseIntercepts(mouseX, mouseY)) {
            int direction = switch(button) {
                case GLFW.GLFW_MOUSE_BUTTON_1 -> 1;
                case GLFW.GLFW_MOUSE_BUTTON_2 -> -1;
                default -> 0;
            };
            if(direction == 0) {
                playDeniedSound();
            }
            else {
                playOkSound();
                state.setVisibility(Helpers.cycle(state.getVisibility(), direction));
            }
            return true;
        }
        if(isDeletable() && delete.mouseIntercepts(mouseX, mouseY)) {
            if(Screen.hasShiftDown()) {
                playOkSound();
                onDelete.run();
                wasDeleted = true;
                updater.run();
            }
            else {
                playDeniedSound();
            }
            return true;
        }
        if(isEditable() && edit.mouseIntercepts(mouseX, mouseY)) {
            boolean editing = editNode();
            if(editing) {
                playOkSound();
            }
            else {
                playDeniedSound();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean wasDeleted() {
        return wasDeleted;
    }

    protected abstract boolean editNode();

    @Override
    public void setUpdater(Runnable function) {
        this.updater = function;
    }

    @Override
    public int getWidth() {
        return getParent().getWidth();
    }

    @Override
    public int getTextHeight() {
        return 12;
    }
}
