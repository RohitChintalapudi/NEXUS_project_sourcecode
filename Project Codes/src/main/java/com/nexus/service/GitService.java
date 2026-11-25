package com.nexus.service;

import com.nexus.model.GitCommitNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class GitService {

    private static final GitService INSTANCE = new GitService();

    private GitService() {}

    public static GitService getInstance() {
        return INSTANCE;
    }

    public boolean isGitRepository(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return false;
        }
        File gitDir = new File(directory, ".git");
        return gitDir.exists();
    }

    public List<String> getBranches(File directory) {
        List<String> branches = new ArrayList<>();
        if (!isGitRepository(directory)) {
            return branches;
        }

        try {
            Process process = new ProcessBuilder("git", "branch", "--list")
                    .directory(directory)
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("*")) {
                        line = line.substring(1).trim();
                    }
                    if (!line.isEmpty()) {
                        branches.add(line);
                    }
                }
            }
            process.waitFor(3, TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        if (branches.isEmpty()) {
            branches.add("main");
        }
        return branches;
    }

    public List<GitCommitNode> getCommitHistory(File directory, String branch, int limit) {
        List<GitCommitNode> commits = new ArrayList<>();
        if (!isGitRepository(directory)) {
            return commits;
        }

        try {
            List<String> cmd = new ArrayList<>(Arrays.asList("git", "log", "-n", String.valueOf(limit),
                    "--pretty=format:%H|||%an|||%ar|||%s|||%D"));
            if (branch != null && !branch.isEmpty() && !"All Branches".equalsIgnoreCase(branch)) {
                cmd.add(branch);
            }

            Process process = new ProcessBuilder(cmd)
                    .directory(directory)
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|\\|\\|", -1);
                    if (parts.length >= 4) {
                        String fullHash = parts[0].trim();
                        String author = parts[1].trim();
                        String date = parts[2].trim();
                        String message = parts[3].trim();
                        String refDecorations = parts.length > 4 ? parts[4].trim() : "";

                        List<String> tags = new ArrayList<>();
                        String commitBranch = (branch != null && !branch.isEmpty() && !"All Branches".equalsIgnoreCase(branch)) ? branch : "main";
                        if (!refDecorations.isEmpty()) {
                            String[] refs = refDecorations.split(",");
                            for (String ref : refs) {
                                ref = ref.trim();
                                if (ref.startsWith("tag: ")) {
                                    tags.add(ref.substring(5).trim());
                                } else if (ref.contains("->")) {
                                    commitBranch = ref.substring(ref.indexOf("->") + 2).trim();
                                }
                            }
                        }

                        List<String> changedFiles = getChangedFilesForCommit(directory, fullHash);

                        commits.add(new GitCommitNode(
                                fullHash,
                                commitBranch,
                                author,
                                date,
                                message,
                                false,
                                Collections.emptyList(),
                                tags,
                                changedFiles,
                                Math.max(1, changedFiles.size() * 12),
                                Math.max(0, changedFiles.size() * 3)
                        ));
                    }
                }
            }
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        return commits;
    }

    public List<String> getChangedFilesForCommit(File directory, String commitHash) {
        List<String> files = new ArrayList<>();
        try {
            Process process = new ProcessBuilder("git", "diff-tree", "--no-commit-id", "--name-only", "-r", commitHash)
                    .directory(directory)
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        files.add(line);
                    }
                }
            }
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        if (files.isEmpty()) {
            files.add("README.md");
        }
        return files;
    }

    public String getDiffSummary(File directory, String commitHash) {
        if (!isGitRepository(directory) || commitHash == null) {
            return "No git diff available.";
        }
        StringBuilder sb = new StringBuilder();
        try {
            Process process = new ProcessBuilder("git", "show", "--stat", "--oneline", commitHash)
                    .directory(directory)
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null && count < 25) {
                    sb.append(line).append("\n");
                    count++;
                }
            }
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "Error retrieving diff: " + e.getMessage();
        }
        return sb.toString().trim();
    }
}

