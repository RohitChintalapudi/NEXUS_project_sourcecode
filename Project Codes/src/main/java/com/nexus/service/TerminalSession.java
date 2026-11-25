package com.nexus.service;

import com.nexus.model.CommandBlock;
import com.nexus.model.ShellType;
import com.nexus.util.AnsiRenderer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TerminalSession {

    public enum State {
        READY,
        RUNNING,
        CANCELLED,
        TERMINATED
    }

    private static final Pattern BOUNDARY_PATTERN =
            Pattern.compile("\\[NEXUS_BOUNDARY:PWD=(.*?)\\|EXIT[=:(](.*?)\\]");

    private final String id;
    private String title;
    private final ShellType shellType;
    private String workingDirectory;
    private State state = State.READY;

    private Process process;
    private BufferedWriter stdinWriter;
    private BufferedReader stdoutReader;
    private Thread outputReaderThread;

    private String currentRunningCommand = null;
    private long commandStartTime = 0;
    private final StringBuilder currentOutputBuffer = new StringBuilder();

    private final ObservableList<CommandBlock> blocks = FXCollections.observableArrayList();
    private final List<Consumer<TerminalSession>> stateChangeListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<CommandBlock>> blockAddedListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> outputStreamListeners = new CopyOnWriteArrayList<>();

    public TerminalSession(String id, String title, ShellType shellType, String initialDir) {
        this.id = id;
        this.title = title;
        this.shellType = shellType != null ? shellType : ShellDetector.getInstance().getDefaultShell();

        if (initialDir != null && new File(initialDir).isDirectory()) {
            this.workingDirectory = initialDir;
        } else {
            this.workingDirectory = System.getProperty("user.dir");
        }
    }

    public synchronized void start() {
        if (state == State.RUNNING && process != null && process.isAlive()) {
            return;
        }

        try {
            List<String> commandList = new ArrayList<>();
            commandList.add(shellType.getExecutable());
            commandList.addAll(Arrays.asList(shellType.getDefaultArgs()));

            ProcessBuilder pb = new ProcessBuilder(commandList);
            pb.directory(new File(workingDirectory));

            Map<String, String> env = pb.environment();
            env.put("TERM", "xterm-256color");
            env.put("COLORTERM", "truecolor");
            env.put("NEXUS_TERMINAL_SESSION", id);

            // Merge error stream into output stream for consistent interleaved output
            pb.redirectErrorStream(true);

            process = pb.start();
            stdinWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

            state = State.READY;
            notifyStateChange();

            startOutputReader();
            initializeShellEnvironment();
        } catch (IOException e) {
            System.err.println("[NEXUS] Failed to spawn shell session: " + e.getMessage());
            state = State.TERMINATED;
            notifyStateChange();
        }
    }

    private void startOutputReader() {
        outputReaderThread = new Thread(() -> {
            try {
                char[] buffer = new char[1024];
                int read;
                StringBuilder lineBuilder = new StringBuilder();

                while ((read = stdoutReader.read(buffer, 0, buffer.length)) != -1) {
                    String chunk = new String(buffer, 0, read);

                    for (int i = 0; i < chunk.length(); i++) {
                        char c = chunk.charAt(i);
                        if (c == '\n') {
                            processOutputLine(lineBuilder.toString());
                            lineBuilder.setLength(0);
                        } else if (c != '\r') {
                            lineBuilder.append(c);
                        }
                    }

                    // Forward raw chunk to live stream listeners if any
                    for (Consumer<String> l : outputStreamListeners) {
                        l.accept(chunk);
                    }
                }
            } catch (IOException ignored) {
                // Process terminated
            } finally {
                onProcessExited();
            }
        }, "Nexus-TerminalSessionReader-" + id);
        outputReaderThread.setDaemon(true);
        outputReaderThread.start();
    }

    private synchronized void processOutputLine(String line) {
        if (line.contains("NEXUS_BOUNDARY")) {
            // If this is the shell's echo of the probe command, ignore it
            if (line.contains("echo ") || line.contains("Write-Output") || line.contains("Write-Host") || line.contains("$(") || line.contains("%CD%") || line.contains("$__ec") || line.contains("WriteLine") || line.contains("& {")) {
                return;
            }

            Matcher matcher = BOUNDARY_PATTERN.matcher(line);
            if (matcher.find()) {
                String newPwd = matcher.group(1).trim();
                String exitStr = matcher.group(2).trim();

                int exitCode = 0;
                try {
                    if (!exitStr.isEmpty()) {
                        exitCode = Integer.parseInt(exitStr);
                    }
                } catch (NumberFormatException ignored) {}

                if (!newPwd.isEmpty() && new File(newPwd).exists()) {
                    this.workingDirectory = newPwd;
                }

                if (currentRunningCommand != null) {
                    finishActiveCommand(exitCode);
                }
            }
            return;
        }

        // Standard terminal output line
        if (currentRunningCommand != null) {
            currentOutputBuffer.append(line).append("\n");
        }
    }

    private synchronized void finishActiveCommand(int exitCode) {
        long durationMs = Math.max(1, System.currentTimeMillis() - commandStartTime);
        String rawOutput = currentOutputBuffer.toString().trim();
        String cleanedOutput = AnsiRenderer.stripAnsi(rawOutput);

        CommandBlock block = new CommandBlock(
                "cmd-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4),
                workingDirectory,
                detectGitBranch(),
                currentRunningCommand,
                cleanedOutput.isEmpty() ? "[Process completed with exit code " + exitCode + "]" : cleanedOutput,
                exitCode,
                durationMs,
                LocalDateTime.now()
        );

        // Record to Command History
        CommandHistoryService.getInstance().record(
                currentRunningCommand,
                workingDirectory,
                exitCode == 0 ? "SUCCESS" : "FAILED",
                exitCode,
                durationMs
        );

        currentRunningCommand = null;
        currentOutputBuffer.setLength(0);
        state = State.READY;

        Platform.runLater(() -> {
            blocks.add(block);
            for (Consumer<CommandBlock> l : blockAddedListeners) {
                l.accept(block);
            }
        });

        notifyStateChange();
    }

    private void initializeShellEnvironment() {
        if (shellType == ShellType.POWERSHELL || shellType == ShellType.PWSH) {
            String polyfills =
                "function touch { param([Parameter(ValueFromRemainingArguments=$true)]$files); foreach ($f in $files) { if (-not (Test-Path $f)) { New-Item -ItemType File -Path $f -Force | Out-Null } else { (Get-Item $f).LastWriteTime = Get-Date } } }\r\n" +
                "function which { param([Parameter(ValueFromRemainingArguments=$true)]$names); foreach ($n in $names) { $c = Get-Command $n -ErrorAction SilentlyContinue; if ($c) { if ($c.Source) { $c.Source } else { $c.Name } } } }\r\n" +
                "function grep { param([Parameter(ValueFromPipeline=$true)]$InputObject, [string]$Pattern, [string]$Path); if ($InputObject) { $InputObject | Select-String -Pattern $Pattern } elseif ($Path) { Select-String -Path $Path -Pattern $Pattern } else { Select-String -Pattern $Pattern } }\r\n" +
                "function export { param([string]$Assign); if ($Assign -match '([^=]+)=(.*)') { [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process') } }\r\n" +
                "function unset { param([string]$Name); [Environment]::SetEnvironmentVariable($Name, $null, 'Process') }\r\n" +
                "function env { Get-ChildItem env: }\r\n" +
                "function ll { Get-ChildItem -Force }\r\n" +
                "function head { param([string]$Path, [int]$n = 10); if ($Path) { Get-Content $Path -TotalCount $n } }\r\n" +
                "function tail { param([string]$Path, [int]$n = 10); if ($Path) { Get-Content $Path -Tail $n } }\r\n" +
                "function open { param([string]$target = '.'); Start-Process $target }\r\n" +
                "function rm-compat { param([Parameter(ValueFromRemainingArguments=$true)]$args); $paths = @(); foreach ($a in $args) { if ($a -notmatch '^-') { $paths += $a } }; foreach ($p in $paths) { Remove-Item -Path $p -Recurse -Force -ErrorAction SilentlyContinue } }; Set-Alias -Name rm -Value rm-compat -Option AllScope -ErrorAction SilentlyContinue\r\n";
            sendRawCommand(polyfills);
        }
    }

    public synchronized boolean executeCommand(String commandLine) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            return false;
        }

        // If a command is actively running, forward input directly to its standard input
        if (state == State.RUNNING) {
            sendRawInput(commandLine);
            return true;
        }

        if (process == null || !process.isAlive()) {
            start();
        }

        String cmd = commandLine.trim();

        // Built-in clear command interception
        if (cmd.equalsIgnoreCase("clear") || cmd.equalsIgnoreCase("cls")) {
            clearOutput();
            CommandHistoryService.getInstance().record(cmd, workingDirectory, "SUCCESS", 0, 5);
            return true;
        }

        this.currentRunningCommand = cmd;
        this.commandStartTime = System.currentTimeMillis();
        this.currentOutputBuffer.setLength(0);
        this.state = State.RUNNING;
        notifyStateChange();

        String processedCmd = preprocessCommand(cmd);
        String boundaryProbe = ShellDetector.getInstance().getBoundaryProbeCommand(shellType);
        sendRawCommand(processedCmd + "\r\n" + boundaryProbe);
        return true;
    }

    private String preprocessCommand(String cmd) {
        if (shellType == ShellType.POWERSHELL && (cmd.contains("&&") || cmd.contains("||"))) {
            return translatePowerShellChaining(cmd);
        }
        return cmd;
    }

    private String translatePowerShellChaining(String cmd) {
        if (cmd.contains("&&") && !cmd.contains("||")) {
            String[] parts = cmd.split("\\s*&&\\s*");
            if (parts.length > 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    if (i == 0) {
                        sb.append(parts[i]);
                    } else {
                        sb.append("; if ($?) { ").append(parts[i]).append(" }");
                    }
                }
                return sb.toString();
            }
        }
        return cmd;
    }

    public synchronized void sendRawInput(String input) {
        try {
            if (stdinWriter != null) {
                currentOutputBuffer.append("> ").append(input).append("\n");
                stdinWriter.write(input);
                stdinWriter.write("\r\n");
                stdinWriter.flush();
            }
        } catch (IOException e) {
            System.err.println("[NEXUS] Error sending input to running process: " + e.getMessage());
        }
    }

    private synchronized void sendRawCommand(String cmd) {
        try {
            if (stdinWriter != null) {
                String normalized = cmd.replace("\r\n", "\n").replace("\n", "\r\n");
                stdinWriter.write(normalized);
                stdinWriter.write("\r\n");
                stdinWriter.flush();
            }
        } catch (IOException e) {
            System.err.println("[NEXUS] Error writing to shell stdin: " + e.getMessage());
            onProcessExited();
        }
    }

    public synchronized void cancelRunningProcess() {
        if (state != State.RUNNING) return;

        try {
            // Forcibly destroy all descendant subprocesses (the actual running command)
            if (process != null && process.isAlive()) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);

                // Send ASCII break (Ctrl+C) into the shell
                if (stdinWriter != null) {
                    stdinWriter.write("\u0003");
                    stdinWriter.newLine();
                    stdinWriter.flush();
                }
            }

            long durationMs = Math.max(1, System.currentTimeMillis() - commandStartTime);
            CommandBlock cancelledBlock = new CommandBlock(
                    "cmd-cancel-" + System.currentTimeMillis(),
                    workingDirectory,
                    detectGitBranch(),
                    currentRunningCommand != null ? currentRunningCommand : "interrupt",
                    "[Process cancelled by user (SIGINT / Ctrl+C)]",
                    130, // Standard exit code for SIGINT
                    durationMs,
                    LocalDateTime.now()
            );

            CommandHistoryService.getInstance().record(
                    currentRunningCommand != null ? currentRunningCommand : "interrupt",
                    workingDirectory,
                    "CANCELLED",
                    130,
                    durationMs
            );

            currentRunningCommand = null;
            currentOutputBuffer.setLength(0);
            state = State.READY;

            Platform.runLater(() -> {
                blocks.add(cancelledBlock);
                for (Consumer<CommandBlock> l : blockAddedListeners) {
                    l.accept(cancelledBlock);
                }
            });

            notifyStateChange();

            // Re-probe directory
            sendRawCommand(ShellDetector.getInstance().getBoundaryProbeCommand(shellType));

        } catch (Exception e) {
            System.err.println("[NEXUS] Exception during process cancellation: " + e.getMessage());
        }
    }

    public synchronized void restart() {
        terminate();
        blocks.clear();
        start();
    }

    public synchronized void terminate() {
        state = State.TERMINATED;
        try {
            if (process != null) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
            if (stdinWriter != null) {
                stdinWriter.close();
            }
            if (stdoutReader != null) {
                stdoutReader.close();
            }
        } catch (Exception ignored) {}

        notifyStateChange();
    }

    public void clearOutput() {
        Platform.runLater(blocks::clear);
    }

    private void onProcessExited() {
        if (currentRunningCommand != null) {
            int ec = 1;
            try {
                if (process != null) ec = process.exitValue();
            } catch (IllegalThreadStateException ignored) {}
            finishActiveCommand(ec);
        }
        if (state != State.TERMINATED) {
            state = State.TERMINATED;
            notifyStateChange();
        }
    }

    private void notifyStateChange() {
        Platform.runLater(() -> {
            for (Consumer<TerminalSession> l : stateChangeListeners) {
                l.accept(this);
            }
        });
    }

    private String detectGitBranch() {
        try {
            File gitHead = new File(workingDirectory, ".git/HEAD");
            if (gitHead.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(gitHead))) {
                    String line = br.readLine();
                    if (line != null && line.startsWith("ref: refs/heads/")) {
                        return line.substring("ref: refs/heads/".length()).trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "main";
    }

    // Getters and Listeners
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ShellType getShellType() {
        return shellType;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public State getState() {
        return state;
    }

    public String getCurrentRunningCommand() {
        return currentRunningCommand;
    }

    public ObservableList<CommandBlock> getBlocks() {
        return blocks;
    }

    public void addStateChangeListener(Consumer<TerminalSession> listener) {
        stateChangeListeners.add(listener);
    }

    public void addBlockAddedListener(Consumer<CommandBlock> listener) {
        blockAddedListeners.add(listener);
    }

    public void removeBlockAddedListener(Consumer<CommandBlock> listener) {
        blockAddedListeners.remove(listener);
    }

    public void addOutputStreamListener(Consumer<String> listener) {
        outputStreamListeners.add(listener);
    }
}
