package com.nexus.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nexus.model.ApiHistoryItem;
import com.nexus.model.ApiRequestModel;
import com.nexus.model.ApiResponseModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ApiHistoryService {

    private static final ApiHistoryService INSTANCE = new ApiHistoryService();
    private static final int MAX_HISTORY = 50;

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Path configDir = Paths.get(System.getProperty("user.home"), ".nexus");
    private final File historyFile = configDir.resolve("api_history.json").toFile();

    private final List<ApiHistoryItem> historyItems = new ArrayList<>();

    private ApiHistoryService() {
        loadHistory();
    }

    public static ApiHistoryService getInstance() {
        return INSTANCE;
    }

    public synchronized void loadHistory() {
        historyItems.clear();
        if (historyFile.exists()) {
            try {
                List<ApiHistoryItem> loaded = objectMapper.readValue(historyFile, new TypeReference<List<ApiHistoryItem>>() {});
                if (loaded != null) {
                    historyItems.addAll(loaded);
                }
            } catch (Exception ignored) {}
        }
    }

    public synchronized void recordExecution(ApiRequestModel request, ApiResponseModel response) {
        if (request == null) return;
        ApiHistoryItem item = new ApiHistoryItem(request, response);
        historyItems.add(0, item);

        if (historyItems.size() > MAX_HISTORY) {
            historyItems.subList(MAX_HISTORY, historyItems.size()).clear();
        }

        saveHistory();
    }

    public synchronized void saveHistory() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            objectMapper.writeValue(historyFile, historyItems);
        } catch (IOException ignored) {}
    }

    public List<ApiHistoryItem> getHistory() {
        return Collections.unmodifiableList(historyItems);
    }

    public synchronized void clearHistory() {
        historyItems.clear();
        saveHistory();
    }
}

