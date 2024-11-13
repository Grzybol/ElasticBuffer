package org.betterbox.elasticBuffer;

public class LogEntry {
    private final String message;
    private final String level;
    private final String pluginName;
    private final long timestamp;
    private final String transactionID;

    public LogEntry(String message, String level,String pluginName, long timestamp,String transactionID) {
        this.message = message;
        this.level = level;
        this.timestamp = timestamp;
        this.pluginName = pluginName;
        this.transactionID = (transactionID != null) ? transactionID : "N/A";
    }

    public String getMessage() {
        return message;
    }
    public String getPluginName() {
        return pluginName;
    }
    public String getTransactionID() {
        return transactionID;
    }

    public String getLevel() {
        return level;
    }
    public long getTimestamp() {
        return timestamp; // Getter dla znacznika czasu
    }

    @Override
    public String toString() {
        // Możesz dostosować formatowanie według potrzeb
        return String.format("{\"level\": \"%s\", \"message\": \"%s\"}", level, message);
    }
}
