package com.eerussianguy.blazemap.feature.waypoints.service;

public enum ManagementType {
    READONLY(false, false),
    INBOX(true, false),
    FULL(true, true);

    public final boolean canDelete, canCreate;

    ManagementType(boolean delete, boolean create) {
        this.canDelete = delete;
        this.canCreate = create;
    }
}
