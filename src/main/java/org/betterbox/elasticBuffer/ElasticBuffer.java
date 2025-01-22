package org.betterbox.elasticBuffer;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class ElasticBuffer extends JavaPlugin {
    private LogBuffer logBuffer;
    private final int interval = 1200; // 60 seconds * 20 TPS
    //private ElasticBuffer bufferPlugin;
    public ElasticBufferAPI api;
    private ElasticBufferPluginLogger elasticBufferPluginLogger;
    ElasticBufferConfigManager elasticBufferConfigManager;
    private CustomLogHandler customLogHandler;
    private ServerEventLogger serverEventLogger;
    private CustomConsoleInjector consoleInjector;

    @Override
    public void onEnable() {
        int pluginId = 23919; // Zamień na rzeczywisty ID twojego pluginu na bStats
        Metrics metrics = new Metrics(this, pluginId);
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
        ElasticBuffer bufferPlugin = Bukkit.getServicesManager().load(ElasticBuffer.class);
        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.INFO, "bufferPlugin3: "+bufferPlugin);
        assert bufferPlugin != null;
        bufferPlugin.receiveLog("ElasticBuffer initialized successfully!", "INFO", "ElasticBuffer",null);
        bufferPlugin.sendLogs();
        }catch (Exception e){
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.ERROR, "bufferPlugin3: null, exception "+e.getMessage());
        }
        PluginCommand ebCommand = getCommand("eb");
        if (ebCommand != null) {
            ebCommand.setExecutor(new CommandManager(this, this, elasticBufferConfigManager));
        } else {
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.ERROR, "Command 'eb' not found. Check your plugin.yml");
        }
        Logger logger = Bukkit.getLogger();
        customLogHandler = new CustomLogHandler(this);
        logger.addHandler(customLogHandler);
        getServer().getPluginManager().registerEvents(new EventLogger(api,this), this);
        // Rejestracja listenera zdarzeń serwerowych
        serverEventLogger = new ServerEventLogger(api, this,elasticBufferConfigManager);
        getServer().getPluginManager().registerEvents(new ServerEventLogger(api, this,elasticBufferConfigManager), this);

        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.ERROR, "Starting consoleInjector");
        consoleInjector = new CustomConsoleInjector(this, api);
        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.ERROR, "consoleInjector started");


    }


    @Override
    public void onDisable() {
        serverEventLogger.restoreSystemOutAndErr();
        Logger logger = Bukkit.getLogger();
        if (customLogHandler != null) {
            logger.removeHandler(customLogHandler);
        }


    }
    public static ElasticBuffer getInstance() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ElasticBuffer");
        if (plugin instanceof ElasticBuffer) {
            return (ElasticBuffer) plugin;
        }
        return null; // Zwróć null, jeśli instancja nie jest dostępna
    }




    public synchronized void receiveLog(String log, String level, String pluginName,String transactionID,String playerName, String uuid) {
        long logTimestamp = System.currentTimeMillis();
        logBuffer.add(log, level, pluginName,logTimestamp,transactionID,playerName,uuid);
        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "Received log: " + log+", pluginName: "+pluginName+". level: "+level);
    }
    public synchronized void receiveLog(String log, String level, String pluginName,String transactionID) {
        long logTimestamp = System.currentTimeMillis();
        logBuffer.add(log, level, pluginName,logTimestamp,transactionID,null,null);
        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "Received log: " + log+", pluginName: "+pluginName+". level: "+level);
    }

    public void sendLogs() {
        if(!elasticBufferConfigManager.getCheckCerts()) {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }

                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }

                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }
                        }
                };

                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (NoSuchAlgorithmException e) {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.ERROR, "NoSuchAlgorithmException " + e.getMessage());
            } catch (KeyManagementException e) {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.ERROR, "NoSuchAlgorithmException " + e.getMessage());
            }
        }else{
            try {
                // Ścieżka do truststore i hasło
                String trustStorePath = elasticBufferConfigManager.getTruststorePath();
                String trustStorePassword = elasticBufferConfigManager.getTruststorePassword();

                // Załaduj truststore z pliku
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                try (InputStream trustStoreIS = new FileInputStream(trustStorePath)) {
                    trustStore.load(trustStoreIS, trustStorePassword.toCharArray());
                }

                // Utwórz TrustManagerFactory z załadowanym truststore
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);

                // Zainicjalizuj SSLContext z TrustManagerFactory
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

                // Ustaw SSLSocketFactory na HttpsURLConnection
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            } catch (Exception e) {
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.ERROR, "Error setting up SSL context: " + e.getMessage());
                return;
            }
        }

