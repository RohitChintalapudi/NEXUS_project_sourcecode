package com.nexus.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatMessage {
    public enum Role {
        USER,
        ASSISTANT,
        SYSTEM
    }

    private final String id;
    private final Role role;
    private final String text;
    private final String codeSnippet;
    private final LocalDateTime timestamp;

    public ChatMessage(String id, Role role, String text, String codeSnippet, LocalDateTime timestamp) {
        this.id = id;
        this.role = role;
        this.text = text;
        this.codeSnippet = codeSnippet;
        this.timestamp = timestamp;
    }

    public static ChatMessage user(String text) {
        return new ChatMessage(
                "msg-" + System.currentTimeMillis(),
                Role.USER,
                text,
                null,
                LocalDateTime.now()
        );
    }

    public static ChatMessage assistant(String text, String codeSnippet) {
        return new ChatMessage(
                "msg-" + System.currentTimeMillis(),
                Role.ASSISTANT,
                text,
                codeSnippet,
                LocalDateTime.now()
        );
    }

    public static ChatMessage system(String text) {
        return new ChatMessage(
                "msg-" + System.currentTimeMillis(),
                Role.SYSTEM,
                text,
                null,
                LocalDateTime.now()
        );
    }

    public String getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public String getText() {
        return text;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}

