package com.nexus.util;

import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnsiRenderer {

    // Regex pattern matching standard CSI ANSI escape sequences
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[([0-9;]*)m");

    // Standard ANSI 16-color palette mapped to NEXUS futuristic theme
    private static final Color COLOR_BLACK = Color.web("#1e293b");
    private static final Color COLOR_RED = Color.web("#f43f5e");
    private static final Color COLOR_GREEN = Color.web("#10b981");
    private static final Color COLOR_YELLOW = Color.web("#fbbf24");
    private static final Color COLOR_BLUE = Color.web("#38bdf8");
    private static final Color COLOR_MAGENTA = Color.web("#c084fc");
    private static final Color COLOR_CYAN = Color.web("#00d4ff");
    private static final Color COLOR_WHITE = Color.web("#f8fafc");
    private static final Color COLOR_DEFAULT = Color.web("#cbd5e1");

    public static String stripAnsi(String text) {
        if (text == null) return "";
        // Strip CSI sequences and other terminal control codes
        String stripped = text.replaceAll("\u001B\\[[;?0-9]*[a-zA-Z]", "");
        stripped = stripped.replaceAll("\u001B\\([a-zA-Z]", "");
        stripped = stripped.replaceAll("\r\r\n", "\n");
        return stripped;
    }

    public static List<Text> parseToTextNodes(String text, double fontSize) {
        List<Text> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return result;
        }

        Matcher matcher = ANSI_PATTERN.matcher(text);
        int lastIndex = 0;
        Color currentColor = COLOR_DEFAULT;
        boolean bold = false;

        while (matcher.find()) {
            if (matcher.start() > lastIndex) {
                String sub = text.substring(lastIndex, matcher.start());
                Text t = createStyledText(sub, currentColor, bold, fontSize);
                result.add(t);
            }

            String codeString = matcher.group(1);
            if (codeString == null || codeString.isEmpty() || codeString.equals("0")) {
                currentColor = COLOR_DEFAULT;
                bold = false;
            } else {
                String[] codes = codeString.split(";");
                for (String c : codes) {
                    try {
                        int code = Integer.parseInt(c);
                        switch (code) {
                            case 0 -> {
                                currentColor = COLOR_DEFAULT;
                                bold = false;
                            }
                            case 1 -> bold = true;
                            case 22 -> bold = false;
                            case 30 -> currentColor = COLOR_BLACK;
                            case 31, 91 -> currentColor = COLOR_RED;
                            case 32, 92 -> currentColor = COLOR_GREEN;
                            case 33, 93 -> currentColor = COLOR_YELLOW;
                            case 34, 94 -> currentColor = COLOR_BLUE;
                            case 35, 95 -> currentColor = COLOR_MAGENTA;
                            case 36, 96 -> currentColor = COLOR_CYAN;
                            case 37, 97 -> currentColor = COLOR_WHITE;
                            case 39 -> currentColor = COLOR_DEFAULT;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            lastIndex = matcher.end();
        }

        if (lastIndex < text.length()) {
            String remainder = text.substring(lastIndex);
            Text t = createStyledText(remainder, currentColor, bold, fontSize);
            result.add(t);
        }

        return result;
    }

    private static Text createStyledText(String content, Color color, boolean bold, double fontSize) {
        Text text = new Text(content);
        text.setFill(color);
        text.setStyle(String.format("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: %.1fpx; %s",
                fontSize, bold ? "-fx-font-weight: bold;" : ""));
        return text;
    }
}

