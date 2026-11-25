package com.nexus.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nexus.model.ApiAssertion;
import com.nexus.model.ApiCollection;
import com.nexus.model.ApiRequestModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ApiCollectionService {

    private static final ApiCollectionService INSTANCE = new ApiCollectionService();

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Path configDir = Paths.get(System.getProperty("user.home"), ".nexus");
    private final File collectionsFile = configDir.resolve("api_collections.json").toFile();

    private final List<ApiCollection> collections = new ArrayList<>();

    private ApiCollectionService() {
        loadCollections();
    }

    public static ApiCollectionService getInstance() {
        return INSTANCE;
    }

    public synchronized void loadCollections() {
        collections.clear();
        if (collectionsFile.exists()) {
            try {
                List<ApiCollection> loaded = objectMapper.readValue(collectionsFile, new TypeReference<List<ApiCollection>>() {});
                if (loaded != null && !loaded.isEmpty()) {
                    collections.addAll(loaded);
                }
            } catch (Exception ignored) {}
        }

        // Ensure default demo fake API collections are present
        boolean hasDemo = collections.stream().anyMatch(c -> c.getName().contains("JSONPlaceholder"));
        if (collections.isEmpty() || !hasDemo) {
            initDefaultCollections();
            saveCollections();
        }
    }

    public synchronized void resetToDefaultDemoCollections() {
        collections.clear();
        initDefaultCollections();
        saveCollections();
    }

    private void initDefaultCollections() {
        // 1. JSONPlaceholder (The Gold Standard Mock REST API)
        ApiCollection jsonPlaceholder = new ApiCollection("JSONPlaceholder (Fake REST API)", "Universal developer fake REST API for prototyping");

        ApiRequestModel jpPost = new ApiRequestModel("1. Get Blog Post #1", "GET", "https://jsonplaceholder.typicode.com/posts/1");
        jpPost.getHeaders().put("Accept", "application/json");
        jpPost.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        jpPost.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "title", ""));
        jsonPlaceholder.addRequest(jpPost);

        ApiRequestModel jpUsers = new ApiRequestModel("2. Get All Users (10 Records)", "GET", "https://jsonplaceholder.typicode.com/users");
        jpUsers.getHeaders().put("Accept", "application/json");
        jpUsers.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        jpUsers.getAssertions().add(new ApiAssertion(ApiAssertion.Type.BODY_CONTAINS, "", "Leanne Graham"));
        jsonPlaceholder.addRequest(jpUsers);

        ApiRequestModel jpCreate = new ApiRequestModel("3. Create Post (POST JSON)", "POST", "https://jsonplaceholder.typicode.com/posts");
        jpCreate.setBody("{\n  \"title\": \"NEXUS AI Developer Workspace\",\n  \"body\": \"Demonstrating automated HTTP REST client testing with live assertions to Sir.\",\n  \"userId\": 1\n}");
        jpCreate.getHeaders().put("Content-Type", "application/json");
        jpCreate.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "201"));
        jpCreate.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "id", ""));
        jsonPlaceholder.addRequest(jpCreate);

        ApiRequestModel jpTodo = new ApiRequestModel("4. Get Todo Item #1", "GET", "https://jsonplaceholder.typicode.com/todos/1");
        jpTodo.getHeaders().put("Accept", "application/json");
        jpTodo.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        jpTodo.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "completed", ""));
        jsonPlaceholder.addRequest(jpTodo);

        ApiRequestModel jpPut = new ApiRequestModel("5. Update Post #1 (PUT)", "PUT", "https://jsonplaceholder.typicode.com/posts/1");
        jpPut.setBody("{\n  \"id\": 1,\n  \"title\": \"Updated Post Title via NEXUS\",\n  \"body\": \"Modified content verified by automated test assertions.\",\n  \"userId\": 1\n}");
        jpPut.getHeaders().put("Content-Type", "application/json");
        jpPut.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        jsonPlaceholder.addRequest(jpPut);

        collections.add(jsonPlaceholder);

        // 2. DummyJSON (Products & Quotes API)
        ApiCollection dummyJson = new ApiCollection("DummyJSON (Products & Quotes)", "Rich e-commerce, user profiles and random quotes");

        ApiRequestModel djProduct = new ApiRequestModel("1. Product Details (Mascara)", "GET", "https://dummyjson.com/products/1");
        djProduct.getHeaders().put("Accept", "application/json");
        djProduct.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        djProduct.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "price", ""));
        dummyJson.addRequest(djProduct);

        ApiRequestModel djQuote = new ApiRequestModel("2. Random Inspirational Quote", "GET", "https://dummyjson.com/quotes/random");
        djQuote.getHeaders().put("Accept", "application/json");
        djQuote.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        djQuote.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "quote", ""));
        dummyJson.addRequest(djQuote);

        ApiRequestModel djUser = new ApiRequestModel("3. User Profile with Details", "GET", "https://dummyjson.com/users/1");
        djUser.getHeaders().put("Accept", "application/json");
        djUser.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        djUser.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "firstName", ""));
        dummyJson.addRequest(djUser);

        ApiRequestModel djSearch = new ApiRequestModel("4. Search Products (Phone)", "GET", "https://dummyjson.com/products/search?q=phone");
        djSearch.getHeaders().put("Accept", "application/json");
        djSearch.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        djSearch.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "products", ""));
        dummyJson.addRequest(djSearch);

        collections.add(dummyJson);

        // 3. ReqRes (Mock User Engine)
        ApiCollection reqRes = new ApiCollection("ReqRes (Mock User Engine)", "Standardized mock REST responses for users and auth");

        ApiRequestModel rrUsers = new ApiRequestModel("1. List Users Page 1", "GET", "https://reqres.in/api/users?page=1");
        rrUsers.getHeaders().put("Accept", "application/json");
        rrUsers.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        rrUsers.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "data", ""));
        reqRes.addRequest(rrUsers);

        ApiRequestModel rrCreate = new ApiRequestModel("2. Create User Profile", "POST", "https://reqres.in/api/users");
        rrCreate.setBody("{\n  \"name\": \"Alex Vance\",\n  \"job\": \"Lead Architect\"\n}");
        rrCreate.getHeaders().put("Content-Type", "application/json");
        rrCreate.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "201"));
        rrCreate.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "id", ""));
        reqRes.addRequest(rrCreate);

        ApiRequestModel rrLogin = new ApiRequestModel("3. Mock User Login", "POST", "https://reqres.in/api/login");
        rrLogin.setBody("{\n  \"email\": \"eve.holt@reqres.in\",\n  \"password\": \"cityslicka\"\n}");
        rrLogin.getHeaders().put("Content-Type", "application/json");
        rrLogin.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        rrLogin.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "token", ""));
        reqRes.addRequest(rrLogin);

        collections.add(reqRes);

        // 4. Local Authentication & Bearer Token
        ApiCollection authCol = new ApiCollection("Auth & Bearer Token", "Local Authentication & Token Verification");

        ApiRequestModel signupReq = new ApiRequestModel("1. Sign Up User", "POST", "{{authUrl}}/api/auth/signup");
        signupReq.setBody("{\n  \"username\": \"nexus_dev\",\n  \"password\": \"Nexus@Secure2026\",\n  \"email\": \"dev@nexus.io\"\n}");
        signupReq.getHeaders().put("Content-Type", "application/json");
        signupReq.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "201"));
        signupReq.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "token", ""));
        authCol.addRequest(signupReq);

        ApiRequestModel loginReq = new ApiRequestModel("2. Login & Get Token", "POST", "{{authUrl}}/api/auth/login");
        loginReq.setBody("{\n  \"username\": \"admin\",\n  \"password\": \"admin123\"\n}");
        loginReq.getHeaders().put("Content-Type", "application/json");
        loginReq.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        loginReq.getAssertions().add(new ApiAssertion(ApiAssertion.Type.JSON_FIELD_EXISTS, "token", ""));
        authCol.addRequest(loginReq);

        ApiRequestModel profileReq = new ApiRequestModel("3. Protected Profile", "GET", "{{authUrl}}/api/auth/profile");
        profileReq.setAuthType("Bearer Token");
        profileReq.setAuthToken("{{token}}");
        profileReq.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        profileReq.getAssertions().add(new ApiAssertion(ApiAssertion.Type.BODY_CONTAINS, "", "authenticated"));
        authCol.addRequest(profileReq);

        collections.add(authCol);

        // 5. HttpBin Diagnostics
        ApiCollection httpBin = new ApiCollection("HTTP Diagnostics (HttpBin)", "Live request echoing and HTTP method testing");

        ApiRequestModel getEcho = new ApiRequestModel("1. HTTP GET Echo", "GET", "https://httpbin.org/get?env=staging&client=nexus");
        getEcho.getHeaders().put("Accept", "application/json");
        getEcho.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        httpBin.addRequest(getEcho);

        ApiRequestModel postEcho = new ApiRequestModel("2. HTTP POST JSON", "POST", "https://httpbin.org/post");
        postEcho.setBody("{\n  \"project\": \"NEXUS\",\n  \"stage\": \"Stage 4 API Studio\",\n  \"status\": \"ready_for_demo\"\n}");
        postEcho.getHeaders().put("Content-Type", "application/json");
        postEcho.getAssertions().add(new ApiAssertion(ApiAssertion.Type.STATUS_CODE_EQUALS, "", "200"));
        httpBin.addRequest(postEcho);

        collections.add(httpBin);
    }

    public synchronized void saveCollections() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            objectMapper.writeValue(collectionsFile, collections);
        } catch (IOException ignored) {}
    }

    public List<ApiCollection> getCollections() {
        return collections;
    }

    public void addCollection(ApiCollection collection) {
        if (collection != null) {
            collections.add(collection);
            saveCollections();
        }
    }

    public boolean removeCollection(String collectionId) {
        boolean removed = collections.removeIf(c -> c.getId().equals(collectionId));
        if (removed) {
            saveCollections();
        }
        return removed;
    }

    public void saveOrUpdateRequest(ApiRequestModel request, String targetCollectionName) {
        if (request == null) return;

        String colName = (targetCollectionName != null && !targetCollectionName.trim().isEmpty()) ?
                targetCollectionName.trim() : request.getCollectionName();

        ApiCollection targetCol = null;
        for (ApiCollection c : collections) {
            if (c.getName().equalsIgnoreCase(colName)) {
                targetCol = c;
                break;
            }
        }

        if (targetCol == null) {
            targetCol = new ApiCollection(colName);
            collections.add(targetCol);
        }

        request.setCollectionName(targetCol.getName());

        // Check if updating existing or adding new
        boolean updated = false;
        for (int i = 0; i < targetCol.getRequests().size(); i++) {
            if (targetCol.getRequests().get(i).getId().equals(request.getId())) {
                targetCol.getRequests().set(i, request.copy());
                updated = true;
                break;
            }
        }

        if (!updated) {
            targetCol.addRequest(request.copy());
        }

        saveCollections();
    }

    public boolean deleteRequest(String requestId) {
        boolean changed = false;
        for (ApiCollection c : collections) {
            if (c.getRequests().removeIf(r -> r.getId().equals(requestId))) {
                changed = true;
            }
        }
        if (changed) {
            saveCollections();
        }
        return changed;
    }
}

