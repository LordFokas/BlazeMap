package com.eerussianguy.blazemap.feature.waypoints.ui;

import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.feature.waypoints.WaypointEditorFragment;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointGroup;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;

public class WaypointLeaf extends WaypointTreeNode {
    private static final int ICON_SIZE = 8;

    private final Waypoint waypoint;
    private final WaypointGroup group;

    public WaypointLeaf(Waypoint waypoint, WaypointGroup group, Runnable delete) {
        super(waypoint.getName(), group.getState(waypoint.getID()), delete, "blazemap.gui.button.edit_waypoint");
        this.waypoint = waypoint;
        this.group = group;
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        int offset = (getHeight() - ICON_SIZE) / 2;
        if(hasMouse && mouseIntercepts(mouseX, mouseY)) {
            renderFlatBackground(stack, 0xFF222222); // render hover
        }
        RenderHelper.drawTexturedQuad(waypoint.getIcon(), waypoint.getColor(), stack, offset, offset, ICON_SIZE, ICON_SIZE);
        super.render(stack, hasMouse, mouseX, mouseY);
    }

    @Override
    protected boolean editNode() {
        return new WaypointEditorFragment(waypoint).push();
    }

    @Override
    protected boolean isEditable() {
        return group.management.canEditChild;
    }
}
