package com.nexus.service;

import com.nexus.model.*;

import java.time.LocalDateTime;
import java.util.*;

public class MockDataService {
    private static final MockDataService INSTANCE = new MockDataService();

    private MockDataService() {}

    public static MockDataService getInstance() {
        return INSTANCE;
    }

    public List<CommandBlock> getInitialTerminalBlocks() {
        List<CommandBlock> blocks = new ArrayList<>();
        blocks.add(new CommandBlock(
                "cmd-1",
                "~/project/nexus-core",
                "main",
                "git status",
                "On branch main\nYour branch is up to date with 'origin/main'.\n\nChanges not staged for commit:\n  (use \"git add <file>...\" to update what will be committed)\n\n\tmodified:   src/main/resources/styles/nexus-theme.css\n\tmodified:   src/main/java/com/nexus/app/NexusApplication.java\n\nno changes added to commit (use \"git add\")",
                0,
                38,
                LocalDateTime.now().minusMinutes(14)
        ));

        blocks.add(new CommandBlock(
                "cmd-2",
                "~/project/nexus-core",
                "main*",
                "mvn clean compile -DskipTests",
                "[INFO] Scanning for projects...\n[INFO] Building NEXUS - AI Developer Workspace 1.0.0-SNAPSHOT\n[INFO] --- maven-clean-plugin:3.2.0:clean (default-clean) @ nexus-workspace ---\n[INFO] --- maven-resources-plugin:3.3.1:resources (default-resources) @ nexus-workspace ---\n[INFO] Copying 14 resources from src/main/resources to target/classes\n[INFO] --- maven-compiler-plugin:3.13.0:compile (default-compile) @ nexus-workspace ---\n[INFO] Compiling 22 source files with javac [debug target 21] to target/classes\n[INFO] ------------------------------------------------------------------------\n[INFO] BUILD SUCCESS\n[INFO] Total time: 1.842 s\n[INFO] Finished at: " + LocalDateTime.now().minusMinutes(8).toString(),
                0,
                1842,
                LocalDateTime.now().minusMinutes(8)
        ));

        blocks.add(new CommandBlock(
                "cmd-3",
                "~/project/nexus-core",
                "main*",
                "docker compose ps",
                "NAME                IMAGE                COMMAND                  SERVICE             CREATED             STATUS              PORTS\nnexus-cache         redis:7.2-alpine     \"docker-entrypoint.s…\"   redis               3 hours ago         Up 3 hours          0.0.0.0:6379->6379/tcp\nnexus-telemetry     prom/prometheus      \"/bin/prometheus --c…\"   prometheus          3 hours ago         Up 3 hours          0.0.0.0:9090->9090/tcp",
                0,
                124,
                LocalDateTime.now().minusMinutes(2)
        ));

        return blocks;
    }

