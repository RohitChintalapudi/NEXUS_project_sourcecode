package com.nexus.component;

import com.nexus.model.CommandBlock;
import com.nexus.service.NavigationService;
import com.nexus.util.UiUtils;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.function.Consumer;

public class CommandBlockView extends VBox {
    private final CommandBlock block;
    private final Consumer<String> onAskAiCallback;

    public CommandBlockView(CommandBlock block, Consumer<String> onAskAiCallback) {
        this.block = block;
        this.onAskAiCallback = onAskAiCallback;

        getStyleClass().add("terminal-command-block");
        setSpacing(0);

        // Header
        HBox header = buildHeader();
        // Body / Output
        Node outputBody = buildOutputBody();

        getChildren().addAll(header, outputBody);
    }

    private HBox buildHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("command-block-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(8);
        header.setPadding(new Insets(6, 12, 6, 12));

        // Directory & Git Branch pill
        Label dirLabel = new Label(block.getWorkingDirectory());
        dirLabel.getStyleClass().add("command-block-dir");

        Label branchLabel = new Label("git:(" + block.getGitBranch() + ")");
        branchLabel.getStyleClass().add("command-block-branch");

        // Command text
        Label cmdLabel = new Label("$ " + block.getCommand());
        cmdLabel.getStyleClass().add("command-block-command");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status code badge
        Badge statusBadge = new Badge(
                block.isSuccess() ? "0" : String.valueOf(block.getExitCode()),
                block.isSuccess() ? Badge.Type.SUCCESS : Badge.Type.DANGER
        );

        // Duration
        Label durationLabel = new Label(block.getFormattedDuration());
        durationLabel.getStyleClass().add("command-block-meta");

        // Timestamp
        Label timeLabel = new Label(block.getFormattedTimestamp());
        timeLabel.getStyleClass().add("command-block-meta");

        // Copy button
        Button copyBtn = new Button();
        copyBtn.getStyleClass().add("block-action-btn");
        copyBtn.setGraphic(IconFactory.createIcon(IconFactory.ICON_COPY, 0.7, Color.web("#94a3b8")));
        copyBtn.setOnAction(e -> {
            UiUtils.copyToClipboard(block.getOutput());
            copyBtn.setGraphic(IconFactory.createIcon(IconFactory.ICON_CHECK, 0.7, Color.web("#10b981")));
            PauseTransition pt = new PauseTransition(Duration.seconds(1.5));
            pt.setOnFinished(ev -> copyBtn.setGraphic(IconFactory.createIcon(IconFactory.ICON_COPY, 0.7, Color.web("#94a3b8"))));
            pt.play();
        });

        // Ask AI button
        Button askAiBtn = new Button("Explain");
        askAiBtn.getStyleClass().addAll("block-action-btn", "block-ai-btn");
        askAiBtn.setGraphic(IconFactory.createIcon(IconFactory.ICON_SPARKLES, 0.7, Color.web("#a855f7")));
        askAiBtn.setOnAction(e -> {
            if (onAskAiCallback != null) {
                onAskAiCallback.accept("Explain output for: " + block.getCommand());
            }
        });

        header.getChildren().addAll(dirLabel, branchLabel, cmdLabel, spacer, statusBadge, durationLabel, timeLabel, copyBtn, askAiBtn);
        return header;
    }

    private javafx.scene.Node buildOutputBody() {
        TextArea textArea = new TextArea(block.getOutput());
        textArea.getStyleClass().add("command-block-output");
        textArea.setEditable(false);
        textArea.setWrapText(true);

        // Dynamically size based on line count
        int lines = block.getOutput().split("\r\n|\r|\n").length;
        double calculatedHeight = Math.min(320, Math.max(50, lines * 20 + 20));
        textArea.setPrefHeight(calculatedHeight);
        textArea.setMaxHeight(calculatedHeight);

        return textArea;
    }
}
