package com.nexus.controller;

import com.nexus.component.IconFactory;
import com.nexus.config.AppConfig;
import com.nexus.model.NavItem;
import com.nexus.service.NavigationService;
import com.nexus.util.UiUtils;
import com.nexus.util.ViewLoader;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private BorderPane mainBorderPane;
    @FXML private StackPane searchIconContainer;
    @FXML private TextField globalSearchField;
    @FXML private Button btnToggleFullscreen;
    @FXML private Button btnToggleAi;

    @FXML private StackPane sidebarContainer;
    @FXML private StackPane centralWorkspaceContainer;
    @FXML private StackPane aiPanelContainer;
    @FXML private StackPane bottomBarContainer;
    @FXML private StackPane modalOverlay;

    private final Map<NavItem, Node> viewNodes = new EnumMap<>(NavItem.class);
    private SidebarController sidebarController;
    private AiAssistantController aiAssistantController;
    private TerminalController terminalController;
    private RepoGraphController repoGraphController;
    private ApiStudioController apiStudioController;

    private boolean isAiPanelExpanded = true;
    private boolean isAiPanelAnimating = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupGlobalSearchIcon();
        setupAiToggleButton();
        setupFullscreenButton();
        setupAiPanelClipping();

        loadSidebar();
        loadBottomBar();
        loadAiAssistant();
        loadWorkspaceViews();

        // Listen for navigation changes
        NavigationService.getInstance().addNavigationListener(this::switchWorkspaceView);

        // Initial view
        switchWorkspaceView(NavItem.HOME);

        // Global hotkey support (Ctrl+K for search, F11 for Fullscreen)
        Platform.runLater(this::setupGlobalKeyAccelerators);
    }

    private void setupGlobalSearchIcon() {
        if (searchIconContainer != null) {
            searchIconContainer.getChildren().add(IconFactory.createIcon(IconFactory.ICON_SEARCH, 0.8, Color.web("#94a3b8")));
        }
    }

    private void setupAiToggleButton() {
        btnToggleAi.setText("Nexus AI");
        btnToggleAi.setGraphic(IconFactory.createIcon(IconFactory.ICON_SPARKLES, 0.75, Color.web("#ffffff")));
        btnToggleAi.getStyleClass().add("btn-nexus-ai-active");
    }

    private void setupFullscreenButton() {
        if (btnToggleFullscreen != null) {
            btnToggleFullscreen.setText("Maximize");
            btnToggleFullscreen.setGraphic(IconFactory.createIcon(IconFactory.ICON_MAXIMIZE, 0.75, Color.web("#94a3b8")));
        }
    }

    private void setupAiPanelClipping() {
        // Enforce clean rectangular clip so sliding content does not overflow during width animation
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(aiPanelContainer.prefWidthProperty());
        clip.heightProperty().bind(aiPanelContainer.heightProperty());
        aiPanelContainer.setClip(clip);

        aiPanelContainer.setPrefWidth(AppConfig.AI_PANEL_EXPANDED_WIDTH);
        aiPanelContainer.setMaxWidth(AppConfig.AI_PANEL_EXPANDED_WIDTH);
        aiPanelContainer.setMinWidth(0);
    }

    private void setupGlobalKeyAccelerators() {
        if (centralWorkspaceContainer.getScene() != null) {
            Stage stage = (Stage) centralWorkspaceContainer.getScene().getWindow();
            if (stage != null) {
                // Listen to window state to update button label/icon
                stage.maximizedProperty().addListener((obs, oldV, isMax) -> {
                    updateFullscreenButtonState(isMax || stage.isFullScreen());
                });
                stage.fullScreenProperty().addListener((obs, oldV, isFull) -> {
                    updateFullscreenButtonState(isFull || stage.isMaximized());
                });
            }

            centralWorkspaceContainer.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.isControlDown() && event.getCode() == KeyCode.K) {
                    if (globalSearchField != null) {
                        globalSearchField.requestFocus();
                        globalSearchField.selectAll();
                    }
                    event.consume();
                } else if (event.getCode() == KeyCode.F11) {
                    handleToggleFullscreen();
                    event.consume();
                } else if (event.getCode() == KeyCode.ESCAPE && modalOverlay.isVisible()) {
                    closeSettingsModal();
                    event.consume();
                }
            });
        }
    }

    private void updateFullscreenButtonState(boolean isMaximizedOrFull) {
        if (btnToggleFullscreen == null) return;
        if (isMaximizedOrFull) {
            btnToggleFullscreen.setText("Restore");
            btnToggleFullscreen.setGraphic(IconFactory.createIcon(IconFactory.ICON_MINIMIZE, 0.75, Color.web("#94a3b8")));
        } else {
            btnToggleFullscreen.setText("Maximize");
            btnToggleFullscreen.setGraphic(IconFactory.createIcon(IconFactory.ICON_MAXIMIZE, 0.75, Color.web("#94a3b8")));
        }
    }

    @FXML
    private void handleToggleFullscreen() {
        if (centralWorkspaceContainer.getScene() != null && centralWorkspaceContainer.getScene().getWindow() instanceof Stage stage) {
            if (stage.isFullScreen()) {
                stage.setFullScreen(false);
                stage.setMaximized(true);
            } else if (stage.isMaximized()) {
                stage.setMaximized(false);
            } else {
                stage.setMaximized(true);
            }
            updateFullscreenButtonState(stage.isMaximized() || stage.isFullScreen());
        }
    }

    private void loadSidebar() {
        ViewLoader.LoadedView<Node> loaded = ViewLoader.load("/views/SidebarView.fxml");
        sidebarController = loaded.getController();
        sidebarController.setOpenSettingsCallback(this::openSettingsModal);
        sidebarContainer.getChildren().add(loaded.getNode());
    }

    private void loadBottomBar() {
        ViewLoader.LoadedView<Node> loaded = ViewLoader.load("/views/BottomBarView.fxml");
        bottomBarContainer.getChildren().add(loaded.getNode());
    }

    private void loadAiAssistant() {
        ViewLoader.LoadedView<Node> loaded = ViewLoader.load("/views/AiAssistantView.fxml");
        aiAssistantController = loaded.getController();
        aiAssistantController.setCollapseToggleCallback(this::handleToggleAiPanel);
        aiPanelContainer.getChildren().add(loaded.getNode());
    }

    private void loadWorkspaceViews() {
        // Home
        ViewLoader.LoadedView<Node> homeLoaded = ViewLoader.load("/views/HomeView.fxml");
        viewNodes.put(NavItem.HOME, homeLoaded.getNode());

        // Terminal
        ViewLoader.LoadedView<Node> terminalLoaded = ViewLoader.load("/views/TerminalView.fxml");
        terminalController = terminalLoaded.getController();
        terminalController.setOnAskAiListener(this::sendQueryToAi);
        terminalController.setOnToggleExpandWorkspaceCallback(this::toggleTerminalWorkspaceExpansion);
        viewNodes.put(NavItem.TERMINAL, terminalLoaded.getNode());

        // RepoGraph
        ViewLoader.LoadedView<Node> repoLoaded = ViewLoader.load("/views/RepoGraphView.fxml");
        repoGraphController = repoLoaded.getController();
        repoGraphController.setOnAskAiListener(this::sendQueryToAi);
        viewNodes.put(NavItem.REPO_GRAPH, repoLoaded.getNode());

        // Api Studio
        ViewLoader.LoadedView<Node> apiLoaded = ViewLoader.load("/views/ApiStudioView.fxml");
        apiStudioController = apiLoaded.getController();
        apiStudioController.setOnAskAiListener(this::sendQueryToAi);
        viewNodes.put(NavItem.API_STUDIO, apiLoaded.getNode());

        // History & Analytics
        ViewLoader.LoadedView<Node> historyLoaded = ViewLoader.load("/views/HistoryView.fxml");
        viewNodes.put(NavItem.HISTORY, historyLoaded.getNode());
    }

    private void switchWorkspaceView(NavItem navItem) {
        Node targetView = viewNodes.get(navItem);
        if (targetView != null) {
            centralWorkspaceContainer.getChildren().clear();
            centralWorkspaceContainer.getChildren().add(targetView);
            UiUtils.fadeIn(targetView, 160);

            if (aiAssistantController != null) {
                aiAssistantController.updateContextForSection(navItem);
            }
        }
    }

    public void sendQueryToAi(String query) {
        if (!isAiPanelExpanded) {
            expandAiPanelAnimated();
        }
        if (aiAssistantController != null) {
            aiAssistantController.sendUserQuery(query);
        }
    }

    private boolean isWorkspaceExpanded = false;

    private void toggleTerminalWorkspaceExpansion() {
        isWorkspaceExpanded = !isWorkspaceExpanded;
        if (isWorkspaceExpanded) {
            sidebarContainer.setVisible(false);
            sidebarContainer.setManaged(false);
            if (isAiPanelExpanded) {
                collapseAiPanelAnimated();
            }
        } else {
            sidebarContainer.setVisible(true);
            sidebarContainer.setManaged(true);
            if (!isAiPanelExpanded) {
                expandAiPanelAnimated();
            }
        }
    }

    @FXML
    private void handleToggleAiPanel() {
        if (isAiPanelExpanded) {
            collapseAiPanelAnimated();
        } else {
            expandAiPanelAnimated();
        }
    }

    private void expandAiPanelAnimated() {
        if (isAiPanelAnimating) return;
        isAiPanelAnimating = true;
        isAiPanelExpanded = true;

        aiPanelContainer.setVisible(true);
        aiPanelContainer.setManaged(true);

        // Update button visual state
        btnToggleAi.setText("Nexus AI");
        btnToggleAi.getStyleClass().removeAll("btn-nexus-ai-inactive", "btn-secondary");
        if (!btnToggleAi.getStyleClass().contains("btn-nexus-ai-active")) {
            btnToggleAi.getStyleClass().add("btn-nexus-ai-active");
        }
        btnToggleAi.setGraphic(IconFactory.createIcon(IconFactory.ICON_SPARKLES, 0.75, Color.web("#ffffff")));

        // Smooth width, opacity, and translation easing
        Timeline timeline = new Timeline();

        KeyValue kvWidth = new KeyValue(aiPanelContainer.prefWidthProperty(), AppConfig.AI_PANEL_EXPANDED_WIDTH, Interpolator.EASE_BOTH);
        KeyValue kvMaxWidth = new KeyValue(aiPanelContainer.maxWidthProperty(), AppConfig.AI_PANEL_EXPANDED_WIDTH, Interpolator.EASE_BOTH);
        KeyValue kvOpacity = new KeyValue(aiPanelContainer.opacityProperty(), 1.0, Interpolator.EASE_OUT);
        KeyValue kvTranslate = new KeyValue(aiPanelContainer.translateXProperty(), 0.0, Interpolator.EASE_OUT);

        KeyFrame kf = new KeyFrame(Duration.millis(260), kvWidth, kvMaxWidth, kvOpacity, kvTranslate);
        timeline.getKeyFrames().add(kf);

        aiPanelContainer.setOpacity(0.0);
        aiPanelContainer.setTranslateX(50.0);

        timeline.setOnFinished(e -> {
            isAiPanelAnimating = false;
        });

        timeline.play();
    }

    private void collapseAiPanelAnimated() {
        if (isAiPanelAnimating) return;
        isAiPanelAnimating = true;
        isAiPanelExpanded = false;

        // Update button visual state
        btnToggleAi.setText("Open Nexus AI");
        btnToggleAi.getStyleClass().removeAll("btn-nexus-ai-active", "btn-primary");
        if (!btnToggleAi.getStyleClass().contains("btn-nexus-ai-inactive")) {
            btnToggleAi.getStyleClass().add("btn-nexus-ai-inactive");
        }
        btnToggleAi.setGraphic(IconFactory.createIcon(IconFactory.ICON_SPARKLES, 0.75, Color.web("#a855f7")));

        Timeline timeline = new Timeline();

        KeyValue kvWidth = new KeyValue(aiPanelContainer.prefWidthProperty(), 0.0, Interpolator.EASE_BOTH);
        KeyValue kvMaxWidth = new KeyValue(aiPanelContainer.maxWidthProperty(), 0.0, Interpolator.EASE_BOTH);
        KeyValue kvOpacity = new KeyValue(aiPanelContainer.opacityProperty(), 0.0, Interpolator.EASE_IN);
        KeyValue kvTranslate = new KeyValue(aiPanelContainer.translateXProperty(), 50.0, Interpolator.EASE_IN);

        KeyFrame kf = new KeyFrame(Duration.millis(220), kvWidth, kvMaxWidth, kvOpacity, kvTranslate);
        timeline.getKeyFrames().add(kf);

        timeline.setOnFinished(e -> {
            aiPanelContainer.setVisible(false);
            aiPanelContainer.setManaged(false);
            isAiPanelAnimating = false;
        });

        timeline.play();
    }

    @FXML
    private void handleGlobalSearch() {
        if (globalSearchField == null) return;
        String query = globalSearchField.getText().trim().toLowerCase();
        if (query.isEmpty()) return;

        if (query.contains("term") || query.contains("console") || query.contains("bash")) {
            NavigationService.getInstance().navigateTo(NavItem.TERMINAL);
        } else if (query.contains("git") || query.contains("repo") || query.contains("commit") || query.contains("branch")) {
            NavigationService.getInstance().navigateTo(NavItem.REPO_GRAPH);
        } else if (query.contains("api") || query.contains("http") || query.contains("rest") || query.contains("postman")) {
            NavigationService.getInstance().navigateTo(NavItem.API_STUDIO);
        } else if (query.contains("hist") || query.contains("analytics") || query.contains("metric")) {
            NavigationService.getInstance().navigateTo(NavItem.HISTORY);
        } else if (query.contains("home") || query.contains("overview")) {
            NavigationService.getInstance().navigateTo(NavItem.HOME);
        } else if (query.contains("setting")) {
            openSettingsModal();
        } else {
            // Forward to Nexus AI
            sendQueryToAi("Global Search query: " + query);
        }
        globalSearchField.clear();
    }

    private void openSettingsModal() {
        ViewLoader.LoadedView<Node> loaded = ViewLoader.load("/views/SettingsDialog.fxml");
        SettingsDialogController controller = loaded.getController();
        controller.setOnCloseCallback(this::closeSettingsModal);

        modalOverlay.getChildren().clear();
        modalOverlay.getChildren().add(loaded.getNode());
        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);
        UiUtils.fadeIn(modalOverlay, 140);
    }

    private void closeSettingsModal() {
        FadeTransition ft = new FadeTransition(Duration.millis(120), modalOverlay);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> {
            modalOverlay.setVisible(false);
            modalOverlay.setManaged(false);
            modalOverlay.getChildren().clear();
        });
        ft.play();
    }
}
