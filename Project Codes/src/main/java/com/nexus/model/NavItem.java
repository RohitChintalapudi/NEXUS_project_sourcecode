package com.nexus.model;

public enum NavItem {
    HOME("Home", "Welcome overview, quick actions & workspace stats"),
    TERMINAL("NEXUS Terminal", "Block-based modern developer terminal"),
    REPO_GRAPH("RepoGraph", "Interactive repository branch & commit topology"),
    API_STUDIO("API Studio", "REST endpoint testing, inspection & client studio"),
    HISTORY("Command History & Analytics", "Command execution telemetry & activity breakdown");

    private final String title;
    private final String description;

    NavItem(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getLabel() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}

