package com.nexus.model;

import java.time.LocalDateTime;

public class AuditLogEntry {
    private final String id;
    private final LocalDateTime timestamp;
    private final String toolName;
    private final PermissionLevel permissionLevel;
    private final String argumentsSummary;
    private final String decision; // AUTO_PERMITTED, USER_APPROVED, USER_REJECTED, BLOCKED
    private final boolean success;
    private final String resultSummary;

    public AuditLogEntry(String id, LocalDateTime timestamp, String toolName,
                         PermissionLevel permissionLevel, String argumentsSummary,
                         String decision, boolean success, String resultSummary) {
        this.id = id;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.toolName = toolName;
        this.permissionLevel = permissionLevel;
        this.argumentsSummary = argumentsSummary;
        this.decision = decision;
        this.success = success;
        this.resultSummary = resultSummary;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getToolName() {
        return toolName;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public String getArgumentsSummary() {
        return argumentsSummary;
    }

    public String getDecision() {
        return decision;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getResultSummary() {
        return resultSummary;
    }
}

