package com.nexus.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class EnvironmentModel {
    private String id;
    private String name;
    private Map<String, String> variables = new LinkedHashMap<>();

    public EnvironmentModel() {
        this.id = UUID.randomUUID().toString();
        this.name = "Local";
    }

    public EnvironmentModel(String name) {
        this();
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
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getVariables() {
        if (variables == null) variables = new LinkedHashMap<>();
        return variables;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables != null ? variables : new LinkedHashMap<>();
    }

    public void setVariable(String key, String value) {
        getVariables().put(key, value);
    }

    public String getVariable(String key) {
        return getVariables().get(key);
    }

    @Override
    public String toString() {
        return name;
    }
}

