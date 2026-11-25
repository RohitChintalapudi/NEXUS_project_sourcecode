package com.nexus.service;

import com.nexus.model.ProjectInfo;
import com.nexus.model.ToolExecutionResult;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectDiscoveryService {

    private static final ProjectDiscoveryService INSTANCE = new ProjectDiscoveryService();

    private static final Pattern GROUP_ID_PATTERN = Pattern.compile("<groupId>(.*?)</groupId>");
    private static final Pattern ARTIFACT_ID_PATTERN = Pattern.compile("<artifactId>(.*?)</artifactId>");
    private static final Pattern VERSION_PATTERN = Pattern.compile("<version>(.*?)</version>");
    private static final Pattern PACKAGING_PATTERN = Pattern.compile("<packaging>(.*?)</packaging>");

    private final Map<String, ProjectInfo> projectCache = new LinkedHashMap<>();

    private ProjectDiscoveryService() {}

    public static ProjectDiscoveryService getInstance() {
        return INSTANCE;
    }

    public ToolExecutionResult discoverProjects(String searchRoot) {
        List<Path> rootPaths = resolveSearchRoots(searchRoot);
        List<ProjectInfo> discovered = new ArrayList<>();

        for (Path root : rootPaths) {
            if (!Files.exists(root) || !Files.isDirectory(root)) {
                continue;
            }

            try (Stream<Path> walk = Files.walk(root, 4)) {
                walk.filter(Files::isDirectory)
                        .forEach(dir -> {
                            ProjectInfo info = inspectDirectoryForProject(dir);
                            if (info != null) {
                                discovered.add(info);
                                projectCache.put(info.getName().toLowerCase(), info);
                            }
                        });
            } catch (Exception ignored) {}
        }

        if (discovered.isEmpty()) {
            return ToolExecutionResult.success("No Java, Maven, Gradle, or Git projects detected in the searched directories.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Found %d verified developer project(s):\n\n", discovered.size()));
        for (ProjectInfo p : discovered) {
            sb.append(String.format("• %s [%s]\n  Path: %s\n  Artifact: %s:%s\n  Recommended Run: %s\n\n",
                    p.getName(), p.getType(), p.getRootPath(),
                    (p.getGroupId() != null ? p.getGroupId() : "N/A"),
                    (p.getArtifactId() != null ? p.getArtifactId() : "N/A"),
                    p.getRecommendedRunCommand()));
        }

        return ToolExecutionResult.success(sb.toString().trim());
    }

    public ToolExecutionResult findProjectByName(String projectName, String searchRoot) {
        if (projectName == null || projectName.trim().isEmpty()) {
            return ToolExecutionResult.failure("Project name must not be blank.");
        }

        String searchName = projectName.trim().toLowerCase();

        // 1. Check in-memory cache first
        if (projectCache.containsKey(searchName)) {
            ProjectInfo cached = projectCache.get(searchName);
            return ToolExecutionResult.success(formatProjectDetail(cached));
        }

        // 2. Discover across search roots
        List<Path> rootPaths = resolveSearchRoots(searchRoot);
        for (Path root : rootPaths) {
            if (!Files.exists(root) || !Files.isDirectory(root)) continue;

            try (Stream<Path> walk = Files.walk(root, 5)) {
                Optional<Path> found = walk.filter(Files::isDirectory)
                        .filter(dir -> dir.getFileName().toString().equalsIgnoreCase(searchName))
                        .findFirst();

                if (found.isPresent()) {
                    ProjectInfo info = inspectDirectoryForProject(found.get());
                    if (info == null) {
                        // Project directory without build file
                        info = new ProjectInfo(found.get().getFileName().toString(),
                                found.get().toAbsolutePath().toString(),
                                ProjectInfo.ProjectType.JAVA_GENERIC,
                                null, found.get().getFileName().toString(), "1.0.0",
                                "dir", List.of(), "java -jar target/*.jar");
                    }
                    projectCache.put(searchName, info);
                    return ToolExecutionResult.success(formatProjectDetail(info));
                }
            } catch (Exception ignored) {}
        }

        return ToolExecutionResult.failure("No project named '" + projectName + "' found in search paths. Verified search roots: " +
                rootPaths.stream().map(Path::toString).collect(Collectors.joining("; ")));
    }

    public ProjectInfo inspectDirectoryForProject(Path dir) {
        Path pomFile = dir.resolve("pom.xml");
        Path gradleFile = dir.resolve("build.gradle");
        Path gradleKts = dir.resolve("build.gradle.kts");
        Path gitDir = dir.resolve(".git");

        String name = dir.getFileName().toString();

        if (Files.exists(pomFile)) {
            return parseMavenProject(dir, pomFile);
        } else if (Files.exists(gradleFile) || Files.exists(gradleKts)) {
            return new ProjectInfo(name, dir.toAbsolutePath().toString(),
                    ProjectInfo.ProjectType.GRADLE, null, name, "1.0.0", "jar",
                    List.of(), "./gradlew run");
        } else if (Files.exists(gitDir)) {
            return new ProjectInfo(name, dir.toAbsolutePath().toString(),
                    ProjectInfo.ProjectType.GIT_REPO, null, name, "1.0.0", "git",
                    List.of(), "git status");
        }

        return null;
    }

    private ProjectInfo parseMavenProject(Path dir, Path pomFile) {
        String name = dir.getFileName().toString();
        String groupId = null;
        String artifactId = null;
        String version = null;
        String packaging = "jar";
        String recommendedRun = "mvn clean test";

        try {
            String xml = Files.readString(pomFile, StandardCharsets.UTF_8);

            Matcher gMatcher = GROUP_ID_PATTERN.matcher(xml);
            if (gMatcher.find()) groupId = gMatcher.group(1).trim();

            Matcher aMatcher = ARTIFACT_ID_PATTERN.matcher(xml);
            if (aMatcher.find()) artifactId = aMatcher.group(1).trim();

            Matcher vMatcher = VERSION_PATTERN.matcher(xml);
            if (vMatcher.find()) version = vMatcher.group(1).trim();

            Matcher pMatcher = PACKAGING_PATTERN.matcher(xml);
            if (pMatcher.find()) packaging = pMatcher.group(1).trim();

            if (xml.contains("spring-boot")) {
                recommendedRun = "mvn spring-boot:run";
            } else if (xml.contains("javafx-maven-plugin")) {
                recommendedRun = "mvn javafx:run";
            } else {
                recommendedRun = "mvn clean compile exec:java";
            }
        } catch (IOException ignored) {}

        return new ProjectInfo(
                name,
                dir.toAbsolutePath().toString(),
                ProjectInfo.ProjectType.MAVEN,
                groupId,
                artifactId != null ? artifactId : name,
                version != null ? version : "1.0.0",
                packaging,
                List.of(),
                recommendedRun
        );
    }

    private String formatProjectDetail(ProjectInfo p) {
        return String.format("""
                Verified Project Found:
                • Name             : %s
                • Absolute Path    : %s
                • Build System     : %s
                • Maven Coordinates: %s:%s:%s
                • Packaging        : %s
                • Recommended Run  : %s
                """,
                p.getName(),
                p.getRootPath(),
                p.getType(),
                p.getGroupId() != null ? p.getGroupId() : "N/A",
                p.getArtifactId() != null ? p.getArtifactId() : p.getName(),
                p.getVersion() != null ? p.getVersion() : "1.0.0",
                p.getPackaging(),
                p.getRecommendedRunCommand()).trim();
    }

    private List<Path> resolveSearchRoots(String customRoot) {
        List<Path> roots = new ArrayList<>();
        if (customRoot != null && !customRoot.trim().isEmpty()) {
            roots.add(Paths.get(customRoot.trim()).toAbsolutePath().normalize());
            return roots;
        }

        // Default developer workspaces to scan
        Path currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        roots.add(currentDir);

        if (currentDir.getParent() != null) {
            roots.add(currentDir.getParent()); // e.g. C:\Users\rohit\OneDrive\Desktop\project
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            Path desktop = Paths.get(userHome, "Desktop");
            if (Files.exists(desktop)) roots.add(desktop);

            Path projects = Paths.get(userHome, "Projects");
            if (Files.exists(projects)) roots.add(projects);
        }

        return roots.stream().distinct().toList();
    }
}

