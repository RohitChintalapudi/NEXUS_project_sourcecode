package com.nexus.config;

public final class AppConfig {
    private AppConfig() {}

    public static final String APP_NAME = "NEXUS";
    public static final String APP_SUBTITLE = "AI Developer Workspace";
    public static final String APP_VERSION = "v1.0.0-alpha";
    public static final String APP_STAGE = "Stage 2 — Real Terminal Engine & System Analytics";

    public static final String DEFAULT_WORKSPACE_PATH = "~/project/nexus-core";
    public static final String DEFAULT_BRANCH = "main";
    public static final String DEFAULT_AUTHOR = "nexus-dev <dev@nexus.internal>";

    public static final double DEFAULT_WINDOW_WIDTH = 1380.0;
    public static final double DEFAULT_WINDOW_HEIGHT = 880.0;
    public static final double MIN_WINDOW_WIDTH = 800.0;
    public static final double MIN_WINDOW_HEIGHT = 520.0;

    public static final double SIDEBAR_WIDTH = 250.0;
    public static final double AI_PANEL_EXPANDED_WIDTH = 380.0;
    public static final double AI_PANEL_COLLAPSED_WIDTH = 0.0;
}

