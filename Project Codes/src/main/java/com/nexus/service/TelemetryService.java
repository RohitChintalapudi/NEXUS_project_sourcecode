package com.nexus.service;

import com.nexus.model.SystemTelemetry;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.Instant;

public class TelemetryService {
    private static final TelemetryService INSTANCE = new TelemetryService();

    private final ObjectProperty<SystemTelemetry> telemetryProperty =
            new SimpleObjectProperty<>(SystemTelemetry.createDefaultSimulated());

    private final Instant startTime = Instant.now().minusSeconds(13720);
    private Timeline heartbeatTimeline;

    private TelemetryService() {
        startSimulatedHeartbeat();
    }

    public static TelemetryService getInstance() {
        return INSTANCE;
    }

    public ObjectProperty<SystemTelemetry> telemetryProperty() {
        return telemetryProperty;
    }

    public SystemTelemetry getCurrentTelemetry() {
        return telemetryProperty.get();
    }

    private void startSimulatedHeartbeat() {
        heartbeatTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(3), e -> {
            // Update uptime
            long elapsedSeconds = java.time.Duration.between(startTime, Instant.now()).getSeconds();
            long hours = elapsedSeconds / 3600;
            long minutes = (elapsedSeconds % 3600) / 60;
            long seconds = elapsedSeconds % 60;
            String uptime = String.format("%02dh %02dm %02ds", hours, minutes, seconds);

            // Subtle fluctuation around realistic development workspace values
            double cpu = 22.0 + (Math.random() * 8.0);
            double ramUsed = 4.6 + (Math.random() * 0.4);
            double netUp = 280.0 + (Math.random() * 90.0);
            double netDown = 1100.0 + (Math.random() * 300.0);

            telemetryProperty.set(new SystemTelemetry(
                    Math.round(cpu * 10.0) / 10.0,
                    Math.round(ramUsed * 10.0) / 10.0,
                    16.0,
                    142.4,
                    512.0,
                    Math.round(netUp * 10.0) / 10.0,
                    Math.round(netDown * 10.0) / 10.0,
                    uptime,
                    true // Always marked as simulated
            ));
        }));
        heartbeatTimeline.setCycleCount(Animation.INDEFINITE);
        heartbeatTimeline.play();
    }

    public void stop() {
        if (heartbeatTimeline != null) {
            heartbeatTimeline.stop();
        }
    }
}
