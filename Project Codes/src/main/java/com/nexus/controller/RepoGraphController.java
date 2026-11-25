package com.nexus.controller;

import com.nexus.component.HorizontalRepoTreeView;
import com.nexus.model.FileItem;
import com.nexus.model.GitCommitNode;
import com.nexus.model.GitStatusResult;
import com.nexus.service.JGitService;
import com.nexus.service.MockDataService;
import com.nexus.util.UiUtils;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

public class RepoGraphController implements Initializable {

    // Toolbar
    @FXML private Button btnBrowseFolder;
    @FXML private Label lblSelectedFolderPath;
    @FXML private ComboBox<String> branchSelector;
    @FXML private Button btnCheckoutBranch;
    @FXML private TextField searchCommitInput;
    @FXML private Button btnViewTree;
    @FXML private Button btnViewCommits;
    @FXML private Button btnZoomOut;
    @FXML private Label lblZoomLevel;
    @FXML private Button btnZoomIn;
    @FXML private Button btnResetZoom;
    @FXML private Button btnRefreshRepo;

    // SplitPane
    @FXML private SplitPane mainSplitPane;

    // Project Tree (Left Pane)
    @FXML private VBox projectTreeContainer;
    @FXML private Label lblCurrentProjectName;
    @FXML private Button btnCollapseAll;
    @FXML private Button btnExpandAll;
    @FXML private Button btnRefreshTree;
    @FXML private TextField treeFilterInput;
    @FXML private TreeView<FileItem> projectTreeView;
    @FXML private Label lblTreeStats;
    @FXML private Label lblGitBranchBadge;

    // Center Canvas
    @FXML private ScrollPane graphScrollPane;
    @FXML private VBox graphCanvasContainer;
    @FXML private Label lblCanvasTitle;
    @FXML private Label lblRepoStatus;
    @FXML private Label lblActiveRepoPath;
    @FXML private StackPane canvasStackPane;
    @FXML private VBox horizontalTreeWrapper;
    @FXML private VBox horizontalNodesContainer;

    // Inspector (Right Pane)
    @FXML private VBox inspectorContainer;
    @FXML private TabPane inspectorTabPane;
    @FXML private Tab tabFilePreview;
    @FXML private Tab tabCommitInspector;
    @FXML private Tab tabGitStatus;

    // Tab 1: File Preview
    @FXML private Label lblFileIcon;
    @FXML private Label lblFileName;
    @FXML private Label lblFileType;
    @FXML private Label lblFileSize;
    @FXML private Label lblFileLineCount;
    @FXML private Label lblFileGitStatusBadge;
    @FXML private TextArea filePreviewArea;
    @FXML private Button btnCopyFileContent;
    @FXML private Button btnExplainFileAi;
    @FXML private Button btnOpenFileTerminal;

    // Tab 2: Commit Inspector
    @FXML private Label lblInspectorHash;
    @FXML private Label lblInspectorBranch;
    @FXML private Label lblInspectorMessage;
    @FXML private Label lblInspectorAuthor;
    @FXML private Label lblInspectorDate;
    @FXML private ListView<String> changedFilesList;
    @FXML private Button btnCopySha;
    @FXML private Button btnViewDiff;
    @FXML private Button btnExplainCommitAi;

    // Tab 3: Git Status
    @FXML private Label lblModifiedCount;
    @FXML private ListView<String> modifiedFilesList;
    @FXML private Label lblStagedCount;
    @FXML private ListView<String> stagedFilesList;
    @FXML private Label lblUntrackedCount;
    @FXML private ListView<String> untrackedFilesList;
    @FXML private Button btnExplainChangesAi;

    // Internal State
    private File currentProjectDirectory;
    private GitStatusResult currentGitStatus;
    private List<GitCommitNode> allCommits = new ArrayList<>();
    private GitCommitNode selectedCommit;
    private FileItem selectedFileItem;
    private double currentScale = 1.0;
    private Consumer<String> onAskAiListener;
    private VBox selectedNodeView = null;
    private TreeItem<FileItem> originalRootTreeItem;
    private HorizontalRepoTreeView horizontalTreeView;
    private boolean isTreeMode = true;

    private static final Set<String> IGNORED_FOLDERS = Set.of(
            ".git", ".idea", ".vscode", "target", "build", "node_modules", ".gradle", ".system_generated"
    );

    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            "jar", "war", "class", "exe", "dll", "so", "dylib", "zip", "tar", "gz", "7z",
            "png", "jpg", "jpeg", "gif", "ico", "bmp", "pdf"
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupDarkListViewStyles();
        setupProjectTreeViewStyle();
        setupHorizontalTreeViewComponent();

