package com.eerussianguy.blazemap.feature.waypoints.service;

public enum ManagementType {
    READONLY(false, false, false, false, false),
    INBOX(false, false, true, false, false),
    FULL(true, true, true, true, true);

    public final boolean canDelete, canEdit, canDeleteChild, canEditChild, canCreateChild;

    ManagementType(boolean canDelete, boolean canEdit, boolean canDeleteChild, boolean canEditChild, boolean canCreateChild) {
        this.canDelete = canDelete;
        this.canEdit = canEdit;
        this.canDeleteChild = canDeleteChild;
        this.canEditChild = canEditChild;
        this.canCreateChild = canCreateChild;
    }
}
