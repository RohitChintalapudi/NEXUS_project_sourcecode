package com.nexus.model;

import java.util.Collections;
import java.util.Set;

public class GitStatusResult {
    private final String branch;
    private final Set<String> staged;
    private final Set<String> modified;
    private final Set<String> untracked;
    private final Set<String> missing;
    private final boolean isClean;

    public GitStatusResult(String branch, Set<String> staged, Set<String> modified,
                           Set<String> untracked, Set<String> missing) {
        this.branch = branch != null ? branch : "main";
        this.staged = staged != null ? Collections.unmodifiableSet(staged) : Collections.emptySet();
        this.modified = modified != null ? Collections.unmodifiableSet(modified) : Collections.emptySet();
        this.untracked = untracked != null ? Collections.unmodifiableSet(untracked) : Collections.emptySet();
        this.missing = missing != null ? Collections.unmodifiableSet(missing) : Collections.emptySet();
        this.isClean = this.staged.isEmpty() && this.modified.isEmpty() && this.untracked.isEmpty() && this.missing.isEmpty();
    }

    public static GitStatusResult clean(String branch) {
        return new GitStatusResult(branch, Collections.emptySet(), Collections.emptySet(),
                Collections.emptySet(), Collections.emptySet());
    }

    public String getBranch() {
        return branch;
    }

    public Set<String> getStaged() {
        return staged;
    }

    public Set<String> getModified() {
        return modified;
    }

    public Set<String> getUntracked() {
        return untracked;
    }

    public Set<String> getMissing() {
        return missing;
    }

    public boolean isClean() {
        return isClean;
    }

    public int getTotalChanges() {
        return staged.size() + modified.size() + untracked.size() + missing.size();
    }

    public boolean isFileModified(String relativePath) {
        return modified.contains(relativePath);
    }

    public boolean isFileStaged(String relativePath) {
        return staged.contains(relativePath);
    }

    public boolean isFileUntracked(String relativePath) {
        return untracked.contains(relativePath);
    }
}

