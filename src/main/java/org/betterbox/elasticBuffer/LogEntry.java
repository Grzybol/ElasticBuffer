package org.betterbox.elasticBuffer;

import java.util.UUID;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LogEntry {
    private final String message;
    private final String level;
    private final String pluginName;
    private final long timestamp;
    private final String transactionID;
    private final String playerName;
    private final String uuid;
    private final double keyValue;
    private final Map<String, Object> additionalFields;
    public LogEntry(String message, String level,String pluginName, long timestamp,String transactionID,String playerName, String uuid, double keyValue, Map<String, Object> additionalFields) {
        this.message = message;
        this.level = level;
        this.timestamp = timestamp;
        this.pluginName = pluginName;
        this.transactionID = (transactionID != null) ? transactionID : "N/A";
        this.playerName = (playerName != null) ? playerName : "N/A";
        this.uuid = (uuid != null) ? uuid : "N/A";
        this.keyValue = keyValue;
        this.additionalFields = additionalFields != null ? Collections.unmodifiableMap(new HashMap<>(additionalFields)) : Collections.emptyMap();
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
    public Map<String, Object> getAdditionalFields() { return additionalFields; }

    @Override
    public String toString() {
        // Możesz dostosować formatowanie według potrzeb
        return String.format("{\"level\": \"%s\", \"message\": \"%s\"}", level, message);
    }
}