    public List<GitCommitNode> getGitCommitHistory() {
        List<GitCommitNode> commits = new ArrayList<>();

        commits.add(new GitCommitNode(
                "f8a4e12d9b231456a7bc",
                "main",
                "Alex Vance",
                "10 mins ago",
                "feat(ui): complete Stage 1 dark-mode architecture and workspace navigation",
                false,
                List.of("b7c3901a"),
                List.of("v0.1.0-alpha", "HEAD"),
                List.of(
                        "src/main/resources/styles/nexus-theme.css",
                        "src/main/java/com/nexus/controller/MainController.java",
                        "src/main/java/com/nexus/controller/TerminalController.java",
                        "src/main/java/com/nexus/controller/RepoGraphController.java",
                        "src/main/java/com/nexus/controller/ApiStudioController.java"
                ),
                480, 24
        ));

        commits.add(new GitCommitNode(
                "b7c3901a8ef14152cd3e",
                "main",
                "Elena Rostova",
                "2 hours ago",
                "merge: sync feature/api-studio into main with updated contract specs",
                true,
                List.of("9d4e21a7", "4c8f0012"),
                Collections.emptyList(),
                List.of(
                        "src/main/java/com/nexus/model/ApiRequestModel.java",
                        "src/main/java/com/nexus/model/ApiResponseModel.java",
                        "src/main/resources/views/ApiStudioView.fxml"
                ),
                312, 45
        ));

        commits.add(new GitCommitNode(
                "4c8f00127a908234bb41",
                "feature/api-studio",
                "Marcus Brody",
                "4 hours ago",
                "feat(api-studio): add HTTP method selector, tabs and JSON response viewer",
                false,
                List.of("1a2b3c4d"),
                Collections.emptyList(),
                List.of(
                        "src/main/java/com/nexus/controller/ApiStudioController.java",
                        "src/main/resources/styles/api-studio.css"
                ),
                256, 12
        ));

        commits.add(new GitCommitNode(
                "9d4e21a71ef08412ac98",
                "main",
                "Alex Vance",
                "6 hours ago",
                "refactor(terminal): implement Warp-inspired command block container model",
                false,
                List.of("1a2b3c4d"),
                Collections.emptyList(),
                List.of(
                        "src/main/java/com/nexus/component/CommandBlockView.java",
                        "src/main/java/com/nexus/model/CommandBlock.java"
                ),
                189, 58
        ));

        commits.add(new GitCommitNode(
                "1a2b3c4de0914835fc55",
                "main",
                "Elena Rostova",
                "Yesterday",
                "chore(build): setup Java 21+ and JavaFX Maven plugins with shader bundle",
                false,
                List.of("00af4592"),
                List.of("v0.0.9"),
                List.of("pom.xml", "README.md"),
                110, 8
        ));

        commits.add(new GitCommitNode(
                "00af45928d148726bc11",
                "main",
                "Alex Vance",
                "2 days ago",
                "initial commit: project scaffold, modular packages, and base stylesheet",
                false,
                Collections.emptyList(),
                List.of("v0.0.1"),
                List.of(".gitignore", "pom.xml", "src/main/java/com/nexus/app/NexusApplication.java"),
                95, 0
        ));

        return commits;
    }

    public List<CommandHistoryItem> getCommandHistory() {
        List<CommandHistoryItem> history = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        history.add(new CommandHistoryItem("hist-1", now.minusMinutes(2), "docker compose ps", "~/project/nexus-core", "SUCCESS", 0, 124, "docker"));
        history.add(new CommandHistoryItem("hist-2", now.minusMinutes(8), "mvn clean compile -DskipTests", "~/project/nexus-core", "SUCCESS", 0, 1842, "maven"));
        history.add(new CommandHistoryItem("hist-3", now.minusMinutes(14), "git status", "~/project/nexus-core", "SUCCESS", 0, 38, "git"));
        history.add(new CommandHistoryItem("hist-4", now.minusMinutes(25), "git diff --stat HEAD~1", "~/project/nexus-core", "SUCCESS", 0, 82, "git"));
        history.add(new CommandHistoryItem("hist-5", now.minusMinutes(42), "curl -i http://localhost:8080/api/v1/health", "~/project/nexus-core", "SUCCESS", 0, 46, "system"));
        history.add(new CommandHistoryItem("hist-6", now.minusHours(1).minusMinutes(15), "mvn test", "~/project/nexus-core", "FAILED", 1, 4120, "maven"));
        history.add(new CommandHistoryItem("hist-7", now.minusHours(2), "git checkout -b feature/ui-foundation", "~/project/nexus-core", "SUCCESS", 0, 29, "git"));
        history.add(new CommandHistoryItem("hist-8", now.minusHours(3), "npm run lint", "~/project/nexus-core/web", "SUCCESS", 0, 2410, "npm"));
        history.add(new CommandHistoryItem("hist-9", now.minusHours(4), "docker compose up -d redis prometheus", "~/project/nexus-core", "SUCCESS", 0, 3510, "docker"));
        history.add(new CommandHistoryItem("hist-10", now.minusHours(5), "git fetch origin --prune", "~/project/nexus-core", "SUCCESS", 0, 540, "git"));
        history.add(new CommandHistoryItem("hist-11", now.minusHours(6), "cargo check --workspace", "~/project/nexus-core/native", "SUCCESS", 0, 890, "system"));
        history.add(new CommandHistoryItem("hist-12", now.minusHours(8), "mvn clean", "~/project/nexus-core", "SUCCESS", 0, 620, "maven"));

        return history;
    }