// Wyłączanie weryfikacji nazwy hosta
        HostnameVerifier allHostsValid = (hostname, session) -> true;
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "sendLogs() called");
        String urlString;
        String webhookUrl= elasticBufferConfigManager.getWebhookURL();;
        int port= elasticBufferConfigManager.getPort();
        String indexPattern= elasticBufferConfigManager.getIndexPattern().toLowerCase();
        String apiKey = elasticBufferConfigManager.getApiKey();
        if(elasticBufferConfigManager.isLocal()){
            urlString = "http://localhost:"+port+"/"+indexPattern+"/_bulk";
            if(elasticBufferConfigManager.getAuthorization()){
                urlString = "https://localhost:"+port+"/"+indexPattern+"/_bulk";
            }

        }else{
            urlString = webhookUrl + ":" + port + "/" + indexPattern + "/_bulk";
            if (elasticBufferConfigManager.isUseSSL()) {
                try {
                    System.setProperty("javax.net.ssl.trustStore", elasticBufferConfigManager.getTruststorePath());
                    System.setProperty("javax.net.ssl.trustStorePassword", elasticBufferConfigManager.getTruststorePassword());
                    elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.INFO, "SSL properties set for Elasticsearch connection.");
                } catch (Exception e) {
                    elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.ERROR, "Failed to set SSL properties: " + e.getMessage());
                    return;
                }
            }
        }
        elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.INFO, "isLocal() "+elasticBufferConfigManager.isLocal()+", urlString: "+urlString);
        //api.log("sendLogs() called", "DEBUG", "ElasticBuffer",null);
        List<LogEntry> logsToSend = logBuffer.getAndClear();

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-ndjson; charset=UTF-8");
            if(elasticBufferConfigManager.getAuthorization()){
                connection.setRequestProperty("Authorization", "ApiKey " + apiKey);
                // Dodaj logowanie klucza API tylko jeśli jest to bezpieczne!
                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "Authorization set: ApiKey "+apiKey);
            }
            connection.setDoOutput(true);
            // Logowanie dodatkowych ustawień połączenia
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "Do Output: " + connection.getDoOutput());
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "Connect Timeout: " + connection.getConnectTimeout());
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "Read Timeout: " + connection.getReadTimeout());
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "Using Proxy: " + (connection.usingProxy() ? "Yes" : "No"));

            //DO WYJEBY MOZE
            try (OutputStream os = connection.getOutputStream()) {
                for (LogEntry logEntry : logsToSend) {
                    List<String> messageChunks = splitMessage(logEntry.getMessage());
                    for (String chunk : messageChunks) {
                        // Podział dużych wiadomości
                        if (chunk.length() > 30 * 1024) {
                            int chunkSize = 30 * 1024;
                            for (int i = 0; i < chunk.length(); i += chunkSize) {
                                String subChunk = chunk.substring(i, Math.min(chunk.length(), i + chunkSize));
                                String ndjsonChunk = buildNdjsonChunk(logEntry, subChunk);
                                byte[] input = ndjsonChunk.getBytes(StandardCharsets.UTF_8);
                                os.write(input, 0, input.length);
                                elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "Sending NDJSON chunk: " + ndjsonChunk);
                            }
                        } else {
                            String ndjson = buildNdjsonChunk(logEntry, chunk);
                            byte[] input = ndjson.getBytes(StandardCharsets.UTF_8);
                            os.write(input, 0, input.length);
                            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "Sending NDJSON: " + ndjson);
                        }
                    }
                }
            }

            int responseCode = connection.getResponseCode();
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "Elasticsearch responseCode: " + responseCode + ", getResponseMessage: " + connection.getResponseMessage());
        } catch (Exception e) {
            getLogger().severe("Error sending logs to Elasticsearch: " + e.getMessage());
            elasticBufferPluginLogger.log(ElasticBufferPluginLogger.LogLevel.ERROR, "Error sending logs to Elasticsearch: " + e.getMessage());
        }
    }
    private List<String> splitMessage(String message) {
        return Arrays.asList(message.split("\n"));
    }

    private String buildNdjsonChunk(LogEntry logEntry, String messageChunk) {
        String sanitizedMessage = sanitizeMessage(messageChunk);
        StringBuilder ndjsonBuilder = new StringBuilder();
        ndjsonBuilder.append("{\"index\":{}}\n");
        ndjsonBuilder.append(String.format(
                "{\"timestamp\":\"%s\",\"plugin\":\"%s\",\"transactionID\":\"%s\",\"level\":\"%s\",\"message\":\"%s\",\"playerName\":\"%s\",\"uuid\":\"%s\",\"serverName\":\"%s\"}\n",
                java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.ofEpochMilli(logEntry.getTimestamp())),
                logEntry.getPluginName(),
                logEntry.getTransactionID(),
                logEntry.getLevel(),
                sanitizedMessage,
                logEntry.getPlayerName(),
                logEntry.getUuid(),
                elasticBufferConfigManager.getServerName()
        ));
        return ndjsonBuilder.toString();
    }
    private String sanitizeMessage(String message) {
        // Usuwanie znaku przejścia do nowej linii i cudzysłowu
        return message.replace("\"", "");
    }
}
