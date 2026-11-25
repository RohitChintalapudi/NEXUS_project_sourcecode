package com.nexus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.nexus.model.ApiCollection;
import com.nexus.model.ApiRequestModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;

public class OpenApiImportService {

    private static final OpenApiImportService INSTANCE = new OpenApiImportService();
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    private OpenApiImportService() {}

    public static OpenApiImportService getInstance() {
        return INSTANCE;
    }

    public ApiCollection importFromFile(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File does not exist.");
        }
        String content = Files.readString(file.toPath());
        return importFromString(content, file.getName().endsWith(".yaml") || file.getName().endsWith(".yml"));
    }

    public ApiCollection importFromString(String specContent, boolean isYaml) throws IOException {
        if (specContent == null || specContent.trim().isEmpty()) {
            throw new IllegalArgumentException("API specification content is empty.");
        }

        JsonNode root;
        if (isYaml) {
            root = yamlMapper.readTree(specContent);
        } else {
            // Try JSON first, fallback to YAML if JSON parse fails
            try {
                root = jsonMapper.readTree(specContent);
            } catch (Exception e) {
                root = yamlMapper.readTree(specContent);
            }
        }

        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("Invalid OpenAPI/Swagger specification format.");
        }

        // Title and description
        String title = "Imported API Specification";
        String description = "Imported OpenAPI/Swagger collection";
        if (root.has("info")) {
            JsonNode info = root.get("info");
            if (info.has("title")) title = info.get("title").asText();
            if (info.has("description")) description = info.get("description").asText();
        }

        ApiCollection collection = new ApiCollection(title, description);

        // Base URL from servers or host
        String baseUrl = "https://api.example.com";
        if (root.has("servers") && root.get("servers").isArray() && root.get("servers").size() > 0) {
            JsonNode firstServer = root.get("servers").get(0);
            if (firstServer.has("url")) {
                baseUrl = firstServer.get("url").asText();
            }
        } else if (root.has("host")) {
            String scheme = (root.has("schemes") && root.get("schemes").size() > 0) ?
                    root.get("schemes").get(0).asText() : "https";
            String basePath = root.has("basePath") ? root.get("basePath").asText() : "";
            baseUrl = scheme + "://" + root.get("host").asText() + basePath;
        }

        // Extract Paths
        if (root.has("paths") && root.get("paths").isObject()) {
            JsonNode pathsNode = root.get("paths");
            Iterator<Map.Entry<String, JsonNode>> pathFields = pathsNode.fields();

            while (pathFields.hasNext()) {
                Map.Entry<String, JsonNode> pathEntry = pathFields.next();
                String path = pathEntry.getKey();
                JsonNode pathItem = pathEntry.getValue();

                Iterator<Map.Entry<String, JsonNode>> methodFields = pathItem.fields();
                while (methodFields.hasNext()) {
                    Map.Entry<String, JsonNode> methodEntry = methodFields.next();
                    String method = methodEntry.getKey().toUpperCase();

                    // Only standard HTTP methods
                    if (!isHttpMethod(method)) {
                        continue;
                    }

                    JsonNode operation = methodEntry.getValue();
                    String summary = operation.has("summary") ? operation.get("summary").asText() :
                            (operation.has("operationId") ? operation.get("operationId").asText() : method + " " + path);

                    String fullUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
                    fullUrl = fullUrl + (path.startsWith("/") ? path : "/" + path);

                    ApiRequestModel request = new ApiRequestModel(summary, method, fullUrl);
                    request.setCollectionName(title);

                    // Extract query parameters
                    if (operation.has("parameters") && operation.get("parameters").isArray()) {
                        for (JsonNode param : operation.get("parameters")) {
                            if (param.has("in") && "query".equalsIgnoreCase(param.get("in").asText())) {
                                String paramName = param.get("name").asText();
                                String example = param.has("example") ? param.get("example").asText() : "";
                                request.getParams().put(paramName, example);
                            }
                        }
                    }

                    // Extract sample request body if present
                    if (operation.has("requestBody")) {
                        request.setBody("{\n  \"sampleKey\": \"sampleValue\"\n}");
                    }

                    collection.addRequest(request);
                }
            }
        }

        return collection;
    }

    private boolean isHttpMethod(String method) {
        return "GET".equals(method) || "POST".equals(method) || "PUT".equals(method) ||
                "DELETE".equals(method) || "PATCH".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    }
}

