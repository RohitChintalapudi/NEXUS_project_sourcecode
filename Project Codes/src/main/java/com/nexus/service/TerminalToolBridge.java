package com.nexus.service;

import com.nexus.model.CommandBlock;
import com.nexus.model.ToolExecutionResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class TerminalToolBridge {

    private static final TerminalToolBridge INSTANCE = new TerminalToolBridge();

    private TerminalToolBridge() {}

    public static TerminalToolBridge getInstance() {
        return INSTANCE;
    }

    public ToolExecutionResult executeCommand(String command, String targetDirectory) {
        TerminalProcessManager manager = TerminalProcessManager.getInstance();
        TerminalSession session = manager.getActiveSession();

        if (session == null) {
            session = manager.createSession("AI Task Runner", null);
        }

        // If a target directory is requested and differs from session working directory, cd first
        if (targetDirectory != null && !targetDirectory.trim().isEmpty() &&
                !targetDirectory.equalsIgnoreCase(session.getWorkingDirectory())) {
            session.executeCommand("cd \"" + targetDirectory + "\"");
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CommandBlock> resultRef = new AtomicReference<>();

        Consumer<CommandBlock> listener = block -> {
            if (block.getCommandLine().equals(command)) {
                resultRef.set(block);
                latch.countDown();
            }
        };

        session.addBlockAddedListener(listener);

        try {
            boolean started = session.executeCommand(command);
            if (!started) {
                return ToolExecutionResult.failure("Terminal session is currently busy running another process.");
            }

            // Wait up to 30 seconds for command completion
            boolean finished = latch.await(30, TimeUnit.SECONDS);
            if (!finished) {
                return ToolExecutionResult.failure("Command execution timed out after 30 seconds: " + command);
            }

            CommandBlock block = resultRef.get();
            if (block == null) {
                return ToolExecutionResult.success("[Command executed successfully]");
            }

            String output = String.format("Exit Code: %d (Duration: %d ms)\nOutput:\n%s",
                    block.getExitCode(),
                    block.getDurationMs(),
                    block.getOutput());

            return block.getExitCode() == 0
                    ? ToolExecutionResult.success(output)
                    : ToolExecutionResult.failure("Process exited with code " + block.getExitCode() + "\n" + output);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolExecutionResult.failure("Command execution was interrupted.");
        } finally {
            session.removeBlockAddedListener(listener);
        }
    }

    public String getActiveWorkingDirectory() {
        TerminalSession active = TerminalProcessManager.getInstance().getActiveSession();
        return active != null ? active.getWorkingDirectory() : System.getProperty("user.dir");
    }

    public String listActiveSessions() {
        StringBuilder sb = new StringBuilder();
        for (TerminalSession s : TerminalProcessManager.getInstance().getSessions()) {
            sb.append(String.format("Session '%s' [%s] | State: %s | PWD: %s\n",
                    s.getTitle(), s.getShellType().getDisplayName(), s.getState(), s.getWorkingDirectory()));
        }
        return sb.toString().trim();
    }
}

