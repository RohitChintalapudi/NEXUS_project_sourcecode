package com.nexus.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ApiCollection {
    private String id;
    private String name;
    private String description;
    private List<ApiRequestModel> requests = new ArrayList<>();

    public ApiCollection() {
        this.id = UUID.randomUUID().toString();
        this.name = "New Collection";
    }

    public ApiCollection(String name) {
        this();
        this.name = name;
    }

    public ApiCollection(String name, String description) {
        this(name);
        this.description = description;
    }

    public String getId() {
        if (id == null) id = UUID.randomUUID().toString();
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ApiRequestModel> getRequests() {
        if (requests == null) requests = new ArrayList<>();
        return requests;
    }

    public void setRequests(List<ApiRequestModel> requests) {
        this.requests = requests != null ? requests : new ArrayList<>();
    }

    public void addRequest(ApiRequestModel request) {
        if (request != null) {
            request.setCollectionName(this.name);
            getRequests().add(request);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}

