package org.betterbox.elasticBuffer;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
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
    //private ElasticBuffer bufferPlugin;
    public ElasticBufferAPI api;
    private ElasticBufferPluginLogger elasticBufferPluginLogger;
    ElasticBufferConfigManager elasticBufferConfigManager;
    @Override
    public void onEnable() {
        Bukkit.getServicesManager().register(ElasticBuffer.class, this, this, ServicePriority.Normal);
        api = new ElasticBufferAPI(this);
        Bukkit.getServicesManager().register(ElasticBufferAPI.class, api, this, ServicePriority.Normal);

        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        Set<ElasticBufferPluginLogger.LogLevel> defaultLogLevels = EnumSet.of(ElasticBufferPluginLogger.LogLevel.INFO, ElasticBufferPluginLogger.LogLevel.WARNING, ElasticBufferPluginLogger.LogLevel.ERROR);
        elasticBufferPluginLogger = new ElasticBufferPluginLogger(getDataFolder().getAbsolutePath(), defaultLogLevels,this);
        elasticBufferConfigManager = new ElasticBufferConfigManager(this, elasticBufferPluginLogger, getDataFolder().getAbsolutePath());
        logBuffer = new LogBuffer();
        getServer().getScheduler().runTaskTimerAsynchronously(this, this::sendLogs, interval, interval);
        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.INFO, "ElasticBufferAPI registered with ServicesManager");
        try{
        ElasticBuffer bufferPlugin3 = Bukkit.getServicesManager().load(ElasticBuffer.class);
        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.INFO, "bufferPlugin3: "+bufferPlugin3);
        assert bufferPlugin3 != null;
        bufferPlugin3.receiveLog("ElasticBuffer initialized successfully!", "INFO", "ElasticBuffer");
        bufferPlugin3.sendLogs();
        }catch (Exception e){
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.ERROR, "bufferPlugin3: null, exception "+e.getMessage());
        }


        /*
        // Uzyskaj instancję ElasticBuffer z PluginManager
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ElasticBuffer");
        if (plugin instanceof ElasticBuffer) {
            bufferPlugin = (ElasticBuffer) plugin;
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.INFO, "ElasticBuffer instance found via PluginManager");
            api.log("bufferPlugin test log", "INFO", bufferPlugin.getDescription().getName());
        } else {
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.ERROR, "ElasticBuffer instance is null via PluginManager");
        }

        sendLogs();

         */

    }

    @Override
    public void onDisable() {

    }
    public static ElasticBuffer getInstance() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ElasticBuffer");
        if (plugin instanceof ElasticBuffer) {
            return (ElasticBuffer) plugin;
        }
        return null; // Zwróć null, jeśli instancja nie jest dostępna
    }




    public void receiveLog(String log, String level, String pluginName) {
        logBuffer.add(log, level, pluginName);
        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "Received log: " + log+", pluginName: "+pluginName+". level: "+level);
        logBuffer.add(log, level, pluginName);
    }

    public void sendLogs() {
        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "sendLogs() called");
        api.log("sendLogs() called", "DEBUG", "ElasticBuffer");
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
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "Sending NDJSON: " + ndjsonContent);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = reader.readLine()) != null) {
                        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "Elasticsearch response: "+responseLine);
                    }
                }
                getLogger().info("Logs successfully sent to Elasticsearch");
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.INFO, "Logs successfully sent to Elasticsearch");
            } else {
                getLogger().warning("Failed to send logs to Elasticsearch: HTTP " + responseCode);
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.ERROR, "Failed to send logs to Elasticsearch: HTTP " + responseCode);
            }
        } catch (Exception e) {
            getLogger().severe("Error sending logs to Elasticsearch: " + e.getMessage());
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.ERROR, "Error sending logs to Elasticsearch: " + e.getMessage());
        }
    }

}
