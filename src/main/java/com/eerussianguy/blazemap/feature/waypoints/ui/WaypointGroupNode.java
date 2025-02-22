package com.eerussianguy.blazemap.feature.waypoints.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.feature.waypoints.WaypointEditorFragment;
import com.eerussianguy.blazemap.feature.waypoints.WaypointGroupEditorFragment;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointGroup;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.eerussianguy.blazemap.lib.gui.components.Tree;
import com.eerussianguy.blazemap.lib.gui.core.ContainerAnchor;
import com.eerussianguy.blazemap.lib.gui.core.EdgeReference;
import com.eerussianguy.blazemap.lib.gui.core.TooltipService;
import com.mojang.blaze3d.vertex.PoseStack;

public class WaypointGroupNode extends WaypointTreeNode {
    private final WaypointGroup group;
    private final ArrayList<? extends Tree.TreeItem> children;
    private final EdgeReference add = new EdgeReference(this, ContainerAnchor.TOP_RIGHT).setSize(8, 8).setPosition(32, 2);
    private boolean open = true;

    public WaypointGroupNode(WaypointGroup group, Runnable delete) {
        super(group.getName(), group.getState(), delete, "blazemap.gui.button.edit_group");
        this.group = group;
        this.children = new ArrayList<>(group.getAll().stream().map(this::makeChild).toList());
    }

    private WaypointLeaf makeChild(Waypoint waypoint) {
        ResourceLocation id = waypoint.getID();
        return new WaypointLeaf(
            waypoint,
            group,
            () -> {
                group.remove(id);
            }
        );
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        renderFlatBackground(stack, 0xFF444444);
        if(group.management.canCreateChild) {
            RenderHelper.drawTexturedQuad(ADD, Colors.NO_TINT, stack, add.getPositionX(), add.getPositionY(), add.getWidth(), add.getHeight());
        }
        stack.pushPose();
        stack.translate(getHeight(), getHeight() / 2F - 4, 0);
        font.draw(stack, open ? "v" : ">", -9, 1, Colors.BLACK);
        stack.popPose();
        super.render(stack, hasMouse, mouseX, mouseY);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Tree.TreeItem> getChildren() {
        if(open) {
            children.removeIf(Tree.TreeItem::wasDeleted);
            return (List<Tree.TreeItem>) children;
        }
        else {
            return Collections.EMPTY_LIST;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(super.mouseClicked(mouseX, mouseY, button)) return true;

        if(group.management.canCreateChild && add.mouseIntercepts(mouseX, mouseY)) {
            boolean opened = new WaypointEditorFragment(Helpers.getPlayer().blockPosition(), group).push(updater);
            if(opened) {
                playOkSound();
            }
            else {
                playDeniedSound();
            }
            return true;
        }

        this.open = !this.open;
        updater.run();

        if(open) playUpSound();
        else playDownSound();

        return true;
    }

    @Override
    protected boolean isDeletable() {
        return group.management.canDelete;
    }

    @Override
    protected boolean isEditable() {
        return group.management.canEdit;
    }

    @Override
    protected boolean editNode() {
        return new WaypointGroupEditorFragment(group).push();
    }

    @Override
    protected void renderTooltip(PoseStack stack, int mouseX, int mouseY, TooltipService service) {
        if(group.management.canCreateChild && add.mouseIntercepts(mouseX, mouseY)) {
            service.drawTooltip(stack, mouseX, mouseY, Helpers.translate("blazemap.gui.button.add_waypoint"));
            return;
        }
        super.renderTooltip(stack, mouseX, mouseY, service);
    }
}
