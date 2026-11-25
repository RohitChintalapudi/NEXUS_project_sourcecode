package com.nexus.util;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;

public class UiUtils {

    public static void copyToClipboard(String text) {
        if (text == null || text.isEmpty()) return;
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    public static void fadeIn(Node node, double durationMs) {
        node.setOpacity(0.0);
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    public static void slideInFromBottom(Node node, double distance, double durationMs) {
        node.setOpacity(0.0);
        node.setTranslateY(distance);

        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);

        TranslateTransition tt = new TranslateTransition(Duration.millis(durationMs), node);
        tt.setFromY(distance);
        tt.setToY(0.0);

        ft.play();
        tt.play();
    }

    public static Label createBadge(String text, String styleClass) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("nexus-badge", styleClass);
        return badge;
    }
}

