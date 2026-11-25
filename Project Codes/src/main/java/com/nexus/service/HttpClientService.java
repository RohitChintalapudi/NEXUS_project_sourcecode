package com.nexus.service;

import com.nexus.model.ApiAssertion;
import com.nexus.model.ApiAuthType;
import com.nexus.model.ApiRequestModel;
import com.nexus.model.ApiResponseModel;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class HttpClientService {

    private static final HttpClientService INSTANCE = new HttpClientService();

    private final HttpClient httpClient;

    private HttpClientService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(12))
                .build();
    }

    public static HttpClientService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<ApiResponseModel> executeAsync(ApiRequestModel requestModel) {
        return CompletableFuture.supplyAsync(() -> execute(requestModel));
    }

    public ApiResponseModel execute(ApiRequestModel requestModel) {
        if (requestModel == null) {
            return ApiResponseModel.networkError("Empty or null request", 0);
        }

        long startTime = System.currentTimeMillis();
        try {
            // 1. Resolve environment variables
            EnvironmentService envService = EnvironmentService.getInstance();
            String rawUrl = envService.resolveVariables(requestModel.getUrl());
            if (rawUrl == null || rawUrl.trim().isEmpty()) {
                return ApiResponseModel.networkError("URL is empty or undefined", 0);
            }

            // 2. Build complete URL with query parameters
            String finalUrl = buildUrlWithParams(rawUrl.trim(), requestModel.getParams(), requestModel.getParsedAuthType(),
                    requestModel.getAuthKey(), requestModel.getAuthToken());

            URI uri = URI.create(finalUrl);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return ApiResponseModel.networkError("Invalid URL protocol. Only HTTP and HTTPS are permitted: " + finalUrl, 0);
            }

            // 3. Build HttpRequest
            HttpRequest.Builder httpReqBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(15));

            // Set headers
            boolean hasContentType = false;
            for (Map.Entry<String, String> header : requestModel.getHeaders().entrySet()) {
                String key = envService.resolveVariables(header.getKey());
                String val = envService.resolveVariables(header.getValue());
                if (key != null && !key.trim().isEmpty() && val != null) {
                    if ("Content-Type".equalsIgnoreCase(key.trim())) {
                        hasContentType = true;
                    }
                    httpReqBuilder.header(key.trim(), val.trim());
                }
            }

            // Authentication headers
            applyAuthenticationHeaders(httpReqBuilder, requestModel);

            // Method and Body
            String method = requestModel.getMethod().toUpperCase();
            String resolvedBody = envService.resolveVariables(requestModel.getBody());

            HttpRequest.BodyPublisher bodyPublisher;
            if (resolvedBody != null && !resolvedBody.trim().isEmpty()) {
                bodyPublisher = HttpRequest.BodyPublishers.ofString(resolvedBody, StandardCharsets.UTF_8);
                if (!hasContentType && ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))) {
                    httpReqBuilder.header("Content-Type", "application/json; charset=utf-8");
                }
            } else {
                bodyPublisher = HttpRequest.BodyPublishers.noBody();
            }

            switch (method) {
                case "GET" -> httpReqBuilder.GET();
                case "POST" -> httpReqBuilder.POST(bodyPublisher);
                case "PUT" -> httpReqBuilder.PUT(bodyPublisher);
                case "DELETE" -> httpReqBuilder.DELETE();
                case "PATCH" -> httpReqBuilder.method("PATCH", bodyPublisher);
                case "HEAD" -> httpReqBuilder.method("HEAD", HttpRequest.BodyPublishers.noBody());
                case "OPTIONS" -> httpReqBuilder.method("OPTIONS", HttpRequest.BodyPublishers.noBody());
                default -> httpReqBuilder.method(method, bodyPublisher);
            }

            // 4. Send request
            HttpRequest httpRequest = httpReqBuilder.build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long durationMs = Math.max(1, System.currentTimeMillis() - startTime);

            // 5. Parse response headers
            Map<String, String> responseHeaders = new LinkedHashMap<>();
            HttpHeaders headers = response.headers();
            for (Map.Entry<String, List<String>> entry : headers.map().entrySet()) {
                responseHeaders.put(entry.getKey(), String.join(", ", entry.getValue()));
            }

            String responseBody = response.body() != null ? response.body() : "";
            long sizeBytes = responseBody.getBytes(StandardCharsets.UTF_8).length;

            // 6. Build response model and evaluate assertions
            ApiResponseModel apiResponse = new ApiResponseModel(
                    response.statusCode(),
                    lookupStatusText(response.statusCode()),
                    durationMs,
                    sizeBytes,
                    responseBody,
                    responseHeaders
            );

            List<ApiAssertion> evaluated = ApiAssertionService.getInstance()
                    .evaluateAssertions(requestModel.getAssertions(), apiResponse);

            return new ApiResponseModel(
                    response.statusCode(),
                    lookupStatusText(response.statusCode()),
                    durationMs,
                    sizeBytes,
                    responseBody,
                    responseHeaders,
                    evaluated,
                    false,
                    null
            );

        } catch (Exception e) {
            long durationMs = Math.max(1, System.currentTimeMillis() - startTime);
            return ApiResponseModel.networkError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(), durationMs);
        }
    }

    private String buildUrlWithParams(String baseUrl, Map<String, String> params, ApiAuthType authType,
                                      String authKey, String authToken) {
        StringBuilder sb = new StringBuilder(baseUrl);
        boolean hasQuery = baseUrl.contains("?");

        // Add regular query params
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> p : params.entrySet()) {
                if (p.getKey() != null && !p.getKey().trim().isEmpty()) {
                    sb.append(hasQuery ? "&" : "?");
                    sb.append(URLEncoder.encode(p.getKey().trim(), StandardCharsets.UTF_8));
                    sb.append("=");
                    sb.append(URLEncoder.encode(p.getValue() != null ? p.getValue() : "", StandardCharsets.UTF_8));
                    hasQuery = true;
                }
            }
        }

        // Add API Key query param if configured
        if (authType == ApiAuthType.API_KEY_QUERY && authToken != null && !authToken.trim().isEmpty()) {
            String key = (authKey != null && !authKey.trim().isEmpty()) ? authKey.trim() : "api_key";
            sb.append(hasQuery ? "&" : "?");
            sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
            sb.append("=");
            sb.append(URLEncoder.encode(authToken.trim(), StandardCharsets.UTF_8));
        }

        return sb.toString();
    }

    private void applyAuthenticationHeaders(HttpRequest.Builder builder, ApiRequestModel request) {
        ApiAuthType authType = request.getParsedAuthType();
        String token = EnvironmentService.getInstance().resolveVariables(request.getAuthToken());

        switch (authType) {
            case BEARER_TOKEN -> {
                if (token != null && !token.trim().isEmpty()) {
                    builder.header("Authorization", "Bearer " + token.trim());
                }
            }
            case BASIC_AUTH -> {
                String user = EnvironmentService.getInstance().resolveVariables(request.getAuthUsername());
                String pass = EnvironmentService.getInstance().resolveVariables(request.getAuthPassword());
                if (user != null || pass != null) {
                    String authStr = (user != null ? user : "") + ":" + (pass != null ? pass : "");
                    String base64 = Base64.getEncoder().encodeToString(authStr.getBytes(StandardCharsets.UTF_8));
                    builder.header("Authorization", "Basic " + base64);
                }
            }
            case API_KEY_HEADER -> {
                if (token != null && !token.trim().isEmpty()) {
                    String key = request.getAuthKey();
                    builder.header(key != null && !key.trim().isEmpty() ? key.trim() : "X-API-Key", token.trim());
                }
            }
            default -> {}
        }
    }

    private String lookupStatusText(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 202 -> "Accepted";
            case 204 -> "No Content";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 304 -> "Not Modified";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 408 -> "Request Timeout";
            case 409 -> "Conflict";
            case 422 -> "Unprocessable Entity";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> (statusCode >= 200 && statusCode < 300) ? "Success" : "Response";
        };
    }
}

