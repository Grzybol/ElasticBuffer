package org.betterbox.elasticBuffer;

import java.util.UUID;

public class LogEntry {
    private final String message;
    private final String level;
    private final String pluginName;
    private final long timestamp;
    private final String transactionID;
    private final String playerName;
    private final String uuid;
    private final double keyValue;
    public LogEntry(String message, String level,String pluginName, long timestamp,String transactionID,String playerName, String uuid, double keyValue) {
        this.message = message;
        this.level = level;
        this.timestamp = timestamp;
        this.pluginName = pluginName;
        this.transactionID = (transactionID != null) ? transactionID : "N/A";
        this.playerName = (playerName != null) ? playerName : "N/A";
        this.uuid = (uuid != null) ? uuid : "N/A";
        this.keyValue = keyValue;
    }
    public String getMessage() {
        return message;
    }
    public String getPlayerName(){return playerName;}
    public String getUuid(){return  uuid;}
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
    public double getKeyValue() {
        return keyValue;
    }

    @Override
    public String toString() {
        // Możesz dostosować formatowanie według potrzeb
        return String.format("{\"level\": \"%s\", \"message\": \"%s\"}", level, message);
    }
}
