package com.eerussianguy.blazemap.feature.waypoints;

import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.lib.gui.components.Label;
import com.eerussianguy.blazemap.lib.gui.fragment.BaseFragment;
import com.eerussianguy.blazemap.lib.gui.fragment.FragmentContainer;

public class WaypointManagerFragment extends BaseFragment {

    protected WaypointManagerFragment() {
        super(Helpers.translate("blazemap.gui.waypoint_manager.title"), true, false);
    }

    @Override
    public void compose(FragmentContainer container) {
        int y = 0;

        if(container.titleConsumer.isPresent()) {
            container.titleConsumer.get().accept(getTitle());
        } else {
            container.add(new Label(getTitle()), 0, y);
            y = 15;
        }

        
    }
}
