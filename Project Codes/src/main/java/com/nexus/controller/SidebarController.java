package com.nexus.controller;

import com.nexus.component.IconFactory;
import com.nexus.model.NavItem;
import com.nexus.service.NavigationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class SidebarController implements Initializable {

    @FXML private StackPane logoContainer;
    @FXML private Button navHome;
    @FXML private Button navTerminal;
    @FXML private Button navRepoGraph;
    @FXML private Button navApiStudio;
    @FXML private Button navHistory;
    @FXML private Button navSettings;

    private final Map<NavItem, Button> navButtonMap = new HashMap<>();
    private Runnable openSettingsCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupIcons();
        setupNavigationMapping();

        // Listen for navigation state changes
        NavigationService.getInstance().currentNavItemProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> updateActiveState(newVal));
        });

        // Set initial active state
        updateActiveState(NavigationService.getInstance().getCurrentNavItem());
    }

    public void setOpenSettingsCallback(Runnable openSettingsCallback) {
        this.openSettingsCallback = openSettingsCallback;
    }

    private void setupIcons() {
        // App Brand Logo from custom cat-robot icon
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/icons/nexus-icon.png");
            if (iconStream != null) {
                javafx.scene.image.Image iconImg = new javafx.scene.image.Image(iconStream);
                javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView(iconImg);
                imgView.setFitWidth(32);
                imgView.setFitHeight(32);
                imgView.setPreserveRatio(true);
                imgView.setSmooth(true);

                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(32, 32);
                clip.setArcWidth(10);
                clip.setArcHeight(10);
                imgView.setClip(clip);

                logoContainer.getChildren().add(imgView);
            } else {
                SVGPath logo = IconFactory.createFilledIcon(IconFactory.ICON_LOGO_NEXUS, 1.1, Color.web("#fbbf24"));
                logoContainer.getChildren().add(logo);
            }
        } catch (Exception e) {
            SVGPath logo = IconFactory.createFilledIcon(IconFactory.ICON_LOGO_NEXUS, 1.1, Color.web("#fbbf24"));
            logoContainer.getChildren().add(logo);
        }

        // Nav icons
        navHome.setGraphic(IconFactory.createIcon(IconFactory.ICON_HOME, 0.85, Color.web("#a1a1aa")));
        navTerminal.setGraphic(IconFactory.createIcon(IconFactory.ICON_TERMINAL, 0.85, Color.web("#a1a1aa")));
        navRepoGraph.setGraphic(IconFactory.createIcon(IconFactory.ICON_GIT_BRANCH, 0.85, Color.web("#a1a1aa")));
        navApiStudio.setGraphic(IconFactory.createIcon(IconFactory.ICON_API, 0.85, Color.web("#a1a1aa")));
        navHistory.setGraphic(IconFactory.createIcon(IconFactory.ICON_ANALYTICS, 0.85, Color.web("#a1a1aa")));
        navSettings.setGraphic(IconFactory.createIcon(IconFactory.ICON_SETTINGS, 0.85, Color.web("#a1a1aa")));
    }

    private void setupNavigationMapping() {
        navButtonMap.put(NavItem.HOME, navHome);
        navButtonMap.put(NavItem.TERMINAL, navTerminal);
        navButtonMap.put(NavItem.REPO_GRAPH, navRepoGraph);
        navButtonMap.put(NavItem.API_STUDIO, navApiStudio);
        navButtonMap.put(NavItem.HISTORY, navHistory);
    }

    private void updateActiveState(NavItem activeItem) {
        navButtonMap.forEach((item, button) -> {
            if (item == activeItem) {
                if (!button.getStyleClass().contains("nav-button-active")) {
                    button.getStyleClass().add("nav-button-active");
                }
                // Highlight icon in gold yellow
                setButtonIconColor(item, button, Color.web("#fbbf24"));
            } else {
                button.getStyleClass().remove("nav-button-active");
                setButtonIconColor(item, button, Color.web("#a1a1aa"));
            }
        });
    }

    private void setButtonIconColor(NavItem item, Button button, Color color) {
        switch (item) {
            case HOME -> button.setGraphic(IconFactory.createIcon(IconFactory.ICON_HOME, 0.85, color));
            case TERMINAL -> button.setGraphic(IconFactory.createIcon(IconFactory.ICON_TERMINAL, 0.85, color));
            case REPO_GRAPH -> button.setGraphic(IconFactory.createIcon(IconFactory.ICON_GIT_BRANCH, 0.85, color));
            case API_STUDIO -> button.setGraphic(IconFactory.createIcon(IconFactory.ICON_API, 0.85, color));
            case HISTORY -> button.setGraphic(IconFactory.createIcon(IconFactory.ICON_ANALYTICS, 0.85, color));
        }
    }

    @FXML
    private void handleNavHome() {
        NavigationService.getInstance().navigateTo(NavItem.HOME);
    }

    @FXML
    private void handleNavTerminal() {
        NavigationService.getInstance().navigateTo(NavItem.TERMINAL);
    }

    @FXML
    private void handleNavRepoGraph() {
        NavigationService.getInstance().navigateTo(NavItem.REPO_GRAPH);
    }

    @FXML
    private void handleNavApiStudio() {
        NavigationService.getInstance().navigateTo(NavItem.API_STUDIO);
    }

    @FXML
    private void handleNavHistory() {
        NavigationService.getInstance().navigateTo(NavItem.HISTORY);
    }

    @FXML
    private void handleOpenSettings() {
        if (openSettingsCallback != null) {
            openSettingsCallback.run();
        }
    }
}

