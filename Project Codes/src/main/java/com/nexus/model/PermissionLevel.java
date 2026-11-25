package com.nexus.model;

public enum PermissionLevel {
    READ_ONLY("Read-Only (Auto-Permitted)"),
    APPROVAL_REQUIRED("Approval Required"),
    RESTRICTED("Restricted (Forbidden)");

    private final String displayName;

    PermissionLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

