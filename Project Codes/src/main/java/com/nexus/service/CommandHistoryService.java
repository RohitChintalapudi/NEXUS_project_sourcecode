package com.nexus.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.nexus.model.CommandHistoryItem;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class CommandHistoryService {
    private static final CommandHistoryService INSTANCE = new CommandHistoryService();

    private final ObservableList<CommandHistoryItem> historyObservable = FXCollections.observableArrayList();
    private final List<CommandHistoryItem> masterList = new CopyOnWriteArrayList<>();
    private final List<Consumer<CommandHistoryItem>> listeners = new CopyOnWriteArrayList<>();
    private final File historyFile;
    private final Gson gson;

    private CommandHistoryService() {
        String userHome = System.getProperty("user.home", ".");
        File nexusDir = new File(userHome, ".nexus");
        if (!nexusDir.exists()) {
            nexusDir.mkdirs();
        }
        historyFile = new File(nexusDir, "command_history.json");

        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .setPrettyPrinting()
                .create();

        loadFromFile();
    }

    public static CommandHistoryService getInstance() {
        return INSTANCE;
    }

    public ObservableList<CommandHistoryItem> getHistoryObservable() {
        return historyObservable;
    }

    public List<CommandHistoryItem> getMasterList() {
        return Collections.unmodifiableList(masterList);
    }

    public synchronized CommandHistoryItem record(String command, String directory, String status, int exitCode, long durationMs) {
        if (command == null || command.trim().isEmpty()) {
            return null;
        }

        String tool = detectTool(command.trim());
        CommandHistoryItem item = new CommandHistoryItem(
                "hist-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4),
                LocalDateTime.now(),
                command.trim(),
                directory,
                status,
                exitCode,
                durationMs,
                tool
        );

        masterList.add(0, item);

        // Add to beginning of observable list on FX Thread
        if (Platform.isFxApplicationThread()) {
            historyObservable.add(0, item);
        } else {
            Platform.runLater(() -> historyObservable.add(0, item));
        }

        for (Consumer<CommandHistoryItem> listener : listeners) {
            listener.accept(item);
        }

        saveToFile();
        return item;
    }

    public static String detectTool(String command) {
        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0) return "system";

        String first = parts[0].toLowerCase();
        if (first.endsWith(".exe") || first.endsWith(".cmd") || first.endsWith(".bat")) {
            first = first.substring(0, first.lastIndexOf('.'));
        }

        return switch (first) {
            case "mvn", "maven" -> "maven";
            case "git" -> "git";
            case "docker", "podman" -> "docker";
            case "npm", "yarn", "pnpm", "node", "bun" -> "npm";
            case "cargo", "rustc" -> "cargo";
            case "powershell", "pwsh" -> "powershell";
            case "cmd" -> "cmd";
            case "python", "python3", "pip" -> "python";
            case "java", "javac", "jar" -> "java";
            case "curl", "wget", "ping" -> "network";
            default -> "system";
        };
    }

    public synchronized void clear() {
        masterList.clear();
        if (Platform.isFxApplicationThread()) {
            historyObservable.clear();
        } else {
            Platform.runLater(historyObservable::clear);
        }
        if (historyFile.exists()) {
            historyFile.delete();
        }
    }

    public void addListener(Consumer<CommandHistoryItem> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<CommandHistoryItem> listener) {
        listeners.remove(listener);
    }

    private synchronized void saveToFile() {
        new Thread(() -> {
            synchronized (CommandHistoryService.this) {
                try (FileWriter writer = new FileWriter(historyFile)) {
                    List<CommandHistoryItem> snapshot = new ArrayList<>(historyObservable);
                    gson.toJson(snapshot, writer);
                } catch (IOException e) {
                    System.err.println("[NEXUS] Failed to persist command history: " + e.getMessage());
                }
            }
        }, "Nexus-History-Writer").start();
    }

    private synchronized void loadFromFile() {
        if (!historyFile.exists()) {
            // Seed with initial realistic dev data from MockDataService if completely fresh
            List<CommandHistoryItem> seed = MockDataService.getInstance().getCommandHistory();
            masterList.addAll(seed);
            historyObservable.addAll(seed);
            saveToFile();
            return;
        }

        try (FileReader reader = new FileReader(historyFile)) {
            Type listType = new TypeToken<List<CommandHistoryItem>>() {}.getType();
            List<CommandHistoryItem> loaded = gson.fromJson(reader, listType);
            if (loaded != null && !loaded.isEmpty()) {
                masterList.addAll(loaded);
                historyObservable.addAll(loaded);
            } else {
                List<CommandHistoryItem> seed = MockDataService.getInstance().getCommandHistory();
                masterList.addAll(seed);
                historyObservable.addAll(seed);
            }
        } catch (Exception e) {
            System.err.println("[NEXUS] Could not read existing history, initializing defaults: " + e.getMessage());
            List<CommandHistoryItem> seed = MockDataService.getInstance().getCommandHistory();
            masterList.addAll(seed);
            historyObservable.addAll(seed);
        }
    }

    private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.format(FORMATTER));
            }
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return LocalDateTime.parse(in.nextString(), FORMATTER);
        }
    }
}
