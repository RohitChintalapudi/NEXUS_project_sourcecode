package com.nexus.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class TerminalTabModel {
    private final String id;
    private String title;
    private String workingDirectory;
    private final ObservableList<CommandBlock> blocks = FXCollections.observableArrayList();

    public TerminalTabModel(String id, String title, String workingDirectory) {
        this.id = id;
        this.title = title;
        this.workingDirectory = workingDirectory;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public ObservableList<CommandBlock> getBlocks() {
        return blocks;
    }

    public void addBlock(CommandBlock block) {
        blocks.add(block);
    }
}

