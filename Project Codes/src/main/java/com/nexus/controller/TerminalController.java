package com.nexus.controller;

import com.nexus.component.CommandBlockView;
import com.nexus.component.IconFactory;
import com.nexus.model.CommandBlock;
import com.nexus.model.ShellType;
import com.nexus.service.CommandHistoryService;
import com.nexus.service.ShellDetector;
import com.nexus.service.TerminalProcessManager;
import com.nexus.service.TerminalSession;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

public class TerminalController implements Initializable {

    @FXML private HBox tabsContainer;
    @FXML private MenuButton btnNewSessionMenu;
    @FXML private MenuItem menuItemPowerShell;
    @FXML private MenuItem menuItemCmd;
    @FXML private MenuItem menuItemBash;

    @FXML private Label lblActiveShell;
    @FXML private Label lblExecutionState;
    @FXML private Button btnFontDecrease;
    @FXML private Label lblFontSize;
    @FXML private Button btnFontIncrease;
    @FXML private Button btnCancelProcess;
    @FXML private Button btnRestartSession;
    @FXML private Button btnClearTerminal;
    @FXML private Button btnToggleExpand;

    @FXML private ScrollPane terminalScrollPane;
    @FXML private VBox commandBlocksContainer;
    @FXML private HBox runningIndicatorBar;
    @FXML private Label lblRunningCommandText;

    @FXML private Label lblPromptShellTag;
    @FXML private Label lblPromptDir;
    @FXML private Label lblPromptBranch;
    @FXML private TextField commandInput;
    @FXML private Button btnRun;
    @FXML private Button btnAskAi;

    private Consumer<String> onAskAiListener;
    private Runnable onToggleExpandWorkspaceCallback;

    private double currentFontSize = 12.0;
    private final List<String> sessionHistory = new ArrayList<>();
    private int historyPointer = -1;
    private String tempDraft = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupShellMenuItems();
        setupInputKeyEvents();

        TerminalProcessManager manager = TerminalProcessManager.getInstance();

        // Listen for tab list changes
        manager.getSessions().addListener((ListChangeListener<TerminalSession>) c -> {
            Platform.runLater(this::renderTabs);
        });

        // Listen for active session changes
        manager.activeSessionProperty().addListener((obs, oldSession, newSession) -> {
            Platform.runLater(() -> attachToSession(newSession));
        });

        // Ensure default session exists
        if (manager.getSessions().isEmpty()) {
            manager.createSession(null, ShellDetector.getInstance().getDefaultShell());
        } else {
            attachToSession(manager.getActiveSession());
        }

