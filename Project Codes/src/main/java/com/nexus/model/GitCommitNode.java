package com.nexus.model;

import java.util.Collections;
import java.util.List;

public class GitCommitNode {
    private final String hash;
    private final String branch;
    private final String author;
    private final String date;
    private final String message;
    private final boolean isMerge;
    private final List<String> parentHashes;
    private final List<String> tags;
    private final List<String> filesChanged;
    private final int additions;
    private final int deletions;

    public GitCommitNode(String hash, String branch, String author, String date, String message,
                         boolean isMerge, List<String> parentHashes, List<String> tags,
                         List<String> filesChanged, int additions, int deletions) {
        this.hash = hash;
        this.branch = branch;
        this.author = author;
        this.date = date;
        this.message = message;
        this.isMerge = isMerge;
        this.parentHashes = parentHashes != null ? parentHashes : Collections.emptyList();
        this.tags = tags != null ? tags : Collections.emptyList();
        this.filesChanged = filesChanged != null ? filesChanged : Collections.emptyList();
        this.additions = additions;
        this.deletions = deletions;
    }

    public String getHash() {
        return hash;
    }

    public String getShortHash() {
        return hash.length() > 7 ? hash.substring(0, 7) : hash;
    }

    public String getBranch() {
        return branch;
    }

    public String getAuthor() {
        return author;
    }

    public String getDate() {
        return date;
    }

    public String getMessage() {
        return message;
    }

    public boolean isMerge() {
        return isMerge;
    }

    public List<String> getParentHashes() {
        return parentHashes;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<String> getFilesChanged() {
        return filesChanged;
    }

    public int getAdditions() {
        return additions;
    }

    public int getDeletions() {
        return deletions;
    }
}

