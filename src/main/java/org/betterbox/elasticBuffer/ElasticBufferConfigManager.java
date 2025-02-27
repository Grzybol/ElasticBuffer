package org.betterbox.elasticBuffer;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ElasticBufferConfigManager {
    private JavaPlugin plugin;
    private final ElasticBufferPluginLogger elasticBufferPluginLogger;
    private File configFile = null;
    List<String> logLevels = null;
    Set<ElasticBufferPluginLogger.LogLevel> enabledLogLevels;
    private Map<Integer, String> rankHierarchy;
    public int port;
    private String webhookURL,apiKey,indexPattern;
    private String truststorePath, truststorePassword,serverName="default-server";
    private boolean useSSL,local=true,authorization=false,checkCerts=false;
    // New variables for monitoring thresholds
    private double highMemoryUsageThreshold;
    private double lowTPSThreshold;
    private double highCpuUsageThreshold;
    private double highDiskUsageThreshold;
    private long monitoringIntervalTicks;


    public ElasticBufferConfigManager(JavaPlugin plugin, ElasticBufferPluginLogger elasticBufferPluginLogger, String folderPath) {

        this.plugin = plugin;
        this.elasticBufferPluginLogger = elasticBufferPluginLogger;
        this.rankHierarchy = new LinkedHashMap<>();
        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG,"ConfigManager called");
        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG,"ConfigManager: calling configureLogger");
        configureLogger();
        //CreateExampleConfigFile(folderPath);
    }
    private void CreateExampleConfigFile(String folderPath){
        File exampleConfigFile = new File(folderPath, "config.yml");
        try (InputStream in = plugin.getResource("exampleFiles/config.yml")) {
            if (in == null) {
                plugin.getLogger().severe("Resource 'exampleFiles/config.yml not found.");
                return;
            }
            Files.copy(in, exampleConfigFile.toPath());
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml to " + exampleConfigFile + ": " + e.getMessage());
        }
    }

    private void configureLogger() {
        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG,"ConfigManager: configureLogger called");
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "Config file does not exist, creating new one.");
            CreateExampleConfigFile(plugin.getDataFolder().toString());
            updateConfig("log_level:\n  - INFO\n  - WARNING\n  - ERROR");
        }
        ReloadConfig();
    }
    public void ReloadConfig(){
        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG,"ConfigManager: ReloadConfig called");
        // Odczytanie ustawień log_level z pliku konfiguracyjnego
        configFile = new File(plugin.getDataFolder(), "config.yml");
        plugin.reloadConfig();
        logLevels = plugin.getConfig().getStringList("log_level");
        enabledLogLevels = new HashSet<>();
        if (logLevels == null || logLevels.isEmpty()) {
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.ERROR,"ConfigManager: ReloadConfig: no config file or no configured log levels! Saving default settings.");
            // Jeśli konfiguracja nie określa poziomów logowania, użyj domyślnych ustawień
            enabledLogLevels = EnumSet.of(ElasticBufferPluginLogger.LogLevel.INFO, ElasticBufferPluginLogger.LogLevel.WARNING, ElasticBufferPluginLogger.LogLevel.ERROR);
            updateConfig("log_level:\n  - INFO\n  - WARNING\n  - ERROR");

        }

        for (String level : logLevels) {
            try {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG_LVL2,"ConfigManager: ReloadConfig: adding "+level.toUpperCase());
                enabledLogLevels.add(ElasticBufferPluginLogger.LogLevel.valueOf(level.toUpperCase()));
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG_LVL2,"ConfigManager: ReloadConfig: current log levels: "+ Arrays.toString(enabledLogLevels.toArray()));

            } catch (IllegalArgumentException e) {
                // Jeśli podano nieprawidłowy poziom logowania, zaloguj błąd
                plugin.getServer().getLogger().warning("Invalid log level in config: " + level);
            }
        }
        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG,"ConfigManager: ReloadConfig: calling pluginLogger.setEnabledLogLevels(enabledLogLevels) with parameters: "+ Arrays.toString(enabledLogLevels.toArray()));




        // Ustawienie aktywnych poziomów logowania w loggerze
        elasticBufferPluginLogger.setEnabledLogLevels(enabledLogLevels);
        /*
            URL url = new URL("http://localhost:9200/betterbox/_bulk");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-ndjson; charset=UTF-8");
            connection.setDoOutput(true);
         */

        port = plugin.getConfig().getInt("elasticsearch_port");
        if (plugin.getConfig().contains("elasticsearch_port")){
            if (plugin.getConfig().isInt("elasticsearch_port")){
                port = plugin.getConfig().getInt("elasticsearch_port");
            }
            else {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: elasticsearch_port incorrect! Restoring default");
                plugin.getConfig().set("elasticsearch_port", 9200);
                plugin.saveConfig();
            }
        }
        else{
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: elasticsearch_port section not found in config! Creating new section..");
            plugin.getConfig().createSection("elasticsearch_port");
            plugin.getConfig().set("elasticsearch_port", 9200);
            plugin.saveConfig();
        }

        webhookURL = plugin.getConfig().getString("webhookURL");
        if (plugin.getConfig().contains("webhookURL")){
            if (plugin.getConfig().isString("webhookURL")){
                webhookURL = plugin.getConfig().getString("webhookURL");
            }
            else {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: webhookURL incorrect! Restoring default");
                plugin.getConfig().set("webhookURL", "localhost");
                plugin.saveConfig();
            }
        }
        else{
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: webhookURL section not found in config! Creating new section..");
            plugin.getConfig().createSection("webhookURL");
            plugin.getConfig().set("webhookURL", "localhost");
            plugin.saveConfig();
        }

        apiKey = plugin.getConfig().getString("apiKey");
        if (plugin.getConfig().contains("apiKey")){
            if (plugin.getConfig().isString("apiKey")){
                apiKey = plugin.getConfig().getString("apiKey");
            }
            else {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: apiKey incorrect! Restoring default");
                plugin.getConfig().set("apiKey", "");
                plugin.saveConfig();
            }
        }
        else{
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: apiKey section not found in config! Creating new section..");
            plugin.getConfig().createSection("apiKey");
            plugin.getConfig().set("apiKey", "");
            plugin.saveConfig();
        }

        indexPattern = plugin.getConfig().getString("index_pattern");
        if (plugin.getConfig().contains("index_pattern")){
            if (plugin.getConfig().isString("index_pattern")){
                indexPattern = plugin.getConfig().getString("index_pattern");
            }
            else {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: index_pattern incorrect! Restoring default");
                plugin.getConfig().set("index_pattern", "YourServerName");
                plugin.saveConfig();
            }
        }
        else{
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: index_pattern section not found in config! Creating new section..");
            plugin.getConfig().createSection("index_pattern");
            plugin.getConfig().set("index_pattern", "YourServerName");
            plugin.saveConfig();
        }

        local = plugin.getConfig().getBoolean("local");
        if (plugin.getConfig().contains("local")){
            if (plugin.getConfig().isBoolean("local")){
                local = plugin.getConfig().getBoolean("local", true);
            }
            else {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: local incorrect! Restoring default");
                plugin.getConfig().set("local", true);
                plugin.saveConfig();
            }
        }
        else{
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: local section not found in config! Creating new section..");
            plugin.getConfig().createSection("local");
            plugin.getConfig().set("local", true);
            plugin.saveConfig();
        }



        /*
        useSSL = plugin.getConfig().getBoolean("use_ssl");
        if (plugin.getConfig().contains("use_ssl")){
            if (plugin.getConfig().isBoolean("use_ssl")){
                useSSL = plugin.getConfig().getBoolean("use_ssl", false);
            }
            else {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: use_ssl incorrect! Restoring default");
                plugin.getConfig().set("use_ssl", false);
                plugin.saveConfig();
            }
        }
        else{
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: use_ssl section not found in config! Creating new section..");
            plugin.getConfig().createSection("use_ssl");
            plugin.getConfig().set("use_ssl", false);
            plugin.saveConfig();
        }
         */

        truststorePath = plugin.getConfig().getString("truststore_path");
        if (plugin.getConfig().contains("truststore_path")){
            if (plugin.getConfig().isString("truststore_path")){
                truststorePath = plugin.getConfig().getString("truststore_path");
            }
            else {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: truststore_path incorrect! Restoring default");
                plugin.getConfig().set("truststore_path", "truststorePath");
                plugin.saveConfig();
            }
        }
        else{
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: truststore_path section not found in config! Creating new section..");
            plugin.getConfig().createSection("truststore_path");
            plugin.getConfig().set("truststore_path", "truststorePath");
            plugin.saveConfig();
        }

        truststorePassword = plugin.getConfig().getString("truststore_password");
        if (plugin.getConfig().contains("truststore_password")){
            if (plugin.getConfig().isString("truststore_password")){
                truststorePassword = plugin.getConfig().getString("truststore_password");
            }
            else {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: truststore_password incorrect! Restoring default");
                plugin.getConfig().set("truststore_password", "truststore_password");
                plugin.saveConfig();
            }
        }
        else{
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: truststore_password section not found in config! Creating new section..");
            plugin.getConfig().createSection("truststore_password");
            plugin.getConfig().set("truststore_password", "truststore_password");
            plugin.saveConfig();
        }

        authorization = plugin.getConfig().getBoolean("authorization");
        if (plugin.getConfig().contains("authorization")){
            if (plugin.getConfig().isBoolean("authorization")){
                authorization = plugin.getConfig().getBoolean("authorization", false);
            }
            else {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: authorization incorrect! Restoring default");
                plugin.getConfig().set("authorization", false);
                plugin.saveConfig();
            }
        }
        else{
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: authorization section not found in config! Creating new section..");
            plugin.getConfig().createSection("authorization");
            plugin.getConfig().set("authorization", false);
            plugin.saveConfig();
        }

        checkCerts = plugin.getConfig().getBoolean("checkCerts");
        if (plugin.getConfig().contains("checkCerts")){
            if (plugin.getConfig().isBoolean("checkCerts")){
                checkCerts = plugin.getConfig().getBoolean("checkCerts", false);
            }
            else {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: checkCerts incorrect! Restoring default");
                plugin.getConfig().set("checkCerts", false);
                plugin.saveConfig();
            }
        }
        else{
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: checkCerts section not found in config! Creating new section..");
            plugin.getConfig().createSection("checkCerts");
            plugin.getConfig().set("checkCerts", false);
            plugin.saveConfig();
        }

        serverName = plugin.getConfig().getString("serverName");
        if (plugin.getConfig().contains("serverName")){
            if (plugin.getConfig().isString("serverName")){
                serverName = plugin.getConfig().getString("serverName", "default-server-name");
            }
            else {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: serverName incorrect! Restoring default");
                plugin.getConfig().set("serverName", "default-server-name");
                plugin.saveConfig();
            }
        }
        else{
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: serverName section not found in config! Creating new section..");
            plugin.getConfig().createSection("serverName");
            plugin.getConfig().set("serverName", "default-server-name");
            plugin.saveConfig();
        }
        // High Memory Usage Threshold
        if (plugin.getConfig().contains("highMemoryUsageThreshold")) {
            if (plugin.getConfig().isDouble("highMemoryUsageThreshold")) {
                highMemoryUsageThreshold = plugin.getConfig().getDouble("highMemoryUsageThreshold");
            } else {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: highMemoryUsageThreshold incorrect! Restoring default");
                highMemoryUsageThreshold = 80.0; // Default value
                plugin.getConfig().set("highMemoryUsageThreshold", highMemoryUsageThreshold);
                plugin.saveConfig();
            }
        } else {
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: highMemoryUsageThreshold section not found in config! Creating new section...");
            highMemoryUsageThreshold = 80.0; // Default value
            plugin.getConfig().set("highMemoryUsageThreshold", highMemoryUsageThreshold);
            plugin.saveConfig();
        }

