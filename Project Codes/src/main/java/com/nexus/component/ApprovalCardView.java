package com.nexus.component;

import com.nexus.model.ToolRequest;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.Map;

public class ApprovalCardView extends VBox {

    public ApprovalCardView(ToolRequest request) {
        setSpacing(10);
        getStyleClass().add("ai-approval-card");
        setStyle("""
                -fx-background-color: #141417;
                -fx-border-color: rgba(245, 158, 11, 0.4);
                -fx-border-width: 1px;
                -fx-border-radius: 8px;
                -fx-background-radius: 8px;
                -fx-padding: 12px 14px;
                """);

        // Header: Badge + Title
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label("SECURITY APPROVAL REQUIRED");
        badge.setStyle("""
                -fx-background-color: rgba(245, 158, 11, 0.2);
                -fx-text-fill: #f59e0b;
                -fx-font-family: 'JetBrains Mono', Consolas, monospace;
                -fx-font-size: 10px;
                -fx-font-weight: bold;
                -fx-padding: 2px 6px;
                -fx-background-radius: 4px;
                """);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label toolLabel = new Label(request.getToolName());
        toolLabel.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 10px; -fx-text-fill: #94a3b8;");

        header.getChildren().addAll(badge, spacer, toolLabel);

        // Description
        Label descLabel = new Label(request.getDescription());
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #e2e8f0; -fx-font-weight: bold;");

        // Target / Working Directory if available
        VBox detailsBox = new VBox(4);
        if (request.getWorkingDirectory() != null) {
            Label pwdLabel = new Label("Working Directory: " + request.getWorkingDirectory());
            pwdLabel.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 10px; -fx-text-fill: #38bdf8;");
            detailsBox.getChildren().add(pwdLabel);
        }

        // Arguments snippet
        if (!request.getArguments().isEmpty()) {
            StringBuilder argsSb = new StringBuilder();
            for (Map.Entry<String, Object> e : request.getArguments().entrySet()) {
                argsSb.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
            }
            Label argsLabel = new Label(argsSb.toString().trim());
            argsLabel.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 10px; -fx-text-fill: #94a3b8;");
            detailsBox.getChildren().add(argsLabel);
        }

        // Action Buttons
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnReject = new Button("Reject ✕");
        btnReject.setStyle("""
                -fx-background-color: rgba(244, 63, 94, 0.15);
                -fx-text-fill: #f43f5e;
                -fx-border-color: rgba(244, 63, 94, 0.4);
                -fx-border-radius: 4px;
                -fx-background-radius: 4px;
                -fx-padding: 4px 12px;
                -fx-cursor: hand;
                -fx-font-weight: bold;
                """);

        Button btnApprove = new Button("Approve & Execute ✓");
        btnApprove.setStyle("""
                -fx-background-color: rgba(16, 185, 129, 0.2);
                -fx-text-fill: #10b981;
                -fx-border-color: rgba(16, 185, 129, 0.4);
                -fx-border-radius: 4px;
                -fx-background-radius: 4px;
                -fx-padding: 4px 12px;
                -fx-cursor: hand;
                -fx-font-weight: bold;
                """);

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-font-family: 'JetBrains Mono', Consolas, monospace;");
        statusLabel.setVisible(false);

        btnApprove.setOnAction(e -> {
            btnApprove.setDisable(true);
            btnReject.setDisable(true);
            statusLabel.setText("● APPROVED");
            statusLabel.setTextFill(Color.web("#10b981"));
            statusLabel.setVisible(true);
            request.approve();
        });

        btnReject.setOnAction(e -> {
            btnApprove.setDisable(true);
            btnReject.setDisable(true);
            statusLabel.setText("● REJECTED");
            statusLabel.setTextFill(Color.web("#f43f5e"));
            statusLabel.setVisible(true);
            request.reject();
        });

        actions.getChildren().addAll(statusLabel, btnReject, btnApprove);

        getChildren().addAll(header, descLabel, detailsBox, actions);
    }
}

