package com.nexus.model;

public class ToolExecutionResult {
    private final boolean success;
    private final String output;
    private final String errorMessage;

    public ToolExecutionResult(boolean success, String output, String errorMessage) {
        this.success = success;
        this.output = output != null ? output : "";
        this.errorMessage = errorMessage != null ? errorMessage : "";
    }

    public static ToolExecutionResult success(String output) {
        return new ToolExecutionResult(true, output, null);
    }

    public static ToolExecutionResult failure(String errorMessage) {
        return new ToolExecutionResult(false, null, errorMessage);
    }

    public static ToolExecutionResult blocked(String reason) {
        return new ToolExecutionResult(false, null, "SECURITY RESTRICTION: " + reason);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOutput() {
        return output;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return success ? output : "Error: " + errorMessage;
    }
}

