package com.nexus.service;

import com.nexus.model.ShellType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ShellDetector {
    private static final ShellDetector INSTANCE = new ShellDetector();

    private final boolean isWindows;
    private final boolean isMac;
    private final boolean isLinux;
    private final List<ShellType> availableShells = new ArrayList<>();
    private ShellType defaultShell;
    private String windowsBashExecutable = null;

    private ShellDetector() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        isWindows = osName.contains("win");
        isMac = osName.contains("mac");
        isLinux = osName.contains("nix") || osName.contains("nux") || osName.contains("aix");

        detectAvailableShells();
    }

    public static ShellDetector getInstance() {
        return INSTANCE;
    }

    private void detectAvailableShells() {
        availableShells.clear();

        if (isWindows) {
            // Check for PowerShell 7 (pwsh)
            if (isExecutableAvailable("pwsh.exe")) {
                availableShells.add(ShellType.PWSH);
            }
            // Check for Windows PowerShell
            if (isExecutableAvailable("powershell.exe")) {
                availableShells.add(ShellType.POWERSHELL);
            }
            // Check for CMD
            if (isExecutableAvailable("cmd.exe")) {
                availableShells.add(ShellType.CMD);
            }
            // Check for Git Bash or system Bash on Windows
            windowsBashExecutable = resolveWindowsBash();
            if (windowsBashExecutable != null) {
                availableShells.add(ShellType.BASH);
            }

            // Default
            if (availableShells.contains(ShellType.POWERSHELL)) {
                defaultShell = ShellType.POWERSHELL;
            } else if (availableShells.contains(ShellType.PWSH)) {
                defaultShell = ShellType.PWSH;
            } else if (availableShells.contains(ShellType.CMD)) {
                defaultShell = ShellType.CMD;
            } else {
                defaultShell = ShellType.POWERSHELL;
            }
        } else {
            // Unix / macOS
            if (isExecutableAvailable("/bin/zsh") || isExecutableAvailable("zsh")) {
                availableShells.add(ShellType.ZSH);
            }
            if (isExecutableAvailable("/bin/bash") || isExecutableAvailable("bash")) {
                availableShells.add(ShellType.BASH);
            }
            if (isExecutableAvailable("/bin/sh") || isExecutableAvailable("sh")) {
                availableShells.add(ShellType.SH);
            }

            // Default
            String envShell = System.getenv("SHELL");
            if (envShell != null && envShell.contains("zsh") && availableShells.contains(ShellType.ZSH)) {
                defaultShell = ShellType.ZSH;
            } else if (availableShells.contains(ShellType.BASH)) {
                defaultShell = ShellType.BASH;
            } else if (!availableShells.isEmpty()) {
                defaultShell = availableShells.get(0);
            } else {
                defaultShell = ShellType.BASH;
            }
        }
    }

    private String resolveWindowsBash() {
        String[] candidatePaths = new String[] {
            "C:\\Program Files\\Git\\bin\\bash.exe",
            "C:\\Program Files (x86)\\Git\\bin\\bash.exe",
            System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Git\\bin\\bash.exe",
            "C:\\Windows\\System32\\bash.exe"
        };

        for (String path : candidatePaths) {
            File f = new File(path);
            if (f.exists() && f.canExecute()) {
                return path;
            }
        }

        if (isExecutableAvailable("bash.exe")) {
            return "bash.exe";
        }
        return null;
    }

    public String getWindowsBashExecutable() {
        if (windowsBashExecutable == null) {
            windowsBashExecutable = resolveWindowsBash();
        }
        return windowsBashExecutable != null ? windowsBashExecutable : "bash.exe";
    }

    private boolean isExecutableAvailable(String binaryName) {
        if (new File(binaryName).isAbsolute() && new File(binaryName).exists()) {
            return true;
        }

        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return false;

        String pathSeparator = File.pathSeparator;
        String[] paths = pathEnv.split(pathSeparator);

        for (String p : paths) {
            File f = new File(p, binaryName);
            if (f.exists() && f.canExecute()) {
                return true;
            }
        }
        return false;
    }

    public boolean isWindows() {
        return isWindows;
    }

    public boolean isMac() {
        return isMac;
    }

    public boolean isLinux() {
        return isLinux;
    }

    public List<ShellType> getAvailableShells() {
        return availableShells;
    }

    public ShellType getDefaultShell() {
        return defaultShell;
    }

    /**
     * Formats a boundary command used to reliably capture working directory and exit code
     * after each executed command without blocking the user's interactive session.
     */
    public String getBoundaryProbeCommand(ShellType shell) {
        if (shell == ShellType.POWERSHELL || shell == ShellType.PWSH) {
            return "$__ec = [int](-not $?); [Console]::Out.WriteLine([string]::Concat('[NEXUS_BOUNDARY:PWD=', (Get-Location).Path, '|EXIT=', $__ec, ']'))";
        } else if (shell == ShellType.CMD) {
            return "echo. & echo [NEXUS_BOUNDARY:PWD=%CD%^|EXIT=%ERRORLEVEL%]";
        } else {
            return "echo -e \"\\n[NEXUS_BOUNDARY:PWD=$(pwd)|EXIT:$?]\"";
        }
    }
}
