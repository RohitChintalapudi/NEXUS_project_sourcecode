package com.nexus.app;

import com.nexus.config.AppConfig;
import com.nexus.service.TelemetryService;
import com.nexus.util.ViewLoader;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

public class NexusApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            ViewLoader.LoadedView<Parent> rootLoaded = ViewLoader.load("/views/RootLayout.fxml");
            Parent root = rootLoaded.getNode();

            Scene scene = new Scene(root, AppConfig.DEFAULT_WINDOW_WIDTH, AppConfig.DEFAULT_WINDOW_HEIGHT);

            // Apply global CSS theme
            String cssPath = getClass().getResource("/styles/nexus-theme.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            com.nexus.service.ThemeService.getInstance().registerScene(scene);

            primaryStage.setTitle(AppConfig.APP_NAME + " — " + AppConfig.APP_SUBTITLE + " [" + AppConfig.APP_STAGE + "]");

            // Set custom Nexus cat-robot icon
            InputStream iconStream = getClass().getResourceAsStream("/icons/nexus-icon.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }

            primaryStage.setScene(scene);
            primaryStage.setMinWidth(AppConfig.MIN_WINDOW_WIDTH);
            primaryStage.setMinHeight(AppConfig.MIN_WINDOW_HEIGHT);
            primaryStage.setResizable(true);
            primaryStage.setMaximized(true); // Automatically open in maximized window mode

            // F11 Fullscreen toggle shortcut
            scene.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.F11) {
                    primaryStage.setFullScreen(!primaryStage.isFullScreen());
                    event.consume();
                }
            });

            // Set shutdown hook
            primaryStage.setOnCloseRequest(event -> {
                com.nexus.service.TelemetryService.getInstance().stop();
                com.nexus.service.SystemMetricsService.getInstance().stop();
                com.nexus.service.TerminalProcessManager.getInstance().shutdownAll();
            });

            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fatal error during NEXUS application bootstrap: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

