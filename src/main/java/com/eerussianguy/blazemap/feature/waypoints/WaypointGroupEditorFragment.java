package com.eerussianguy.blazemap.feature.waypoints;

import com.eerussianguy.blazemap.feature.waypoints.service.WaypointGroup;
import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.lib.ObjHolder;
import com.eerussianguy.blazemap.lib.gui.components.Label;
import com.eerussianguy.blazemap.lib.gui.components.TextButton;
import com.eerussianguy.blazemap.lib.gui.components.VanillaComponents;
import com.eerussianguy.blazemap.lib.gui.core.VolatileContainer;
import com.eerussianguy.blazemap.lib.gui.fragment.BaseFragment;
import com.eerussianguy.blazemap.lib.gui.fragment.FragmentContainer;

public class WaypointGroupEditorFragment extends BaseFragment {
    private final WaypointGroup group;

    public WaypointGroupEditorFragment(WaypointGroup group) {
        super(Helpers.translate("blazemap.gui.waypoint_group_editor.title"), true, false);
        this.group = group;
    }

    @Override
    public void compose(FragmentContainer container, VolatileContainer volatiles) {
        int y = 0;

        if(container.titleConsumer.isPresent()) {
            container.titleConsumer.get().accept(getTitle());
        } else {
            container.add(new Label(getTitle()), 0, y);
            y = 15;
        }

        ObjHolder<String> name = new ObjHolder<>(group.getNameString());
        container.add(VanillaComponents.makeTextField(font, 160, 14, name), 0, y);

        TextButton submit = new TextButton(Helpers.translate("blazemap.gui.button.save"), button -> {
            group.setUserGivenName(name.get());
            container.dismiss();
        });
        container.add(submit.setSize(80, 20), 40, y+20);
    }
}
