package com.nexus.component;

import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * High-definition vector SVG icon factory for the NEXUS application.
 * All paths are resolution-independent and styled to match modern developer tool aesthetics.
 */
public class IconFactory {

    // Lucide / Feather inspired SVG paths
    public static final String ICON_LOGO_NEXUS = "M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5";
    public static final String ICON_HOME = "M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z M9 22V12h6v10";
    public static final String ICON_TERMINAL = "M4 17l6-6-6-6 M12 19h8";
    public static final String ICON_GIT_BRANCH = "M6 3v12 M18 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M6 21a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M18 9a9 9 0 0 1-9 9";
    public static final String ICON_API = "M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z M3.27 6.96L12 12.01l8.73-5.05 M12 22.08V12";
    public static final String ICON_ANALYTICS = "M18 20V10 M12 20V4 M6 20v-6";
    public static final String ICON_SETTINGS = "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z";
    public static final String ICON_SPARKLES = "M12 3l1.912 5.813a2 2 0 0 0 1.275 1.275L21 12l-5.813 1.912a2 2 0 0 0-1.275 1.275L12 21l-1.912-5.813a2 2 0 0 0-1.275-1.275L3 12l5.813-1.912a2 2 0 0 0 1.275-1.275L12 3z";
    public static final String ICON_SEARCH = "M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z M21 21l-4.35-4.35";
    public static final String ICON_SEND = "M22 2L11 13 M22 2l-7 20-4-9-9-4 20-7z";
    public static final String ICON_COPY = "M8 4H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2 M16 4h2a2 2 0 0 1 2 2v4 M21 14H11a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h6.586a1 1 0 0 1 .707.293l3.414 3.414a1 1 0 0 1 .293.707V12a2 2 0 0 1-2 2z";
    public static final String ICON_CHECK = "M20 6L9 17l-5-5";
    public static final String ICON_CHEVRON_RIGHT = "M9 18l6-6-6-6";
    public static final String ICON_CHEVRON_LEFT = "M15 18l-6-6 6-6";
    public static final String ICON_CHEVRON_DOWN = "M6 9l6 6 6-6";
    public static final String ICON_PLUS = "M12 5v14 M5 12h14";
    public static final String ICON_TRASH = "M3 6h18 M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2";
    public static final String ICON_PLAY = "M5 3l14 9-14 9V3z";
    public static final String ICON_REFRESH = "M23 4v6h-6 M1 20v-6h6 M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15";
    public static final String ICON_CPU = "M4 4h16v16H4z M9 9h6v6H9z M9 1v3 M15 1v3 M9 20v3 M15 20v3 M20 9h3 M20 15h3 M1 9h3 M1 15h3";
    public static final String ICON_RAM = "M2 6h20v12H2z M6 10v4 M10 10v4 M14 10v4 M18 10v4";
    public static final String ICON_DISK = "M22 12A10 10 0 0 0 12 2v10z M12 2a10 10 0 1 0 10 10H12z";
    public static final String ICON_NETWORK = "M5 12.55a11 11 0 0 1 14.08 0 M1.42 9a16 16 0 0 1 21.16 0 M8.53 16.11a6 6 0 0 1 6.95 0 M12 20h.01";
    public static final String ICON_INFO = "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z M12 16v-4 M12 8h.01";
    public static final String ICON_CLOCK = "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z M12 6v6l4 2";
    public static final String ICON_CODE = "M16 18l6-6-6-6 M8 6l-6 6 6 6";
    public static final String ICON_COLLAPSE = "M18 15l-6-6-6 6";
    public static final String ICON_SIDEBAR_TOGGLE = "M3 3h18v18H3z M9 3v18";
    public static final String ICON_CLOSE = "M18 6L6 18 M6 6l12 12";
    public static final String ICON_MAXIMIZE = "M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3";
    public static final String ICON_MINIMIZE = "M8 3v3a2 2 0 0 1-2 2H3m18 0h-3a2 2 0 0 1-2-2V3m0 18v-3a2 2 0 0 1 2-2h3M3 16h3a2 2 0 0 1 2 2v3";

    public static SVGPath createIcon(String content, double scale, Color strokeColor) {
        SVGPath svg = new SVGPath();
        svg.setContent(content);
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        svg.setStroke(strokeColor);
        svg.setFill(Color.TRANSPARENT);
        svg.setStrokeWidth(1.8);
        svg.setStrokeLineCap(StrokeLineCap.ROUND);
        svg.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return svg;
    }

    public static SVGPath createFilledIcon(String content, double scale, Color fillColor) {
        SVGPath svg = new SVGPath();
        svg.setContent(content);
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        svg.setFill(fillColor);
        svg.setStroke(Color.TRANSPARENT);
        return svg;
    }

    public static SVGPath createIcon(String content) {
        return createIcon(content, 0.85, Color.web("#94a3b8"));
    }

    public static SVGPath createIcon(String content, Color strokeColor) {
        return createIcon(content, 0.85, strokeColor);
    }
}

