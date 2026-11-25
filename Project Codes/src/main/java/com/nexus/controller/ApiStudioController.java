package com.nexus.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nexus.model.*;
import com.nexus.service.*;
import com.nexus.util.UiUtils;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

public class ApiStudioController implements Initializable {

    // Top Bar
    @FXML private ComboBox<String> methodCombo;
    @FXML private TextField urlField;
    @FXML private ComboBox<String> envCombo;
    @FXML private Button btnManageEnvs;
    @FXML private Button btnSend;
    @FXML private Button btnSave;

    // Presets & Import
    @FXML private ComboBox<String> demoApisCombo;
    @FXML private Button chipJpPost;
    @FXML private Button chipJpUsers;
    @FXML private Button chipJpCreate;
    @FXML private Button chipDjProduct;
    @FXML private Button chipDjQuote;
    @FXML private Button chipRrUsers;
    @FXML private Button chipHealth;
    @FXML private Button chipPost;
    @FXML private Button chipGet;
    @FXML private Button chipAuthSignup;
    @FXML private Button chipAuthLogin;
    @FXML private Button chipAuthProfile;
    @FXML private Button btnImportOpenApi;

    // Sidebar: Collections & History
    @FXML private TreeView<Object> collectionsTreeView;
    @FXML private ListView<ApiHistoryItem> historyListView;

    // Request Editor Tabs
    @FXML private TextArea requestBodyArea;

    @FXML private TableView<HeaderEntry> headersTable;
    @FXML private TableColumn<HeaderEntry, String> headerKeyCol;
    @FXML private TableColumn<HeaderEntry, String> headerValueCol;
    @FXML private TableColumn<HeaderEntry, Void> headerActionCol;

    // Auth
    @FXML private ComboBox<String> authTypeCombo;
    @FXML private VBox boxBearerAuth;
    @FXML private PasswordField authTokenField;
    @FXML private VBox boxBasicAuth;
    @FXML private TextField authUsernameField;
    @FXML private PasswordField authPasswordField;
    @FXML private VBox boxApiKeyAuth;
    @FXML private TextField apiKeyNameField;
    @FXML private PasswordField apiKeyValueField;

    // Assertions Config
    @FXML private TableView<ApiAssertion> assertionsTable;
    @FXML private TableColumn<ApiAssertion, String> assertionTypeCol;
    @FXML private TableColumn<ApiAssertion, String> assertionPropCol;
    @FXML private TableColumn<ApiAssertion, String> assertionExpectedCol;

    // Response Viewer
    @FXML private Label lblResponseStatus;
    @FXML private Label lblResponseTime;
    @FXML private Label lblResponseSize;
    @FXML private Label lblAssertionSummary;
    @FXML private TextArea responseBodyArea;
    @FXML private TextArea rawResponseArea;
    @FXML private ListView<String> responseHeadersList;
    @FXML private ListView<String> assertionResultsList;
    @FXML private Button btnCopyResponse;
    @FXML private Button btnExplainApiAi;

    // State
    private final ObservableList<HeaderEntry> headersData = FXCollections.observableArrayList();
    private final ObservableList<ApiAssertion> assertionsData = FXCollections.observableArrayList();

    private ApiRequestModel currentActiveRequest;
    private Consumer<String> onAskAiListener;
    private final ObjectMapper prettyObjectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LocalAuthServer.getInstance().start();
        setupMethodCombo();
        setupEnvCombo();
        setupAuthCombo();
        setupDemoApisCombo();
        setupTables();
        setupCollectionsTree();
        setupHistoryListView();
        setupDarkListViewStyles();

