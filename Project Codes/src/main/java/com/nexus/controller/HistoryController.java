package com.nexus.controller;

import com.nexus.component.Badge;
import com.nexus.model.CommandHistoryItem;
import com.nexus.model.NavItem;
import com.nexus.service.CommandHistoryService;
import com.nexus.service.NavigationService;
import com.nexus.service.TerminalProcessManager;
import com.nexus.service.TerminalSession;
import com.nexus.util.UiUtils;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;

public class HistoryController implements Initializable {

    @FXML private Label lblTotalCommands;
    @FXML private Label lblCommandsStatus;
    @FXML private Label lblSuccessRate;
    @FXML private Label lblSuccessCounts;
    @FXML private Label lblAvgDuration;
    @FXML private Label lblFastestExecution;
    @FXML private Label lblTopTool;
    @FXML private Label lblTopToolShare;
    @FXML private Label lblSampleTotal;

    @FXML private ComboBox<String> filterToolCombo;
    @FXML private TextField searchHistoryField;
    @FXML private Button btnClearHistory;

    @FXML private TableView<CommandHistoryItem> historyTable;
    @FXML private TableColumn<CommandHistoryItem, String> colTimestamp;
    @FXML private TableColumn<CommandHistoryItem, String> colCommand;
    @FXML private TableColumn<CommandHistoryItem, String> colTool;
    @FXML private TableColumn<CommandHistoryItem, String> colStatus;
    @FXML private TableColumn<CommandHistoryItem, String> colDuration;
    @FXML private TableColumn<CommandHistoryItem, String> colDirectory;

    private final ObservableList<CommandHistoryItem> filteredList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFilterControls();
        setupTableColumns();

        // Listen for new history items from CommandHistoryService
        CommandHistoryService.getInstance().getHistoryObservable().addListener((ListChangeListener<CommandHistoryItem>) c -> {
            filterData();
            updateLiveAnalytics();
        });

        filterData();
        updateLiveAnalytics();
    }

    private void setupFilterControls() {
        filterToolCombo.setItems(FXCollections.observableArrayList("All Tools", "maven", "git", "docker", "npm", "cargo", "powershell", "cmd", "java", "system"));
        filterToolCombo.setValue("All Tools");
        filterToolCombo.setOnAction(e -> filterData());

        searchHistoryField.textProperty().addListener((obs, oldV, newV) -> filterData());
    }

    private void setupTableColumns() {
        colTimestamp.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedDate()));
        colCommand.setCellValueFactory(cellData -> new SimpleStringProperty("$ " + cellData.getValue().getCommand()));
        colTool.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTool().toUpperCase()));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    CommandHistoryItem historyItem = getTableRow().getItem();
                    boolean isSuccess = "SUCCESS".equalsIgnoreCase(historyItem.getStatus());
                    boolean isCancelled = "CANCELLED".equalsIgnoreCase(historyItem.getStatus());
                    Badge badge = new Badge(
                            historyItem.getStatus(),
                            isSuccess ? Badge.Type.SUCCESS :
                            isCancelled ? Badge.Type.WARNING : Badge.Type.DANGER
                    );
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        colDuration.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedDuration()));
        colDirectory.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDirectory()));

        colCommand.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 11px;");
        colTimestamp.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
        colDuration.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 11px;");
        colDirectory.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 11px; -fx-text-fill: -color-text-muted;");

        historyTable.setItems(filteredList);
    }

    private void filterData() {
        String selectedTool = filterToolCombo.getValue();
        String query = searchHistoryField.getText() != null ? searchHistoryField.getText().toLowerCase().trim() : "";

        List<CommandHistoryItem> master = CommandHistoryService.getInstance().getHistoryObservable();
        List<CommandHistoryItem> results = master.stream().filter(item -> {
            boolean matchTool = "All Tools".equals(selectedTool) || item.getTool().equalsIgnoreCase(selectedTool);
            boolean matchQuery = query.isEmpty() ||
                    item.getCommand().toLowerCase().contains(query) ||
                    item.getDirectory().toLowerCase().contains(query);
            return matchTool && matchQuery;
        }).toList();

        filteredList.setAll(results);
    }

    private void updateLiveAnalytics() {
        List<CommandHistoryItem> all = CommandHistoryService.getInstance().getHistoryObservable();
        int total = all.size();
        lblTotalCommands.setText(String.format("%,d", total));
        lblSampleTotal.setText("Total Recorded: " + total + " commands");

        if (total == 0) {
            lblSuccessRate.setText("100%");
            lblSuccessCounts.setText("0 Succeeded · 0 Failed");
            lblAvgDuration.setText("0 ms");
            lblTopTool.setText("None");
            return;
        }

        long successCount = all.stream().filter(i -> "SUCCESS".equalsIgnoreCase(i.getStatus())).count();
        long failedCount = total - successCount;
        double successRate = (successCount * 100.0) / total;
        lblSuccessRate.setText(String.format("%.1f%%", successRate));
        lblSuccessCounts.setText(successCount + " Succeeded · " + failedCount + " Failed");

        long totalDuration = all.stream().mapToLong(CommandHistoryItem::getDurationMs).sum();
        long avgDuration = totalDuration / total;
        lblAvgDuration.setText(avgDuration < 1000 ? avgDuration + " ms" : String.format("%.2f s", avgDuration / 1000.0));

        // Top tool calculation
        Map<String, Long> toolCounts = new HashMap<>();
        for (CommandHistoryItem item : all) {
            toolCounts.merge(item.getTool(), 1L, Long::sum);
        }

        toolCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(top -> {
                    lblTopTool.setText(top.getKey().toUpperCase());
                    double pct = (top.getValue() * 100.0) / total;
                    lblTopToolShare.setText(String.format("%.0f%% of total executions (%d runs)", pct, top.getValue()));
                });
    }

    @FXML
    private void handleClearHistory() {
        CommandHistoryService.getInstance().clear();
        btnClearHistory.setText("Cleared!");
        PauseTransition pt = new PauseTransition(Duration.seconds(1.5));
        pt.setOnFinished(e -> btnClearHistory.setText("Clear Log"));
        pt.play();
    }
}
