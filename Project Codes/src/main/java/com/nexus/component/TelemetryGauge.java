package com.nexus.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class TelemetryGauge extends HBox {
    private final Label titleLabel;
    private final Label valueLabel;
    private final ProgressBar progressBar;

    public TelemetryGauge(String title, String initialValue, double initialProgress, String barColorClass) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        getStyleClass().add("telemetry-gauge");

        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("telemetry-title");

        progressBar = new ProgressBar(initialProgress);
        progressBar.getStyleClass().addAll("telemetry-progress-bar", barColorClass);
        progressBar.setPrefWidth(65);
        progressBar.setPrefHeight(6);

        valueLabel = new Label(initialValue);
        valueLabel.getStyleClass().add("telemetry-value");

        getChildren().addAll(titleLabel, progressBar, valueLabel);
    }

    public void update(String valueText, double progressFraction) {
        valueLabel.setText(valueText);
        progressBar.setProgress(Math.max(0.0, Math.min(1.0, progressFraction)));
    }
}

