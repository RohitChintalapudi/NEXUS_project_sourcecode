package com.nexus.model;

import com.nexus.service.ShellDetector;

public enum ShellType {
    POWERSHELL("PowerShell", "powershell.exe", "-NoLogo", "-NoExit", "-ExecutionPolicy", "Bypass"),
    PWSH("PowerShell 7", "pwsh.exe", "-NoLogo", "-NoExit", "-ExecutionPolicy", "Bypass"),
    CMD("Command Prompt", "cmd.exe", "/K"),
    BASH("Bash", "/bin/bash", "-i"),
    ZSH("Zsh", "/bin/zsh", "-i"),
    SH("Sh", "/bin/sh", "-i");

    private final String displayName;
    private final String executable;
    private final String[] defaultArgs;

    ShellType(String displayName, String executable, String... defaultArgs) {
        this.displayName = displayName;
        this.executable = executable;
        this.defaultArgs = defaultArgs;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getExecutable() {
        if (this == BASH && System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return ShellDetector.getInstance().getWindowsBashExecutable();
        }
        return executable;
    }

    public String[] getDefaultArgs() {
        return defaultArgs;
    }
}

