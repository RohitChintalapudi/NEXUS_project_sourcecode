package com.nexus.controller;

import com.nexus.component.TelemetryGauge;
import com.nexus.model.SystemTelemetry;
import com.nexus.service.SystemMetricsService;
import com.nexus.service.TerminalProcessManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;

public class BottomBarController implements Initializable {

    @FXML private Label lblTelemetryStatus;
    @FXML private HBox cpuGaugeContainer;
    @FXML private HBox ramGaugeContainer;
    @FXML private HBox diskGaugeContainer;
    @FXML private Label lblNetworkTraffic;
    @FXML private Label lblSystemUptime;
    @FXML private Label lblActiveShellIndicator;

    private TelemetryGauge cpuGauge;
    private TelemetryGauge ramGauge;
    private TelemetryGauge diskGauge;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupGauges();

        // Bind to actual live system metrics from OSHI
        SystemMetricsService.getInstance().metricsProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> updateMetrics(newVal));
        });

        // Bind to active terminal session shell
        TerminalProcessManager.getInstance().activeSessionProperty().addListener((obs, oldVal, session) -> {
            if (session != null) {
                Platform.runLater(() -> {
                    lblActiveShellIndicator.setText("SHELL: " + session.getShellType().getDisplayName());
                });
            }
        });

        // Initial render
        updateMetrics(SystemMetricsService.getInstance().getCurrentMetrics());
    }

    private void setupGauges() {
        cpuGauge = new TelemetryGauge("CPU:", "0.0%", 0.0, "bar-cyan");
        ramGauge = new TelemetryGauge("RAM:", "0.0 / 0.0 GB", 0.0, "bar-purple");
        diskGauge = new TelemetryGauge("DISK:", "0.0 / 0.0 GB", 0.0, "bar-emerald");

        cpuGaugeContainer.getChildren().add(cpuGauge);
        ramGaugeContainer.getChildren().add(ramGauge);
        diskGaugeContainer.getChildren().add(diskGauge);
    }

    private void updateMetrics(SystemTelemetry telemetry) {
        if (telemetry == null) return;

        cpuGauge.update(String.format("%.1f%%", telemetry.getCpuUsagePercent()), telemetry.getCpuUsagePercent() / 100.0);
        ramGauge.update(String.format("%.1f / %.1f GB", telemetry.getRamUsedGb(), telemetry.getRamTotalGb()), telemetry.getRamUsagePercent() / 100.0);
        diskGauge.update(String.format("%.0f / %.0f GB", telemetry.getDiskUsedGb(), telemetry.getDiskTotalGb()), telemetry.getDiskUsagePercent() / 100.0);

        lblNetworkTraffic.setText(String.format("▲ %.0f KB/s  ▼ %.1f KB/s",
                telemetry.getNetworkUpKbps(),
                telemetry.getNetworkDownKbps()));

        lblSystemUptime.setText(telemetry.getUptimeFormatted());
    }
}
