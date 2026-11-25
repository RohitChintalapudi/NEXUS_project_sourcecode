package com.nexus.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nexus.model.EnvironmentModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnvironmentService {

    private static final EnvironmentService INSTANCE = new EnvironmentService();
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_.-]+)\\}\\}");

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Path configDir = Paths.get(System.getProperty("user.home"), ".nexus");
    private final File envFile = configDir.resolve("api_environments.json").toFile();

    private final List<EnvironmentModel> environments = new ArrayList<>();
    private EnvironmentModel activeEnvironment;

    private EnvironmentService() {
        loadEnvironments();
    }

    public static EnvironmentService getInstance() {
        return INSTANCE;
    }

    public synchronized void loadEnvironments() {
        environments.clear();
        if (envFile.exists()) {
            try {
                List<EnvironmentModel> loaded = objectMapper.readValue(envFile, new TypeReference<List<EnvironmentModel>>() {});
                if (loaded != null && !loaded.isEmpty()) {
                    environments.addAll(loaded);
                }
            } catch (Exception ignored) {}
        }

        if (environments.isEmpty()) {
            initDefaultEnvironments();
            saveEnvironments();
        }

        activeEnvironment = environments.get(0);
    }

    private void initDefaultEnvironments() {
        EnvironmentModel local = new EnvironmentModel("Local");
        local.setVariable("baseUrl", "http://localhost:8080");
        local.setVariable("token", "nexus_dev_token_abc123");
        local.setVariable("timeout", "5000");

        EnvironmentModel dev = new EnvironmentModel("Development");
        dev.setVariable("baseUrl", "https://api.nexus.internal/v1");
        dev.setVariable("token", "nexus_live_token_7af94e1c2b8a");
        dev.setVariable("timeout", "8000");

        EnvironmentModel prod = new EnvironmentModel("Production");
        prod.setVariable("baseUrl", "https://api.nexus.com/v1");
        prod.setVariable("token", "nexus_prod_key_9988776655");
        prod.setVariable("timeout", "10000");

        environments.add(local);
        environments.add(dev);
        environments.add(prod);
    }

    public synchronized void saveEnvironments() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            objectMapper.writeValue(envFile, environments);
        } catch (IOException ignored) {}
    }

    public List<EnvironmentModel> getEnvironments() {
        return environments;
    }

    public EnvironmentModel getActiveEnvironment() {
        if (activeEnvironment == null && !environments.isEmpty()) {
            activeEnvironment = environments.get(0);
        }
        return activeEnvironment;
    }

    public void setActiveEnvironment(EnvironmentModel env) {
        if (env != null) {
            this.activeEnvironment = env;
        }
    }

    public void setActiveEnvironmentByName(String name) {
        if (name == null) return;
        for (EnvironmentModel env : environments) {
            if (env.getName().equalsIgnoreCase(name.trim())) {
                this.activeEnvironment = env;
                return;
            }
        }
    }

    public void setVariable(String key, String value) {
        EnvironmentModel current = getActiveEnvironment();
        if (current != null) {
            current.setVariable(key, value);
        }
    }

    public String getVariable(String key) {
        EnvironmentModel current = getActiveEnvironment();
        return current != null ? current.getVariable(key) : null;
    }

    /**
     * Resolves all {{variable}} placeholders using variables from the active environment.
     */
    public String resolveVariables(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        EnvironmentModel current = getActiveEnvironment();
        if (current == null) {
            return text;
        }

        Matcher matcher = VAR_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String val = current.getVariable(varName);
            if (val != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(val));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}

