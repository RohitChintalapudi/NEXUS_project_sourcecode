package com.nexus.controller;

import com.nexus.component.ApprovalCardView;
import com.nexus.component.IconFactory;
import com.nexus.model.AITaskStep;
import com.nexus.model.ChatMessage;
import com.nexus.model.ToolRequest;
import com.nexus.service.*;
import com.nexus.util.MarkdownRenderer;
import com.nexus.util.UiUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class AiAssistantController implements Initializable {

    @FXML private StackPane aiHeaderIconContainer;
    @FXML private Button btnClearChat;
    @FXML private Button btnCollapsePanel;
    @FXML private Label lblActiveContext;
    @FXML private HBox taskStatusBar;
    @FXML private Label lblTaskStatus;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox messagesContainer;
    @FXML private TextField chatInput;
    @FXML private Button btnSendChat;

    private Runnable collapseToggleCallback;
    private final Consumer<ToolRequest> approvalListener = this::handleApprovalRequest;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupIcons();
        loadInitialMessages();
        updateWorkspaceContextLabel();

        // Register listener for security approval requests
        PermissionService.getInstance().addApprovalListener(approvalListener);
    }

    public void setCollapseToggleCallback(Runnable collapseToggleCallback) {
        this.collapseToggleCallback = collapseToggleCallback;
    }

    public void updateContextForSection(com.nexus.model.NavItem navItem) {
        if (navItem == null) return;
        ConversationService.getInstance().setActiveSectionName(navItem.getLabel());
        Platform.runLater(() -> {
            String dir = System.getProperty("user.dir");
            String folder = new java.io.File(dir).getName();
            lblActiveContext.setText("[" + navItem.getLabel().toUpperCase() + "] " + folder + " (main)");
            lblActiveContext.setTooltip(new javafx.scene.control.Tooltip(dir));
        });
    }

    private javafx.scene.Node activeThinkingBubbleNode = null;
    private javafx.animation.Timeline thinkingAnimation = null;

    private void setupIcons() {
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/icons/nexus-icon.png");
            if (iconStream != null) {
                javafx.scene.image.Image iconImg = new javafx.scene.image.Image(iconStream);
                javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView(iconImg);
                imgView.setFitWidth(24);
                imgView.setFitHeight(24);
                imgView.setPreserveRatio(true);
                imgView.setSmooth(true);

                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(24, 24);
                clip.setArcWidth(6);
                clip.setArcHeight(6);
                imgView.setClip(clip);

                aiHeaderIconContainer.getChildren().add(imgView);
            } else {
                aiHeaderIconContainer.getChildren().add(IconFactory.createIcon(IconFactory.ICON_SPARKLES, 1.0, Color.web("#fbbf24")));
            }
        } catch (Exception e) {
            aiHeaderIconContainer.getChildren().add(IconFactory.createIcon(IconFactory.ICON_SPARKLES, 1.0, Color.web("#fbbf24")));
        }

        btnCollapsePanel.setGraphic(IconFactory.createIcon(IconFactory.ICON_CHEVRON_RIGHT, 0.8, Color.web("#a1a1aa")));
        btnSendChat.setGraphic(IconFactory.createIcon(IconFactory.ICON_SEND, 0.8, Color.web("#fbbf24")));
    }

    private void updateWorkspaceContextLabel() {
        String pwd = TerminalToolBridge.getInstance().getActiveWorkingDirectory();
        String folder = new java.io.File(pwd).getName();
        lblActiveContext.setText(folder + " (main)");
        lblActiveContext.setTooltip(new javafx.scene.control.Tooltip(pwd));
    }

    private void loadInitialMessages() {
        messagesContainer.getChildren().clear();
        ChatMessage welcome = ChatMessage.assistant(
                "Hello! I am **Nexus AI**, your developer co-pilot.\n\n" +
                "I can discover your local projects, inspect configurations, run terminal commands, and monitor hardware telemetry.\n\n" +
                "### Quick Actions\n" +
                "• **Find Projects**: *\"Where is my NoteHub project?\"* or *\"Find all Java projects\"*\n" +
                "• **Inspect Structure**: *\"Inspect pom.xml & structure\"*\n" +
                "• **Run Commands**: *\"Run test suite\"* or *\"Build project\"*\n" +
                "• **System Telemetry**: *\"Show CPU and memory usage\"*",
                null
        );
        appendMessageView(welcome);
        scrollToBottom();
    }

    public void sendUserQuery(String query) {
        if (query == null || query.trim().isEmpty()) return;

        ChatMessage userMsg = ChatMessage.user(query.trim());
        appendMessageView(userMsg);
        showThinkingIndicator();
        scrollToBottom();

        // Update task status
        setTaskStatus(true, "Analyzing request...");

        String activeSection = NavigationService.getInstance().getCurrentNavItem().name();

        AIService.getInstance().processQuery(
                query.trim(),
                activeSection,
                step -> Platform.runLater(() -> setTaskStatus(true, step.getTitle())),
                token -> {}
        ).thenAccept(reply -> Platform.runLater(() -> {
            hideThinkingIndicator();
            setTaskStatus(false, null);
            appendMessageView(reply);
            updateWorkspaceContextLabel();
            scrollToBottom();
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                hideThinkingIndicator();
                setTaskStatus(false, null);
                ChatMessage errorMsg = ChatMessage.assistant("Failed to process request: " + ex.getMessage(), null);
                appendMessageView(errorMsg);
                scrollToBottom();
            });
            return null;
        });
    }

    private void showThinkingIndicator() {
        hideThinkingIndicator();

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);

        VBox bubble = new VBox(6);
        bubble.setMaxWidth(390);
        bubble.getStyleClass().add("chat-bubble-assistant");
        bubble.setStyle("-fx-background-color: #151518; -fx-border-color: rgba(245, 158, 11, 0.35); -fx-border-radius: 10px 10px 10px 2px; -fx-background-radius: 10px 10px 10px 2px; -fx-padding: 10px 14px;");

        // Header
        HBox metaRow = new HBox(6);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/icons/nexus-icon.png");
            if (iconStream != null) {
                javafx.scene.image.ImageView avatar = new javafx.scene.image.ImageView(new javafx.scene.image.Image(iconStream));
                avatar.setFitWidth(16);
                avatar.setFitHeight(16);
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(16, 16);
                clip.setArcWidth(4);
                clip.setArcHeight(4);
                avatar.setClip(clip);
                metaRow.getChildren().add(avatar);
            }
        } catch (Exception ignored) {}

        Label senderLabel = new Label("Nexus AI");
        senderLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #fbbf24;");

        Label thinkingStatus = new Label("Thinking");
        thinkingStatus.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted; -fx-font-family: 'JetBrains Mono', Consolas, monospace;");

        metaRow.getChildren().addAll(senderLabel, thinkingStatus);

        // Animated dots row
        HBox dotsRow = new HBox(6);
        dotsRow.setAlignment(Pos.CENTER_LEFT);
        dotsRow.setPadding(new Insets(4, 0, 2, 0));

        Label dotsLabel = new Label("...");
        dotsLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: #fbbf24; -fx-letter-spacing: 2px; -fx-font-family: 'JetBrains Mono', Consolas, monospace;");

        Label thinkingHint = new Label("Processing prompt & tools...");
        thinkingHint.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #a1a1aa; -fx-font-style: italic;");

        dotsRow.getChildren().addAll(dotsLabel, thinkingHint);
        bubble.getChildren().addAll(metaRow, dotsRow);
        row.getChildren().add(bubble);

        activeThinkingBubbleNode = row;
        messagesContainer.getChildren().add(row);
        scrollToBottom();

        // Animate the dots smoothly (. -> .. -> ... -> ....)
        thinkingAnimation = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(300), e -> dotsLabel.setText(".")),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(600), e -> dotsLabel.setText(". .")),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(900), e -> dotsLabel.setText(". . .")),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(1200), e -> dotsLabel.setText(". . . ."))
        );
        thinkingAnimation.setCycleCount(javafx.animation.Animation.INDEFINITE);
        thinkingAnimation.play();
    }

    private void hideThinkingIndicator() {
        if (thinkingAnimation != null) {
            thinkingAnimation.stop();
            thinkingAnimation = null;
        }
        if (activeThinkingBubbleNode != null) {
            messagesContainer.getChildren().remove(activeThinkingBubbleNode);
            activeThinkingBubbleNode = null;
        }
    }

    private void handleApprovalRequest(ToolRequest request) {
        Platform.runLater(() -> {
            setTaskStatus(true, "Waiting for your approval: " + request.getToolName());
            ApprovalCardView card = new ApprovalCardView(request);
            messagesContainer.getChildren().add(card);
            scrollToBottom();
        });
    }

    private void setTaskStatus(boolean visible, String text) {
        if (taskStatusBar != null) {
            taskStatusBar.setVisible(visible);
            taskStatusBar.setManaged(visible);
            if (lblTaskStatus != null && text != null) {
                lblTaskStatus.setText(text);
            }
        }
    }

    @FXML
    private void handleSendMessage() {
        String text = chatInput.getText().trim();
        if (text.isEmpty()) return;

        chatInput.clear();
        sendUserQuery(text);
    }

    private void appendMessageView(ChatMessage msg) {
        VBox bubble = new VBox(6);
        bubble.setMaxWidth(390);

        HBox metaRow = new HBox(6);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        if (msg.getRole() == ChatMessage.Role.ASSISTANT) {
            try {
                java.io.InputStream iconStream = getClass().getResourceAsStream("/icons/nexus-icon.png");
                if (iconStream != null) {
                    javafx.scene.image.ImageView avatar = new javafx.scene.image.ImageView(new javafx.scene.image.Image(iconStream));
                    avatar.setFitWidth(15);
                    avatar.setFitHeight(15);
                    javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(15, 15);
                    clip.setArcWidth(4);
                    clip.setArcHeight(4);
                    avatar.setClip(clip);
                    metaRow.getChildren().add(avatar);
                }
            } catch (Exception ignored) {}
        }

        Label senderLabel = new Label(
                msg.getRole() == ChatMessage.Role.USER ? "You" :
                msg.getRole() == ChatMessage.Role.ASSISTANT ? "Nexus AI" : "SYSTEM"
        );
        senderLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; " +
                (msg.getRole() == ChatMessage.Role.USER ? "-fx-text-fill: #38bdf8;" :
                 msg.getRole() == ChatMessage.Role.ASSISTANT ? "-fx-text-fill: #fbbf24;" : "-fx-text-fill: #f59e0b;"));

        Label timeLabel = new Label(msg.getFormattedTime());
        timeLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");

        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);

        Button copyBtn = new Button("Copy");
        copyBtn.setStyle("""
                -fx-background-color: rgba(255, 255, 255, 0.06);
                -fx-text-fill: #94a3b8;
                -fx-font-size: 10px;
                -fx-cursor: hand;
                -fx-padding: 2px 8px;
                -fx-border-color: rgba(255, 255, 255, 0.12);
                -fx-border-radius: 4px;
                -fx-background-radius: 4px;
                """);
        copyBtn.setOnAction(e -> {
            UiUtils.copyToClipboard(msg.getText());
            copyBtn.setText("Copied! ✓");
            copyBtn.setStyle("""
                    -fx-background-color: rgba(16, 185, 129, 0.18);
                    -fx-text-fill: #10b981;
                    -fx-font-size: 10px;
                    -fx-font-weight: bold;
                    -fx-border-color: rgba(16, 185, 129, 0.4);
                    -fx-border-radius: 4px;
                    -fx-background-radius: 4px;
                    -fx-padding: 2px 8px;
                    """);
            javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
            pt.setOnFinished(ev -> {
                copyBtn.setText("Copy");
                copyBtn.setStyle("""
                        -fx-background-color: rgba(255, 255, 255, 0.06);
                        -fx-text-fill: #94a3b8;
                        -fx-font-size: 10px;
                        -fx-cursor: hand;
                        -fx-padding: 2px 8px;
                        -fx-border-color: rgba(255, 255, 255, 0.12);
                        -fx-border-radius: 4px;
                        -fx-background-radius: 4px;
                        """);
            });
            pt.play();
        });

        metaRow.getChildren().addAll(senderLabel, timeLabel, metaSpacer, copyBtn);

        // Context menu on the whole bubble for right-click copy
        javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
        javafx.scene.control.MenuItem copyItem = new javafx.scene.control.MenuItem("Copy Message Text");
        copyItem.setOnAction(e -> UiUtils.copyToClipboard(msg.getText()));
        contextMenu.getItems().add(copyItem);
        bubble.setOnContextMenuRequested(e -> contextMenu.show(bubble, e.getScreenX(), e.getScreenY()));

        // Rich Markdown formatted body
        VBox markdownBody = MarkdownRenderer.render(msg.getText(), command -> {
            TerminalToolBridge.getInstance().executeCommand(command, null);
        });

        bubble.getChildren().addAll(metaRow, markdownBody);

        // Add code snippet with Copy and Run buttons
        if (msg.getCodeSnippet() != null && !msg.getCodeSnippet().isEmpty()) {
            VBox codeBox = createCodeSnippetBox(msg.getCodeSnippet());
            bubble.getChildren().add(codeBox);
        }

        // Wrapper row for alignment
        HBox row = new HBox();
        if (msg.getRole() == ChatMessage.Role.USER) {
            bubble.getStyleClass().add("chat-bubble-user");
            row.setAlignment(Pos.CENTER_RIGHT);
        } else if (msg.getRole() == ChatMessage.Role.ASSISTANT) {
            bubble.getStyleClass().add("chat-bubble-assistant");
            row.setAlignment(Pos.CENTER_LEFT);
        } else {
            bubble.getStyleClass().add("chat-bubble-system");
            row.setAlignment(Pos.CENTER);
            bubble.setMaxWidth(360);
        }

        row.getChildren().add(bubble);
        messagesContainer.getChildren().add(row);
    }

    private VBox createCodeSnippetBox(String snippet) {
        VBox box = new VBox(6);
        box.setStyle("""
                -fx-background-color: #0b111e;
                -fx-border-color: #1e293d;
                -fx-border-width: 1px;
                -fx-border-radius: 6px;
                -fx-background-radius: 6px;
                -fx-padding: 8px 10px;
                """);

        Label codeLabel = new Label(snippet);
        codeLabel.setWrapText(true);
        codeLabel.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 11px; -fx-text-fill: #38bdf8;");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnCopy = new Button("Copy");
        btnCopy.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 10px; -fx-cursor: hand;");
        btnCopy.setOnAction(e -> UiUtils.copyToClipboard(snippet));

        Button btnRun = new Button("Run in Terminal ⏎");
        btnRun.setStyle("""
                -fx-background-color: rgba(0, 212, 255, 0.15);
                -fx-text-fill: #00d4ff;
                -fx-font-size: 10px;
                -fx-font-weight: bold;
                -fx-border-color: rgba(0, 212, 255, 0.4);
                -fx-border-radius: 3px;
                -fx-background-radius: 3px;
                -fx-padding: 2px 6px;
                -fx-cursor: hand;
                """);
        btnRun.setOnAction(e -> {
            TerminalToolBridge.getInstance().executeCommand(snippet, null);
        });

        actions.getChildren().addAll(btnCopy, btnRun);
        box.getChildren().addAll(codeLabel, actions);
        return box;
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            chatScrollPane.setVvalue(1.0);
        });
    }

    @FXML
    private void handleClearChat() {
        messagesContainer.getChildren().clear();
        ConversationService.getInstance().startNewConversation();
        ChatMessage resetMsg = ChatMessage.system("Conversation context reset. Entity memory cleared.");
        appendMessageView(resetMsg);
        scrollToBottom();
    }

    @FXML
    private void handleToggleCollapse() {
        if (collapseToggleCallback != null) {
            collapseToggleCallback.run();
        }
    }

    @FXML
    private void handlePromptFindProjects() {
        sendUserQuery("Find all my Java projects");
    }

    @FXML
    private void handlePromptFindNoteHub() {
        sendUserQuery("Where is my NoteHub project?");
    }

    @FXML
    private void handlePromptInspectPom() {
        sendUserQuery("Inspect pom.xml and explain project structure");
    }

    @FXML
    private void handlePromptRunTests() {
        sendUserQuery("Run tests for this project");
    }
}
