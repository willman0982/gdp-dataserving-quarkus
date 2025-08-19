package com.sc.gdp.dataserving.client.model;

/**
 * Client-side data status enumeration for data items
 */
public enum DataStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    PENDING("Pending"),
    ARCHIVED("Archived"),
    DELETED("Deleted");

    private final String displayName;

    DataStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}