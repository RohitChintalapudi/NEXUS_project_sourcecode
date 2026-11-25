package com.nexus.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

public class LLMProviderService {

    private static final LLMProviderService INSTANCE = new LLMProviderService();

    private String apiKey;
    private String baseUrl;
    private String modelName;
    private double temperature;

    private LLMProviderService() {
        loadConfiguration();
    }

    public static LLMProviderService getInstance() {
        return INSTANCE;
    }

    public void loadConfiguration() {
        // 1. Environment variables
        String envKey = System.getenv("OPENAI_API_KEY");
        if (envKey == null || envKey.trim().isEmpty()) {
            envKey = System.getenv("NEXUS_AI_API_KEY");
        }

        String envBaseUrl = System.getenv("OPENAI_BASE_URL");
        if (envBaseUrl == null || envBaseUrl.trim().isEmpty()) {
            envBaseUrl = System.getenv("NEXUS_AI_BASE_URL");
        }

        String envModel = System.getenv("OPENAI_MODEL_NAME");
        if (envModel == null || envModel.trim().isEmpty()) {
            envModel = System.getenv("NEXUS_AI_MODEL");
        }

        // 2. Check local config file ~/.nexus/config.properties
        File configFile = new File(System.getProperty("user.home"), ".nexus/config.properties");
        if (configFile.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                if (envKey == null) envKey = props.getProperty("ai.api.key");
                if (envBaseUrl == null) envBaseUrl = props.getProperty("ai.base.url");
                if (envModel == null) envModel = props.getProperty("ai.model.name");
            } catch (IOException ignored) {}
        }

        this.apiKey = envKey != null ? envKey.trim() : null;
        this.baseUrl = (envBaseUrl != null && !envBaseUrl.trim().isEmpty()) ? envBaseUrl.trim() : "https://api.openai.com/v1";
        this.modelName = (envModel != null && !envModel.trim().isEmpty()) ? envModel.trim() : "gpt-4o-mini";
        this.temperature = 0.2;
    }

    public boolean hasValidApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equals("demo-key");
    }

    public ChatLanguageModel buildChatModel() {
        if (!hasValidApiKey()) {
            return null;
        }

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(60))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    public StreamingChatLanguageModel buildStreamingChatModel() {
        if (!hasValidApiKey()) {
            return null;
        }

        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}

