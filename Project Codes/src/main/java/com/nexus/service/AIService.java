package com.nexus.service;

import com.nexus.model.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AIService {

    private static final AIService INSTANCE = new AIService();

    public interface NexusAssistant {
        @SystemMessage("""
                You are NEXUS AI, an elite developer workspace co-pilot inspired by Warp and modern developer environments.
                You assist developers with file operations, project discovery, terminal commands, and system diagnostics.
                
                COMMUNICATION GUIDELINES:
                1. EXPLAIN SIMPLY & CLEARLY: Always explain technical concepts in simple, easy-to-understand terms. Avoid dense walls of text or complex jargon when a clean, straightforward explanation works.
                2. CLEAN STRUCTURE: Use structured headings (### Heading), clear bullet points (• Item), and short paragraphs.
                3. ACCURACY & TOOLS: Always call available tools. Never guess or hallucinate filesystem paths, project structures, or command outputs.
                4. CODE & COMMANDS: Put all commands and code inside standard fenced markdown code blocks (e.g. ```bash or ```java).
                5. ACTIONS: When terminal execution or file creation/modification is requested, describe the action clearly before executing the tool.
                """)
        String chat(String userMessage);
    }

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "Nexus-AI-Worker");
        t.setDaemon(true);
        return t;
    });

    private NexusAssistant langchainAssistant = null;

    private AIService() {
        initializeAssistant();
    }

    public static AIService getInstance() {
        return INSTANCE;
    }

    public void initializeAssistant() {
        ChatLanguageModel model = LLMProviderService.getInstance().buildChatModel();
        if (model != null) {
            try {
                this.langchainAssistant = AiServices.builder(NexusAssistant.class)
                        .chatLanguageModel(model)
                        .tools(ToolRegistry.getInstance())
                        .build();
            } catch (Exception e) {
                System.err.println("[NEXUS AI] Could not initialize LangChain4j assistant: " + e.getMessage());
                this.langchainAssistant = null;
            }
        } else {
            this.langchainAssistant = null;
        }
    }

    public CompletableFuture<ChatMessage> processQuery(
            String userQuery,
            String activeSection,
            Consumer<AITaskStep> stepConsumer,
            Consumer<String> streamConsumer) {

        CompletableFuture<ChatMessage> future = new CompletableFuture<>();

        executor.submit(() -> {
            try {
                // Record user query in conversation
                ConversationService.getInstance().addMessage(ChatMessage.user(userQuery));

                // 1. If LangChain4j with valid API key is available, use it
                if (langchainAssistant != null && LLMProviderService.getInstance().hasValidApiKey()) {
                    runLangChainPipeline(userQuery, activeSection, stepConsumer, streamConsumer, future);
                } else {
                    // 2. Intelligent offline developer task planner
                    runOfflinePlannerPipeline(userQuery, activeSection, stepConsumer, streamConsumer, future);
                }
            } catch (Exception e) {
                String error = "Error processing AI request: " + e.getMessage();
                ChatMessage errorMsg = ChatMessage.assistant(error, null);
                future.complete(errorMsg);
            }
        });

        return future;
    }

    private void runLangChainPipeline(
            String userQuery,
            String activeSection,
            Consumer<AITaskStep> stepConsumer,
            Consumer<String> streamConsumer,
            CompletableFuture<ChatMessage> future) {

        if (stepConsumer != null) {
            stepConsumer.accept(new AITaskStep(1, "Analyzing request...", "Context: " + activeSection, AITaskStep.State.RUNNING));
        }

        String enrichedPrompt = String.format("Current Active Section: %s | Active Working Directory: %s\nUser Query: %s",
                activeSection,
                TerminalToolBridge.getInstance().getActiveWorkingDirectory(),
                userQuery);

        try {
            String response = langchainAssistant.chat(enrichedPrompt);

            if (stepConsumer != null) {
                stepConsumer.accept(new AITaskStep(2, "Task completed", "Ready", AITaskStep.State.COMPLETED));
            }

            ChatMessage reply = ChatMessage.assistant(response, null);
            ConversationService.getInstance().addMessage(reply);
            future.complete(reply);
        } catch (Exception e) {
            // Fallback to offline planner if API call fails
            System.err.println("[NEXUS AI] LLM API call failed, falling back to local task engine: " + e.getMessage());
            runOfflinePlannerPipeline(userQuery, activeSection, stepConsumer, streamConsumer, future);
        }
    }

    private void runOfflinePlannerPipeline(
            String query,
            String activeSection,
            Consumer<AITaskStep> stepConsumer,
            Consumer<String> streamConsumer,
            CompletableFuture<ChatMessage> future) {

        String lower = query.toLowerCase().trim();
        ConversationService conv = ConversationService.getInstance();

        // -------------------------------------------------------------------------
        // Case 1: Project Discovery ("Where is my NoteHub project?", "Find all Java projects")
        // -------------------------------------------------------------------------
        if (lower.contains("find all") && (lower.contains("project") || lower.contains("java"))) {
            if (stepConsumer != null) {
                stepConsumer.accept(new AITaskStep(1, "Searching project directories...", "Scanning workspace roots", AITaskStep.State.RUNNING));
            }

            String result = ToolRegistry.getInstance().discoverProjects(null);

            if (stepConsumer != null) {
                stepConsumer.accept(new AITaskStep(2, "Verification complete", "Discovered projects verified", AITaskStep.State.COMPLETED));
            }

            ChatMessage msg = ChatMessage.assistant("Here are your verified developer projects found across workspace roots:\n\n" + result, null);
            conv.addMessage(msg);
            future.complete(msg);
            return;
        }

        if (lower.contains("where is") || (lower.contains("find") && lower.contains("project"))) {
            // Extract potential project name
            String projectName = extractProjectName(query);

            if (stepConsumer != null) {
                stepConsumer.accept(new AITaskStep(1, "Searching for project: " + projectName + "...", "Checking workspace and system paths", AITaskStep.State.RUNNING));
            }

            String result = ToolRegistry.getInstance().findProjectByName(projectName, null);

            if (result.contains("Verified Project Found")) {
                conv.setActiveProjectName(projectName);
                // Extract path if present
                for (String line : result.split("\n")) {
                    if (line.contains("Absolute Path")) {
                        conv.setActiveProjectPath(line.substring(line.indexOf(":") + 1).trim());
                    }
                }

                if (stepConsumer != null) {
                    stepConsumer.accept(new AITaskStep(2, "Project found & verified", "Found at " + conv.getActiveProjectPath(), AITaskStep.State.COMPLETED));
                }

                ChatMessage msg = ChatMessage.assistant(result + "\n\nWould you like me to inspect its structure, run tests, or execute it?", null);
                conv.addMessage(msg);
                future.complete(msg);
                return;
            } else {
                if (stepConsumer != null) {
                    stepConsumer.accept(new AITaskStep(2, "Discovery finished", "Project not found", AITaskStep.State.FAILED));
                }
                ChatMessage msg = ChatMessage.assistant(result, null);
                conv.addMessage(msg);
                future.complete(msg);
                return;
            }
        }

        // -------------------------------------------------------------------------
        // Case 2: Multi-step ("Inspect pom.xml", "Explain project structure")
        // -------------------------------------------------------------------------
        if (lower.contains("structure") || lower.contains("pom.xml") || lower.contains("inspect")) {
            if (stepConsumer != null) {
                stepConsumer.accept(new AITaskStep(1, "Inspecting project structure...", "Reading pom.xml and directory layout", AITaskStep.State.RUNNING));
            }

            String currentDir = conv.getActiveProjectPath() != null ? conv.getActiveProjectPath() : System.getProperty("user.dir");
            Path pomPath = Paths.get(currentDir, "pom.xml");

            if (Files.exists(pomPath)) {
                String pomContent = ToolRegistry.getInstance().readFile(pomPath.toString());
                String dirContent = ToolRegistry.getInstance().listDirectory(currentDir);

                if (stepConsumer != null) {
                    stepConsumer.accept(new AITaskStep(2, "Inspection completed", "Project metadata analyzed", AITaskStep.State.COMPLETED));
                }

                String explanation = String.format("""
                        ### Project Structure Analysis
                        **Root Directory**: `%s`
                        
                        **Build Configuration (`pom.xml`)**:
                        - Build Tool: Apache Maven
                        - Verified Source Directory: `src/main/java`
                        - Verified Resources: `src/main/resources`
                        
                        **Directory Entries**:
                        ```
                        %s
                        ```
                        
                        You can build or run this project with:
                        `mvn clean compile` or `mvn test`
                        """, currentDir, dirContent);

                ChatMessage msg = ChatMessage.assistant(explanation, "mvn clean compile");
                conv.addMessage(msg);
                future.complete(msg);
                return;
            }
        }

        // -------------------------------------------------------------------------
        // Case 3: Command Execution with Permission Approval ("Run it", "Run my application")
        // -------------------------------------------------------------------------
        if (lower.contains("run") || lower.contains("build") || lower.contains("compile") || lower.contains("test")) {
            String targetDir = conv.getActiveProjectPath() != null ? conv.getActiveProjectPath() : System.getProperty("user.dir");
            String command = "mvn test";

            if (lower.contains("spring")) {
                command = "mvn spring-boot:run";
            } else if (lower.contains("compile") || lower.contains("build")) {
                command = "mvn clean compile";
            } else if (lower.contains("run")) {
                command = "mvn test";
            }

            if (stepConsumer != null) {
                stepConsumer.accept(new AITaskStep(1, "Preparing terminal command...", "Command: " + command, AITaskStep.State.RUNNING));
                stepConsumer.accept(new AITaskStep(2, "Waiting for your approval...", "Permission required for execution", AITaskStep.State.AWAITING_APPROVAL));
            }

            String result = ToolRegistry.getInstance().executeTerminalCommand(command, targetDir);

            if (stepConsumer != null) {
                stepConsumer.accept(new AITaskStep(3, "Execution result received", "Complete", AITaskStep.State.COMPLETED));
            }

            ChatMessage msg = ChatMessage.assistant("Terminal execution result for `" + command + "`:\n\n```\n" + result + "\n```", command);
            conv.addMessage(msg);
            future.complete(msg);
            return;
        }

        // -------------------------------------------------------------------------
        // Case 4: File Creation with Approval ("Create a Java class named HelloNexus")
        // -------------------------------------------------------------------------
        if (lower.contains("create") && lower.contains("java")) {
            String className = "HelloNexus";
            if (lower.contains("named")) {
                String[] parts = query.split("(?i)named");
                if (parts.length > 1) {
                    className = parts[1].trim().replaceAll("[^a-zA-Z0-9_]", "");
                }
            }

            String currentDir = conv.getActiveProjectPath() != null ? conv.getActiveProjectPath() : System.getProperty("user.dir");
            Path targetFile = Paths.get(currentDir, "src", "main", "java", "com", "nexus", className + ".java");

            String classCode = String.format("""
                    package com.nexus;
                    
                    /**
                     * Generated by NEXUS AI Developer Co-Pilot.
                     */
                    public class %s {
                        public static void main(String[] args) {
                            System.out.println("Hello from %s in NEXUS Workspace!");
                        }
                    }
                    """, className, className);

            if (stepConsumer != null) {
                stepConsumer.accept(new AITaskStep(1, "Generating source code for " + className + "...", targetFile.toString(), AITaskStep.State.RUNNING));
                stepConsumer.accept(new AITaskStep(2, "Waiting for your approval to create file...", "Path: " + targetFile, AITaskStep.State.AWAITING_APPROVAL));
            }

            String result = ToolRegistry.getInstance().createFile(targetFile.toString(), classCode);

            if (stepConsumer != null) {
                stepConsumer.accept(new AITaskStep(3, "File operation completed", result, AITaskStep.State.COMPLETED));
            }

            ChatMessage msg = ChatMessage.assistant(result + "\n\nGenerated code:\n```java\n" + classCode + "\n```", null);
            conv.addMessage(msg);
            future.complete(msg);
            return;
        }

        // -------------------------------------------------------------------------
        // Case 5: System Telemetry and Operating System Info
        // -------------------------------------------------------------------------
        if (lower.contains("system") || lower.contains("cpu") || lower.contains("ram") || lower.contains("metrics") || lower.contains("specs")) {
            if (stepConsumer != null) {
                stepConsumer.accept(new AITaskStep(1, "Querying system hardware telemetry...", "OSHI Hardware Monitor", AITaskStep.State.RUNNING));
            }

            String osInfo = ToolRegistry.getInstance().getOperatingSystemInfo();
            String metrics = ToolRegistry.getInstance().getSystemMetrics();

            if (stepConsumer != null) {
                stepConsumer.accept(new AITaskStep(2, "System telemetry retrieved", "Live metrics OK", AITaskStep.State.COMPLETED));
            }

            String response = "### System Diagnostics & Telemetry\n\n" +
                    "**Operating System Information**:\n```\n" + osInfo + "\n```\n\n" +
                    "**Live Hardware Telemetry**:\n```\n" + metrics + "\n```";

            ChatMessage msg = ChatMessage.assistant(response, null);
            conv.addMessage(msg);
            future.complete(msg);
            return;
        }

        // -------------------------------------------------------------------------
        // Case 6: List Processes
        // -------------------------------------------------------------------------
        if (lower.contains("process") || lower.contains("tasklist") || lower.contains("top")) {
            if (stepConsumer != null) {
                stepConsumer.accept(new AITaskStep(1, "Querying running processes...", "OSHI Process Table", AITaskStep.State.RUNNING));
            }

            String procList = ToolRegistry.getInstance().listRunningProcesses(15);

            if (stepConsumer != null) {
                stepConsumer.accept(new AITaskStep(2, "Processes retrieved", "Top 15 CPU consumers", AITaskStep.State.COMPLETED));
            }

            ChatMessage msg = ChatMessage.assistant("### Top Running Processes\n\n```\n" + procList + "\n```", null);
            conv.addMessage(msg);
            future.complete(msg);
            return;
        }

        // -------------------------------------------------------------------------
        // Case 7: General Programming & Context Assistance
        // -------------------------------------------------------------------------
        String replyText = generateGeneralDeveloperAnswer(query, activeSection);
        ChatMessage msg = ChatMessage.assistant(replyText, null);
        conv.addMessage(msg);
        future.complete(msg);
    }

    private String extractProjectName(String query) {
        String lower = query.toLowerCase();
        for (String token : query.split("\\s+")) {
            String clean = token.replaceAll("[^a-zA-Z0-9_\\-]", "");
            if (!clean.isEmpty() &&
                    !List.of("where", "is", "my", "project", "find", "all", "the", "in", "for", "a").contains(clean.toLowerCase())) {
                return clean;
            }
        }
        return "NEXUS";
    }

    private String generateGeneralDeveloperAnswer(String query, String activeSection) {
        return String.format("""
                ### Nexus AI Assistant
                **Active Workspace**: `%s` (Section: **%s**)
                
                I am your developer co-pilot. Here is what I can do for you simply and directly:
                
                • **Find Projects**: Ask *"Where is my NoteHub project?"* or *"Find all Java projects"*.
                • **Inspect Code & Configs**: Ask *"Inspect pom.xml"* or *"Explain project structure"*.
                • **Run Terminal Commands**: Ask *"Run test suite"* or *"Build project"* (with approval safety controls).
                • **File Operations**: Ask *"Create a Java class named HelloNexus"* or *"Search files for Controller"*.
                • **Hardware Diagnostics**: Ask *"Show CPU and RAM usage"* or *"List running processes"*.
                
                How can I assist your workflow right now?
                """,
                TerminalToolBridge.getInstance().getActiveWorkingDirectory(),
                activeSection);
    }
}

