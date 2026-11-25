package com.nexus.service;

import com.nexus.model.ShellType;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class TerminalProcessManager {
    private static final TerminalProcessManager INSTANCE = new TerminalProcessManager();

    private final ObservableList<TerminalSession> sessions = FXCollections.observableArrayList();
    private final ObjectProperty<TerminalSession> activeSession = new SimpleObjectProperty<>();
    private final AtomicInteger sessionCounter = new AtomicInteger(1);

    private TerminalProcessManager() {
        // Register shutdown hook to clean up all background shell processes
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownAll, "Nexus-TerminalShutdownHook"));
    }

    public static TerminalProcessManager getInstance() {
        return INSTANCE;
    }

    public ObservableList<TerminalSession> getSessions() {
        return sessions;
    }

    public ObjectProperty<TerminalSession> activeSessionProperty() {
        return activeSession;
    }

    public TerminalSession getActiveSession() {
        return activeSession.get();
    }

    public TerminalSession createSession(String title, ShellType shellType) {
        ShellType effectiveShell = shellType != null ? shellType : ShellDetector.getInstance().getDefaultShell();
        String effectiveTitle = title != null && !title.isEmpty() ? title :
                effectiveShell.getDisplayName() + " " + sessionCounter.getAndIncrement();

        String currentDir = activeSession.get() != null ? activeSession.get().getWorkingDirectory() : System.getProperty("user.dir");

        TerminalSession session = new TerminalSession(
                "session-" + UUID.randomUUID().toString().substring(0, 8),
                effectiveTitle,
                effectiveShell,
                currentDir
        );

        session.start();

        if (Platform.isFxApplicationThread()) {
            sessions.add(session);
            activeSession.set(session);
        } else {
            Platform.runLater(() -> {
                sessions.add(session);
                activeSession.set(session);
            });
        }

        return session;
    }

    public void closeSession(TerminalSession session) {
        if (session == null) return;

        session.terminate();

        Platform.runLater(() -> {
            int idx = sessions.indexOf(session);
            sessions.remove(session);

            if (activeSession.get() == session) {
                if (!sessions.isEmpty()) {
                    int nextIdx = Math.max(0, Math.min(idx, sessions.size() - 1));
                    activeSession.set(sessions.get(nextIdx));
                } else {
                    // Always maintain at least one active terminal session
                    createSession(null, null);
                }
            }
        });
    }

    public void selectSession(TerminalSession session) {
        if (session != null && sessions.contains(session)) {
            activeSession.set(session);
        }
    }

    public void shutdownAll() {
        for (TerminalSession session : sessions) {
            try {
                session.terminate();
            } catch (Exception ignored) {}
        }
    }
}