        // Default initial request: Gold standard online mock API ready to click Send
        ApiRequestModel initialDemo = new ApiRequestModel("Get Blog Post #1", "GET", "https://jsonplaceholder.typicode.com/posts/1");
        initialDemo.getHeaders().put("Accept", "application/json");
        initialDemo.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        initialDemo.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "title", ""));
        loadRequestIntoEditor(initialDemo);
    }

    public void setOnAskAiListener(Consumer<String> onAskAiListener) {
        this.onAskAiListener = onAskAiListener;
    }

    private void setupMethodCombo() {
        methodCombo.setItems(FXCollections.observableArrayList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"
        ));
        methodCombo.setValue("GET");
    }

    private void setupEnvCombo() {
        EnvironmentService envService = EnvironmentService.getInstance();
        List<String> envNames = new ArrayList<>();
        for (EnvironmentModel env : envService.getEnvironments()) {
            envNames.add(env.getName());
        }
        envCombo.setItems(FXCollections.observableArrayList(envNames));
        if (envService.getActiveEnvironment() != null) {
            envCombo.setValue(envService.getActiveEnvironment().getName());
        }
        envCombo.setOnAction(e -> {
            String selected = envCombo.getValue();
            if (selected != null) {
                envService.setActiveEnvironmentByName(selected);
            }
        });
    }

    private void setupAuthCombo() {
        authTypeCombo.setItems(FXCollections.observableArrayList(
                "No Auth", "Bearer Token", "Basic Auth", "API Key (Header)", "API Key (Query Param)"
        ));
        authTypeCombo.setValue("Bearer Token");
        authTypeCombo.setOnAction(e -> updateAuthBoxVisibility(authTypeCombo.getValue()));
    }

    private void updateAuthBoxVisibility(String selected) {
        boxBearerAuth.setVisible("Bearer Token".equals(selected));
        boxBearerAuth.setManaged("Bearer Token".equals(selected));

        boxBasicAuth.setVisible("Basic Auth".equals(selected));
        boxBasicAuth.setManaged("Basic Auth".equals(selected));

        boolean isApiKey = selected != null && selected.startsWith("API Key");
        boxApiKeyAuth.setVisible(isApiKey);
        boxApiKeyAuth.setManaged(isApiKey);
    }

    private void setupTables() {
        // Headers Table (Fully editable with inline TextFieldTableCell & delete action)
        headerKeyCol.setCellValueFactory(c -> c.getValue().keyProperty());
        headerKeyCol.setCellFactory(TextFieldTableCell.forTableColumn());
        headerKeyCol.setOnEditCommit(e -> e.getRowValue().setKey(e.getNewValue()));

        headerValueCol.setCellValueFactory(c -> c.getValue().valueProperty());
        headerValueCol.setCellFactory(TextFieldTableCell.forTableColumn());
        headerValueCol.setOnEditCommit(e -> e.getRowValue().setValue(e.getNewValue()));

        headerActionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("✕");
            {
                btnDelete.setStyle("-fx-text-fill: #f43f5e; -fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 2px 6px;");
                btnDelete.setOnAction(e -> {
                    HeaderEntry entry = getTableRow().getItem();
                    if (entry != null) {
                        headersData.remove(entry);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(btnDelete);
                }
            }
        });

        headersTable.setItems(headersData);

        // Assertions Table
        assertionTypeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().getLabel()));
        assertionPropCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProperty()));
        assertionExpectedCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getExpectedValue()));
        assertionsTable.setItems(assertionsData);
    }

    private void setupCollectionsTree() {
        refreshCollectionsTree();

        collectionsTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV.getValue() instanceof ApiRequestModel req) {
                loadRequestIntoEditor(req);
            }
        });
    }

    private void refreshCollectionsTree() {
        TreeItem<Object> root = new TreeItem<>("Collections Root");
        List<ApiCollection> collections = ApiCollectionService.getInstance().getCollections();

        for (ApiCollection col : collections) {
            TreeItem<Object> colItem = new TreeItem<>(col);
            colItem.setExpanded(true);

            for (ApiRequestModel req : col.getRequests()) {
                TreeItem<Object> reqItem = new TreeItem<>(req);
                colItem.getChildren().add(reqItem);
            }

            root.getChildren().add(colItem);
        }

        collectionsTreeView.setRoot(root);
        collectionsTreeView.setCellFactory(tv -> new TreeCell<Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else if (item instanceof ApiCollection col) {
                    setText(col.getName() + " (" + col.getRequests().size() + ")");
                    Label icon = new Label("📁");
                    setGraphic(icon);
                    setStyle("-fx-background-color: transparent; -fx-text-fill: -color-accent-cyan; -fx-font-weight: bold; -fx-font-size: 11px;");
                } else if (item instanceof ApiRequestModel req) {
                    setText(req.getMethod() + "  " + req.getName());
                    Label icon = new Label(getMethodIcon(req.getMethod()));
                    setGraphic(icon);
                    setStyle("-fx-background-color: transparent; -fx-text-fill: #e2e8f0; -fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 11px;");
                }
            }
        });
    }

    private String getMethodIcon(String method) {
        return switch (method.toUpperCase()) {
            case "GET" -> "🟢";
            case "POST" -> "🟣";
            case "PUT" -> "🔵";
            case "DELETE" -> "🔴";
            default -> "⚡";
        };
    }

    private void setupHistoryListView() {
        refreshHistoryList();

        historyListView.setCellFactory(lv -> new ListCell<ApiHistoryItem>() {
            @Override
            protected void updateItem(ApiHistoryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #0c1220; -fx-border-color: transparent;");
                } else {
                    setText(String.format("%s [%s] %d  %s (%d ms)",
                            item.getFormattedTime(), item.getMethod(), item.getStatusCode(), item.getUrl(), item.getResponseTimeMs()));
                    setStyle("-fx-background-color: #0c1220; -fx-text-fill: #e2e8f0; -fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 10px; -fx-padding: 4px 6px;");
                }
            }
        });

        historyListView.setOnMouseClicked(e -> {
            ApiHistoryItem selected = historyListView.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getRequestSnapshot() != null) {
                loadRequestIntoEditor(selected.getRequestSnapshot());
            }
        });
    }

    private void refreshHistoryList() {
        historyListView.setItems(FXCollections.observableArrayList(ApiHistoryService.getInstance().getHistory()));
    }

    private void setupDarkListViewStyles() {
        responseHeadersList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #0c1220; -fx-border-color: transparent;");
                } else {
                    setText(item);
                    setStyle("-fx-background-color: #0c1220; -fx-text-fill: #e2e8f0; -fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 11px; -fx-padding: 3px 6px;");
                }
            }
        });

        assertionResultsList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #0c1220; -fx-border-color: transparent;");
                } else {
                    setText(item);
                    boolean passed = item.startsWith("✓");
                    String color = passed ? "#34d399" : "#fb7185";
                    setStyle("-fx-background-color: #0c1220; -fx-text-fill: " + color + "; -fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 11px; -fx-padding: 4px 6px;");
                }
            }
        });
    }

    private void loadRequestIntoEditor(ApiRequestModel request) {
        if (request == null) return;
        this.currentActiveRequest = request.copy();

        methodCombo.setValue(request.getMethod());
        urlField.setText(request.getUrl());
        requestBodyArea.setText(request.getBody() != null ? request.getBody() : "");

        // Headers
        headersData.clear();
        for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
            headersData.add(new HeaderEntry(entry.getKey(), entry.getValue()));
        }

        // Auth
        authTypeCombo.setValue(request.getAuthType());
        updateAuthBoxVisibility(request.getAuthType());
        authTokenField.setText(request.getAuthToken() != null ? request.getAuthToken() : "");
        authUsernameField.setText(request.getAuthUsername() != null ? request.getAuthUsername() : "");
        authPasswordField.setText(request.getAuthPassword() != null ? request.getAuthPassword() : "");
        apiKeyNameField.setText(request.getAuthKey() != null ? request.getAuthKey() : "X-API-Key");
        apiKeyValueField.setText(request.getAuthToken() != null ? request.getAuthToken() : "");

        // Assertions
        assertionsData.clear();
        assertionsData.addAll(request.getAssertions());
    }

    private ApiRequestModel extractRequestFromEditor() {
        ApiRequestModel req = (currentActiveRequest != null) ? currentActiveRequest.copy() : new ApiRequestModel();
        req.setMethod(methodCombo.getValue());
        req.setUrl(urlField.getText().trim());
        req.setBody(requestBodyArea.getText());

        // Headers
        req.getHeaders().clear();
        for (HeaderEntry entry : headersData) {
            if (entry.getKey() != null && !entry.getKey().trim().isEmpty()) {
                req.getHeaders().put(entry.getKey().trim(), entry.getValue() != null ? entry.getValue() : "");
            }
        }

        // Auth
        String authType = authTypeCombo.getValue();
        req.setAuthType(authType);
        if ("Bearer Token".equals(authType)) {
            req.setAuthToken(authTokenField.getText().trim());
        } else if ("Basic Auth".equals(authType)) {
            req.setAuthUsername(authUsernameField.getText().trim());
            req.setAuthPassword(authPasswordField.getText());
        } else if (authType != null && authType.startsWith("API Key")) {
            req.setAuthKey(apiKeyNameField.getText().trim());
            req.setAuthToken(apiKeyValueField.getText().trim());
        }

        // Assertions
        req.getAssertions().clear();
        req.getAssertions().addAll(assertionsData);

        return req;
    }

    @FXML
    public void handleSendRequest() {
        ApiRequestModel requestModel = extractRequestFromEditor();
        if (requestModel.getUrl().isEmpty()) {
            lblResponseStatus.setText("Error: URL Empty");
            return;
        }

        btnSend.setDisable(true);
        btnSend.setText("Sending...");
        lblResponseStatus.setText("Sending...");

        HttpClientService.getInstance().executeAsync(requestModel)
                .thenAccept(response -> Platform.runLater(() -> {
                    updateResponseView(response);
                    ApiHistoryService.getInstance().recordExecution(requestModel, response);
                    refreshHistoryList();
                    btnSend.setDisable(false);
                    btnSend.setText("Send ⚡");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        lblResponseStatus.setText("Error: " + ex.getMessage());
                        btnSend.setDisable(false);
                        btnSend.setText("Send ⚡");
                    });
                    return null;
                });
    }

    private void updateResponseView(ApiResponseModel response) {
        int code = response.getStatusCode();
        lblResponseStatus.setText(code > 0 ? (code + " " + response.getStatusText()) : "Error: " + response.getStatusText());

        if (code >= 200 && code < 300) {
            lblResponseStatus.setStyle("-fx-background-color: rgba(16,185,129,0.2); -fx-text-fill: #34d399; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 4px;");
        } else if (code >= 400 && code < 500) {
            lblResponseStatus.setStyle("-fx-background-color: rgba(245,158,11,0.2); -fx-text-fill: #fbbf24; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 4px;");
        } else if (code >= 500) {
            lblResponseStatus.setStyle("-fx-background-color: rgba(244,63,94,0.2); -fx-text-fill: #fb7185; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 4px;");
        } else {
            lblResponseStatus.setStyle("-fx-background-color: rgba(244,63,94,0.2); -fx-text-fill: #fb7185; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 4px;");
        }

        lblResponseTime.setText(response.getResponseTimeMs() + " ms");
        lblResponseSize.setText(response.getFormattedSize());

        // Body formatting
        String body = response.getBody();
        rawResponseArea.setText(body);

        try {
            Object json = prettyObjectMapper.readValue(body, Object.class);
            responseBodyArea.setText(prettyObjectMapper.writeValueAsString(json));
        } catch (Exception e) {
            responseBodyArea.setText(body);
        }

        // Auto-extract Bearer token if response is from login or signup
        if (body != null && body.contains("\"token\"")) {
            try {
                JsonNode root = prettyObjectMapper.readTree(body);
                if (root.has("token")) {
                    String extractedToken = root.get("token").asText();
                    if (!extractedToken.isEmpty()) {
                        EnvironmentService.getInstance().setVariable("token", extractedToken);
                        if (authTokenField != null) {
                            authTokenField.setText(extractedToken);
                        }
                        lblResponseStatus.setText(lblResponseStatus.getText() + " • Token saved to {{token}}");
                    }
                }
            } catch (Exception ignored) {}
        }

        // Headers
        responseHeadersList.getItems().clear();
        for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
            responseHeadersList.getItems().add(entry.getKey() + ": " + entry.getValue());
        }

        // Assertions
        assertionResultsList.getItems().clear();
        int passedCount = 0;
        for (ApiAssertion a : response.getAssertionResults()) {
            if (a.isPassed()) {
                passedCount++;
                assertionResultsList.getItems().add("✓ PASS: " + a.getMessage());
            } else {
                assertionResultsList.getItems().add("✕ FAIL: " + a.getMessage());
            }
        }

        if (!response.getAssertionResults().isEmpty()) {
            lblAssertionSummary.setText(String.format("Tests: %d/%d passed", passedCount, response.getAssertionResults().size()));
            lblAssertionSummary.setStyle(passedCount == response.getAssertionResults().size() ?
                    "-fx-text-fill: #34d399; -fx-font-weight: bold;" : "-fx-text-fill: #fb7185; -fx-font-weight: bold;");
        } else {
            lblAssertionSummary.setText("");
        }
    }

    @FXML
    private void handleFormatBody() {
        String body = requestBodyArea.getText();
        if (body != null && !body.trim().isEmpty()) {
            try {
                Object json = prettyObjectMapper.readValue(body, Object.class);
                requestBodyArea.setText(prettyObjectMapper.writeValueAsString(json));
            } catch (Exception ignored) {}
        }
    }

    @FXML
    private void handleSaveRequest() {
        ApiRequestModel current = extractRequestFromEditor();
        TextInputDialog dialog = new TextInputDialog(current.getName());
        dialog.setTitle("Save API Request");
        dialog.setHeaderText("Save request to collection");
        dialog.setContentText("Request name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            current.setName(name.trim());
            ApiCollectionService.getInstance().saveOrUpdateRequest(current, current.getCollectionName());
            refreshCollectionsTree();
            btnSave.setText("Saved!");
            PauseTransition pt = new PauseTransition(Duration.seconds(1.5));
            pt.setOnFinished(e -> btnSave.setText("Save 💾"));
            pt.play();
        });
    }

    @FXML
    private void handleNewRequest() {
        ApiRequestModel blank = new ApiRequestModel("New Request", "GET", "{{baseUrl}}/api/v1/resource");
        loadRequestIntoEditor(blank);
    }

    @FXML
    private void handleNewCollection() {
        TextInputDialog dialog = new TextInputDialog("New API Collection");
        dialog.setTitle("Create API Collection");
        dialog.setHeaderText("Create a new request collection");
        dialog.setContentText("Collection name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            ApiCollection col = new ApiCollection(name.trim());
            ApiCollectionService.getInstance().addCollection(col);
            refreshCollectionsTree();
        });
    }

    @FXML
    private void handleDuplicateRequest() {
        TreeItem<Object> selected = collectionsTreeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue() instanceof ApiRequestModel req) {
            ApiRequestModel copy = req.copy();
            ApiCollectionService.getInstance().saveOrUpdateRequest(copy, req.getCollectionName());
            refreshCollectionsTree();
        }
    }

    @FXML
    private void handleDeleteSelectedRequest() {
        TreeItem<Object> selected = collectionsTreeView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Confirmation");
            confirm.setHeaderText("Delete: " + selected.getValue());
            confirm.setContentText("Are you sure you want to permanently delete this item?");

            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                if (selected.getValue() instanceof ApiRequestModel req) {
                    ApiCollectionService.getInstance().deleteRequest(req.getId());
                } else if (selected.getValue() instanceof ApiCollection col) {
                    ApiCollectionService.getInstance().removeCollection(col.getId());
                }
                refreshCollectionsTree();
            }
        }
    }

    @FXML
    private void handleClearHistory() {
        ApiHistoryService.getInstance().clearHistory();
        refreshHistoryList();
    }

    @FXML
    private void handleAddJsonHeader() {
        headersData.removeIf(h -> "Content-Type".equalsIgnoreCase(h.getKey()));
        headersData.add(new HeaderEntry("Content-Type", "application/json"));
    }

    @FXML
    private void handleAddBearerHeader() {
        String token = EnvironmentService.getInstance().getVariable("token");
        if (token == null || token.isEmpty()) {
            token = authTokenField != null && !authTokenField.getText().isEmpty() ? authTokenField.getText() : "{{token}}";
        }
        headersData.removeIf(h -> "Authorization".equalsIgnoreCase(h.getKey()));
        headersData.add(new HeaderEntry("Authorization", "Bearer " + token));
    }

    @FXML
    private void handleAddHeaderRow() {
        headersData.add(new HeaderEntry("Custom-Header", "value"));
    }

    @FXML
    private void handlePresetSignup() {
        ApiRequestModel signupReq = new ApiRequestModel("1. Sign Up User", "POST", "{{authUrl}}/api/auth/signup");
        signupReq.setBody("{\n  \"username\": \"nexus_dev\",\n  \"password\": \"Nexus@Secure2026\",\n  \"email\": \"dev@nexus.io\"\n}");
        signupReq.getHeaders().put("Content-Type", "application/json");
        signupReq.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "201"));
        signupReq.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "token", ""));
        loadRequestIntoEditor(signupReq);
        handleSendRequest();
    }

    @FXML
    private void handlePresetLogin() {
        ApiRequestModel loginReq = new ApiRequestModel("2. Login & Get Token", "POST", "{{authUrl}}/api/auth/login");
        loginReq.setBody("{\n  \"username\": \"nexus_dev\",\n  \"password\": \"Nexus@Secure2026\"\n}");
        loginReq.getHeaders().put("Content-Type", "application/json");
        loginReq.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        loginReq.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "token", ""));
        loadRequestIntoEditor(loginReq);
        handleSendRequest();
    }

    @FXML
    private void handlePresetProfile() {
        ApiRequestModel profileReq = new ApiRequestModel("3. Protected Profile", "GET", "{{authUrl}}/api/auth/profile");
        String token = EnvironmentService.getInstance().getVariable("token");
        if (token == null || token.isEmpty()) {
            token = "{{token}}";
        }
        profileReq.getHeaders().put("Authorization", "Bearer " + token);
        profileReq.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        profileReq.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "user", ""));
        loadRequestIntoEditor(profileReq);
        handleSendRequest();
    }

    private void setupDemoApisCombo() {
        if (demoApisCombo == null) return;
        demoApisCombo.setItems(FXCollections.observableArrayList(
                "⚡ Select Demo Fake API to Test...",
                "📄 JSONPlaceholder: GET Blog Post #1",
                "👥 JSONPlaceholder: GET 10 Users List",
                "✍️ JSONPlaceholder: POST Create Post",
                "🛍️ DummyJSON: GET E-Commerce Product",
                "💬 DummyJSON: GET Random Quote",
                "🧑‍💼 ReqRes: GET Paginated Users",
                "🔐 ReqRes: POST User Login",
                "🛡️ Local Auth: POST Sign Up User",
                "🔑 Local Auth: POST Login & Token",
                "👤 Local Auth: GET Protected Profile",
                "📡 HttpBin: GET Request Diagnostics",
                "📦 HttpBin: POST Echo JSON Payload"
        ));
        demoApisCombo.setValue("⚡ Select Demo Fake API to Test...");

        demoApisCombo.setOnAction(e -> {
            String val = demoApisCombo.getValue();
            if (val == null || val.startsWith("⚡")) return;

            if (val.contains("Blog Post")) handlePresetJpPost();
            else if (val.contains("Users List")) handlePresetJpUsers();
            else if (val.contains("Create Post")) handlePresetJpCreate();
            else if (val.contains("E-Commerce Product")) handlePresetDjProduct();
            else if (val.contains("Random Quote")) handlePresetDjQuote();
            else if (val.contains("Paginated Users")) handlePresetRrUsers();
            else if (val.contains("ReqRes: POST User Login")) {
                ApiRequestModel rrLogin = new ApiRequestModel("Mock User Login", "POST", "https://reqres.in/api/login");
                rrLogin.setBody("{\n  \"email\": \"eve.holt@reqres.in\",\n  \"password\": \"cityslicka\"\n}");
                rrLogin.getHeaders().put("Content-Type", "application/json");
                rrLogin.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
                rrLogin.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "token", ""));
                loadRequestIntoEditor(rrLogin);
                handleSendRequest();
            }
            else if (val.contains("Local Auth: POST Sign Up")) handlePresetSignup();
            else if (val.contains("Local Auth: POST Login")) handlePresetLogin();
            else if (val.contains("Local Auth: GET Protected")) handlePresetProfile();
            else if (val.contains("HttpBin: GET")) handlePresetGet();
            else if (val.contains("HttpBin: POST")) handlePresetPost();
        });
    }

    @FXML
    private void handlePresetJpPost() {
        ApiRequestModel req = new ApiRequestModel("Get Blog Post #1", "GET", "https://jsonplaceholder.typicode.com/posts/1");
        req.getHeaders().put("Accept", "application/json");
        req.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        req.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "title", ""));
        loadRequestIntoEditor(req);
        handleSendRequest();
    }

    @FXML
    private void handlePresetJpUsers() {
        ApiRequestModel req = new ApiRequestModel("Get Users Directory", "GET", "https://jsonplaceholder.typicode.com/users");
        req.getHeaders().put("Accept", "application/json");
        req.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        req.getAssertions().add(new ApiAssertion(ApiAssertion.Type.BODY_CONTAINS, "", "Leanne Graham"));
        loadRequestIntoEditor(req);
        handleSendRequest();
    }

    @FXML
    private void handlePresetJpCreate() {
        ApiRequestModel req = new ApiRequestModel("Create Post", "POST", "https://jsonplaceholder.typicode.com/posts");
        req.setBody("{\n  \"title\": \"NEXUS AI Developer Workspace\",\n  \"body\": \"Demonstrating real API testing with live assertions to Sir.\",\n  \"userId\": 1\n}");
        req.getHeaders().put("Content-Type", "application/json");
        req.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "201"));
        req.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "id", ""));
        loadRequestIntoEditor(req);
        handleSendRequest();
    }

    @FXML
    private void handlePresetDjProduct() {
        ApiRequestModel req = new ApiRequestModel("Product Details (Mascara)", "GET", "https://dummyjson.com/products/1");
        req.getHeaders().put("Accept", "application/json");
        req.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        req.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "price", ""));
        loadRequestIntoEditor(req);
        handleSendRequest();
    }

    @FXML
    private void handlePresetDjQuote() {
        ApiRequestModel req = new ApiRequestModel("Random Inspirational Quote", "GET", "https://dummyjson.com/quotes/random");
        req.getHeaders().put("Accept", "application/json");
        req.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        req.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "quote", ""));
        loadRequestIntoEditor(req);
        handleSendRequest();
    }

    @FXML
    private void handlePresetRrUsers() {
        ApiRequestModel req = new ApiRequestModel("List Users Page 1", "GET", "https://reqres.in/api/users?page=1");
        req.getHeaders().put("Accept", "application/json");
        req.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        req.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "data", ""));
        loadRequestIntoEditor(req);
        handleSendRequest();
    }

    @FXML
    private void handleResetDemos() {
        ApiCollectionService.getInstance().resetToDefaultDemoCollections();
        refreshCollectionsTree();
    }

    @FXML
    private void handleAddAssertionRow() {
        ChoiceDialog<ApiAssertion.Type> typeDialog = new ChoiceDialog<>(
                ApiAssertion.Type.STATUS_CODE_EQUALS,
                ApiAssertion.Type.values()
        );
        typeDialog.setTitle("Add Assertion");
        typeDialog.setHeaderText("Select assertion rule type");
        typeDialog.setContentText("Type:");

        Optional<ApiAssertion.Type> typeOpt = typeDialog.showAndWait();
        if (typeOpt.isPresent()) {
            ApiAssertion.Type type = typeOpt.get();
            String prop = type == ApiAssertion.Type.JSON_FIELD_EQUALS || type == ApiAssertion.Type.JSON_FIELD_EXISTS ? "status" : "";
            String expected = type == ApiAssertion.Type.STATUS_CODE_EQUALS ? "200" :
                    (type == ApiAssertion.Type.RESPONSE_TIME_LESS_THAN ? "1000" : "success");
            assertionsData.add(new ApiAssertion(type, prop, expected));
        }
    }

    @FXML
    private void handleImportOpenApi() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select OpenAPI 3.0 or Swagger Spec (JSON / YAML)");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("API Specifications", "*.json", "*.yaml", "*.yml"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        Window window = btnImportOpenApi.getScene() != null ? btnImportOpenApi.getScene().getWindow() : null;
        File file = chooser.showOpenDialog(window);

        if (file != null) {
            try {
                ApiCollection imported = OpenApiImportService.getInstance().importFromFile(file);
                ApiCollectionService.getInstance().addCollection(imported);
                refreshCollectionsTree();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("OpenAPI Specification Imported");
                alert.setHeaderText("Successfully imported: " + imported.getName());
                alert.setContentText("Imported " + imported.getRequests().size() + " endpoints into collections.");
                alert.showAndWait();
            } catch (Exception e) {
                Alert err = new Alert(Alert.AlertType.ERROR, "Failed to import OpenAPI specification: " + e.getMessage());
                err.showAndWait();
            }
        }
    }

    @FXML
    private void handleManageEnvironments() {
        Dialog<ButtonType> envDialog = new Dialog<>();
        envDialog.setTitle("API Environments Manager");
        envDialog.setHeaderText("Manage Environment Variables");

        EnvironmentModel activeEnv = EnvironmentService.getInstance().getActiveEnvironment();
        if (activeEnv == null) return;

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.add(new Label("Environment:"), 0, 0);
        grid.add(new Label(activeEnv.getName()), 1, 0);

        grid.add(new Label("baseUrl:"), 0, 1);
        TextField baseField = new TextField(activeEnv.getVariable("baseUrl") != null ? activeEnv.getVariable("baseUrl") : "");
        baseField.setPrefWidth(300);
        grid.add(baseField, 1, 1);

        grid.add(new Label("token:"), 0, 2);
        PasswordField tokenField = new PasswordField();
        tokenField.setText(activeEnv.getVariable("token") != null ? activeEnv.getVariable("token") : "");
        tokenField.setPrefWidth(300);
        grid.add(tokenField, 1, 2);

        envDialog.getDialogPane().setContent(grid);
        envDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> res = envDialog.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            activeEnv.setVariable("baseUrl", baseField.getText().trim());
            activeEnv.setVariable("token", tokenField.getText().trim());
            EnvironmentService.getInstance().saveEnvironments();
        }
    }

    @FXML
    private void handlePresetHealth() {
        loadRequestIntoEditor(new ApiRequestModel("Health Check", "GET", "{{baseUrl}}/health"));
        handleSendRequest();
    }

    @FXML
    private void handlePresetPost() {
        ApiRequestModel postReq = new ApiRequestModel("Echo POST", "POST", "https://httpbin.org/post");
        postReq.setBody("{\n  \"action\": \"test_payload\",\n  \"client\": \"NEXUS-Studio\",\n  \"status\": \"ready\"\n}");
        postReq.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        loadRequestIntoEditor(postReq);
        handleSendRequest();
    }

    @FXML
    private void handlePresetGet() {
        ApiRequestModel getReq = new ApiRequestModel("Query Test", "GET", "https://httpbin.org/get");
        getReq.getParams().put("env", "staging");
        getReq.getParams().put("limit", "25");
        getReq.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        loadRequestIntoEditor(getReq);
        handleSendRequest();
    }

    @FXML
    private void handleCopyResponse() {
        if (responseBodyArea != null && !responseBodyArea.getText().isEmpty()) {
            UiUtils.copyToClipboard(responseBodyArea.getText());
            btnCopyResponse.setText("Copied!");
            PauseTransition pt = new PauseTransition(Duration.seconds(1.5));
            pt.setOnFinished(e -> btnCopyResponse.setText("Copy Body"));
            pt.play();
        }
    }

    @FXML
    private void handleExplainApiAi() {
        if (onAskAiListener != null) {
            String method = methodCombo.getValue();
            String url = urlField.getText().trim();
            String status = lblResponseStatus.getText();
            String bodySnippet = responseBodyArea.getText();
            if (bodySnippet.length() > 300) {
                bodySnippet = bodySnippet.substring(0, 300) + "...";
            }
            onAskAiListener.accept("Explain API response for [" + method + " " + url + "] with status [" +
                    status + "] and body snippet:\n" + bodySnippet);
        }
    }

    public static class HeaderEntry {
        private final SimpleStringProperty key;
        private final SimpleStringProperty value;

        public HeaderEntry(String key, String value) {
            this.key = new SimpleStringProperty(key != null ? key : "");
            this.value = new SimpleStringProperty(value != null ? value : "");
        }

        public String getKey() { return key.get(); }
        public void setKey(String k) { this.key.set(k); }
        public SimpleStringProperty keyProperty() { return key; }

        public String getValue() { return value.get(); }
        public void setValue(String v) { this.value.set(v); }
        public SimpleStringProperty valueProperty() { return value; }
    }
}
