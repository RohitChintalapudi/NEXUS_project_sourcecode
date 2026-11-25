package com.nexus.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nexus.model.AuditLogEntry;
import com.nexus.model.PermissionLevel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuditLogService {

    private static final AuditLogService INSTANCE = new AuditLogService();
    private static final int MAX_MEMORY_LOGS = 200;

    private final List<AuditLogEntry> inMemoryLogs = new CopyOnWriteArrayList<>();
    private final File auditLogFile;
    private final Gson gson;

    private AuditLogService() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                        new com.google.gson.JsonPrimitive(src.toString()))
                .setPrettyPrinting()
                .create();

        String userHome = System.getProperty("user.home", ".");
        File nexusDir = new File(userHome, ".nexus");
        if (!nexusDir.exists()) {
            nexusDir.mkdirs();
        }
        this.auditLogFile = new File(nexusDir, "ai_audit.log");
    }

    public static AuditLogService getInstance() {
        return INSTANCE;
    }

    public synchronized void record(String toolName, PermissionLevel level,
                                    String argumentsSummary, String decision,
                                    boolean success, String resultSummary) {
        AuditLogEntry entry = new AuditLogEntry(
                "audit-" + UUID.randomUUID().toString().substring(0, 8),
                LocalDateTime.now(),
                toolName,
                level,
                argumentsSummary,
                decision,
                success,
                resultSummary
        );

        if (inMemoryLogs.size() >= MAX_MEMORY_LOGS) {
            inMemoryLogs.remove(0);
        }
        inMemoryLogs.add(entry);

        // Append to file asynchronously or safely
        appendToFile(entry);
    }

    private void appendToFile(AuditLogEntry entry) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(auditLogFile, true))) {
            String line = String.format("[%s] [%s] Tool=%s, Decision=%s, Success=%s, Args=%s, Result=%s",
                    entry.getTimestamp(),
                    entry.getPermissionLevel(),
                    entry.getToolName(),
                    entry.getDecision(),
                    entry.isSuccess(),
                    entry.getArgumentsSummary(),
                    entry.getResultSummary());
            pw.println(line);
        } catch (IOException e) {
            System.err.println("[NEXUS] Failed to write to audit log: " + e.getMessage());
        }
    }

    public List<AuditLogEntry> getLogs() {
        return Collections.unmodifiableList(new ArrayList<>(inMemoryLogs));
    }
}

