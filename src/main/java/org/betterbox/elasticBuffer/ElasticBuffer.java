package org.betterbox.elasticBuffer;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class ElasticBuffer extends JavaPlugin {
    private LogBuffer logBuffer;
    private final int interval = 1200; // 60 seconds * 20 TPS
    private static ElasticBuffer instance;
    private PluginLogger pluginLogger;
    ConfigManager configManager;
    @Override
    public void onEnable() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        Set<PluginLogger.LogLevel> defaultLogLevels = EnumSet.of(PluginLogger.LogLevel.INFO, PluginLogger.LogLevel.WARNING, PluginLogger.LogLevel.ERROR);
        pluginLogger = new PluginLogger(getDataFolder().getAbsolutePath(), defaultLogLevels,this);
        configManager = new ConfigManager(this, pluginLogger, getDataFolder().getAbsolutePath());
        instance = this;
        logBuffer = new LogBuffer();
        getServer().getScheduler().runTaskTimerAsynchronously(this, this::sendLogs, interval, interval);
        Bukkit.getServicesManager().register(ElasticBuffer.class, this, this, ServicePriority.Normal);
        pluginLogger.log(PluginLogger.LogLevel.INFO, "ElasticBuffer loaded, testing kibana connection");
        ElasticBufferAPI.getInstance().log("ElasticBuffer loaded, testing kibana connection","INFO","ElasticBuffer");
        sendLogs();

    }

    @Override
    public void onDisable() {

    }
    public static ElasticBuffer getInstance() {
        return instance;
    }



    public void receiveLog(String log, String level, String pluginName) {
        logBuffer.add(log, level, pluginName);
        pluginLogger.log(PluginLogger.LogLevel.DEBUG, "Received log: " + log+", pluginName: "+pluginName+". level: "+level);
        logBuffer.add(log, level, pluginName);
    }

    public void sendLogs() {
        pluginLogger.log(PluginLogger.LogLevel.DEBUG, "sendLogs() called");
        List<LogEntry> logsToSend = logBuffer.getAndClear();

        // Jeśli bufor jest pusty, nie ma nic do wysłania
        /*
        if (logsToSend.isEmpty()) {
            return;
        }

         */

        try {
            URL url = new URL("http://localhost:9200/betterbox/_bulk");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-ndjson; charset=UTF-8");
            connection.setDoOutput(true);

            // Tworzymy NDJSON (newline-delimited JSON) dla operacji bulk
            StringBuilder ndjsonBuilder = new StringBuilder();
            for (LogEntry logEntry : logsToSend) {
                // Linia metadanych dla akcji index
                ndjsonBuilder.append("{\"index\":{}}\n");
                // Dane logu
                ndjsonBuilder.append(String.format(
                        "{\"timestamp\":\"%s\",\"level\":\"%s\",\"message\":\"%s\"}\n",
                        java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()),
                        logEntry.getLevel(),
                        logEntry.getMessage()
                ));
            }

            // Wysyłanie logów
            try (OutputStream os = connection.getOutputStream()) {
                String ndjsonContent = ndjsonBuilder.toString();
                byte[] input = ndjsonBuilder.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                pluginLogger.log(PluginLogger.LogLevel.DEBUG, "Sending NDJSON: " + ndjsonContent);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = reader.readLine()) != null) {
                        pluginLogger.log(PluginLogger.LogLevel.DEBUG, "Elasticsearch response: "+responseLine);
                    }
                }
                getLogger().info("Logs successfully sent to Elasticsearch");
                pluginLogger.log(PluginLogger.LogLevel.INFO, "Logs successfully sent to Elasticsearch");
            } else {
                getLogger().warning("Failed to send logs to Elasticsearch: HTTP " + responseCode);
                pluginLogger.log(PluginLogger.LogLevel.ERROR, "Failed to send logs to Elasticsearch: HTTP " + responseCode);
            }
        } catch (Exception e) {
            getLogger().severe("Error sending logs to Elasticsearch: " + e.getMessage());
            pluginLogger.log(PluginLogger.LogLevel.ERROR, "Error sending logs to Elasticsearch: " + e.getMessage());
        }
    }

}
