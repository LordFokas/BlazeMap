package com.eerussianguy.blazemap.feature.waypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;

import com.eerussianguy.blazemap.api.event.DimensionChangedEvent;
import com.eerussianguy.blazemap.api.markers.IMarkerStorage;
import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.gui.BlazeGui;
import com.eerussianguy.blazemap.gui.SelectionList;
import com.eerussianguy.blazemap.util.Helpers;
import com.eerussianguy.blazemap.util.RenderHelper;

public class WaypointManagerGui extends BlazeGui {
    private static IMarkerStorage<Waypoint> waypointStorage;

    public static void onDimensionChanged(DimensionChangedEvent event) {
        waypointStorage = event.waypoints;
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new WaypointManagerGui());
    }

    private SelectionList<Waypoint> list;
    private Waypoint selected;
    private Button delete, edit;

    protected WaypointManagerGui() {
        super(Helpers.translate("blazemap.gui.waypoint_manager.title"), 190, 224);
    }

    @Override
    protected void init() {
        super.init();

        delete = addRenderableWidget(
            Button.builder(Helpers.translate("blazemap.gui.waypoint_manager.delete"), this::onDelete)
                .pos(left + 12, top + 192)
                .size(80, 20)
                .build());
        edit = addRenderableWidget(
            Button.builder(Helpers.translate("blazemap.gui.waypoint_manager.edit"), this::onEdit)
            .pos(left + 98, top + 192)
            .size(80, 20)
            .build());
        list = addRenderableWidget(new SelectionList<>(left + 12, top + 25, 166, 162, 20, this::renderWaypoint)).setResponder(this::onSelected).setItems(waypointStorage.getAll().stream().toList());
        updateButtons();
    }

    private void renderWaypoint(GuiGraphics graphics, Waypoint waypoint) {
        RenderHelper.drawTexturedQuad(waypoint.getIcon(), waypoint.getColor(), graphics, 2, 2, 16, 16);
        graphics.drawString(font, waypoint.getName(), 20, 6, waypoint.getColor());
    }

    private void onSelected(Waypoint waypoint) {
        this.selected = waypoint;
        updateButtons();
    }

    private void updateButtons() {
        delete.active = edit.active = selected != null;
    }

    private void onDelete(Button b) {
        waypointStorage.remove(selected);
        list.setItems(waypointStorage.getAll().stream().toList());
    }

    private void onEdit(Button b) {
        Waypoint waypoint = list.getSelected();
        onClose();
        WaypointEditorGui.open(waypoint);
    }
}
