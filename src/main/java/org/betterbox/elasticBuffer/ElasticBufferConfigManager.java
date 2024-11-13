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
            try {
                configFile.createNewFile();
                updateConfig("log_level:\n  - INFO\n  - WARNING\n  - ERROR");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
    }
    public Map<Integer, String> getRankHierarchy() {
        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "ConfigManager: getRankHierarchy called");
        return rankHierarchy;
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
}

