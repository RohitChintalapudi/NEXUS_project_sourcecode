package com.nexus.service;

import com.nexus.model.ChatMessage;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConversationService {

    private static final ConversationService INSTANCE = new ConversationService();
    private static final int MAX_HISTORY_MESSAGES = 40;

    private String currentConversationId;
    private final List<ChatMessage> messageHistory = new CopyOnWriteArrayList<>();

    // Entity memory
    private String activeProjectName = null;
    private String activeProjectPath = null;
    private String lastGeneratedCommand = null;
    private String activeSectionName = "Home";

    private ConversationService() {
        startNewConversation();
    }

    public static ConversationService getInstance() {
        return INSTANCE;
    }

    public synchronized void startNewConversation() {
        this.currentConversationId = "conv-" + UUID.randomUUID().toString().substring(0, 8);
        this.messageHistory.clear();
        this.activeProjectName = null;
        this.activeProjectPath = null;
        this.lastGeneratedCommand = null;
        this.activeSectionName = "Home";
    }

    public String getActiveSectionName() {
        return activeSectionName;
    }

    public void setActiveSectionName(String activeSectionName) {
        this.activeSectionName = activeSectionName;
    }

    public void addMessage(ChatMessage message) {
        if (messageHistory.size() >= MAX_HISTORY_MESSAGES) {
            messageHistory.remove(0);
        }
        messageHistory.add(message);
    }

    public List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messageHistory));
    }

    public String getCurrentConversationId() {
        return currentConversationId;
    }

    public String getActiveProjectName() {
        return activeProjectName;
    }

    public void setActiveProjectName(String activeProjectName) {
        this.activeProjectName = activeProjectName;
    }

    public String getActiveProjectPath() {
        return activeProjectPath;
    }

    public void setActiveProjectPath(String activeProjectPath) {
        this.activeProjectPath = activeProjectPath;
    }

    public String getLastGeneratedCommand() {
        return lastGeneratedCommand;
    }

    public void setLastGeneratedCommand(String lastGeneratedCommand) {
        this.lastGeneratedCommand = lastGeneratedCommand;
    }
}

