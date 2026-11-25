package com.nexus.service;

import com.nexus.model.PermissionLevel;
import com.nexus.model.ToolRequest;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class PermissionService {

    private static final PermissionService INSTANCE = new PermissionService();

    // Sensitive files and directories that must NEVER be accessed by AI
    private static final List<String> SENSITIVE_PATTERNS = List.of(
            ".ssh", "id_rsa", "id_ecdsa", "id_ed25519", "known_hosts",
            ".aws/credentials", ".azure", ".gcloud",
            ".env", ".env.local", ".env.production",
            "passwd", "shadow", "master.passwd",
            "system32\\config", "system32/config", "sam",
            "keychain", ".keystore", ".kdbx"
    );

    // Destructive / dangerous terminal commands that are strictly FORBIDDEN
    private static final List<Pattern> DANGEROUS_COMMAND_PATTERNS = List.of(
            Pattern.compile("(?i)\\bformat\\s+[a-z]:"),
            Pattern.compile("(?i)\\brmdir\\s+/[sS]"),
            Pattern.compile("(?i)\\bdel\\s+/[fF]\\s+/[sS]"),
            Pattern.compile("(?i)\\brm\\s+-rf\\s+[/~\\\\]"),
            Pattern.compile("(?i)\\bmkfs\\b"),
            Pattern.compile("(?i)\\bdiskpart\\b"),
            Pattern.compile("(?i)\\b(shutdown|reboot)\\b"),
            Pattern.compile("(?i):\\(\\)\\s*\\{\\s*:\\|:&\\s*\\};:"), // fork bomb
            Pattern.compile("(?i)\\b(wget|curl)\\b.*\\|\\s*(sh|bash|powershell)")
    );

    private final List<Consumer<ToolRequest>> approvalListeners = new CopyOnWriteArrayList<>();

    private PermissionService() {}

    public static PermissionService getInstance() {
        return INSTANCE;
    }

    public void addApprovalListener(Consumer<ToolRequest> listener) {
        approvalListeners.add(listener);
    }

    public void removeApprovalListener(Consumer<ToolRequest> listener) {
        approvalListeners.remove(listener);
    }

    public PermissionLevel evaluateToolRequest(String toolName, Map<String, Object> arguments) {
        // 1. Check for restricted paths or credentials
        for (Object val : arguments.values()) {
            if (val instanceof String str) {
                String normalized = str.toLowerCase().replace('/', '\\');
                for (String sensitive : SENSITIVE_PATTERNS) {
                    if (normalized.contains(sensitive.toLowerCase())) {
                        return PermissionLevel.RESTRICTED;
                    }
                }
            }
        }

        // 2. Check for dangerous terminal commands
        if ("execute_terminal_command".equalsIgnoreCase(toolName)) {
            Object cmdObj = arguments.get("command");
            if (cmdObj instanceof String cmd) {
                for (Pattern p : DANGEROUS_COMMAND_PATTERNS) {
                    if (p.matcher(cmd).find()) {
                        return PermissionLevel.RESTRICTED;
                    }
                }
            }
            return PermissionLevel.APPROVAL_REQUIRED;
        }

        // 3. Modifying operations require approval
        if (toolName.startsWith("create_") ||
                toolName.startsWith("modify_") ||
                toolName.startsWith("rename_") ||
                toolName.startsWith("move_") ||
                toolName.startsWith("delete_") ||
                toolName.startsWith("write_") ||
                toolName.startsWith("checkout_")) {
            return PermissionLevel.APPROVAL_REQUIRED;
        }

        // 4. State-modifying API Requests require approval (POST, PUT, PATCH, DELETE)
        if ("execute_api_request".equalsIgnoreCase(toolName)) {
            Object methodObj = arguments.get("method");
            if (methodObj instanceof String m) {
                String method = m.toUpperCase();
                if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method)) {
                    return PermissionLevel.APPROVAL_REQUIRED;
                }
            }
            return PermissionLevel.READ_ONLY;
        }

        // 4. Read-only tools
        return PermissionLevel.READ_ONLY;
    }

    public CompletableFuture<Boolean> requestPermission(ToolRequest request) {
        PermissionLevel level = request.getPermissionLevel();

        if (level == PermissionLevel.RESTRICTED) {
            AuditLogService.getInstance().record(
                    request.getToolName(),
                    level,
                    request.getArguments().toString(),
                    "BLOCKED",
                    false,
                    "Tool operation denied by security boundary"
            );
            return CompletableFuture.completedFuture(false);
        }

        if (level == PermissionLevel.READ_ONLY) {
            AuditLogService.getInstance().record(
                    request.getToolName(),
                    level,
                    request.getArguments().toString(),
                    "AUTO_PERMITTED",
                    true,
                    "Read-only tool operation auto-approved"
            );
            return CompletableFuture.completedFuture(true);
        }

        // APPROVAL_REQUIRED: Notify UI listeners to display the approval card
        for (Consumer<ToolRequest> listener : approvalListeners) {
            listener.accept(request);
        }

        return request.getApprovalFuture().thenApply(approved -> {
            AuditLogService.getInstance().record(
                    request.getToolName(),
                    level,
                    request.getArguments().toString(),
                    approved ? "USER_APPROVED" : "USER_REJECTED",
                    approved,
                    approved ? "User approved operation" : "User rejected operation"
            );
            return approved;
        });
    }
}

