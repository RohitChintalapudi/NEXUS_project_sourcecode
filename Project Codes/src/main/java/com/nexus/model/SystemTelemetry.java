package com.nexus.model;

/**
 * System telemetry model for the persistent bottom analytics strip.
 * Note: Per Stage 1 architecture requirements, all metrics are explicitly labeled
 * as simulated/placeholder telemetry.
 */
public class SystemTelemetry {
    private final double cpuUsagePercent;
    private final double ramUsedGb;
    private final double ramTotalGb;
    private final double diskUsedGb;
    private final double diskTotalGb;
    private final double networkUpKbps;
    private final double networkDownKbps;
    private final String uptimeFormatted;
    private final boolean simulated;

    public SystemTelemetry(double cpuUsagePercent, double ramUsedGb, double ramTotalGb,
                           double diskUsedGb, double diskTotalGb, double networkUpKbps,
                           double networkDownKbps, String uptimeFormatted, boolean simulated) {
        this.cpuUsagePercent = cpuUsagePercent;
        this.ramUsedGb = ramUsedGb;
        this.ramTotalGb = ramTotalGb;
        this.diskUsedGb = diskUsedGb;
        this.diskTotalGb = diskTotalGb;
        this.networkUpKbps = networkUpKbps;
        this.networkDownKbps = networkDownKbps;
        this.uptimeFormatted = uptimeFormatted;
        this.simulated = simulated;
    }

    public static SystemTelemetry createDefaultSimulated() {
        return new SystemTelemetry(
                24.5,
                4.8, 16.0,
                142.0, 512.0,
                340.5, 1250.8,
                "03h 48m 12s",
                true
        );
    }

    public double getCpuUsagePercent() {
        return cpuUsagePercent;
    }

    public double getCpuPercent() {
        return cpuUsagePercent;
    }

    public double getRamUsedGb() {
        return ramUsedGb;
    }

    public double getRamTotalGb() {
        return ramTotalGb;
    }

    public double getRamUsagePercent() {
        return (ramUsedGb / ramTotalGb) * 100.0;
    }

    public double getDiskUsedGb() {
        return diskUsedGb;
    }

    public double getDiskTotalGb() {
        return diskTotalGb;
    }

    public double getDiskUsagePercent() {
        return (diskUsedGb / diskTotalGb) * 100.0;
    }

    public double getNetworkUpKbps() {
        return networkUpKbps;
    }

    public double getNetworkDownKbps() {
        return networkDownKbps;
    }

    public String getUptimeFormatted() {
        return uptimeFormatted;
    }

    public boolean isSimulated() {
        return simulated;
    }
}

