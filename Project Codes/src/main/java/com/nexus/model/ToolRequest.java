package com.nexus.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ToolRequest {

    public enum Status {
        PENDING_APPROVAL,
        APPROVED,
        REJECTED,
        EXECUTING,
        COMPLETED,
        FAILED,
        BLOCKED
    }

    private final String id;
    private final String toolName;
    private final String description;
    private final PermissionLevel permissionLevel;
    private final Map<String, Object> arguments;
    private final String workingDirectory;
    private Status status;
    private final LocalDateTime createdAt;
    private final CompletableFuture<Boolean> approvalFuture;

    public ToolRequest(String id, String toolName, String description,
                       PermissionLevel permissionLevel, Map<String, Object> arguments,
                       String workingDirectory) {
        this.id = id;
        this.toolName = toolName;
        this.description = description;
        this.permissionLevel = permissionLevel;
        this.arguments = arguments != null ? arguments : Map.of();
        this.workingDirectory = workingDirectory;
        this.status = (permissionLevel == PermissionLevel.APPROVAL_REQUIRED)
                ? Status.PENDING_APPROVAL
                : (permissionLevel == PermissionLevel.RESTRICTED ? Status.BLOCKED : Status.APPROVED);
        this.createdAt = LocalDateTime.now();
        this.approvalFuture = new CompletableFuture<>();
    }

    public String getId() {
        return id;
    }

    public String getToolName() {
        return toolName;
    }

    public String getDescription() {
        return description;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public CompletableFuture<Boolean> getApprovalFuture() {
        return approvalFuture;
    }

    public void approve() {
        this.status = Status.APPROVED;
        approvalFuture.complete(true);
    }

    public void reject() {
        this.status = Status.REJECTED;
        approvalFuture.complete(false);
    }
}

