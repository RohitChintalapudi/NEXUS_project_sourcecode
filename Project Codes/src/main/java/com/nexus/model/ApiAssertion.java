package com.nexus.model;

public class ApiAssertion {

    public enum Type {
        STATUS_CODE_EQUALS("Status Code Equals"),
        BODY_CONTAINS("Body Contains Text"),
        JSON_FIELD_EQUALS("JSON Field Equals"),
        JSON_FIELD_EXISTS("JSON Field Exists"),
        RESPONSE_TIME_LESS_THAN("Response Time < (ms)");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private Type type;
    private String property;
    private String expectedValue;
    private String actualValue;
    private Boolean passed;
    private String message;

    public ApiAssertion() {
        this.type = Type.STATUS_CODE_EQUALS;
        this.expectedValue = "200";
    }

    public ApiAssertion(Type type, String property, String expectedValue) {
        this.type = type;
        this.property = property;
        this.expectedValue = expectedValue;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public String getActualValue() {
        return actualValue;
    }

    public void setActualValue(String actualValue) {
        this.actualValue = actualValue;
    }

    public Boolean isPassed() {
        return passed != null && passed;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

