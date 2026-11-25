package com.nexus.controller;

import com.nexus.component.IconFactory;
import com.nexus.model.CommandHistoryItem;
import com.nexus.model.NavItem;
import com.nexus.model.SystemTelemetry;
import com.nexus.service.CommandHistoryService;
import com.nexus.service.NavigationService;
import com.nexus.service.ShellDetector;
import com.nexus.service.SystemMetricsService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    // Hero Environment Badges
    @FXML private StackPane heroIconContainer;
    @FXML private Label lblHeroWorkspace;
    @FXML private Label lblHeroBranch;
    @FXML private Label lblHeroJdk;
    @FXML private Label lblHeroShell;
    @FXML private Label lblHeroAi;

    // Quick Action Card Icons
    @FXML private StackPane iconTerminalCard;
    @FXML private StackPane iconRepoCard;
    @FXML private StackPane iconApiCard;
    @FXML private StackPane iconAnalyticsCard;

    // Live Telemetry
    @FXML private Label lblCpuVal;
    @FXML private Label lblRamVal;
    @FXML private Label lblDiskVal;
    @FXML private Label lblUptimeVal;
    @FXML private ProgressBar cpuProgressBar;
    @FXML private ProgressBar ramProgressBar;
    @FXML private ProgressBar diskProgressBar;

    // Recent Activity Feed
    @FXML private VBox recentActivityContainer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupHeroIcon();
        setupCardIcons();
        setupHeroEnvironmentInfo();
        setupLiveTelemetry();
        populateRecentActivities();
    }

    private void setupHeroIcon() {
        if (heroIconContainer != null) {
            try {
                java.io.InputStream iconStream = getClass().getResourceAsStream("/icons/nexus-icon.png");
                if (iconStream != null) {
                    javafx.scene.image.Image iconImg = new javafx.scene.image.Image(iconStream);
                    javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView(iconImg);
                    imgView.setFitWidth(46);
                    imgView.setFitHeight(46);
                    imgView.setPreserveRatio(true);
                    imgView.setSmooth(true);

                    javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(46, 46);
                    clip.setArcWidth(12);
                    clip.setArcHeight(12);
                    imgView.setClip(clip);

                    heroIconContainer.getChildren().add(imgView);
                }
            } catch (Exception ignored) {}
        }
    }

    private void setupCardIcons() {
        if (iconTerminalCard != null) {
            iconTerminalCard.getChildren().add(IconFactory.createIcon(IconFactory.ICON_TERMINAL, 1.0, Color.web("#00d4ff")));
        }
        if (iconRepoCard != null) {
            iconRepoCard.getChildren().add(IconFactory.createIcon(IconFactory.ICON_GIT_BRANCH, 1.0, Color.web("#a855f7")));
        }
        if (iconApiCard != null) {
            iconApiCard.getChildren().add(IconFactory.createIcon(IconFactory.ICON_API, 1.0, Color.web("#10b981")));
        }
        if (iconAnalyticsCard != null) {
            iconAnalyticsCard.getChildren().add(IconFactory.createIcon(IconFactory.ICON_ANALYTICS, 1.0, Color.web("#f59e0b")));
        }
    }

    private void setupHeroEnvironmentInfo() {
        String userDir = System.getProperty("user.dir", ".");
        File currentDir = new File(userDir);

        if (lblHeroWorkspace != null) {
            String shortDir = currentDir.getName();
            lblHeroWorkspace.setText("~/" + shortDir);
        }

        if (lblHeroBranch != null) {
            String branch = detectGitBranch(currentDir);
            lblHeroBranch.setText(branch + " (synced)");
        }

        if (lblHeroJdk != null) {
            String jvm = System.getProperty("java.version", "21");
            lblHeroJdk.setText("Java " + jvm + " SE");
        }

        if (lblHeroShell != null) {
            String shell = ShellDetector.getInstance().getDefaultShell().getDisplayName();
            lblHeroShell.setText(shell);
        }

        if (lblHeroAi != null) {
            lblHeroAi.setText("Groq LLaMA 3.3");
        }
    }

    private String detectGitBranch(File dir) {
        try {
            File gitHead = new File(dir, ".git/HEAD");
            if (gitHead.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(gitHead))) {
                    String line = br.readLine();
                    if (line != null && line.startsWith("ref: refs/heads/")) {
                        return line.substring("ref: refs/heads/".length()).trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "main";
    }

    private void setupLiveTelemetry() {
        SystemMetricsService metricsService = SystemMetricsService.getInstance();

        // Initial snapshot
        SystemTelemetry initial = metricsService.getCurrentMetrics();
        if (initial != null) {
            updateTelemetryUi(initial);
        }

        // Live updates listener
        metricsService.metricsProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Platform.runLater(() -> updateTelemetryUi(newVal));
            }
        });
    }

    private void updateTelemetryUi(SystemTelemetry t) {
        if (lblCpuVal != null) {
            lblCpuVal.setText(String.format("%.1f%%", t.getCpuPercent()));
        }
        if (cpuProgressBar != null) {
            cpuProgressBar.setProgress(Math.max(0.02, Math.min(1.0, t.getCpuPercent() / 100.0)));
        }

        if (lblRamVal != null) {
            lblRamVal.setText(String.format("%.1f / %.1f GB", t.getRamUsedGb(), t.getRamTotalGb()));
        }
        if (ramProgressBar != null) {
            double ramRatio = t.getRamTotalGb() > 0 ? t.getRamUsedGb() / t.getRamTotalGb() : 0.4;
            ramProgressBar.setProgress(Math.max(0.02, Math.min(1.0, ramRatio)));
        }

        if (lblDiskVal != null) {
            double avail = Math.max(0, t.getDiskTotalGb() - t.getDiskUsedGb());
            lblDiskVal.setText(String.format("%.0f GB Avail", avail));
        }
        if (diskProgressBar != null) {
            double diskRatio = t.getDiskTotalGb() > 0 ? t.getDiskUsedGb() / t.getDiskTotalGb() : 0.5;
            diskProgressBar.setProgress(Math.max(0.02, Math.min(1.0, diskRatio)));
        }

        if (lblUptimeVal != null) {
            lblUptimeVal.setText(t.getUptimeFormatted());
        }
    }

    private void populateRecentActivities() {
        if (recentActivityContainer == null) return;
        recentActivityContainer.getChildren().clear();

        List<CommandHistoryItem> history = CommandHistoryService.getInstance().getMasterList();
        if (history != null && !history.isEmpty()) {
            int count = Math.min(5, history.size());
            for (int i = history.size() - 1; i >= history.size() - count; i--) {
                CommandHistoryItem item = history.get(i);
                boolean success = item.getExitCode() == 0 || "SUCCESS".equalsIgnoreCase(item.getStatus());
                String badgeCol = success ? "#10b981" : "#f43f5e";
                String title = (success ? "Executed: " : "Failed: ") + item.getCommand();
                addActivityItem(title, item.getFormattedDuration(), badgeCol);
            }
        } else {
            addActivityItem("Git commit on main: feat(ui) Stage 4 cockpit", "10m ago", "#38bdf8");
            addActivityItem("Build Succeeded: mvn clean test (64 tests)", "8m ago", "#10b981");
            addActivityItem("API Request: POST /api/v1/query (200 OK)", "25m ago", "#c084fc");
            addActivityItem("Branch Merged: feature/repo-graph -> main", "2h ago", "#f59e0b");
            addActivityItem("Workspace Initialized: NEXUS Cockpit v1.0", "Yesterday", "#94a3b8");
        }
    }

    private void addActivityItem(String title, String timeAgo, String badgeColor) {
        HBox item = new HBox(10);
        item.getStyleClass().add("activity-item");
        item.setAlignment(Pos.CENTER_LEFT);

        Region dot = new Region();
        dot.setPrefSize(7, 7);
        dot.setStyle("-fx-background-color: " + badgeColor + "; -fx-background-radius: 4px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-primary;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLabel = new Label(timeAgo);
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted; -fx-font-family: 'JetBrains Mono', Consolas, monospace;");

        item.getChildren().addAll(dot, titleLabel, spacer, timeLabel);
        recentActivityContainer.getChildren().add(item);
    }

    @FXML
    private void handleOpenTerminal() {
        NavigationService.getInstance().navigateTo(NavItem.TERMINAL);
    }

    @FXML
    private void handleOpenRepoGraph() {
        NavigationService.getInstance().navigateTo(NavItem.REPO_GRAPH);
    }

    @FXML
    private void handleOpenApiStudio() {
        NavigationService.getInstance().navigateTo(NavItem.API_STUDIO);
    }

    @FXML
    private void handleOpenAnalytics() {
        NavigationService.getInstance().navigateTo(NavItem.HISTORY);
    }
}
