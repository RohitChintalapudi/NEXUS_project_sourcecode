package com.nexus.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ApiResponseModel {
    private final int statusCode;
    private final String statusText;
    private final long responseTimeMs;
    private final long responseSizeBytes;
    private final String body;
    private final Map<String, String> headers;
    private final List<ApiAssertion> assertionResults;
    private final boolean isNetworkError;
    private final String errorMessage;

    public ApiResponseModel(int statusCode, String statusText, long responseTimeMs, long responseSizeBytes,
                            String body, Map<String, String> headers) {
        this(statusCode, statusText, responseTimeMs, responseSizeBytes, body, headers, Collections.emptyList(), false, null);
    }

    public ApiResponseModel(int statusCode, String statusText, long responseTimeMs, long responseSizeBytes,
                            String body, Map<String, String> headers, List<ApiAssertion> assertionResults,
                            boolean isNetworkError, String errorMessage) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.responseTimeMs = responseTimeMs;
        this.responseSizeBytes = responseSizeBytes;
        this.body = body != null ? body : "";
        this.headers = headers != null ? headers : Collections.emptyMap();
        this.assertionResults = assertionResults != null ? new ArrayList<>(assertionResults) : new ArrayList<>();
        this.isNetworkError = isNetworkError;
        this.errorMessage = errorMessage;
    }

    public static ApiResponseModel networkError(String errorMessage, long responseTimeMs) {
        return new ApiResponseModel(0, "Network Error", responseTimeMs, 0,
                "Network Error: " + errorMessage, Collections.emptyMap(), Collections.emptyList(), true, errorMessage);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusText() {
        return statusText;
    }

    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    public boolean isNetworkError() {
        return isNetworkError;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public long getResponseSizeBytes() {
        return responseSizeBytes;
    }

    public String getFormattedSize() {
        if (responseSizeBytes < 1024) {
            return responseSizeBytes + " B";
        } else if (responseSizeBytes < 1024 * 1024) {
            return String.format("%.2f KB", responseSizeBytes / 1024.0);
        } else {
            return String.format("%.2f MB", responseSizeBytes / (1024.0 * 1024.0));
        }
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public List<ApiAssertion> getAssertionResults() {
        return assertionResults;
    }

    public int getPassedAssertionsCount() {
        int passed = 0;
        for (ApiAssertion a : assertionResults) {
            if (a.isPassed()) passed++;
        }
        return passed;
    }
}
