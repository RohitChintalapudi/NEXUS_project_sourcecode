package com.nexus.service;

import com.nexus.model.GitStatusResult;
import com.nexus.model.PermissionLevel;
import com.nexus.model.ToolExecutionResult;
import com.nexus.model.ToolRequest;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ToolRegistry {

    private static final ToolRegistry INSTANCE = new ToolRegistry();

    private ToolRegistry() {}

    public static ToolRegistry getInstance() {
        return INSTANCE;
    }

    // ==========================================
    // A. FILESYSTEM TOOLS
    // ==========================================

    @Tool("List files and subdirectories inside a directory path")
    public String listDirectory(@P("Directory path to list") String path) {
        Map<String, Object> args = Map.of("path", path != null ? path : ".");
        return executeWithPermission("list_directory", "List directory: " + path, args, null, () ->
                FileSystemService.getInstance().listDirectory(path));
    }

    @Tool("Search for files matching a pattern within a directory")
    public String searchFiles(@P("Root path to start search") String rootPath,
                              @P("File name pattern or substring to search for") String pattern) {
        Map<String, Object> args = Map.of("rootPath", rootPath != null ? rootPath : ".", "pattern", pattern);
        return executeWithPermission("search_files", "Search files for pattern '" + pattern + "' in " + rootPath, args, null, () ->
                FileSystemService.getInstance().searchFiles(rootPath, pattern));
    }

    @Tool("Read the textual content of a file")
    public String readFile(@P("Path to the file to read") String filePath) {
        Map<String, Object> args = Map.of("filePath", filePath);
        return executeWithPermission("read_file", "Read file content: " + filePath, args, null, () ->
                FileSystemService.getInstance().readFile(filePath));
    }

    @Tool("Create a new file with specified content")
    public String createFile(@P("Path of the file to create") String filePath,
                             @P("Content to write into the file") String content) {
        Map<String, Object> args = new HashMap<>();
        args.put("filePath", filePath);
        args.put("contentSnippet", (content != null && content.length() > 80) ? content.substring(0, 80) + "..." : content);

        return executeWithPermission("create_file", "Create new file: " + filePath, args, null, () ->
                FileSystemService.getInstance().createFile(filePath, content));
    }

    @Tool("Modify an existing file with new content")
    public String modifyFile(@P("Path of the file to modify") String filePath,
                             @P("Content to write or append") String content,
                             @P("True to append to file, false to overwrite") boolean append) {
        Map<String, Object> args = new HashMap<>();
        args.put("filePath", filePath);
        args.put("append", append);
        args.put("contentSnippet", (content != null && content.length() > 80) ? content.substring(0, 80) + "..." : content);

        return executeWithPermission("modify_file", (append ? "Append to" : "Overwrite") + " file: " + filePath, args, null, () ->
                FileSystemService.getInstance().modifyFile(filePath, content, append));
    }

    @Tool("Create a new directory")
    public String createDirectory(@P("Path of directory to create") String dirPath) {
        Map<String, Object> args = Map.of("dirPath", dirPath);
        return executeWithPermission("create_directory", "Create directory: " + dirPath, args, null, () ->
                FileSystemService.getInstance().createDirectory(dirPath));
    }

    @Tool("Rename an existing file or directory")
    public String renameFile(@P("Current path of file") String oldPath,
                             @P("New file name") String newName) {
        Map<String, Object> args = Map.of("oldPath", oldPath, "newName", newName);
        return executeWithPermission("rename_file", "Rename file " + oldPath + " to " + newName, args, null, () ->
                FileSystemService.getInstance().renameFile(oldPath, newName));
    }

    @Tool("Move a file to a target directory")
    public String moveFile(@P("Source file path") String sourcePath,
                           @P("Target directory path") String targetDirectory) {
        Map<String, Object> args = Map.of("sourcePath", sourcePath, "targetDirectory", targetDirectory);
        return executeWithPermission("move_file", "Move file " + sourcePath + " to " + targetDirectory, args, null, () ->
                FileSystemService.getInstance().moveFile(sourcePath, targetDirectory));
    }

    @Tool("Delete a file with explicit user confirmation")
    public String deleteFile(@P("Path of file to delete") String filePath) {
        Map<String, Object> args = Map.of("filePath", filePath);
        return executeWithPermission("delete_file", "DELETE FILE: " + filePath, args, null, () ->
                FileSystemService.getInstance().deleteFile(filePath));
    }

    // ==========================================
    // B. OPERATING SYSTEM TOOLS
    // ==========================================

    @Tool("Get current Operating System, architecture, Java version and working directory info")
    public String getOperatingSystemInfo() {
        return executeWithPermission("get_os_info", "Retrieve operating system metadata", Map.of(), null, () ->
                OperatingSystemService.getInstance().getOsInfo());
    }

    @Tool("Retrieve live CPU, RAM, Disk, and System Uptime hardware telemetry")
    public String getSystemMetrics() {
        return executeWithPermission("get_system_metrics", "Retrieve live system telemetry", Map.of(), null, () ->
                OperatingSystemService.getInstance().getSystemMetrics());
    }

    @Tool("List running system processes sorted by CPU and memory load")
    public String listRunningProcesses(@P("Maximum number of processes to list (e.g. 15)") int limit) {
        Map<String, Object> args = Map.of("limit", limit);
        return executeWithPermission("list_processes", "List running processes (limit=" + limit + ")", args, null, () ->
                OperatingSystemService.getInstance().listRunningProcesses(limit));
    }

    @Tool("Retrieve environment variables with security filter applied")
    public String getEnvironmentVariables() {
        return executeWithPermission("get_environment_variables", "Inspect sanitized environment variables", Map.of(), null, () ->
                OperatingSystemService.getInstance().getSanitizedEnvironmentVariables());
    }

    // ==========================================
    // C. TERMINAL EXECUTION TOOLS
    // ==========================================

    @Tool("Execute a shell command in the NEXUS terminal workspace (Requires developer approval)")
    public String executeTerminalCommand(@P("The exact command line string to execute") String command,
                                         @P("Optional target working directory, or null for current directory") String workingDirectory) {
        Map<String, Object> args = new HashMap<>();
        args.put("command", command);
        if (workingDirectory != null) args.put("workingDirectory", workingDirectory);

        String pwd = workingDirectory != null ? workingDirectory : TerminalToolBridge.getInstance().getActiveWorkingDirectory();

        return executeWithPermission("execute_terminal_command",
                "Execute terminal command: `" + command + "` in " + pwd,
                args, pwd, () -> TerminalToolBridge.getInstance().executeCommand(command, workingDirectory));
    }

    @Tool("Retrieve the active terminal working directory path")
    public String getActiveWorkingDirectory() {
        return TerminalToolBridge.getInstance().getActiveWorkingDirectory();
    }

    // ==========================================
    // D. PROJECT DISCOVERY TOOLS
    // ==========================================

    @Tool("Discover Java, Maven, Gradle, or Git projects in developer directories")
    public String discoverProjects(@P("Optional search root directory, or null for default developer paths") String searchRoot) {
        Map<String, Object> args = searchRoot != null ? Map.of("searchRoot", searchRoot) : Map.of();
        return executeWithPermission("discover_projects", "Discover developer projects in " + (searchRoot != null ? searchRoot : "workspace roots"), args, null, () ->
                ProjectDiscoveryService.getInstance().discoverProjects(searchRoot));
    }

    @Tool("Find a specific project by name and verify its build system and run command")
    public String findProjectByName(@P("Name of the project to find (e.g. NoteHub, NEXUS)") String projectName,
                                    @P("Optional root path to search") String searchRoot) {
        Map<String, Object> args = new HashMap<>();
        args.put("projectName", projectName);
        if (searchRoot != null) args.put("searchRoot", searchRoot);

        return executeWithPermission("find_project_by_name", "Find project named '" + projectName + "'", args, null, () ->
                ProjectDiscoveryService.getInstance().findProjectByName(projectName, searchRoot));
    }

    // ==========================================
    // E. REPOGRAPH GIT & ARCHITECTURE TOOLS (STAGE 4)
    // ==========================================

    @Tool("Explain the structure, architecture, build files and directory layout of a repository")
    public String explainRepositoryStructure(@P("Optional path to repository, or null for current workspace") String repoPath) {
        String path = (repoPath != null && !repoPath.trim().isEmpty()) ? repoPath.trim() : System.getProperty("user.dir");
        Map<String, Object> args = Map.of("repoPath", path);

        return executeWithPermission("explain_repo_structure", "Inspect repository structure: " + path, args, path, () -> {
            java.io.File dir = new java.io.File(path);
            if (!dir.exists() || !dir.isDirectory()) {
                return ToolExecutionResult.failure("Repository directory not found: " + path);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Repository: ").append(dir.getName()).append("\n");
            sb.append("Path: ").append(dir.getAbsolutePath()).append("\n");

            if (JGitService.getInstance().isGitRepository(dir)) {
                sb.append("Git Branch: ").append(JGitService.getInstance().getCurrentBranch(dir)).append("\n");
                GitStatusResult status = JGitService.getInstance().getStatus(dir);
                sb.append("Git Status: ").append(status.isClean() ? "Clean" : (status.getTotalChanges() + " changed file(s)")).append("\n");
            }

            // Check build files
            if (new java.io.File(dir, "pom.xml").exists()) sb.append("Build System: Maven (pom.xml detected)\n");
            if (new java.io.File(dir, "build.gradle").exists()) sb.append("Build System: Gradle (build.gradle detected)\n");

            sb.append("\nTop-level layout:\n");
            java.io.File[] files = dir.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    if (f.getName().startsWith(".")) continue;
                    sb.append(f.isDirectory() ? "📁 " : "📄 ").append(f.getName()).append("\n");
                }
            }
            return ToolExecutionResult.success(sb.toString().trim());
        });
    }

    @Tool("Get real-time Git status showing modified, staged, and untracked files in the repository")
    public String getGitStatus(@P("Optional path to repository, or null for current workspace") String repoPath) {
        String path = (repoPath != null && !repoPath.trim().isEmpty()) ? repoPath.trim() : System.getProperty("user.dir");
        Map<String, Object> args = Map.of("repoPath", path);

        return executeWithPermission("get_git_status", "Retrieve Git status for " + path, args, path, () -> {
            java.io.File dir = new java.io.File(path);
            if (!JGitService.getInstance().isGitRepository(dir)) {
                return ToolExecutionResult.failure("Directory is not a Git repository: " + path);
            }

            GitStatusResult status = JGitService.getInstance().getStatus(dir);
            StringBuilder sb = new StringBuilder();
            sb.append("Branch: ").append(status.getBranch()).append("\n");
            sb.append(status.isClean() ? "Working tree clean.\n" : "Changes detected:\n");

            if (!status.getStaged().isEmpty()) {
                sb.append("\nStaged changes (").append(status.getStaged().size()).append("):\n");
                status.getStaged().forEach(f -> sb.append("  🟢 ").append(f).append("\n"));
            }
            if (!status.getModified().isEmpty()) {
                sb.append("\nModified changes (").append(status.getModified().size()).append("):\n");
                status.getModified().forEach(f -> sb.append("  🟡 ").append(f).append("\n"));
            }
            if (!status.getUntracked().isEmpty()) {
                sb.append("\nUntracked files (").append(status.getUntracked().size()).append("):\n");
                status.getUntracked().forEach(f -> sb.append("  ⚪ ").append(f).append("\n"));
            }

            return ToolExecutionResult.success(sb.toString().trim());
        });
    }

    @Tool("Get recent Git commit history with hashes, authors, dates and messages")
    public String getGitCommitHistory(@P("Optional repository path, or null for current") String repoPath,
                                      @P("Maximum number of commits to retrieve (e.g. 5)") int limit) {
        String path = (repoPath != null && !repoPath.trim().isEmpty()) ? repoPath.trim() : System.getProperty("user.dir");
        Map<String, Object> args = Map.of("repoPath", path, "limit", limit);

        return executeWithPermission("get_git_commits", "Retrieve Git commit history (limit=" + limit + ")", args, path, () -> {
            java.io.File dir = new java.io.File(path);
            if (!JGitService.getInstance().isGitRepository(dir)) {
                return ToolExecutionResult.failure("Directory is not a Git repository: " + path);
            }

            var commits = JGitService.getInstance().getCommitHistory(dir, null, Math.max(1, limit));
            if (commits.isEmpty()) {
                return ToolExecutionResult.success("No commits found in repository.");
            }

            StringBuilder sb = new StringBuilder("Recent Git Commits:\n\n");
            for (var c : commits) {
                sb.append(String.format("• [%s] %s\n  Author: %s | Date: %s\n  Files: %s\n\n",
                        c.getShortHash(), c.getMessage(), c.getAuthor(), c.getDate(), String.join(", ", c.getFilesChanged())));
            }
            return ToolExecutionResult.success(sb.toString().trim());
        });
    }

    @Tool("Get the Git diff patch for a specific commit hash or unstaged changes in working tree")
    public String getGitDiff(@P("Repository path") String repoPath,
                             @P("Commit hash or relative file path to diff") String target) {
        String path = (repoPath != null && !repoPath.trim().isEmpty()) ? repoPath.trim() : System.getProperty("user.dir");
        Map<String, Object> args = Map.of("repoPath", path, "target", target != null ? target : "HEAD");

        return executeWithPermission("get_git_diff", "Inspect Git diff for " + target, args, path, () -> {
            java.io.File dir = new java.io.File(path);
            if (!JGitService.getInstance().isGitRepository(dir)) {
                return ToolExecutionResult.failure("Directory is not a Git repository: " + path);
            }

            String diff;
            if (target != null && target.length() >= 7 && target.matches("[a-fA-F0-9]+")) {
                diff = JGitService.getInstance().getDiffForCommit(dir, target);
            } else {
                diff = JGitService.getInstance().getWorkingTreeDiff(dir, target);
            }

            if (diff == null || diff.isEmpty()) {
                return ToolExecutionResult.success("No diff found for: " + target);
            }
            if (diff.length() > 3000) {
                diff = diff.substring(0, 3000) + "\n... [diff truncated for brevity]";
            }
            return ToolExecutionResult.success(diff);
        });
    }

    // ==========================================
    // F. API STUDIO EXECUTION TOOLS (STAGE 4)
    // ==========================================

    @Tool("Execute an HTTP API request (GET, POST, PUT, DELETE, PATCH). State-modifying requests require approval.")
    public String executeApiRequest(@P("HTTP method (GET, POST, PUT, DELETE, PATCH)") String method,
                                    @P("Target URL (e.g. {{baseUrl}}/health or https://httpbin.org/get)") String url,
                                    @P("Optional JSON request body for POST/PUT/PATCH") String body,
                                    @P("Optional headers separated by semicolon, e.g. Content-Type=application/json;Accept=application/json") String headers) {
        String cleanMethod = method != null ? method.toUpperCase() : "GET";
        Map<String, Object> args = new HashMap<>();
        args.put("method", cleanMethod);
        args.put("url", url);
        if (body != null) args.put("body", body);

        return executeWithPermission("execute_api_request",
                "Execute HTTP " + cleanMethod + " " + url, args, null, () -> {

            com.nexus.model.ApiRequestModel req = new com.nexus.model.ApiRequestModel("AI Request", cleanMethod, url);
            if (body != null && !body.trim().isEmpty()) {
                req.setBody(body);
            }

            if (headers != null && !headers.trim().isEmpty()) {
                for (String part : headers.split(";")) {
                    String[] kv = part.split("=", 2);
                    if (kv.length == 2) {
                        req.getHeaders().put(kv[0].trim(), kv[1].trim());
                    }
                }
            }

            com.nexus.model.ApiResponseModel response = HttpClientService.getInstance().execute(req);
            StringBuilder sb = new StringBuilder();
            sb.append("HTTP ").append(response.getStatusCode()).append(" ").append(response.getStatusText()).append("\n");
            sb.append("Time: ").append(response.getResponseTimeMs()).append(" ms | Size: ").append(response.getFormattedSize()).append("\n\n");
            sb.append("Response Body:\n");
            String responseBody = response.getBody();
            if (responseBody.length() > 2000) {
                sb.append(responseBody.substring(0, 2000)).append("\n... [body truncated for brevity]");
            } else {
                sb.append(responseBody);
            }

            return ToolExecutionResult.success(sb.toString());
        });
    }

    @Tool("List all saved API collections and requests in API Studio")
    public String listApiCollections() {
        return executeWithPermission("list_api_collections", "List API collections", Map.of(), null, () -> {
            var collections = ApiCollectionService.getInstance().getCollections();
            if (collections.isEmpty()) {
                return ToolExecutionResult.success("No API collections saved yet.");
            }

            StringBuilder sb = new StringBuilder("API Studio Collections:\n\n");
            for (var col : collections) {
                sb.append("📁 Collection: ").append(col.getName()).append(" (").append(col.getRequests().size()).append(" requests)\n");
                for (var req : col.getRequests()) {
                    sb.append("   • [").append(req.getMethod()).append("] ").append(req.getName()).append(" -> ").append(req.getUrl()).append("\n");
                }
                sb.append("\n");
            }
            return ToolExecutionResult.success(sb.toString().trim());
        });
    }

    // ==========================================
    // PERMISSION CHECK WRAPPER
    // ==========================================

    private String executeWithPermission(String toolName, String description,
                                         Map<String, Object> args, String workingDir,
                                         java.util.function.Supplier<ToolExecutionResult> action) {
        PermissionLevel level = PermissionService.getInstance().evaluateToolRequest(toolName, args);

        ToolRequest request = new ToolRequest(
                "req-" + UUID.randomUUID().toString().substring(0, 8),
                toolName,
                description,
                level,
                args,
                workingDir
        );

        try {
            boolean approved = PermissionService.getInstance().requestPermission(request).join();
            if (!approved) {
                if (level == PermissionLevel.RESTRICTED) {
                    return "[RESTRICTED BY NEXUS SECURITY POLICY] Access denied for " + toolName + ". Sensitive system files and dangerous commands are prohibited.";
                }
                return "[OPERATION REJECTED] The user denied permission to execute this operation.";
            }

            ToolExecutionResult result = action.get();
            AuditLogService.getInstance().record(toolName, level, args.toString(), "COMPLETED", result.isSuccess(),
                    result.isSuccess() ? "OK" : result.getErrorMessage());

            return result.toString();
        } catch (Exception e) {
            AuditLogService.getInstance().record(toolName, level, args.toString(), "ERROR", false, e.getMessage());
            return "Error executing tool " + toolName + ": " + e.getMessage();
        }
    }
}

