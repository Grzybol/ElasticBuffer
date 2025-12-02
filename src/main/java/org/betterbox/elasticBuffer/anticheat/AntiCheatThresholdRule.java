package org.betterbox.elasticBuffer.anticheat;

/**
 * Represents a user-defined reaction rule triggered when severity and count thresholds are met.
 */
public class AntiCheatThresholdRule {
    private final double severityMin;
    private final int countMin;
    private final long windowMillis;
    private final String command;

    public AntiCheatThresholdRule(double severityMin, int countMin, long windowMillis, String command) {
        this.severityMin = severityMin;
        this.countMin = countMin;
        this.windowMillis = windowMillis;
        this.command = command;
    }

    public double getSeverityMin() {
        return severityMin;
    }

    public int getCountMin() {
        return countMin;
    }

    public long getWindowMillis() {
        return windowMillis;
    }

    public String getCommand() {
        return command;
    }
}
