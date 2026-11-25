package com.nexus.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class Badge extends HBox {

    public enum Type {
        PRIMARY("badge-primary"),
        SECONDARY("badge-secondary"),
        SUCCESS("badge-success"),
        WARNING("badge-warning"),
        DANGER("badge-danger"),
        INFO("badge-info"),
        MUTED("badge-muted");

        private final String cssClass;

        Type(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    private final Label label;

    public Badge(String text, Type type) {
        this.label = new Label(text);
        this.label.getStyleClass().add("badge-label");

        setAlignment(Pos.CENTER);
        setPadding(new Insets(2, 8, 2, 8));
        getStyleClass().addAll("nexus-badge", type.getCssClass());
        getChildren().add(label);
    }

    public static Badge of(String text, Type type) {
        return new Badge(text, type);
    }

    public void setText(String text) {
        label.setText(text);
    }

    public String getText() {
        return label.getText();
    }
}

