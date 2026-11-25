package com.nexus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.model.ApiAssertion;
import com.nexus.model.ApiResponseModel;

import java.util.ArrayList;
import java.util.List;

public class ApiAssertionService {

    private static final ApiAssertionService INSTANCE = new ApiAssertionService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ApiAssertionService() {}

    public static ApiAssertionService getInstance() {
        return INSTANCE;
    }

    public List<ApiAssertion> evaluateAssertions(List<ApiAssertion> assertions, ApiResponseModel response) {
        List<ApiAssertion> results = new ArrayList<>();
        if (assertions == null || assertions.isEmpty()) {
            return results;
        }

        JsonNode jsonBody = null;
        if (response.getBody() != null && !response.getBody().trim().isEmpty()) {
            try {
                jsonBody = objectMapper.readTree(response.getBody());
            } catch (Exception ignored) {}
        }

        for (ApiAssertion a : assertions) {
            ApiAssertion evaluated = new ApiAssertion(a.getType(), a.getProperty(), a.getExpectedValue());
            try {
                switch (a.getType()) {
                    case STATUS_CODE_EQUALS -> {
                        int expected = Integer.parseInt(a.getExpectedValue().trim());
                        evaluated.setActualValue(String.valueOf(response.getStatusCode()));
                        boolean passed = response.getStatusCode() == expected;
                        evaluated.setPassed(passed);
                        evaluated.setMessage(passed ? "Status code is " + expected :
                                "Expected status " + expected + " but got " + response.getStatusCode());
                    }
                    case BODY_CONTAINS -> {
                        String expected = a.getExpectedValue() != null ? a.getExpectedValue() : "";
                        boolean passed = response.getBody() != null && response.getBody().contains(expected);
                        evaluated.setActualValue(passed ? "(contains text)" : "(not found)");
                        evaluated.setPassed(passed);
                        evaluated.setMessage(passed ? "Body contains \"" + expected + "\"" :
                                "Body does not contain expected substring \"" + expected + "\"");
                    }
                    case JSON_FIELD_EQUALS -> {
                        if (jsonBody == null) {
                            evaluated.setPassed(false);
                            evaluated.setActualValue("null (non-JSON body)");
                            evaluated.setMessage("Response body is not valid JSON.");
                        } else {
                            JsonNode node = findJsonNodeByPath(jsonBody, a.getProperty());
                            String actualVal = node != null ? node.asText() : "null";
                            evaluated.setActualValue(actualVal);
                            boolean passed = actualVal.equals(a.getExpectedValue());
                            evaluated.setPassed(passed);
                            evaluated.setMessage(passed ? "JSON field [" + a.getProperty() + "] equals \"" + a.getExpectedValue() + "\"" :
                                    "JSON field [" + a.getProperty() + "] was \"" + actualVal + "\", expected \"" + a.getExpectedValue() + "\"");
                        }
                    }
                    case JSON_FIELD_EXISTS -> {
                        if (jsonBody == null) {
                            evaluated.setPassed(false);
                            evaluated.setActualValue("false");
                            evaluated.setMessage("Response body is not valid JSON.");
                        } else {
                            JsonNode node = findJsonNodeByPath(jsonBody, a.getProperty());
                            boolean exists = node != null && !node.isMissingNode() && !node.isNull();
                            evaluated.setActualValue(String.valueOf(exists));
                            evaluated.setPassed(exists);
                            evaluated.setMessage(exists ? "JSON field [" + a.getProperty() + "] exists." :
                                    "JSON field [" + a.getProperty() + "] does not exist.");
                        }
                    }
                    case RESPONSE_TIME_LESS_THAN -> {
                        long maxMs = Long.parseLong(a.getExpectedValue().trim());
                        evaluated.setActualValue(response.getResponseTimeMs() + " ms");
                        boolean passed = response.getResponseTimeMs() < maxMs;
                        evaluated.setPassed(passed);
                        evaluated.setMessage(passed ? "Response time (" + response.getResponseTimeMs() + " ms) < " + maxMs + " ms" :
                                "Response time (" + response.getResponseTimeMs() + " ms) exceeded threshold of " + maxMs + " ms");
                    }
                }
            } catch (Exception e) {
                evaluated.setPassed(false);
                evaluated.setActualValue("Error");
                evaluated.setMessage("Evaluation error: " + e.getMessage());
            }
            results.add(evaluated);
        }

        return results;
    }

    private JsonNode findJsonNodeByPath(JsonNode root, String path) {
        if (root == null || path == null || path.trim().isEmpty()) {
            return null;
        }

        String[] tokens = path.split("\\.");
        JsonNode current = root;
        for (String token : tokens) {
            token = token.trim();
            if (current == null) return null;

            // Handle array index e.g. items[0]
            if (token.contains("[") && token.endsWith("]")) {
                int bracket = token.indexOf('[');
                String prop = token.substring(0, bracket);
                int index = Integer.parseInt(token.substring(bracket + 1, token.length() - 1));
                current = current.path(prop).path(index);
            } else {
                current = current.path(token);
            }
        }
        return (current != null && !current.isMissingNode()) ? current : null;
    }
}