// Low TPS Threshold
        if (plugin.getConfig().contains("lowTPSThreshold")) {
            if (plugin.getConfig().isDouble("lowTPSThreshold")) {
                lowTPSThreshold = plugin.getConfig().getDouble("lowTPSThreshold");
            } else {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: lowTPSThreshold incorrect! Restoring default");
                lowTPSThreshold = 18.0; // Default value
                plugin.getConfig().set("lowTPSThreshold", lowTPSThreshold);
                plugin.saveConfig();
            }
        } else {
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: lowTPSThreshold section not found in config! Creating new section...");
            lowTPSThreshold = 18.0; // Default value
            plugin.getConfig().set("lowTPSThreshold", lowTPSThreshold);
            plugin.saveConfig();
        }

// High CPU Usage Threshold
        if (plugin.getConfig().contains("highCpuUsageThreshold")) {
            if (plugin.getConfig().isDouble("highCpuUsageThreshold")) {
                highCpuUsageThreshold = plugin.getConfig().getDouble("highCpuUsageThreshold");
            } else {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: highCpuUsageThreshold incorrect! Restoring default");
                highCpuUsageThreshold = 85.0; // Default value
                plugin.getConfig().set("highCpuUsageThreshold", highCpuUsageThreshold);
                plugin.saveConfig();
            }
        } else {
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: highCpuUsageThreshold section not found in config! Creating new section...");
            highCpuUsageThreshold = 85.0; // Default value
            plugin.getConfig().set("highCpuUsageThreshold", highCpuUsageThreshold);
            plugin.saveConfig();
        }

