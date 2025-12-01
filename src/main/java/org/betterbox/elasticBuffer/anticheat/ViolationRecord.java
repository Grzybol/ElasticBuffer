package org.betterbox.elasticBuffer.anticheat;

/**
 * Represents a single violation detected by a check.
 */
public class ViolationRecord {
    private final CheckType type;
    private final double severity;
    private final long timestamp;

    public ViolationRecord(CheckType type, double severity, long timestamp) {
        this.type = type;
        this.severity = severity;
        this.timestamp = timestamp;
    }

    public CheckType getType() {
        return type;
    }

    public double getSeverity() {
        return severity;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
