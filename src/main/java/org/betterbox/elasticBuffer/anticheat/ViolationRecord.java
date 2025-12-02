package org.betterbox.elasticBuffer.anticheat;

import java.util.Map;

/**
 * Represents a single violation detected by a check.
 */
public class ViolationRecord {
    private final CheckType type;
    private final double severity;
    private final long timestamp;
    private final String reason;
    private final Map<String, Object> details;

    public ViolationRecord(CheckType type, double severity, long timestamp) {
        this(type, severity, timestamp, null, Map.of());
    }

    public ViolationRecord(CheckType type, double severity, long timestamp, String reason, Map<String, Object> details) {
        this.type = type;
        this.severity = severity;
        this.timestamp = timestamp;
        this.reason = reason;
        this.details = details == null ? Map.of() : Map.copyOf(details);
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

    public String getReason() {
        return reason;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
