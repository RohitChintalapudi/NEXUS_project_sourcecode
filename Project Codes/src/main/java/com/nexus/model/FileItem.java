package com.nexus.model;

import java.io.File;

public class FileItem {
    private final File file;
    private final String displayName;
    private final boolean isDirectory;
    private final long size;
    private final String extension;
    private String gitStatus; // MODIFIED, STAGED, UNTRACKED, or null

    public FileItem(File file) {
        this(file, null);
    }

    public FileItem(File file, String gitStatus) {
        this.file = file;
        this.displayName = file != null ? file.getName() : "";
        this.isDirectory = file != null && file.isDirectory();
        this.size = (file != null && file.isFile()) ? file.length() : 0;
        this.extension = extractExtension(file);
        this.gitStatus = gitStatus;
    }

    public String getGitStatus() {
        return gitStatus;
    }

    public void setGitStatus(String gitStatus) {
        this.gitStatus = gitStatus;
    }

    public File getFile() {
        return file;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public long getSize() {
        return size;
    }

    public String getExtension() {
        return extension;
    }

    public String getFormattedSize() {
        if (isDirectory) {
            return "Folder";
        }
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        }
    }

    public String getIconGlyph() {
        if (isDirectory) {
            return "\uD83D\uDCC1";
        }
        return switch (extension.toLowerCase()) {
            case "java" -> "☕";
            case "xml", "json", "yml", "yaml", "gradle" -> "⚙️";
            case "md", "txt", "properties" -> "\uD83D\uDCDD";
            case "css", "fxml" -> "\uD83C\uDFA8";
            case "jar", "zip", "tar", "gz" -> "\uD83D\uDCE6";
            case "sql", "db" -> "\uD83D\uDDC4";
            case "sh", "bat", "ps1", "cmd" -> "\u26A1";
            case "png", "jpg", "jpeg", "svg", "ico" -> "\uD83D\uDDBC";
            default -> "\uD83D\uDCC4";
        };
    }

    private String extractExtension(File f) {
        if (f == null || f.isDirectory()) {
            return "";
        }
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1) : "";
    }

    @Override
    public String toString() {
        return displayName;
    }
}

