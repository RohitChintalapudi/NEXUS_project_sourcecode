package com.nexus.model;

import java.util.ArrayList;
import java.util.List;

public class ProjectInfo {

    public enum ProjectType {
        MAVEN,
        GRADLE,
        GIT_REPO,
        JAVA_GENERIC
    }

    private final String name;
    private final String rootPath;
    private final ProjectType type;
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String packaging;
    private final List<String> dependencies;
    private final String recommendedRunCommand;

    public ProjectInfo(String name, String rootPath, ProjectType type,
                       String groupId, String artifactId, String version,
                       String packaging, List<String> dependencies,
                       String recommendedRunCommand) {
        this.name = name;
        this.rootPath = rootPath;
        this.type = type;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packaging = packaging != null ? packaging : "jar";
        this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
        this.recommendedRunCommand = recommendedRunCommand;
    }

    public String getName() {
        return name;
    }

    public String getRootPath() {
        return rootPath;
    }

    public ProjectType getType() {
        return type;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getPackaging() {
        return packaging;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public String getRecommendedRunCommand() {
        return recommendedRunCommand;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) at %s [Build: %s, Run: %s]",
                name, type, rootPath, (artifactId != null ? artifactId : "unknown"), recommendedRunCommand);
    }
}

