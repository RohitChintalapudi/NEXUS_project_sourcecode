package com.nexus.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a Warp-style self-contained terminal command execution block.
 */
public class CommandBlock {
    private final String id;
    private final String workingDirectory;
    private final String gitBranch;
    private final String command;
    private final String output;
    private final int exitCode;
    private final long durationMs;
    private final LocalDateTime timestamp;

    public CommandBlock(String id, String workingDirectory, String gitBranch, String command, String output, int exitCode, long durationMs, LocalDateTime timestamp) {
        this.id = id;
        this.workingDirectory = workingDirectory;
        this.gitBranch = gitBranch;
        this.command = command;
        this.output = output;
        this.exitCode = exitCode;
        this.durationMs = durationMs;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public String getCommand() {
        return command;
    }

    public String getCommandLine() {
        return command;
    }

    public String getOutput() {
        return output;
    }

    public int getExitCode() {
        return exitCode;
    }

    public boolean isSuccess() {
        return exitCode == 0;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}

