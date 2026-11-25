package com.nexus.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CommandHistoryItem {
    private final String id;
    private final LocalDateTime timestamp;
    private final String command;
    private final String directory;
    private final String status; // SUCCESS, FAILED, RUNNING, CANCELLED
    private final int exitCode;
    private final long durationMs;
    private final String tool; // maven, git, docker, npm, system

    public CommandHistoryItem(String id, LocalDateTime timestamp, String command, String directory,
                              String status, int exitCode, long durationMs, String tool) {
        this.id = id;
        this.timestamp = timestamp;
        this.command = command;
        this.directory = directory;
        this.status = status;
        this.exitCode = exitCode;
        this.durationMs = durationMs;
        this.tool = tool;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedDate() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getCommand() {
        return command;
    }

    public String getDirectory() {
        return directory;
    }

    public String getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return exitCode == 0 || "SUCCESS".equalsIgnoreCase(status);
    }

    public int getExitCode() {
        return exitCode;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getFormattedDuration() {
        if (durationMs < 1000) {
            return durationMs + "ms";
        }
        return String.format("%.2fs", durationMs / 1000.0);
    }

    public String getTool() {
        return tool;
    }
}

