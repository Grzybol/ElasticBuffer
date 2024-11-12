package org.betterbox.elasticBuffer;

public class LogEntry {
    private final String message;
    private final String level;
    private final String pluginName;

    public LogEntry(String message, String level,String pluginName) {
        this.message = message;
        this.level = level;
        this.pluginName = pluginName;
    }

    public String getMessage() {
        return message;
    }

    public String getLevel() {
        return level;
    }

    @Override
    public String toString() {
        // Możesz dostosować formatowanie według potrzeb
        return String.format("{\"level\": \"%s\", \"message\": \"%s\"}", level, message);
    }
}
