package com.nexus.model;

import java.util.*;

public class ApiRequestModel {
    private String id;
    private String name;
    private String collectionName;
    private String method; // GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
    private String url;
    private final Map<String, String> params = new LinkedHashMap<>();
    private final Map<String, String> headers = new LinkedHashMap<>();
    private String body;
    private String authType; // None, Bearer, Basic, API Key
    private String authToken;
    private String authKey; // For API_KEY auth (e.g., X-API-Key or apiKey)
    private String authUsername; // For BASIC_AUTH
    private String authPassword; // For BASIC_AUTH
    private List<ApiAssertion> assertions = new ArrayList<>();

    public ApiRequestModel() {
        this.id = UUID.randomUUID().toString();
        this.method = "GET";
        this.url = "https://httpbin.org/get";
        this.authType = "No Auth";
    }

    public ApiRequestModel(String method, String url) {
        this();
        this.method = method;
        this.url = url;
    }

    public ApiRequestModel(String name, String method, String url) {
        this(method, url);
        this.name = name;
    }

    public String getId() {
        if (id == null) id = UUID.randomUUID().toString();
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return (name != null && !name.trim().isEmpty()) ? name : (getMethod() + " " + (url != null ? url : ""));
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCollectionName() {
        return collectionName != null ? collectionName : "Default Collection";
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getMethod() {
        return method != null ? method.toUpperCase() : "GET";
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

    public Map<String, String> getParams() {
        return params;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAuthType() {
        return authType != null ? authType : "No Auth";
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public ApiAuthType getParsedAuthType() {
        return ApiAuthType.fromDisplayName(authType);
    }

    public void setParsedAuthType(ApiAuthType type) {
        this.authType = type != null ? type.getDisplayName() : "No Auth";
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getAuthKey() {
        return (authKey != null && !authKey.trim().isEmpty()) ? authKey : "X-API-Key";
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public void setAuthUsername(String authUsername) {
        this.authUsername = authUsername;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }

    public List<ApiAssertion> getAssertions() {
        if (assertions == null) assertions = new ArrayList<>();
        return assertions;
    }

    public void setAssertions(List<ApiAssertion> assertions) {
        this.assertions = assertions != null ? assertions : new ArrayList<>();
    }

    public ApiRequestModel copy() {
        ApiRequestModel clone = new ApiRequestModel();
        clone.setId(this.getId());
        clone.setName(this.name);
        clone.setCollectionName(this.collectionName);
        clone.setMethod(this.method);
        clone.setUrl(this.url);
        clone.setBody(this.body);
        clone.setAuthType(this.authType);
        clone.setAuthToken(this.authToken);
        clone.setAuthKey(this.authKey);
        clone.setAuthUsername(this.authUsername);
        clone.setAuthPassword(this.authPassword);
        clone.getHeaders().putAll(this.getHeaders());
        clone.getParams().putAll(this.getParams());
        for (ApiAssertion a : this.getAssertions()) {
            clone.getAssertions().add(new ApiAssertion(a.getType(), a.getProperty(), a.getExpectedValue()));
        }
        return clone;
    }

    public ApiRequestModel duplicate() {
        ApiRequestModel dup = copy();
        dup.setId(UUID.randomUUID().toString());
        dup.setName((this.name != null ? this.name : "Request") + " (Copy)");
        return dup;
    }
}