        renderTabs();
    }

    public void setOnAskAiListener(Consumer<String> onAskAiListener) {
        this.onAskAiListener = onAskAiListener;
    }

    public void setOnToggleExpandWorkspaceCallback(Runnable onToggleExpandWorkspaceCallback) {
        this.onToggleExpandWorkspaceCallback = onToggleExpandWorkspaceCallback;
    }

    private void setupShellMenuItems() {
        ShellDetector detector = ShellDetector.getInstance();
        menuItemPowerShell.setVisible(detector.isWindows() && detector.getAvailableShells().contains(ShellType.POWERSHELL));
        menuItemCmd.setVisible(detector.isWindows());
        menuItemBash.setVisible(!detector.isWindows() || detector.getAvailableShells().contains(ShellType.BASH));
    }

    private void setupInputKeyEvents() {
        commandInput.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.UP) {
                navigateHistory(-1);
                event.consume();
            } else if (event.getCode() == KeyCode.DOWN) {
                navigateHistory(1);
                event.consume();
            } else if (event.isControlDown() && event.getCode() == KeyCode.C) {
                handleCancelProcess();
                event.consume();
            } else if (event.isControlDown() && event.getCode() == KeyCode.L) {
                handleClearTerminal();
                event.consume();
            }
        });
    }

    private void navigateHistory(int direction) {
        if (sessionHistory.isEmpty()) return;

        if (historyPointer == -1) {
            tempDraft = commandInput.getText();
            historyPointer = sessionHistory.size();
        }

        int newPointer = historyPointer + direction;
        if (newPointer >= 0 && newPointer < sessionHistory.size()) {
            historyPointer = newPointer;
            commandInput.setText(sessionHistory.get(historyPointer));
            commandInput.positionCaret(commandInput.getText().length());
        } else if (newPointer >= sessionHistory.size()) {
            historyPointer = -1;
            commandInput.setText(tempDraft);
            commandInput.positionCaret(commandInput.getText().length());
        }
    }

    private void attachToSession(TerminalSession session) {
        if (session == null) return;

        lblActiveShell.setText(session.getShellType().getDisplayName());
        lblPromptShellTag.setText(getShellTag(session.getShellType()));
        lblPromptDir.setText(session.getWorkingDirectory());

        updateSessionStateUi(session);

        // Bind state changes
        session.addStateChangeListener(this::updateSessionStateUi);

        // Render existing command blocks for this session
        commandBlocksContainer.getChildren().clear();
        for (CommandBlock block : session.getBlocks()) {
            addBlockView(block);
        }

        // Listen for new blocks
        session.addBlockAddedListener(block -> {
            Platform.runLater(() -> addBlockView(block));
        });

        renderTabs();
        scrollToBottom();
    }

    private String getShellTag(ShellType type) {
        return switch (type) {
            case POWERSHELL, PWSH -> "ps>";
            case CMD -> "cmd>";
            case BASH, ZSH, SH -> "$";
        };
    }

    private void updateSessionStateUi(TerminalSession session) {
        if (session == null) return;
        lblPromptDir.setText(session.getWorkingDirectory());

        switch (session.getState()) {
            case READY -> {
                lblExecutionState.setText("IDLE");
                lblExecutionState.setStyle("-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: -color-accent-emerald; -fx-border-color: rgba(16,185,129,0.4); -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 3px 8px; -fx-font-size: 11px; -fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-weight: bold;");
                runningIndicatorBar.setVisible(false);
                runningIndicatorBar.setManaged(false);
                btnRun.setDisable(false);
                btnRun.setText("Run ⏎");
                commandInput.setPromptText("Enter shell command (e.g. git status, mvn clean, docker ps)...");
            }
            case RUNNING -> {
                lblExecutionState.setText("RUNNING");
                lblExecutionState.setStyle("-fx-background-color: rgba(245,158,11,0.15); -fx-text-fill: -color-accent-amber; -fx-border-color: rgba(245,158,11,0.4); -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 3px 8px; -fx-font-size: 11px; -fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-weight: bold;");
                lblRunningCommandText.setText("Running: " + (session.getCurrentRunningCommand() != null ? session.getCurrentRunningCommand() : "command") + "...");
                runningIndicatorBar.setVisible(true);
                runningIndicatorBar.setManaged(true);
                btnRun.setDisable(false);
                btnRun.setText("Send ⏎");
                commandInput.setPromptText("Process is running... Enter input or click Cancel (Ctrl+C) to stop");
            }
            case CANCELLED -> {
                lblExecutionState.setText("CANCELLED");
                lblExecutionState.setStyle("-fx-background-color: rgba(244,63,94,0.15); -fx-text-fill: -color-accent-rose; -fx-border-color: rgba(244,63,94,0.4); -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 3px 8px; -fx-font-size: 11px; -fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-weight: bold;");
                runningIndicatorBar.setVisible(false);
                runningIndicatorBar.setManaged(false);
                btnRun.setDisable(false);
                btnRun.setText("Run ⏎");
                commandInput.setPromptText("Enter shell command (e.g. git status, mvn clean, docker ps)...");
            }
            case TERMINATED -> {
                lblExecutionState.setText("TERMINATED");
                lblExecutionState.setStyle("-fx-background-color: rgba(100,116,139,0.15); -fx-text-fill: -color-text-muted; -fx-border-color: rgba(100,116,139,0.4); -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 3px 8px; -fx-font-size: 11px; -fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-weight: bold;");
                runningIndicatorBar.setVisible(false);
                runningIndicatorBar.setManaged(false);
                btnRun.setDisable(false);
                btnRun.setText("Run ⏎");
                commandInput.setPromptText("Terminal session ended. Click restart to resume.");
            }
        }
    }

    private void renderTabs() {
        tabsContainer.getChildren().clear();
        TerminalProcessManager manager = TerminalProcessManager.getInstance();
        TerminalSession active = manager.getActiveSession();

        for (TerminalSession session : manager.getSessions()) {
            HBox tabPill = new HBox(6);
            tabPill.setAlignment(Pos.CENTER_LEFT);
            tabPill.getStyleClass().add("terminal-tab-pill");
            if (session == active) {
                tabPill.getStyleClass().add("terminal-tab-pill-active");
            }

            Label titleLabel = new Label(session.getTitle());
            titleLabel.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 11px; -fx-text-fill: inherit; -fx-cursor: hand;");

            // Double click to rename
            titleLabel.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    promptRenameTab(session);
                } else {
                    manager.selectSession(session);
                }
            });

            // Close button
            Button closeBtn = new Button("✕");
            closeBtn.setStyle("-fx-background-color: transparent; -fx-padding: 0 4px; -fx-font-size: 10px; -fx-text-fill: -color-text-muted; -fx-cursor: hand;");
            closeBtn.setOnAction(e -> {
                manager.closeSession(session);
                e.consume();
            });

            tabPill.getChildren().addAll(titleLabel, closeBtn);
            tabPill.setOnMouseClicked(e -> manager.selectSession(session));

            tabsContainer.getChildren().add(tabPill);
        }
    }

    private void promptRenameTab(TerminalSession session) {
        TextInputDialog dialog = new TextInputDialog(session.getTitle());
        dialog.setTitle("Rename Terminal Tab");
        dialog.setHeaderText("Set custom session label:");
        dialog.setContentText("Title:");
        dialog.showAndWait().ifPresent(newTitle -> {
            if (!newTitle.trim().isEmpty()) {
                session.setTitle(newTitle.trim());
                renderTabs();
            }
        });
    }

    private void addBlockView(CommandBlock block) {
        CommandBlockView blockView = new CommandBlockView(block, query -> {
            if (onAskAiListener != null) {
                onAskAiListener.accept(query);
            }
        });
        commandBlocksContainer.getChildren().add(blockView);
        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> terminalScrollPane.setVvalue(1.0));
    }

    @FXML
    private void handleExecuteCommand() {
        String input = commandInput.getText().trim();
        if (input.isEmpty()) return;

        TerminalSession active = TerminalProcessManager.getInstance().getActiveSession();
        if (active == null) return;

        sessionHistory.add(input);
        historyPointer = -1;

        commandInput.clear();
        boolean started = active.executeCommand(input);
        if (started) {
            scrollToBottom();
        }
    }

    @FXML
    private void handleCancelProcess() {
        TerminalSession active = TerminalProcessManager.getInstance().getActiveSession();
        if (active != null) {
            active.cancelRunningProcess();
        }
    }

    @FXML
    private void handleRestartSession() {
        TerminalSession active = TerminalProcessManager.getInstance().getActiveSession();
        if (active != null) {
            commandBlocksContainer.getChildren().clear();
            active.restart();
            attachToSession(active);
        }
    }

    @FXML
    private void handleClearTerminal() {
        TerminalSession active = TerminalProcessManager.getInstance().getActiveSession();
        if (active != null) {
            active.clearOutput();
            commandBlocksContainer.getChildren().clear();
        }
    }

    @FXML
    private void handleNewPowerShellSession() {
        TerminalProcessManager.getInstance().createSession(null, ShellType.POWERSHELL);
    }

    @FXML
    private void handleNewCmdSession() {
        TerminalProcessManager.getInstance().createSession(null, ShellType.CMD);
    }

    @FXML
    private void handleNewBashSession() {
        TerminalProcessManager.getInstance().createSession(null, ShellType.BASH);
    }

    @FXML
    private void handleFontIncrease() {
        if (currentFontSize < 18.0) {
            currentFontSize += 1.0;
            applyFontSize();
        }
    }

    @FXML
    private void handleFontDecrease() {
        if (currentFontSize > 10.0) {
            currentFontSize -= 1.0;
            applyFontSize();
        }
    }

    private void applyFontSize() {
        lblFontSize.setText((int) currentFontSize + "px");
        commandInput.setStyle(String.format("-fx-font-size: %.1fpx;", currentFontSize));
        commandBlocksContainer.setStyle(String.format("-fx-font-size: %.1fpx;", currentFontSize));
    }

    @FXML
    private void handleToggleExpandWorkspace() {
        if (onToggleExpandWorkspaceCallback != null) {
            onToggleExpandWorkspaceCallback.run();
        }
    }

    @FXML
    private void handleAskAiFromInput() {
        String input = commandInput.getText().trim();
        if (!input.isEmpty() && onAskAiListener != null) {
            onAskAiListener.accept("How do I execute and optimize: " + input);
            commandInput.clear();
        }
    }
}
