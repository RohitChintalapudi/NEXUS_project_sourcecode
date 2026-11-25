package com.nexus.component;

import com.nexus.model.FileItem;
import com.nexus.model.GitStatusResult;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class HorizontalRepoTreeView extends HBox {

    private File rootDir;
    private GitStatusResult gitStatus;
    private final Set<String> expandedPaths = new HashSet<>();
    private Consumer<FileItem> onFileSelectedListener;
    private VBox selectedNodeCard = null;
    private String currentSearchFilter = "";

    private static final Set<String> IGNORED_NAMES = Set.of(
            ".git", ".idea", ".vscode", "target", "build", "node_modules", ".gradle", ".system_generated"
    );

    public HorizontalRepoTreeView() {
        setSpacing(28);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(24));
        setStyle("-fx-background-color: transparent;");
    }

    public void setOnFileSelectedListener(Consumer<FileItem> listener) {
        this.onFileSelectedListener = listener;
    }

    public void renderRepository(File directory, GitStatusResult status) {
        this.rootDir = directory;
        this.gitStatus = status;
        this.expandedPaths.clear();

        if (directory != null) {
            // Auto-expand root and first level folders by default
            this.expandedPaths.add(directory.getAbsolutePath());
            File[] firstLevel = directory.listFiles();
            if (firstLevel != null) {
                for (File f : firstLevel) {
                    if (f.isDirectory() && ("src".equalsIgnoreCase(f.getName()) || "main".equalsIgnoreCase(f.getName()))) {
                        this.expandedPaths.add(f.getAbsolutePath());
                    }
                }
            }
        }

        rebuildLayout();
    }

    public void setSearchFilter(String filter) {
        this.currentSearchFilter = (filter != null) ? filter.toLowerCase().trim() : "";
        rebuildLayout();
    }

    public void expandAll() {
        if (rootDir != null) {
            expandAllRecursive(rootDir, 0, 3);
            rebuildLayout();
        }
    }

    private void expandAllRecursive(File dir, int depth, int maxDepth) {
        if (depth > maxDepth || dir == null || !dir.isDirectory()) return;
        expandedPaths.add(dir.getAbsolutePath());
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory() && !IGNORED_NAMES.contains(f.getName())) {
                    expandAllRecursive(f, depth + 1, maxDepth);
                }
            }
        }
    }

    public void collapseAll() {
        expandedPaths.clear();
        if (rootDir != null) {
            expandedPaths.add(rootDir.getAbsolutePath());
        }
        rebuildLayout();
    }

    private void rebuildLayout() {
        getChildren().clear();
        if (rootDir == null || !rootDir.exists()) {
            Label placeholder = new Label("No repository selected. Open a local Git repository to view tree.");
            placeholder.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-muted; -fx-padding: 30px;");
            getChildren().add(placeholder);
            return;
        }

        // Render recursive horizontal columns
        List<File> activePath = new ArrayList<>();
        activePath.add(rootDir);
        buildHorizontalColumns(rootDir, 0);
    }

    private void buildHorizontalColumns(File parentDir, int depth) {
        if (parentDir == null || !parentDir.isDirectory()) return;

        File[] children = parentDir.listFiles();
        if (children == null || children.length == 0) return;

        // Sort: directories first, then alphabetical
        Arrays.sort(children, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        VBox columnBox = new VBox(10);
        columnBox.setAlignment(Pos.CENTER_LEFT);
        columnBox.setMinWidth(210);
        columnBox.setPrefWidth(240);

        // Header for this level
        Label colHeader = new Label(depth == 0 ? "ROOT: " + parentDir.getName().toUpperCase() : parentDir.getName().toUpperCase() + "/");
        colHeader.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted; -fx-font-family: 'JetBrains Mono', Consolas, monospace;");
        columnBox.getChildren().add(colHeader);

        List<File> subDirsToRender = new ArrayList<>();

        for (File child : children) {
            if (IGNORED_NAMES.contains(child.getName())) continue;

            if (!currentSearchFilter.isEmpty()) {
                if (!childMatchesFilter(child, currentSearchFilter)) {
                    continue;
                }
            }

            FileItem item = createFileItem(child);
            VBox card = createNodeCard(item);

            columnBox.getChildren().add(card);

            if (child.isDirectory() && expandedPaths.contains(child.getAbsolutePath())) {
                subDirsToRender.add(child);
            }
        }

        getChildren().add(columnBox);

        // If there are expanded subdirectories, draw connecting line and next columns
        for (File subDir : subDirsToRender) {
            HBox connector = createHorizontalLine();
            getChildren().add(connector);
            buildHorizontalColumns(subDir, depth + 1);
        }
    }

    private boolean childMatchesFilter(File f, String filter) {
        if (f.getName().toLowerCase().contains(filter)) return true;
        if (f.isDirectory()) {
            File[] subs = f.listFiles();
            if (subs != null) {
                for (File s : subs) {
                    if (childMatchesFilter(s, filter)) return true;
                }
            }
        }
        return false;
    }

    private FileItem createFileItem(File f) {
        String status = null;
        if (gitStatus != null && rootDir != null) {
            String rel = rootDir.toPath().relativize(f.toPath()).toString().replace('\\', '/');
            if (gitStatus.isFileModified(rel)) {
                status = "MODIFIED";
            } else if (gitStatus.isFileStaged(rel)) {
                status = "STAGED";
            } else if (gitStatus.isFileUntracked(rel)) {
                status = "UNTRACKED";
            }
        }
        return new FileItem(f, status);
    }

    private VBox createNodeCard(FileItem item) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color: #141417; -fx-border-color: #27272a; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 7px 11px; -fx-cursor: hand;");

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(item.getIconGlyph());
        icon.setStyle("-fx-font-size: 13px;");

        Label name = new Label(item.getDisplayName());
        name.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #f4f4f5; -fx-font-family: 'JetBrains Mono', Consolas, monospace;");
        name.setMaxWidth(140);
        name.setWrapText(false);

        topRow.getChildren().addAll(icon, name);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().add(spacer);

        // If directory, show expand/collapse button
        if (item.isDirectory()) {
            boolean isExpanded = expandedPaths.contains(item.getFile().getAbsolutePath());
            Label toggle = new Label(isExpanded ? "−" : "+");
            toggle.setStyle("-fx-background-color: rgba(245, 158, 11, 0.18); -fx-text-fill: #fbbf24; -fx-padding: 1px 6px; -fx-background-radius: 4px; -fx-font-size: 10px; -fx-font-weight: 800;");
            topRow.getChildren().add(toggle);
        } else {
            Label size = new Label(item.getFormattedSize());
            size.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted; -fx-font-family: 'JetBrains Mono', Consolas, monospace;");
            topRow.getChildren().add(size);
        }

        card.getChildren().add(topRow);

        // Git Status indicator badge if modified, staged, or untracked
        if (item.getGitStatus() != null) {
            HBox statusRow = new HBox(6);
            statusRow.setAlignment(Pos.CENTER_LEFT);

            Label badge = new Label(item.getGitStatus());
            switch (item.getGitStatus()) {
                case "MODIFIED" -> badge.setStyle("-fx-background-color: rgba(245,158,11,0.18); -fx-text-fill: #fbbf24; -fx-font-size: 8px; -fx-font-weight: bold; -fx-padding: 1px 5px; -fx-background-radius: 3px;");
                case "STAGED" -> badge.setStyle("-fx-background-color: rgba(16,185,129,0.18); -fx-text-fill: #34d399; -fx-font-size: 8px; -fx-font-weight: bold; -fx-padding: 1px 5px; -fx-background-radius: 3px;");
                case "UNTRACKED" -> badge.setStyle("-fx-background-color: rgba(148,163,184,0.18); -fx-text-fill: #94a3b8; -fx-font-size: 8px; -fx-font-weight: bold; -fx-padding: 1px 5px; -fx-background-radius: 3px;");
            }
            statusRow.getChildren().add(badge);
            card.getChildren().add(statusRow);
        }

        card.setOnMouseEntered(e -> {
            if (card != selectedNodeCard) {
                card.setStyle("-fx-background-color: #1e1e23; -fx-border-color: #f59e0b; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 7px 11px; -fx-cursor: hand;");
            }
        });
        card.setOnMouseExited(e -> {
            if (card != selectedNodeCard) {
                card.setStyle("-fx-background-color: #141417; -fx-border-color: #27272a; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 7px 11px; -fx-cursor: hand;");
            }
        });

        card.setOnMouseClicked(e -> {
            if (selectedNodeCard != null) {
                selectedNodeCard.setStyle("-fx-background-color: #141417; -fx-border-color: #27272a; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 7px 11px; -fx-cursor: hand;");
            }
            card.setStyle("-fx-background-color: rgba(245, 158, 11, 0.18); -fx-border-color: #fbbf24; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 7px 11px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(245, 158, 11, 0.4), 8, 0.15, 0, 0);");
            selectedNodeCard = card;

            if (item.isDirectory()) {
                String path = item.getFile().getAbsolutePath();
                if (expandedPaths.contains(path)) {
                    expandedPaths.remove(path);
                } else {
                    expandedPaths.add(path);
                }
                rebuildLayout();
            }

            if (onFileSelectedListener != null) {
                onFileSelectedListener.accept(item);
            }
        });

        return card;
    }

    private HBox createHorizontalConnector() {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(24);

        Line line = new Line(0, 0, 20, 0);
        line.setStroke(Color.web("#38bdf8", 0.5));
        line.setStrokeWidth(1.5);
        line.getStrokeDashArray().addAll(4.0, 3.0);

        box.getChildren().add(line);
        return box;
    }

    private HBox createHorizontalLine() {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(22);

        Line line = new Line(0, 0, 18, 0);
        line.setStroke(Color.web("#a855f7", 0.6));
        line.setStrokeWidth(1.5);
        line.getStrokeDashArray().addAll(3.0, 3.0);

        box.getChildren().add(line);
        return box;
    }
}

