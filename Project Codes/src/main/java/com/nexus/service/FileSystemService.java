package com.nexus.service;

import com.nexus.model.ToolExecutionResult;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileSystemService {

    private static final FileSystemService INSTANCE = new FileSystemService();
    private static final int MAX_READ_BYTES = 512 * 1024; // 512 KB safe read limit
    private static final int MAX_LIST_ENTRIES = 200;

    private FileSystemService() {}

    public static FileSystemService getInstance() {
        return INSTANCE;
    }

    public ToolExecutionResult listDirectory(String dirPath) {
        try {
            Path path = resolvePath(dirPath);
            if (!Files.exists(path)) {
                return ToolExecutionResult.failure("Directory does not exist: " + path);
            }
            if (!Files.isDirectory(path)) {
                return ToolExecutionResult.failure("Path is not a directory: " + path);
            }

            List<String> entries = new ArrayList<>();
            try (Stream<Path> stream = Files.list(path)) {
                List<Path> list = stream.limit(MAX_LIST_ENTRIES).sorted().toList();
                for (Path p : list) {
                    boolean isDir = Files.isDirectory(p);
                    long size = isDir ? 0 : Files.size(p);
                    entries.add(String.format("%s %-30s %s",
                            isDir ? "[DIR] " : "[FILE]",
                            p.getFileName().toString(),
                            isDir ? "" : "(" + size + " bytes)"));
                }
            }

            return ToolExecutionResult.success(String.join("\n", entries));
        } catch (Exception e) {
            return ToolExecutionResult.failure("Failed to list directory: " + e.getMessage());
        }
    }

    public ToolExecutionResult searchFiles(String rootPath, String pattern) {
        try {
            Path root = resolvePath(rootPath);
            if (!Files.exists(root) || !Files.isDirectory(root)) {
                return ToolExecutionResult.failure("Root path is not a valid directory: " + root);
            }

            List<String> matches = new ArrayList<>();
            String lowerPattern = pattern.toLowerCase();

            try (Stream<Path> walk = Files.walk(root, 6)) {
                walk.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase().contains(lowerPattern))
                        .limit(50)
                        .forEach(p -> matches.add(root.relativize(p).toString()));
            }

            if (matches.isEmpty()) {
                return ToolExecutionResult.success("No files matching '" + pattern + "' found in " + root);
            }
            return ToolExecutionResult.success("Found " + matches.size() + " matches:\n" + String.join("\n", matches));
        } catch (Exception e) {
            return ToolExecutionResult.failure("Error searching files: " + e.getMessage());
        }
    }

    public ToolExecutionResult readFile(String filePath) {
        try {
            Path path = resolvePath(filePath);
            if (!Files.exists(path)) {
                return ToolExecutionResult.failure("File does not exist: " + path);
            }
            if (!Files.isRegularFile(path)) {
                return ToolExecutionResult.failure("Path is a directory, not a file: " + path);
            }

            long size = Files.size(path);
            if (size > MAX_READ_BYTES) {
                return ToolExecutionResult.failure(String.format("File size (%d bytes) exceeds safe read limit (%d bytes)", size, MAX_READ_BYTES));
            }

            String content = Files.readString(path, StandardCharsets.UTF_8);
            return ToolExecutionResult.success(content);
        } catch (Exception e) {
            return ToolExecutionResult.failure("Failed to read file: " + e.getMessage());
        }
    }

    public ToolExecutionResult createFile(String filePath, String content) {
        try {
            Path path = resolvePath(filePath);
            if (Files.exists(path)) {
                return ToolExecutionResult.failure("File already exists: " + path);
            }

            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }

            Files.writeString(path, content != null ? content : "", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return ToolExecutionResult.success("Successfully created file: " + path.toAbsolutePath());
        } catch (Exception e) {
            return ToolExecutionResult.failure("Failed to create file: " + e.getMessage());
        }
    }

    public ToolExecutionResult createDirectory(String dirPath) {
        try {
            Path path = resolvePath(dirPath);
            if (Files.exists(path)) {
                return ToolExecutionResult.success("Directory already exists: " + path.toAbsolutePath());
            }
            Files.createDirectories(path);
            return ToolExecutionResult.success("Successfully created directory: " + path.toAbsolutePath());
        } catch (Exception e) {
            return ToolExecutionResult.failure("Failed to create directory: " + e.getMessage());
        }
    }

    public ToolExecutionResult modifyFile(String filePath, String content, boolean append) {
        try {
            Path path = resolvePath(filePath);
            if (!Files.exists(path)) {
                return ToolExecutionResult.failure("Cannot modify non-existent file: " + path);
            }

            if (append) {
                Files.writeString(path, content != null ? content : "", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            } else {
                Files.writeString(path, content != null ? content : "", StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            }
            return ToolExecutionResult.success("Successfully updated file: " + path.toAbsolutePath());
        } catch (Exception e) {
            return ToolExecutionResult.failure("Failed to modify file: " + e.getMessage());
        }
    }

    public ToolExecutionResult renameFile(String oldPath, String newName) {
        try {
            Path source = resolvePath(oldPath);
            if (!Files.exists(source)) {
                return ToolExecutionResult.failure("Source file does not exist: " + source);
            }
            Path target = source.resolveSibling(newName);
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            return ToolExecutionResult.success("Renamed file to: " + target.toAbsolutePath());
        } catch (Exception e) {
            return ToolExecutionResult.failure("Failed to rename file: " + e.getMessage());
        }
    }

    public ToolExecutionResult moveFile(String sourcePath, String targetDirectory) {
        try {
            Path source = resolvePath(sourcePath);
            Path targetDir = resolvePath(targetDirectory);
            if (!Files.exists(source)) {
                return ToolExecutionResult.failure("Source does not exist: " + source);
            }
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            Path dest = targetDir.resolve(source.getFileName());
            Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
            return ToolExecutionResult.success("Moved file to: " + dest.toAbsolutePath());
        } catch (Exception e) {
            return ToolExecutionResult.failure("Failed to move file: " + e.getMessage());
        }
    }

    public ToolExecutionResult deleteFile(String filePath) {
        try {
            Path path = resolvePath(filePath);
            if (!Files.exists(path)) {
                return ToolExecutionResult.failure("File does not exist: " + path);
            }
            if (Files.isDirectory(path)) {
                // Delete empty directory
                Files.delete(path);
            } else {
                Files.delete(path);
            }
            return ToolExecutionResult.success("Successfully deleted: " + path.toAbsolutePath());
        } catch (Exception e) {
            return ToolExecutionResult.failure("Failed to delete file: " + e.getMessage());
        }
    }

    private Path resolvePath(String rawPath) {
        if (rawPath == null || rawPath.trim().isEmpty()) {
            return Paths.get(System.getProperty("user.dir"));
        }
        String clean = rawPath.trim();
        if (clean.startsWith("~")) {
            clean = System.getProperty("user.home") + clean.substring(1);
        }
        return Paths.get(clean).toAbsolutePath().normalize();
    }
}

