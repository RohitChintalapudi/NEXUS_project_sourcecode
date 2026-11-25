package com.nexus.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class ApiHistoryItem {
    private String id;
    private long timestamp;
    private String method;
    private String url;
    private int statusCode;
    private long responseTimeMs;
    private long responseSizeBytes;
    private ApiRequestModel requestSnapshot;

    public ApiHistoryItem() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    public ApiHistoryItem(ApiRequestModel request, ApiResponseModel response) {
        this();
        if (request != null) {
            this.method = request.getMethod();
            this.url = request.getUrl();
            this.requestSnapshot = request.copy();
        }
        if (response != null) {
            this.statusCode = response.getStatusCode();
            this.responseTimeMs = response.getResponseTimeMs();
            this.responseSizeBytes = response.getResponseSizeBytes();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedTime() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date(timestamp));
    }

    public String getMethod() {
        return method != null ? method : "GET";
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url != null ? url : "";
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public long getResponseSizeBytes() {
        return responseSizeBytes;
    }

    public void setResponseSizeBytes(long responseSizeBytes) {
        this.responseSizeBytes = responseSizeBytes;
    }

    public ApiRequestModel getRequestSnapshot() {
        return requestSnapshot;
    }

    public void setRequestSnapshot(ApiRequestModel requestSnapshot) {
        this.requestSnapshot = requestSnapshot;
    }
}

