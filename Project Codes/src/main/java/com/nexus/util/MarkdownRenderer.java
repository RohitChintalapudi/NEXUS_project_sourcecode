package com.nexus.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-fidelity Markdown renderer for the NEXUS AI chat bubbles.
 * Converts raw Markdown (headings, bold, italic, inline code, code blocks, bullet points, blockquotes)
 * into rich, beautiful JavaFX nodes.
 */
public class MarkdownRenderer {

    private static final Pattern INLINE_PATTERN = Pattern.compile(
            "(`(?<codeblock>[^`]+)`)|" +
            "(\\*\\*(?<bold>[^*]+?)\\*\\*)|" +
            "(\\*(?<italic>[^*]+?)\\*)|" +
            "(_(?<italic2>[^_]+?)_)|" +
            "(?<text>[^*`_]+)"
    );

    /**
     * Renders a Markdown string into a styled JavaFX VBox.
     *
     * @param markdown Raw Markdown text
     * @param onRunCommand Optional callback when user clicks "Run in Terminal ⏎" on a code/command block
     * @return VBox containing styled JavaFX nodes
     */
    public static VBox render(String markdown, Consumer<String> onRunCommand) {
        VBox container = new VBox(6);
        container.setFillWidth(true);

        if (markdown == null || markdown.trim().isEmpty()) {
            return container;
        }

        String[] lines = markdown.split("\\r?\\n");
        boolean inCodeBlock = false;
        String codeLanguage = "";
        StringBuilder codeBuffer = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            // 1. Code Block Delimiter
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // Close code block
                    container.getChildren().add(createCodeBlockCard(codeBuffer.toString().trim(), codeLanguage, onRunCommand));
                    codeBuffer.setLength(0);
                    codeLanguage = "";
                    inCodeBlock = false;
                } else {
                    // Open code block
                    inCodeBlock = true;
                    codeLanguage = trimmed.length() > 3 ? trimmed.substring(3).trim() : "";
                    codeBuffer.setLength(0);
                }
                continue;
            }

            if (inCodeBlock) {
                codeBuffer.append(line).append("\n");
                continue;
            }

            // 2. Empty Line Spacer
            if (trimmed.isEmpty()) {
                Region spacer = new Region();
                spacer.setMinHeight(4);
                spacer.setPrefHeight(4);
                container.getChildren().add(spacer);
                continue;
            }

            // 3. Horizontal Rule
            if (trimmed.equals("---") || trimmed.equals("***") || trimmed.equals("___")) {
                Region divider = new Region();
                divider.setMinHeight(1);
                divider.setPrefHeight(1);
                divider.setStyle("-fx-background-color: #27272a; -fx-margin: 4 0;");
                container.getChildren().add(divider);
                continue;
            }

            // 4. Headings
            if (trimmed.startsWith("#")) {
                container.getChildren().add(createHeadingNode(trimmed));
                continue;
            }

            // 5. Blockquote (> ...)
            if (trimmed.startsWith(">")) {
                String quoteText = trimmed.substring(1).trim();
                container.getChildren().add(createBlockquoteNode(quoteText));
                continue;
            }

            // 6. Bullet Point List Items (•, *, -, +)
            if (trimmed.startsWith("•") || trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("+ ")) {
                String bulletText = trimmed.startsWith("•") ? trimmed.substring(1).trim() : trimmed.substring(2).trim();
                container.getChildren().add(createBulletItemNode(bulletText));
                continue;
            }

            // 7. Numbered List Items (1. , 2. )
            if (trimmed.matches("^\\d+\\.\\s+.*")) {
                int dotIdx = trimmed.indexOf('.');
                String num = trimmed.substring(0, dotIdx + 1);
                String itemText = trimmed.substring(dotIdx + 1).trim();
                container.getChildren().add(createNumberedItemNode(num, itemText));
                continue;
            }

            // 8. Standard Paragraph with inline formatting
            TextFlow paragraph = createInlineTextFlow(line);
            container.getChildren().add(paragraph);
        }

        // Flush unterminated code block if any
        if (inCodeBlock && codeBuffer.length() > 0) {
            container.getChildren().add(createCodeBlockCard(codeBuffer.toString().trim(), codeLanguage, onRunCommand));
        }

        return container;
    }

    private static Node createHeadingNode(String line) {
        int level = 0;
        while (level < line.length() && line.charAt(level) == '#') {
            level++;
        }
        String text = line.substring(level).trim();

        Label heading = new Label(text);
        heading.setWrapText(true);

        switch (level) {
            case 1 -> heading.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #fbbf24; -fx-padding: 4 0 2 0;");
            case 2 -> heading.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #f59e0b; -fx-padding: 3 0 1 0;");
            default -> heading.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 800; -fx-text-fill: #fbfbfb; -fx-padding: 2 0 1 0;");
        }

        return heading;
    }

    private static Node createBlockquoteNode(String text) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("""
                -fx-background-color: rgba(245, 158, 11, 0.08);
                -fx-border-color: #f59e0b;
                -fx-border-width: 0 0 0 3px;
                -fx-padding: 6px 10px;
                -fx-background-radius: 0 4px 4px 0;
                """);

        TextFlow flow = createInlineTextFlow(text);
        box.getChildren().add(flow);
        return box;
    }

    private static Node createBulletItemNode(String text) {
        HBox item = new HBox(6);
        item.setAlignment(Pos.TOP_LEFT);
        item.setPadding(new Insets(1, 0, 1, 4));

        Label bullet = new Label("•");
        bullet.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 0 2 0 0;");

        TextFlow content = createInlineTextFlow(text);
        HBox.setHgrow(content, Priority.ALWAYS);

        item.getChildren().addAll(bullet, content);
        return item;
    }

    private static Node createNumberedItemNode(String number, String text) {
        HBox item = new HBox(6);
        item.setAlignment(Pos.TOP_LEFT);
        item.setPadding(new Insets(1, 0, 1, 4));

        Label numLabel = new Label(number);
        numLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 11.5px; -fx-font-weight: 800; -fx-font-family: 'JetBrains Mono', Consolas, monospace;");

        TextFlow content = createInlineTextFlow(text);
        HBox.setHgrow(content, Priority.ALWAYS);

        item.getChildren().addAll(numLabel, content);
        return item;
    }

    private static TextFlow createInlineTextFlow(String rawText) {
        TextFlow flow = new TextFlow();
        flow.setMaxWidth(370);

        Matcher matcher = INLINE_PATTERN.matcher(rawText);

        while (matcher.find()) {
            String code = matcher.group("codeblock");
            String bold = matcher.group("bold");
            String italic = matcher.group("italic");
            String italic2 = matcher.group("italic2");
            String plain = matcher.group("text");

            if (code != null) {
                Text textNode = new Text(code);
                textNode.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 11.5));
                textNode.setFill(Color.web("#fbbf24"));
                flow.getChildren().add(textNode);
            } else if (bold != null) {
                Text textNode = new Text(bold);
                textNode.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 12.5));
                textNode.setFill(Color.web("#ffffff"));
                flow.getChildren().add(textNode);
            } else if (italic != null || italic2 != null) {
                String itVal = italic != null ? italic : italic2;
                Text textNode = new Text(itVal);
                textNode.setFont(Font.font("Segoe UI", FontWeight.BOLD, FontPosture.ITALIC, 12.5));
                textNode.setFill(Color.web("#d4d4d8"));
                flow.getChildren().add(textNode);
            } else if (plain != null) {
                Text textNode = new Text(plain);
                textNode.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12.5));
                textNode.setFill(Color.web("#f4f4f5"));
                flow.getChildren().add(textNode);
            }
        }

        return flow;
    }

    private static Node createCodeBlockCard(String code, String language, Consumer<String> onRunCommand) {
        VBox card = new VBox(4);
        card.setStyle("""
                -fx-background-color: #101013;
                -fx-border-color: #27272a;
                -fx-border-width: 1px;
                -fx-border-radius: 6px;
                -fx-background-radius: 6px;
                -fx-padding: 8px 10px;
                """);

        // Header bar with language pill + copy button + run button
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label langBadge = new Label(language.isEmpty() ? "CODE" : language.toUpperCase());
        langBadge.setStyle("""
                -fx-background-color: rgba(245, 158, 11, 0.15);
                -fx-text-fill: #fbbf24;
                -fx-font-family: 'JetBrains Mono', Consolas, monospace;
                -fx-font-size: 9px;
                -fx-font-weight: 800;
                -fx-padding: 1px 6px;
                -fx-background-radius: 3px;
                """);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnCopy = new Button("Copy");
        btnCopy.setStyle("""
                -fx-background-color: rgba(255, 255, 255, 0.06);
                -fx-text-fill: #d4d4d8;
                -fx-font-size: 10px;
                -fx-font-weight: bold;
                -fx-cursor: hand;
                -fx-padding: 2px 7px;
                -fx-border-color: #27272a;
                -fx-border-radius: 4px;
                -fx-background-radius: 4px;
                """);
        btnCopy.setOnAction(e -> {
            UiUtils.copyToClipboard(code);
            btnCopy.setText("Copied! ✓");
            btnCopy.setStyle("""
                    -fx-background-color: rgba(16, 185, 129, 0.18);
                    -fx-text-fill: #10b981;
                    -fx-font-size: 10px;
                    -fx-font-weight: bold;
                    -fx-cursor: hand;
                    -fx-padding: 2px 7px;
                    -fx-border-color: rgba(16, 185, 129, 0.4);
                    -fx-border-radius: 4px;
                    -fx-background-radius: 4px;
                    """);
            javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
            pt.setOnFinished(ev -> {
                btnCopy.setText("Copy");
                btnCopy.setStyle("""
                        -fx-background-color: rgba(255, 255, 255, 0.06);
                        -fx-text-fill: #d4d4d8;
                        -fx-font-size: 10px;
                        -fx-font-weight: bold;
                        -fx-cursor: hand;
                        -fx-padding: 2px 7px;
                        -fx-border-color: #27272a;
                        -fx-border-radius: 4px;
                        -fx-background-radius: 4px;
                        """);
            });
            pt.play();
        });

        header.getChildren().addAll(langBadge, spacer, btnCopy);

        // If it's a shell command (or bash / sh / powershell), add a Run in Terminal button
        boolean isExecutableCommand = isCommandLanguage(language) || (!code.contains("\n") && (code.startsWith("mvn ") || code.startsWith("git ") || code.startsWith("java ") || code.startsWith("cd ")));
        if (isExecutableCommand && onRunCommand != null) {
            Button btnRun = new Button("Run in Terminal ⏎");
            btnRun.setStyle("""
                    -fx-background-color: linear-gradient(to right, #d97706, #f59e0b);
                    -fx-text-fill: #09090b;
                    -fx-font-size: 10px;
                    -fx-font-weight: 800;
                    -fx-border-radius: 3px;
                    -fx-background-radius: 3px;
                    -fx-padding: 2px 8px;
                    -fx-cursor: hand;
                    """);
            btnRun.setOnAction(e -> onRunCommand.accept(code));
            header.getChildren().add(btnRun);
        }

        Label codeText = new Label(code);
        codeText.setWrapText(true);
        codeText.setStyle("""
                -fx-font-family: 'JetBrains Mono', Consolas, monospace;
                -fx-font-size: 11.5px;
                -fx-font-weight: bold;
                -fx-text-fill: #facc15;
                -fx-padding: 4px 0 0 0;
                """);

        card.getChildren().addAll(header, codeText);
        return card;
    }

    private static boolean isCommandLanguage(String lang) {
        if (lang == null) return false;
        String lower = lang.toLowerCase().trim();
        return lower.equals("bash") || lower.equals("sh") || lower.equals("shell") ||
                lower.equals("cmd") || lower.equals("powershell") || lower.equals("terminal");
    }
}