    public List<ChatMessage> getInitialChatMessages() {
        List<ChatMessage> messages = new ArrayList<>();

        messages.add(new ChatMessage(
                "msg-init-1",
                ChatMessage.Role.SYSTEM,
                "Nexus AI initialized. Stage 1 Desktop UI Prototype is active. LLM inference engine will be connected in Stage 2.",
                null,
                LocalDateTime.now().minusMinutes(20)
        ));

        messages.add(new ChatMessage(
                "msg-init-2",
                ChatMessage.Role.ASSISTANT,
                "Welcome to Nexus AI, developer. I am your persistent workspace co-pilot.\n\nI can assist you with terminal command diagnostics, repository topological analysis, API schema validations, and automated refactoring.\n\nWhat would you like to explore in the workspace today?",
                null,
                LocalDateTime.now().minusMinutes(19)
        ));

        messages.add(new ChatMessage(
                "msg-init-3",
                ChatMessage.Role.USER,
                "What is the current status of the workspace and build?",
                null,
                LocalDateTime.now().minusMinutes(10)
        ));

        messages.add(new ChatMessage(
                "msg-init-4",
                ChatMessage.Role.ASSISTANT,
                "Your workspace 'nexus-core' is synchronized on branch 'main'. The last Maven compilation succeeded in 1.84s with 0 errors. You have 2 modified files ready for inspection.",
                "git status\n# modified: styles/nexus-theme.css\n# modified: app/NexusApplication.java",
                LocalDateTime.now().minusMinutes(9)
        ));

        return messages;
    }

    public ApiResponseModel executeMockApiRequest(String method, String url) {
        long latency = 32 + (long) (Math.random() * 45);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("Server", "nexus-edge-gateway/1.4.2");
        headers.put("X-Request-Id", UUID.randomUUID().toString().substring(0, 8));
        headers.put("Date", LocalDateTime.now().toString());
        headers.put("Cache-Control", "no-cache, no-store, must-revalidate");

        String responseJson;
        int status;
        String statusText;

        if (url.contains("health")) {
            status = 200;
            statusText = "OK";
            responseJson = "{\n  \"status\": \"HEALTHY\",\n  \"uptime\": \"14d 06h 22m\",\n  \"services\": {\n    \"database\": \"POSTGRES_DEFERRED_STAGE_3\",\n    \"redis_cache\": \"CONNECTED\",\n    \"telemetry\": \"ACTIVE\"\n  },\n  \"version\": \"1.0.0-alpha\",\n  \"timestamp\": \"" + LocalDateTime.now() + "\"\n}";
        } else if (url.contains("query") || url.contains("completions")) {
            status = 200;
            statusText = "OK";
            responseJson = "{\n  \"model\": \"nexus-developer-v1-preview\",\n  \"execution_stage\": \"STAGE_1_SIMULATION\",\n  \"result\": {\n    \"diagnostic\": \"Clean compilation target Java 21\",\n    \"suggestions\": [\n      \"Ready for Stage 2 LLM Integration\",\n      \"Use RepoGraph inspector for commit diff visualization\"\n    ]\n  },\n  \"tokens_used\": {\n    \"prompt\": 42,\n    \"completion\": 88,\n    \"total\": 130\n  }\n}";
        } else if (url.contains("models") || url.contains("workspaces")) {
            status = 200;
            statusText = "OK";
            responseJson = "{\n  \"workspaces\": [\n    {\n      \"id\": \"ws-nexus-01\",\n      \"name\": \"NEXUS Core Workspace\",\n      \"path\": \"~/project/nexus-core\",\n      \"branch\": \"main\",\n      \"is_active\": true\n    }\n  ],\n  \"total\": 1\n}";
        } else {
            status = 200;
            statusText = "OK";
            responseJson = "{\n  \"status\": \"SUCCESS\",\n  \"method\": \"" + method + "\",\n  \"endpoint\": \"" + url + "\",\n  \"received_at\": \"" + LocalDateTime.now() + "\",\n  \"note\": \"Simulated API Studio Response (Stage 1 Desktop UI Prototype)\"\n}";
        }

        long sizeBytes = responseJson.getBytes().length;
        return new ApiResponseModel(status, statusText, latency, sizeBytes, responseJson, headers);
    }
}

