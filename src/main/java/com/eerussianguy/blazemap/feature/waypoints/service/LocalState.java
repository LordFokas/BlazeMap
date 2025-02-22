package com.eerussianguy.blazemap.feature.waypoints.service;

import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.lib.InheritedBoolean;

public class LocalState {
    public static final LocalState ROOT = new RootLocalState();

    private LocalState parent;
    private InheritedBoolean visibility = InheritedBoolean.DEFAULT;

    public LocalState() {
        this(ROOT);
    }

    public LocalState(LocalState parent) {
        this.parent = parent;
    }

    public void setParent(LocalState parent) {
        this.parent = parent;
    }

    public InheritedBoolean getVisibility() {
        return visibility;
    }

    public void setVisibility(InheritedBoolean visibility) {
        this.visibility = visibility;
    }

    public boolean isVisible() {
        return visibility.getOrInherit(parent::isVisible);
    }

    // =================================================================================================================
    private static final class RootLocalState extends LocalState {
        private RootLocalState() {
            super(null);
        }

        @Override
        public InheritedBoolean getVisibility() {
            return InheritedBoolean.of(BlazeMapConfig.CLIENT.clientFeatures.displayWaypointsOnMap.get());
        }

        @Override
        public boolean isVisible() {
            return getVisibility().getOrThrow();
        }
    }
}