// High Disk Usage Threshold
        if (plugin.getConfig().contains("highDiskUsageThreshold")) {
            if (plugin.getConfig().isDouble("highDiskUsageThreshold")) {
                highDiskUsageThreshold = plugin.getConfig().getDouble("highDiskUsageThreshold");
            } else {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: highDiskUsageThreshold incorrect! Restoring default");
                highDiskUsageThreshold = 90.0; // Default value
                plugin.getConfig().set("highDiskUsageThreshold", highDiskUsageThreshold);
                plugin.saveConfig();
            }
        } else {
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: highDiskUsageThreshold section not found in config! Creating new section...");
            highDiskUsageThreshold = 90.0; // Default value
            plugin.getConfig().set("highDiskUsageThreshold", highDiskUsageThreshold);
            plugin.saveConfig();
        }

// Monitoring Interval Ticks
        if (plugin.getConfig().contains("monitoringIntervalTicks")) {
            if (plugin.getConfig().isInt("monitoringIntervalTicks") || plugin.getConfig().isLong("monitoringIntervalTicks")) {
                monitoringIntervalTicks = plugin.getConfig().getLong("monitoringIntervalTicks");
            } else {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: monitoringIntervalTicks incorrect! Restoring default");
                monitoringIntervalTicks = 1200L; // Default value (60 seconds)
                plugin.getConfig().set("monitoringIntervalTicks", monitoringIntervalTicks);
                plugin.saveConfig();
            }
        } else {
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.WARNING, "ConfigManager: ReloadConfig: monitoringIntervalTicks section not found in config! Creating new section...");
            monitoringIntervalTicks = 1200L; // Default value
            plugin.getConfig().set("monitoringIntervalTicks", monitoringIntervalTicks);
            plugin.saveConfig();
        }



        truststorePassword = plugin.getConfig().getString("truststore_password");
        // Logowanie konfiguracji dla debugowania
        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "ConfigManager.ReloadConfig: useSSL: " + useSSL+", port: "+port+", authorization: "+authorization+", webhook: "+webhookURL+", local: "+local+", apiKey: "+apiKey+", index_pattern: "+indexPattern+", checkCerts: "+checkCerts);
        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "ConfigManager.ReloadConfig: truststorePath: "+truststorePath+", truststore_password: "+truststorePassword);

    }
    public void updateConfig(String configuration) {
        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "ConfigManager: updateConfig called with parameters "+ configuration);
        try {
            List<String> lines = Files.readAllLines(Paths.get(configFile.toURI()));

            // Dodaj nowe zmienne konfiguracyjne
            lines.add("###################################");
            lines.add(configuration);
            // Tutaj możemy dodać nowe zmienne konfiguracyjne
            // ...

            Files.write(Paths.get(configFile.toURI()), lines);
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.INFO, "Config file updated successfully.");
        } catch (IOException e) {
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.ERROR, "Error while updating config file: " + e.getMessage());
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public int getPort() {
        return port;
    }
    public boolean getAuthorization(){return authorization;}

    public String getWebhookURL() {
        return webhookURL;
    }

    public String getIndexPattern() {
        return indexPattern;
    }
    public boolean isUseSSL() {
        return useSSL;
    }

    public String getTruststorePath() {
        return truststorePath;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }
    public boolean isLocal(){
        return local;
    }

    public boolean getCheckCerts(){return checkCerts;}
    public String getServerName() {
        return serverName;
    }
    public double getHighMemoryUsageThreshold() {
        return highMemoryUsageThreshold;
    }

    public double getLowTPSThreshold() {
        return lowTPSThreshold;
    }

    public double getHighCpuUsageThreshold() {
        return highCpuUsageThreshold;
    }

    public double getHighDiskUsageThreshold() {
        return highDiskUsageThreshold;
    }

    public long getMonitoringIntervalTicks() {
        return monitoringIntervalTicks;
    }

}

