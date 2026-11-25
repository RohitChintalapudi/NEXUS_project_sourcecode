package com.nexus.model;

public enum ApiAuthType {
    NO_AUTH("No Auth"),
    BEARER_TOKEN("Bearer Token"),
    BASIC_AUTH("Basic Auth"),
    API_KEY_HEADER("API Key (Header)"),
    API_KEY_QUERY("API Key (Query Param)");

    private final String displayName;

    ApiAuthType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ApiAuthType fromDisplayName(String name) {
        if (name == null) return NO_AUTH;
        for (ApiAuthType type : values()) {
            if (type.displayName.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return NO_AUTH;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

