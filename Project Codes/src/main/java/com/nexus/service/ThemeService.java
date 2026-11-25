package com.nexus.service;

import javafx.application.Platform;
import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class ThemeService {

    public enum Theme {
        MATTE_BLACK_GOLD(
                "Matte Black & Gold Yellow (Default)",
                "#f59e0b",
                "#09090b",
                "-color-bg-base: #09090b; -color-bg-sidebar: #0d0d10; -color-bg-panel: #121215; -color-bg-card: #18181c; -color-bg-card-hover: #222228; -color-bg-input: #101013; -color-bg-terminal: #070709; -color-border-subtle: #27272a; -color-border-medium: #3f3f46; -color-border-glow: #f59e0b; -color-accent-gold: #fbbf24; -color-accent-amber: #f59e0b; -color-brand-primary: #f59e0b; -color-text-primary: #fbfbfb; -color-text-secondary: #d4d4d8; -color-text-muted: #a1a1aa; -fx-focus-color: #fbbf24; -fx-faint-focus: rgba(251, 191, 36, 0.25);"
        ),
        CHRISTMAS_FESTIVE(
                "🎄 Christmas Festive (Pine Green & Candy Cane Crimson)",
                "#ef4444",
                "#07150c",
                "-color-bg-base: #07150c; -color-bg-sidebar: #0b1e13; -color-bg-panel: #0f271a; -color-bg-card: #153423; -color-bg-card-hover: #1c442e; -color-bg-input: #0b1a10; -color-bg-terminal: #030a05; -color-border-subtle: #1e4530; -color-border-medium: #2d6244; -color-border-glow: #ef4444; -color-accent-gold: #facc15; -color-accent-amber: #ef4444; -color-brand-primary: #ef4444; -color-accent-rose: #f43f5e; -color-accent-emerald: #22c55e; -color-text-primary: #ffffff; -color-text-secondary: #e2f5e8; -color-text-muted: #94b8a1; -fx-focus-color: #ef4444; -fx-faint-focus: rgba(239, 68, 68, 0.25);"
        ),
        CYBERPUNK_NEON(
                "⚡ Cyberpunk Neon (Obsidian Violet & Hot Pink)",
                "#ec4899",
                "#0c0817",
                "-color-bg-base: #0c0817; -color-bg-sidebar: #120d24; -color-bg-panel: #181131; -color-bg-card: #201740; -color-bg-card-hover: #2c2057; -color-bg-input: #150f2b; -color-bg-terminal: #07040e; -color-border-subtle: #33245f; -color-border-medium: #4e3594; -color-border-glow: #ec4899; -color-accent-gold: #06b6d4; -color-accent-amber: #ec4899; -color-brand-primary: #ec4899; -color-accent-rose: #f43f5e; -color-text-primary: #faf5ff; -color-text-secondary: #e9d5ff; -color-text-muted: #a894c2; -fx-focus-color: #ec4899; -fx-faint-focus: rgba(236, 72, 153, 0.25);"
        ),
        MIDNIGHT_SAPPHIRE(
                "🌊 Midnight Sapphire (Deep Ocean & Electric Blue)",
                "#38bdf8",
                "#060d17",
                "-color-bg-base: #060d17; -color-bg-sidebar: #0a1322; -color-bg-panel: #0e1c31; -color-bg-card: #132743; -color-bg-card-hover: #1b355c; -color-bg-input: #0a1424; -color-bg-terminal: #03070d; -color-border-subtle: #1c3558; -color-border-medium: #274b7c; -color-border-glow: #0284c7; -color-accent-gold: #38bdf8; -color-accent-amber: #0284c7; -color-brand-primary: #0284c7; -color-text-primary: #f0f9ff; -color-text-secondary: #bae6fd; -color-text-muted: #7ca3c5; -fx-focus-color: #38bdf8; -fx-faint-focus: rgba(56, 189, 248, 0.25);"
        ),
        EMERALD_MATRIX(
                "📟 Emerald Matrix (Hacker Obsidian & Terminal Green)",
                "#10b981",
                "#040d07",
                "-color-bg-base: #040d07; -color-bg-sidebar: #07150c; -color-bg-panel: #0b1e12; -color-bg-card: #0f2a19; -color-bg-card-hover: #153923; -color-bg-input: #07160d; -color-bg-terminal: #020704; -color-border-subtle: #163d24; -color-border-medium: #235835; -color-border-glow: #10b981; -color-accent-gold: #34d399; -color-accent-amber: #10b981; -color-brand-primary: #10b981; -color-text-primary: #f0fdf4; -color-text-secondary: #bbf7d0; -color-text-muted: #75a886; -fx-focus-color: #10b981; -fx-faint-focus: rgba(16, 185, 129, 0.25);"
        );

        private final String displayName;
        private final String primaryColor;
        private final String bgColor;
        private final String cssStyle;

        Theme(String displayName, String primaryColor, String bgColor, String cssStyle) {
            this.displayName = displayName;
            this.primaryColor = primaryColor;
            this.bgColor = bgColor;
            this.cssStyle = cssStyle;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getPrimaryColor() {
            return primaryColor;
        }

        public String getBgColor() {
            return bgColor;
        }

        public String getCssStyle() {
            return cssStyle;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static final ThemeService INSTANCE = new ThemeService();
    private static final String PREF_THEME_KEY = "nexus.ui.selected_theme";
    private final Preferences prefs = Preferences.userNodeForPackage(ThemeService.class);

    private Theme currentTheme = Theme.MATTE_BLACK_GOLD;
    private Scene activeScene;
    private final List<Runnable> themeChangeListeners = new ArrayList<>();

    private ThemeService() {
        String saved = prefs.get(PREF_THEME_KEY, Theme.MATTE_BLACK_GOLD.name());
        try {
            currentTheme = Theme.valueOf(saved);
        } catch (Exception e) {
            currentTheme = Theme.MATTE_BLACK_GOLD;
        }
    }

    public static ThemeService getInstance() {
        return INSTANCE;
    }

    public void registerScene(Scene scene) {
        this.activeScene = scene;
        if (scene != null && scene.getRoot() != null) {
            scene.getRoot().setStyle(currentTheme.getCssStyle());
        }
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public void setTheme(Theme theme) {
        if (theme == null) return;
        this.currentTheme = theme;
        prefs.put(PREF_THEME_KEY, theme.name());

        if (activeScene != null && activeScene.getRoot() != null) {
            Platform.runLater(() -> {
                activeScene.getRoot().setStyle(theme.getCssStyle());
                for (Runnable r : themeChangeListeners) {
                    try { r.run(); } catch (Exception ignored) {}
                }
            });
        }
    }

    public void addThemeChangeListener(Runnable listener) {
        themeChangeListeners.add(listener);
    }
}

