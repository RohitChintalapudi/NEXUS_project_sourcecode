package com.nexus.service;

import com.nexus.model.SystemTelemetry;
import com.nexus.model.ToolExecutionResult;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.util.*;
import java.util.stream.Collectors;

public class OperatingSystemService {

    private static final OperatingSystemService INSTANCE = new OperatingSystemService();

    // Environment variable substrings that should NEVER be leaked
    private static final List<String> SENSITIVE_ENV_KEYS = List.of(
            "KEY", "SECRET", "TOKEN", "PASSWORD", "PASS", "AUTH", "CREDENTIAL",
            "PRIVATE", "SIG", "CERT", "COOKIE"
    );

    private final SystemInfo systemInfo;

    private OperatingSystemService() {
        this.systemInfo = new SystemInfo();
    }

    public static OperatingSystemService getInstance() {
        return INSTANCE;
    }

    public ToolExecutionResult getOsInfo() {
        OperatingSystem os = systemInfo.getOperatingSystem();
        Map<String, String> info = new LinkedHashMap<>();
        info.put("Operating System", os.getFamily() + " (" + os.getVersionInfo().getVersion() + ")");
        info.put("Architecture", System.getProperty("os.arch"));
        info.put("Java Version", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        info.put("User Home", System.getProperty("user.home"));
        info.put("Current Working Directory", System.getProperty("user.dir"));

        String output = info.entrySet().stream()
                .map(e -> String.format("%-25s: %s", e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n"));

        return ToolExecutionResult.success(output);
    }

    public ToolExecutionResult getSystemMetrics() {
        SystemTelemetry telem = SystemMetricsService.getInstance().getCurrentMetrics();
        if (telem == null) {
            return ToolExecutionResult.failure("Telemetry service metrics not currently ready.");
        }

        String output = String.format("""
                === NEXUS SYSTEM TELEMETRY ===
                CPU Utilization    : %.1f%%
                RAM In-Use         : %.2f GB / %.2f GB (%.1f%%)
                Disk Primary In-Use: %.1f GB / %.1f GB (%.1f%%)
                Network Activity   : %.1f KB/s
                System Uptime      : %s
                """,
                telem.getCpuUsagePercent(),
                telem.getRamUsedGb(), telem.getRamTotalGb(), (telem.getRamUsedGb() / Math.max(1.0, telem.getRamTotalGb()) * 100),
                telem.getDiskUsedGb(), telem.getDiskTotalGb(), (telem.getDiskUsedGb() / Math.max(1.0, telem.getDiskTotalGb()) * 100),
                telem.getNetworkDownKbps(),
                telem.getUptimeFormatted()
        );

        return ToolExecutionResult.success(output.trim());
    }

    public ToolExecutionResult listRunningProcesses(int limit) {
        int max = Math.min(Math.max(limit, 5), 50);
        OperatingSystem os = systemInfo.getOperatingSystem();
        List<OSProcess> processes = os.getProcesses(null, OperatingSystem.ProcessSorting.CPU_DESC, max);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-8s %-30s %-10s %-12s\n", "PID", "NAME", "CPU %", "RAM (MB)"));
        sb.append("-----------------------------------------------------------------\n");

        for (OSProcess p : processes) {
            double cpu = p.getProcessCpuLoadCumulative() * 100.0;
            long ramMb = p.getResidentSetSize() / (1024 * 1024);
            sb.append(String.format("%-8d %-30s %-10.1f %-12d\n",
                    p.getProcessID(),
                    p.getName(),
                    cpu,
                    ramMb));
        }

        return ToolExecutionResult.success(sb.toString().trim());
    }

    public ToolExecutionResult getSanitizedEnvironmentVariables() {
        Map<String, String> env = System.getenv();
        Map<String, String> safe = new TreeMap<>();

        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = entry.getKey();
            String upperKey = key.toUpperCase();

            boolean sensitive = SENSITIVE_ENV_KEYS.stream().anyMatch(upperKey::contains);
            if (!sensitive) {
                safe.put(key, entry.getValue());
            } else {
                safe.put(key, "[MASKED_BY_SECURITY_POLICY]");
            }
        }

        String output = safe.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        return ToolExecutionResult.success(output);
    }
}
