package com.nexus.controller;

import com.nexus.service.ThemeService;
import com.nexus.service.ThemeService.Theme;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsDialogController implements Initializable {

    @FXML private Button btnClose;
    @FXML private ComboBox<Theme> themeSelector;
    @FXML private Label lblThemeDescription;
    @FXML private HBox swatchBg;
    @FXML private HBox swatchCard;
    @FXML private HBox swatchAccent;
    @FXML private HBox swatchSecondary;
    @FXML private Label lblAccentText;
    @FXML private Label lblSecondaryText;

    @FXML private ComboBox<String> fontSelector;
    @FXML private CheckBox chkGlowEffects;

    private Runnable onCloseCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupThemeSelector();
        setupFontSelector();
    }

    private void setupThemeSelector() {
        themeSelector.setItems(FXCollections.observableArrayList(Theme.values()));
        Theme current = ThemeService.getInstance().getCurrentTheme();
        themeSelector.setValue(current);
        updatePalettePreview(current);

        themeSelector.setOnAction(e -> {
            Theme selected = themeSelector.getValue();
            if (selected != null) {
                ThemeService.getInstance().setTheme(selected);
                updatePalettePreview(selected);
            }
        });
    }

    private void updatePalettePreview(Theme theme) {
        if (theme == null) return;

        switch (theme) {
            case CHRISTMAS_FESTIVE -> {
                lblThemeDescription.setText("Festive Holiday Edition: Evergreen Pine, Candy Cane Crimson, and Star Gold accents.");
                swatchBg.setStyle("-fx-background-color: #07150c; -fx-background-radius: 4px; -fx-border-color: #1e4530;");
                swatchCard.setStyle("-fx-background-color: #153423; -fx-background-radius: 4px; -fx-border-color: #2d6244;");
                swatchAccent.setStyle("-fx-background-color: #ef4444; -fx-background-radius: 4px;");
                lblAccentText.setText("Crimson");
                lblAccentText.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");
                swatchSecondary.setStyle("-fx-background-color: #facc15; -fx-background-radius: 4px;");
                lblSecondaryText.setText("Star Gold");
                lblSecondaryText.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #000000;");
            }
            case CYBERPUNK_NEON -> {
                lblThemeDescription.setText("High-contrast Cyberpunk: Obsidian Violet, Neon Hot Pink, and Cyan glow.");
                swatchBg.setStyle("-fx-background-color: #0c0817; -fx-background-radius: 4px; -fx-border-color: #33245f;");
                swatchCard.setStyle("-fx-background-color: #201740; -fx-background-radius: 4px; -fx-border-color: #4e3594;");
                swatchAccent.setStyle("-fx-background-color: #ec4899; -fx-background-radius: 4px;");
                lblAccentText.setText("Hot Pink");
                lblAccentText.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");
                swatchSecondary.setStyle("-fx-background-color: #06b6d4; -fx-background-radius: 4px;");
                lblSecondaryText.setText("Neon Cyan");
                lblSecondaryText.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #000000;");
            }
            case MIDNIGHT_SAPPHIRE -> {
                lblThemeDescription.setText("Deep Navy Sapphire: Slate obsidian foundations with Ice Blue and Cyan.");
                swatchBg.setStyle("-fx-background-color: #060d17; -fx-background-radius: 4px; -fx-border-color: #1c3558;");
                swatchCard.setStyle("-fx-background-color: #132743; -fx-background-radius: 4px; -fx-border-color: #274b7c;");
                swatchAccent.setStyle("-fx-background-color: #0284c7; -fx-background-radius: 4px;");
                lblAccentText.setText("Sapphire");
                lblAccentText.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");
                swatchSecondary.setStyle("-fx-background-color: #38bdf8; -fx-background-radius: 4px;");
                lblSecondaryText.setText("Sky Blue");
                lblSecondaryText.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #000000;");
            }
            case EMERALD_MATRIX -> {
                lblThemeDescription.setText("Terminal Matrix: Phosphor Green, Deep Forest Obsidian, and Mint highlights.");
                swatchBg.setStyle("-fx-background-color: #040d07; -fx-background-radius: 4px; -fx-border-color: #163d24;");
                swatchCard.setStyle("-fx-background-color: #0f2a19; -fx-background-radius: 4px; -fx-border-color: #235835;");
                swatchAccent.setStyle("-fx-background-color: #10b981; -fx-background-radius: 4px;");
                lblAccentText.setText("Matrix Green");
                lblAccentText.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");
                swatchSecondary.setStyle("-fx-background-color: #34d399; -fx-background-radius: 4px;");
                lblSecondaryText.setText("Mint");
                lblSecondaryText.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #000000;");
            }
            default -> {
                lblThemeDescription.setText("Signature Matte Black & Gold Yellow: Charcoal backgrounds with warm amber gold highlights.");
                swatchBg.setStyle("-fx-background-color: #09090b; -fx-background-radius: 4px; -fx-border-color: #27272a;");
                swatchCard.setStyle("-fx-background-color: #18181c; -fx-background-radius: 4px; -fx-border-color: #3f3f46;");
                swatchAccent.setStyle("-fx-background-color: #f59e0b; -fx-background-radius: 4px;");
                lblAccentText.setText("Gold Amber");
                lblAccentText.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #000000;");
                swatchSecondary.setStyle("-fx-background-color: #fbbf24; -fx-background-radius: 4px;");
                lblSecondaryText.setText("Yellow");
                lblSecondaryText.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #000000;");
            }
        }
    }

    private void setupFontSelector() {
        fontSelector.setItems(FXCollections.observableArrayList(
                "JetBrains Mono (Bundled / Preferred)",
                "Cascadia Code",
                "Consolas",
                "Fira Code"
        ));
        fontSelector.setValue("JetBrains Mono (Bundled / Preferred)");
    }

    public void setOnCloseCallback(Runnable onCloseCallback) {
        this.onCloseCallback = onCloseCallback;
    }

    @FXML
    private void handleClose() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        } else if (btnClose.getScene() != null && btnClose.getScene().getWindow() instanceof Stage stage) {
            stage.close();
        }
    }
}