        branchSelector.setOnAction(e -> handleBranchSelectionChanged());
        searchCommitInput.textProperty().addListener((obs, oldV, newV) -> handleSearchFilter(newV));
        treeFilterInput.textProperty().addListener((obs, oldV, newV) -> filterProjectTree(newV));

        // Default project directory
        File defaultDir = new File(System.getProperty("user.dir"));
        loadProjectFolder(defaultDir);
    }

    public void setOnAskAiListener(Consumer<String> onAskAiListener) {
        this.onAskAiListener = onAskAiListener;
    }

    private void setupHorizontalTreeViewComponent() {
        horizontalTreeView = new HorizontalRepoTreeView();
        horizontalTreeView.setOnFileSelectedListener(this::previewFile);
        horizontalTreeWrapper.getChildren().add(horizontalTreeView);
    }

    private void setupDarkListViewStyles() {
        setupDarkCellFactory(changedFilesList);
        setupDarkCellFactory(modifiedFilesList);
        setupDarkCellFactory(stagedFilesList);
        setupDarkCellFactory(untrackedFilesList);

        changedFilesList.setOnMouseClicked(e -> {
            String selected = changedFilesList.getSelectionModel().getSelectedItem();
            if (selected != null && currentProjectDirectory != null) {
                File f = new File(currentProjectDirectory, selected.trim());
                if (f.exists() && f.isFile()) {
                    previewFile(new FileItem(f, "CHANGED"));
                }
            }
        });
        modifiedFilesList.setOnMouseClicked(e -> {
            String selected = modifiedFilesList.getSelectionModel().getSelectedItem();
            if (selected != null && currentProjectDirectory != null) {
                previewFile(new FileItem(new File(currentProjectDirectory, selected), "MODIFIED"));
            }
        });
        stagedFilesList.setOnMouseClicked(e -> {
            String selected = stagedFilesList.getSelectionModel().getSelectedItem();
            if (selected != null && currentProjectDirectory != null) {
                previewFile(new FileItem(new File(currentProjectDirectory, selected), "STAGED"));
            }
        });
        untrackedFilesList.setOnMouseClicked(e -> {
            String selected = untrackedFilesList.getSelectionModel().getSelectedItem();
            if (selected != null && currentProjectDirectory != null) {
                previewFile(new FileItem(new File(currentProjectDirectory, selected), "UNTRACKED"));
            }
        });
    }

    private void setupDarkCellFactory(ListView<String> listView) {
        listView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #101013; -fx-border-color: transparent;");
                } else {
                    setText("  " + item);
                    Label icon = new Label(item.endsWith("/") ? "📁" : "📄");
                    icon.setStyle("-fx-font-size: 11px;");
                    setGraphic(icon);
                    setStyle("-fx-background-color: #101013; -fx-text-fill: #e4e4e7; -fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 11px; -fx-padding: 4px 8px; -fx-font-weight: 800;");
                }
            }
        });
    }

    private void setupProjectTreeViewStyle() {
        projectTreeView.setCellFactory(tv -> new TreeCell<FileItem>() {
            @Override
            protected void updateItem(FileItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox box = new HBox(6);
                    box.setAlignment(Pos.CENTER_LEFT);

                    Label icon = new Label(item.getIconGlyph());
                    icon.setStyle("-fx-font-size: 12px;");

                    Label name = new Label(item.getDisplayName());
                    name.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 11px; -fx-text-fill: #e2e8f0;");

                    box.getChildren().addAll(icon, name);

                    if (item.getGitStatus() != null) {
                        Label statusBadge = new Label(item.getGitStatus().equals("MODIFIED") ? "M" : (item.getGitStatus().equals("STAGED") ? "S" : "U"));
                        statusBadge.setStyle(switch (item.getGitStatus()) {
                            case "MODIFIED" -> "-fx-background-color: rgba(245,158,11,0.2); -fx-text-fill: #fbbf24; -fx-font-size: 9px; -fx-padding: 0 4px; -fx-background-radius: 3px; -fx-font-weight: bold;";
                            case "STAGED" -> "-fx-background-color: rgba(16,185,129,0.2); -fx-text-fill: #34d399; -fx-font-size: 9px; -fx-padding: 0 4px; -fx-background-radius: 3px; -fx-font-weight: bold;";
                            default -> "-fx-background-color: rgba(148,163,184,0.2); -fx-text-fill: #94a3b8; -fx-font-size: 9px; -fx-padding: 0 4px; -fx-background-radius: 3px;";
                        });
                        box.getChildren().add(statusBadge);
                    }

                    setText(null);
                    setGraphic(box);
                    setStyle("-fx-background-color: transparent; -fx-padding: 2px 4px;");
                }
            }
        });

        projectTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                previewFile(newVal.getValue());
            }
        });
    }

    @FXML
    public void handleBrowseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Git Repository or Project Folder");

        if (currentProjectDirectory != null && currentProjectDirectory.exists()) {
            chooser.setInitialDirectory(currentProjectDirectory);
        }

        Window window = btnBrowseFolder.getScene() != null ? btnBrowseFolder.getScene().getWindow() : null;
        File selected = chooser.showDialog(window);

        if (selected != null && selected.isDirectory()) {
            loadProjectFolder(selected);
        }
    }

    public void loadProjectFolder(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return;
        }

        this.currentProjectDirectory = dir;
        lblCurrentProjectName.setText(dir.getName().toUpperCase());
        lblSelectedFolderPath.setText(dir.getName().toUpperCase());
        lblSelectedFolderPath.setTooltip(new Tooltip(dir.getAbsolutePath()));
        lblActiveRepoPath.setText(dir.getAbsolutePath());

        // 1. Query JGit
        JGitService git = JGitService.getInstance();
        boolean isGit = git.isGitRepository(dir);

        if (isGit) {
            lblRepoStatus.setText("● LIVE JGIT REPOSITORY");
            lblRepoStatus.setStyle("-fx-font-size: 10px; -fx-text-fill: #10b981; -fx-font-weight: bold;");

            String currentBranch = git.getCurrentBranch(dir);
            lblGitBranchBadge.setText(currentBranch);

            List<String> branches = git.listBranches(dir);
            List<String> branchItems = new ArrayList<>();
            branchItems.add("All Branches");
            branchItems.addAll(branches);

            branchSelector.setItems(FXCollections.observableArrayList(branchItems));
            branchSelector.setValue(currentBranch);

            // JGit Status
            this.currentGitStatus = git.getStatus(dir);
            updateGitStatusTab(this.currentGitStatus);

            // JGit Commits
            this.allCommits = git.getCommitHistory(dir, currentBranch, 40);
            if (this.allCommits.isEmpty()) {
                this.allCommits = MockDataService.getInstance().getGitCommitHistory();
            }
        } else {
            lblRepoStatus.setText("● LOCAL WORKSPACE (NO GIT)");
            lblRepoStatus.setStyle("-fx-font-size: 10px; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");
            lblGitBranchBadge.setText("untracked");

            branchSelector.setItems(FXCollections.observableArrayList("local-workspace"));
            branchSelector.setValue("local-workspace");

            this.currentGitStatus = GitStatusResult.clean("local-workspace");
            updateGitStatusTab(this.currentGitStatus);

            this.allCommits = List.of(
                    new GitCommitNode("HEAD-LOCAL", "local-workspace", System.getProperty("user.name", "Developer"),
                            "Active Workspace", "Local directory initialized: " + dir.getName(), false,
                            Collections.emptyList(), List.of("workspace"), List.of(dir.getName() + " (Project Tree Populated)"), 1, 0)
            );
        }

        // 2. Build Tree structures
        buildProjectTree(dir);
        horizontalTreeView.renderRepository(dir, this.currentGitStatus);
        renderCommitTopologyGraph();
        autoPreviewDefaultFile(dir);

        if (!allCommits.isEmpty()) {
            selectCommit(allCommits.get(0));
        }
    }

    private void updateGitStatusTab(GitStatusResult status) {
        if (status == null) return;

        modifiedFilesList.setItems(FXCollections.observableArrayList(status.getModified()));
        lblModifiedCount.setText("Modified (" + status.getModified().size() + ")");

        stagedFilesList.setItems(FXCollections.observableArrayList(status.getStaged()));
        lblStagedCount.setText("Staged (" + status.getStaged().size() + ")");

        untrackedFilesList.setItems(FXCollections.observableArrayList(status.getUntracked()));
        lblUntrackedCount.setText("Untracked (" + status.getUntracked().size() + ")");
    }

    @FXML
    private void handleRefreshGitStatus() {
        if (currentProjectDirectory != null) {
            JGitService git = JGitService.getInstance();
            if (git.isGitRepository(currentProjectDirectory)) {
                this.currentGitStatus = git.getStatus(currentProjectDirectory);
                updateGitStatusTab(this.currentGitStatus);
                horizontalTreeView.renderRepository(currentProjectDirectory, this.currentGitStatus);
            }
        }
    }

    @FXML
    private void handleCheckoutBranch() {
        String targetBranch = branchSelector.getValue();
        if (targetBranch == null || "All Branches".equalsIgnoreCase(targetBranch)) {
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Git Branch Checkout");
        confirmAlert.setHeaderText("Checkout Branch: " + targetBranch);
        confirmAlert.setContentText("Are you sure you want to switch the working tree to branch [" + targetBranch + "]?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                JGitService.getInstance().checkoutBranch(currentProjectDirectory, targetBranch);
                loadProjectFolder(currentProjectDirectory);
            } catch (Exception e) {
                Alert err = new Alert(Alert.AlertType.ERROR, "Failed to checkout branch: " + e.getMessage());
                err.showAndWait();
            }
        }
    }

    @FXML
    private void handleShowTreeMode() {
        isTreeMode = true;
        btnViewTree.getStyleClass().add("btn-nexus-ai-active");
        btnViewCommits.getStyleClass().remove("btn-nexus-ai-active");
        lblCanvasTitle.setText("HORIZONTAL REPOSITORY TREE");
        horizontalTreeWrapper.setVisible(true);
        horizontalTreeWrapper.setManaged(true);
        horizontalNodesContainer.setVisible(false);
        horizontalNodesContainer.setManaged(false);
        graphScrollPane.setFitToWidth(false);
    }

    @FXML
    private void handleShowCommitsMode() {
        isTreeMode = false;
        btnViewCommits.getStyleClass().add("btn-nexus-ai-active");
        btnViewTree.getStyleClass().remove("btn-nexus-ai-active");
        lblCanvasTitle.setText("VERTICAL COMMIT TIMELINE");
        horizontalTreeWrapper.setVisible(false);
        horizontalTreeWrapper.setManaged(false);
        horizontalNodesContainer.setVisible(true);
        horizontalNodesContainer.setManaged(true);
        graphScrollPane.setFitToWidth(true);
        renderCommitTopologyGraph();
    }

    public void previewFile(FileItem item) {
        if (item == null || item.getFile() == null) return;
        inspectorTabPane.getSelectionModel().select(tabFilePreview);
        renderFileContent(item.getFile(), item);
    }

    private void autoPreviewDefaultFile(File dir) {
        if (dir == null || !dir.isDirectory()) return;
        File readme = new File(dir, "README.md");
        if (readme.exists() && readme.isFile()) {
            renderFileContent(readme, new FileItem(readme, null));
            return;
        }
        File pom = new File(dir, "pom.xml");
        if (pom.exists() && pom.isFile()) {
            renderFileContent(pom, new FileItem(pom, null));
            return;
        }
        File[] files = dir.listFiles(f -> f.isFile() && !f.getName().startsWith("."));
        if (files != null && files.length > 0) {
            renderFileContent(files[0], new FileItem(files[0], null));
        }
    }

    private void renderFileContent(File file, FileItem item) {
        this.selectedFileItem = item;
        lblFileIcon.setText(item.getIconGlyph());
        lblFileName.setText(item.getDisplayName());
        lblFileType.setText(item.isDirectory() ? "Directory / Package" : (item.getExtension().isEmpty() ? "File" : item.getExtension().toUpperCase() + " File"));
        lblFileSize.setText(item.getFormattedSize());

        if (item.getGitStatus() != null) {
            lblFileGitStatusBadge.setVisible(true);
            lblFileGitStatusBadge.setText(item.getGitStatus());
        } else {
            lblFileGitStatusBadge.setVisible(false);
        }

        if (item.isDirectory()) {
            lblFileLineCount.setText("Directory");
            filePreviewArea.setText("📁 Directory: " + file.getAbsolutePath() + "\nContains: " + (file.listFiles() != null ? file.listFiles().length : 0) + " items.");
            return;
        }

        // Binary file check
        if (BINARY_EXTENSIONS.contains(item.getExtension().toLowerCase())) {
            lblFileLineCount.setText("Binary");
            filePreviewArea.setText("🔒 [Binary File: Preview disabled for binary safety]\n\n" +
                    "File: " + file.getName() + "\n" +
                    "Path: " + file.getAbsolutePath() + "\n" +
                    "Size: " + item.getFormattedSize() + "\n\n" +
                    "Binary contents are shielded to prevent terminal or encoding distortion.");
            return;
        }

        // Large file safeguard (> 1 MB)
        if (file.length() > 1024 * 1024) {
            lblFileLineCount.setText("> 1 MB");
            try {
                byte[] sample = Files.readAllBytes(file.toPath());
                String previewText = new String(sample, 0, Math.min(sample.length, 50000), StandardCharsets.UTF_8);
                filePreviewArea.setText("⚠️ [Large File Safeguard: File exceeds 1 MB. Showing first 50 KB preview]:\n\n" + previewText);
            } catch (Exception e) {
                filePreviewArea.setText("Error reading large file: " + e.getMessage());
            }
            return;
        }

        // Standard text file preview (Compact line numbering and UTF-8 safe)
        try {
            byte[] rawBytes = Files.readAllBytes(file.toPath());
            String content = new String(rawBytes, StandardCharsets.UTF_8);
            String[] lines = content.split("\r?\n", -1);
            lblFileLineCount.setText(lines.length + " lines");

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                sb.append(String.format("%3d | %s\n", (i + 1), lines[i]));
            }
            filePreviewArea.setText(sb.toString());
        } catch (Exception e) {
            filePreviewArea.setText("Unable to preview text file: " + e.getMessage());
        }
    }

    private void renderCommitTopologyGraph() {
        horizontalNodesContainer.getChildren().clear();

        String selectedBranch = branchSelector.getValue();
        String query = searchCommitInput.getText() != null ? searchCommitInput.getText().toLowerCase().trim() : "";

        List<GitCommitNode> filtered = allCommits.stream().filter(c -> {
            boolean matchBranch = selectedBranch == null || "All Branches".equals(selectedBranch) ||
                    c.getBranch().equalsIgnoreCase(selectedBranch);
            boolean matchQuery = query.isEmpty() ||
                    c.getHash().toLowerCase().contains(query) ||
                    c.getMessage().toLowerCase().contains(query) ||
                    c.getAuthor().toLowerCase().contains(query);
            return matchBranch && matchQuery;
        }).toList();

        for (int i = 0; i < filtered.size(); i++) {
            GitCommitNode commit = filtered.get(i);
            HBox timelineRow = createVerticalCommitRow(commit, i == filtered.size() - 1);
            horizontalNodesContainer.getChildren().add(timelineRow);
        }
    }

    private HBox createVerticalCommitRow(GitCommitNode commit, boolean isLast) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.TOP_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(row, Priority.ALWAYS);

        Color branchCol = getBranchColor(commit.getBranch());

        // 1. Left Spine (Dot + Vertical connecting line)
        VBox spine = new VBox(0);
        spine.setAlignment(Pos.TOP_CENTER);
        spine.setMinWidth(28);
        spine.setPrefWidth(28);
        spine.setMaxWidth(28);

        Circle dot = new Circle(7);
        dot.setFill(branchCol);
        dot.setStroke(Color.web("#070709"));
        dot.setStrokeWidth(2.0);

        Region line = new Region();
        line.setMinWidth(2);
        line.setPrefWidth(2);
        line.setMaxWidth(2);
        if (!isLast) {
            line.setStyle("-fx-background-color: " + toHex(branchCol) + "; -fx-opacity: 0.35;");
            VBox.setVgrow(line, Priority.ALWAYS);
            line.setMinHeight(56);
        } else {
            line.setMinHeight(20);
            line.setStyle("-fx-background-color: transparent;");
        }
        spine.getChildren().addAll(dot, line);

        // 2. Commit Card
        VBox card = createCommitNodeCard(commit, branchCol);
        HBox.setHgrow(card, Priority.ALWAYS);

        row.getChildren().addAll(spine, card);
        return row;
    }

    private VBox createCommitNodeCard(GitCommitNode commit, Color branchCol) {
        VBox card = new VBox(8);
        card.getStyleClass().add("commit-card-node");
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);

        if (selectedCommit != null && selectedCommit.getHash().equals(commit.getHash())) {
            card.getStyleClass().add("commit-card-node-selected");
            selectedNodeView = card;
        }

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label hashPill = new Label(commit.getShortHash());
        hashPill.getStyleClass().add("commit-hash-pill");
        hashPill.setStyle("-fx-background-color: #1e1e23; -fx-text-fill: -color-accent-gold; -fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 2px 7px; -fx-background-radius: 4px;");

        Label branchLabel = new Label(commit.getBranch());
        branchLabel.setStyle("-fx-background-color: " + toRgbaString(branchCol, 0.15) + "; -fx-text-fill: " + toHex(branchCol) + "; -fx-font-size: 10px; -fx-font-weight: 800; -fx-padding: 2px 7px; -fx-background-radius: 4px; -fx-font-family: 'JetBrains Mono', Consolas, monospace;");

        topRow.getChildren().addAll(hashPill, branchLabel);

        for (String tag : commit.getTags()) {
            Label tagLabel = new Label(tag);
            tagLabel.setStyle("-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10b981; -fx-font-size: 9.5px; -fx-padding: 2px 6px; -fx-background-radius: 3px; -fx-font-weight: 800;");
            topRow.getChildren().add(tagLabel);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().add(spacer);

        Label authorLabel = new Label("\uD83D\uDC64 " + commit.getAuthor());
        authorLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: -color-text-secondary;");

        Label dateLabel = new Label("🕒 " + commit.getDate());
        dateLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: -color-text-muted;");

        topRow.getChildren().addAll(authorLabel, dateLabel);

        // Commit message with conventional commit highlighting (Full text, wrapped, never cut)
        HBox msgBox = new HBox(6);
        msgBox.setAlignment(Pos.CENTER_LEFT);
        String msg = commit.getMessage();
        int colonIdx = msg.indexOf(':');
        if (colonIdx > 0 && colonIdx < 22) {
            String prefix = msg.substring(0, colonIdx + 1);
            String rest = msg.substring(colonIdx + 1).trim();

            Label prefixBadge = new Label(prefix);
            prefixBadge.setStyle(getCommitPrefixStyle(prefix));

            Label restLabel = new Label(rest);
            restLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #f1f5f9;");
            restLabel.setWrapText(true);
            restLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(restLabel, Priority.ALWAYS);

            msgBox.getChildren().addAll(prefixBadge, restLabel);
        } else {
            Label fullMsg = new Label(msg);
            fullMsg.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #f1f5f9;");
            fullMsg.setWrapText(true);
            fullMsg.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(fullMsg, Priority.ALWAYS);
            msgBox.getChildren().add(fullMsg);
        }

        // Bottom Row
        HBox bottomRow = new HBox(10);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        int filesCount = commit.getFilesChanged() != null ? commit.getFilesChanged().size() : 0;
        Label filesChip = new Label("\uD83D\uDCC4 " + filesCount + " files changed");
        filesChip.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 700; -fx-text-fill: -color-text-muted;");
        bottomRow.getChildren().add(filesChip);

        if (commit.getAdditions() > 0) {
            Label addChip = new Label("+" + commit.getAdditions());
            addChip.setStyle("-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10b981; -fx-font-size: 10px; -fx-font-weight: 800; -fx-padding: 1px 6px; -fx-background-radius: 3px; -fx-font-family: 'JetBrains Mono', Consolas, monospace;");
            bottomRow.getChildren().add(addChip);
        }
        if (commit.getDeletions() > 0) {
            Label delChip = new Label("-" + commit.getDeletions());
            delChip.setStyle("-fx-background-color: rgba(244,63,94,0.15); -fx-text-fill: #f43f5e; -fx-font-size: 10px; -fx-font-weight: 800; -fx-padding: 1px 6px; -fx-background-radius: 3px; -fx-font-family: 'JetBrains Mono', Consolas, monospace;");
            bottomRow.getChildren().add(delChip);
        }

        Region statSpacer = new Region();
        HBox.setHgrow(statSpacer, Priority.ALWAYS);
        Label clickHint = new Label("Inspect Diff →");
        clickHint.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 800; -fx-text-fill: -color-accent-gold;");
        bottomRow.getChildren().addAll(statSpacer, clickHint);

        card.getChildren().addAll(topRow, msgBox, bottomRow);

        card.setOnMouseClicked(e -> {
            if (selectedNodeView != null) {
                selectedNodeView.getStyleClass().remove("commit-card-node-selected");
            }
            card.getStyleClass().add("commit-card-node-selected");
            selectedNodeView = card;
            selectCommit(commit);
        });

        return card;
    }

    private String getCommitPrefixStyle(String prefix) {
        String lower = prefix.toLowerCase();
        if (lower.startsWith("feat")) {
            return "-fx-background-color: rgba(0,212,255,0.15); -fx-text-fill: #00d4ff; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 1px 5px; -fx-background-radius: 3px; -fx-font-family: 'JetBrains Mono', Consolas, monospace;";
        } else if (lower.startsWith("fix")) {
            return "-fx-background-color: rgba(244,63,94,0.15); -fx-text-fill: #f43f5e; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 1px 5px; -fx-background-radius: 3px; -fx-font-family: 'JetBrains Mono', Consolas, monospace;";
        } else if (lower.startsWith("style")) {
            return "-fx-background-color: rgba(168,85,247,0.15); -fx-text-fill: #c084fc; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 1px 5px; -fx-background-radius: 3px; -fx-font-family: 'JetBrains Mono', Consolas, monospace;";
        } else if (lower.startsWith("refactor")) {
            return "-fx-background-color: rgba(245,158,11,0.15); -fx-text-fill: #fbbf24; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 1px 5px; -fx-background-radius: 3px; -fx-font-family: 'JetBrains Mono', Consolas, monospace;";
        } else if (lower.startsWith("docs")) {
            return "-fx-background-color: rgba(56,189,248,0.15); -fx-text-fill: #38bdf8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 1px 5px; -fx-background-radius: 3px; -fx-font-family: 'JetBrains Mono', Consolas, monospace;";
        }
        return "-fx-background-color: rgba(148,163,184,0.15); -fx-text-fill: #94a3b8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 1px 5px; -fx-background-radius: 3px; -fx-font-family: 'JetBrains Mono', Consolas, monospace;";
    }

    private void selectCommit(GitCommitNode commit) {
        this.selectedCommit = commit;
        inspectorTabPane.getSelectionModel().select(tabCommitInspector);

        lblInspectorHash.setText(commit.getShortHash());
        lblInspectorBranch.setText(commit.getBranch());
        lblInspectorMessage.setText(commit.getMessage());
        lblInspectorAuthor.setText(commit.getAuthor());
        lblInspectorDate.setText(commit.getDate());

        changedFilesList.setItems(FXCollections.observableArrayList(commit.getFilesChanged()));
    }

    @FXML
    private void handleViewCommitDiff() {
        if (selectedCommit == null || currentProjectDirectory == null) return;

        String diff = JGitService.getInstance().getDiffForCommit(currentProjectDirectory, selectedCommit.getHash());

        Dialog<Void> diffDialog = new Dialog<>();
        diffDialog.setTitle("Git Diff — " + selectedCommit.getShortHash());
        diffDialog.setHeaderText("Commit Diff: " + selectedCommit.getMessage());

        TextArea diffArea = new TextArea(diff);
        diffArea.setEditable(false);
        diffArea.setPrefSize(720, 480);
        diffArea.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 11px; -fx-background-color: #0c1220; -fx-text-fill: #e2e8f0;");

        diffDialog.getDialogPane().setContent(diffArea);
        diffDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        diffDialog.showAndWait();
    }

    @FXML
    private void handleExplainCommitAi() {
        if (selectedCommit != null && onAskAiListener != null) {
            onAskAiListener.accept("Explain the git diff and architectural impact of commit [" +
                    selectedCommit.getShortHash() + "]: " + selectedCommit.getMessage());
        }
    }

    @FXML
    private void handleExplainFileAi() {
        if (selectedFileItem != null && onAskAiListener != null) {
            onAskAiListener.accept("Explain the architectural purpose and role of file [" +
                    selectedFileItem.getDisplayName() + "] located at: " + selectedFileItem.getFile().getAbsolutePath());
        }
    }

    @FXML
    private void handleExplainChangesAi() {
        if (onAskAiListener != null && currentGitStatus != null) {
            onAskAiListener.accept("Which files have changed in this repository? Give me a breakdown of modified, staged and untracked files.");
        }
    }

    @FXML
    private void handleOpenFileInTerminal() {
        if (selectedFileItem != null && onAskAiListener != null) {
            String targetDir = selectedFileItem.isDirectory() ?
                    selectedFileItem.getFile().getAbsolutePath() :
                    selectedFileItem.getFile().getParent();
            onAskAiListener.accept("cd \"" + targetDir + "\"");
        }
    }

    @FXML
    private void handleCopyFileContent() {
        if (filePreviewArea != null && !filePreviewArea.getText().isEmpty()) {
            UiUtils.copyToClipboard(filePreviewArea.getText());
            btnCopyFileContent.setText("Copied!");
            PauseTransition pt = new PauseTransition(Duration.seconds(1.5));
            pt.setOnFinished(e -> btnCopyFileContent.setText("Copy"));
            pt.play();
        }
    }

    @FXML
    private void handleCopySha() {
        if (selectedCommit != null) {
            UiUtils.copyToClipboard(selectedCommit.getHash());
            btnCopySha.setText("Copied!");
            PauseTransition pt = new PauseTransition(Duration.seconds(1.5));
            pt.setOnFinished(e -> btnCopySha.setText("Copy SHA"));
            pt.play();
        }
    }

    private void buildProjectTree(File rootDir) {
        FileItem rootItem = new FileItem(rootDir);
        TreeItem<FileItem> rootTreeItem = new TreeItem<>(rootItem);
        rootTreeItem.setExpanded(true);

        int[] counts = new int[]{0, 0};
        populateTreeChildren(rootDir, rootTreeItem, 0, 3, counts);

        this.originalRootTreeItem = rootTreeItem;
        projectTreeView.setRoot(rootTreeItem);
        lblTreeStats.setText(counts[0] + " files, " + counts[1] + " folders");
    }

    private void populateTreeChildren(File dir, TreeItem<FileItem> parentItem, int depth, int maxDepth, int[] counts) {
        if (depth > maxDepth || dir == null || !dir.isDirectory()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        Arrays.sort(files, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        for (File f : files) {
            if (IGNORED_FOLDERS.contains(f.getName())) continue;

            FileItem item = new FileItem(f);
            TreeItem<FileItem> treeItem = new TreeItem<>(item);

            if (f.isDirectory()) {
                counts[1]++;
                if (depth < 1) treeItem.setExpanded(true);
                populateTreeChildren(f, treeItem, depth + 1, maxDepth, counts);
            } else {
                counts[0]++;
            }

            parentItem.getChildren().add(treeItem);
        }
    }

    private void filterProjectTree(String query) {
        if (originalRootTreeItem == null) return;
        if (query == null || query.trim().isEmpty()) {
            projectTreeView.setRoot(originalRootTreeItem);
            return;
        }

        String filter = query.toLowerCase().trim();
        TreeItem<FileItem> filtered = filterTreeItem(originalRootTreeItem, filter);
        if (filtered != null) {
            filtered.setExpanded(true);
            projectTreeView.setRoot(filtered);
        } else {
            projectTreeView.setRoot(new TreeItem<>(new FileItem(new File("No matching files"))));
        }
    }

    private TreeItem<FileItem> filterTreeItem(TreeItem<FileItem> item, String filter) {
        if (item == null || item.getValue() == null) return null;

        boolean matches = item.getValue().getDisplayName().toLowerCase().contains(filter);
        TreeItem<FileItem> copy = new TreeItem<>(item.getValue());
        copy.setExpanded(true);

        boolean hasMatchingChild = false;
        for (TreeItem<FileItem> child : item.getChildren()) {
            TreeItem<FileItem> filteredChild = filterTreeItem(child, filter);
            if (filteredChild != null) {
                copy.getChildren().add(filteredChild);
                hasMatchingChild = true;
            }
        }

        return (matches || hasMatchingChild) ? copy : null;
    }

    private void handleSearchFilter(String query) {
        horizontalTreeView.setSearchFilter(query);
        renderCommitTopologyGraph();
    }

    private void handleBranchSelectionChanged() {
        renderCommitTopologyGraph();
    }

    @FXML
    private void handleCollapseAll() {
        if (projectTreeView.getRoot() != null) {
            collapseAllNodes(projectTreeView.getRoot());
            projectTreeView.getRoot().setExpanded(true);
        }
        horizontalTreeView.collapseAll();
    }

    private void collapseAllNodes(TreeItem<?> item) {
        if (item == null) return;
        item.setExpanded(false);
        for (TreeItem<?> child : item.getChildren()) {
            collapseAllNodes(child);
        }
    }

    @FXML
    private void handleExpandAll() {
        if (projectTreeView.getRoot() != null) {
            expandAllNodes(projectTreeView.getRoot());
        }
        horizontalTreeView.expandAll();
    }

    private void expandAllNodes(TreeItem<?> item) {
        if (item == null) return;
        item.setExpanded(true);
        for (TreeItem<?> child : item.getChildren()) {
            expandAllNodes(child);
        }
    }

    @FXML
    private void handleRefreshTree() {
        if (currentProjectDirectory != null) {
            buildProjectTree(currentProjectDirectory);
        }
    }

    @FXML
    private void handleZoomIn() {
        currentScale = Math.min(1.5, currentScale + 0.1);
        applyScale();
    }

    @FXML
    private void handleZoomOut() {
        currentScale = Math.max(0.6, currentScale - 0.1);
        applyScale();
    }

    @FXML
    private void handleResetZoom() {
        currentScale = 1.0;
        applyScale();
    }

    private void applyScale() {
        canvasStackPane.setScaleX(currentScale);
        canvasStackPane.setScaleY(currentScale);
        lblZoomLevel.setText((int) (currentScale * 100) + "%");
    }

    @FXML
    private void handleRefreshRepo() {
        if (currentProjectDirectory != null) {
            loadProjectFolder(currentProjectDirectory);
        }
    }

    private HBox createHorizontalConnector(String branch) {
        HBox box = new HBox(0);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(36);
        box.setMinWidth(36);
        box.setMaxWidth(36);
        box.setPrefHeight(118);

        Color branchCol = getBranchColor(branch);
        Line line = new Line(0, 0, 24, 0);
        line.setStroke(branchCol);
        line.setStrokeWidth(2.0);

        Label arrow = new Label("\u25B6"); // ▶
        arrow.setStyle("-fx-font-size: 8px; -fx-text-fill: " + toHex(branchCol) + "; -fx-padding: 0 0 0 -2px;");

        box.getChildren().addAll(line, arrow);
        return box;
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    private String toRgbaString(Color c, double opacity) {
        return String.format(java.util.Locale.US, "rgba(%d,%d,%d,%.2f)",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255),
                opacity);
    }

    private Color getBranchColor(String branch) {
        if ("main".equalsIgnoreCase(branch) || "master".equalsIgnoreCase(branch)) {
            return Color.web("#fbbf24");
        } else if (branch.contains("api") || branch.contains("feat")) {
            return Color.web("#a855f7");
        } else {
            return Color.web("#10b981");
        }
    }
}
