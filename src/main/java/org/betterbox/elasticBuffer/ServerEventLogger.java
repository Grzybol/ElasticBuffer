package org.betterbox.elasticBuffer;

import org.apache.logging.log4j.core.Appender;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Enumeration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
public class ServerEventLogger implements Listener {
    private final ElasticBufferAPI api;
    private final Plugin plugin;
    private final ElasticBufferConfigManager configManager;
    //private final Logger logger = Logger.getLogger("Minecraft");
    private PrintStream originalOut;
    private PrintStream originalErr;
    private final AtomicInteger recentRateLimit = new AtomicInteger(0);
    private final AtomicInteger totalBackoffEvents = new AtomicInteger(0);
    private final AtomicLong lastRateLimitTime = new AtomicLong(0);
    private final ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue<>();


    public ServerEventLogger(ElasticBufferAPI api, Plugin plugin, ElasticBufferConfigManager configManager) {
        this.api = api;
        this.plugin = plugin;
        this.configManager = configManager;
        startMonitoring();
        //startConsoleMonitoring();
    }


    public void startConsoleMonitoring() {
        Logger rootLogger = (Logger) LogManager.getRootLogger();

        Appender appender = new AbstractAppender("ConsoleAppender", null, null, true, null) {
            @Override
            public void append(LogEvent event) {
                String message = event.getMessage().getFormattedMessage();
                String level = event.getLevel().toString();
                String loggerName = event.getLoggerName();
                String sourceClassName = event.getSource().getClassName();
                String sourceMethodName = event.getSource().getMethodName();

                // Jeśli jest wyjątek, pobierz stacktrace
                final String throwableString;
                if (event.getThrown() != null) {
                    StringWriter sw = new StringWriter();
                    event.getThrown().printStackTrace(new PrintWriter(sw));
                    throwableString = sw.toString();
                } else {
                    throwableString = null;
                }

                // Dodaj wiadomość do kolejki
                messageQueue.add(message + " " + (throwableString != null ? throwableString : ""));
            }
        };

        // Dodajemy appender do loggera root
        ((org.apache.logging.log4j.core.Logger) rootLogger).addAppender(appender);

        // Zadanie asynchroniczne do przetwarzania kolejki wiadomości
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {

            final StringBuilder buffer = new StringBuilder();
            String curLine;
            while ((curLine = messageQueue.peek()) != null) {
                if (buffer.length() + curLine.length() > 2000 - 2) { // Ustal limit długości wiadomości
                    api.log(buffer.toString(), "INFO", "Console", "", "", "Console");
                    buffer.setLength(0);
                    continue;
                }
                buffer.append("\n").append(messageQueue.poll());
            }
            if (buffer.length() != 0) {
                api.log(buffer.toString(), "INFO", "Console", "", "", "Console");
            }
        }, 20, 20 * 5);
    }






    // Server start event
    @EventHandler
    public void onServerStart(ServerLoadEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log("Server started.", "INFO", "ServerEventLogger", null, "N/A", "N/A");
        });
    }

    // Console command event
    @EventHandler
    public void onConsoleCommand(ServerCommandEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log("Console command executed: " + event.getCommand(), "INFO", "ServerEventLogger", null, "Console", "N/A");
        });
    }

    // RCON command event
    @EventHandler
    public void onRconCommand(RemoteServerCommandEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log("RCON command executed: " + event.getCommand(), "INFO", "ServerEventLogger", null, "RCON", "N/A");
        });
    }
    // Player command preprocess event
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log("Player command executed: " + event.getMessage(), "INFO", "ServerEventLogger", event.getPlayer().getName(), "Player", "N/A");
        });
    }

    /*
    // Monitoring method for console logs
    private void startConsoleMonitoringOld() {
        // logManager = LogManager.getLogManager();
        //Logger rootLogger = logManager.getLogger("");
        //((Logger) LogManager.getRootLogger()).addAppender(this);
        //Logger rootLogger = (Logger) LogManager.getRootLogger();
        //Logger rootLogger = (Logger) LogManager.getLogManager().getLogger(Logger.GLOBAL_LOGGER_NAME);
        // Utwórz niestandardowy handler
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (!isLoggable(record)) {
                    return;
                }

                // Podstawowe pola
                String message = record.getMessage();
                String level = record.getLevel().getName();
                String loggerName = record.getLoggerName();
                String sourceClassName = record.getSourceClassName();
                String sourceMethodName = record.getSourceMethodName();

                // Jeśli jest wyjątek, pobierz stacktrace
                final String throwableString;
                if (record.getThrown() != null) {
                    StringWriter sw = new StringWriter();
                    record.getThrown().printStackTrace(new PrintWriter(sw));
                    throwableString = sw.toString();
                } else {
                    throwableString = null;
                }

                // Wyślij asynchronicznie do API
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        api.log(message+" "+ throwableString, level, loggerName, sourceClassName, sourceMethodName, "Console");
                    }
                }.runTaskAsynchronously(plugin);
            }

            @Override
            public void flush() { }

            @Override
            public void close() throws SecurityException { }
        };

        handler.setLevel(Level.ALL);
        rootLogger.addHandler(handler);

        // Filtr, który zawsze przepuszcza
        rootLogger.setFilter(new Filter() {
            @Override
            public boolean isLoggable(LogRecord record) {
                return true;
            }
        });

        // Dodaj handler do wszystkich loggerów w JUL
        attachHandlersToAllLoggers(handler);

        // Hook na System.out i System.err
        hookSystemOutAndErr();
    }

    private void attachHandlersToAllLoggers(Handler handler) {
        LogManager manager = LogManager.getLogManager();
        Enumeration<String> loggerNames = manager.getLoggerNames();

        while (loggerNames.hasMoreElements()) {
            String name = loggerNames.nextElement();
            Logger logger = manager.getLogger(name);

            if (logger != null) {
                boolean alreadyAttached = false;
                for (Handler h : logger.getHandlers()) {
                    if (h == handler) {
                        alreadyAttached = true;
                        break;
                    }
                }

                if (!alreadyAttached) {
                    logger.addHandler(handler);
                }
            }
        }
    }

     */


    class CustomHandler extends Handler {
        @Override
        public void publish(LogRecord record) {
            // Przetwarzanie logów (podobnie jak w startConsoleMonitoring)
        }

        @Override
        public void flush() { }

        @Override
        public void close() throws SecurityException { }
    }

    public void hookSystemOutAndErr() {
        originalOut = System.out;
        originalErr = System.err;

        System.setOut(new PrintStream(new OutputStream() {
            private StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) throws IOException {
                if (b == '\n') {
                    String line = buffer.toString();
                    buffer.setLength(0);
                    logLine(line, "INFO", "System.out");
                } else {
                    buffer.append((char) b);
                }
            }
        }));

        System.setErr(new PrintStream(new OutputStream() {
            private StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) throws IOException {
                if (b == '\n') {
                    String line = buffer.toString();
                    buffer.setLength(0);
                    logLine(line, "ERROR", "System.err");
                } else {
                    buffer.append((char) b);
                }
            }
        }));
    }

    private void logLine(String line, String level, String source) {
        new BukkitRunnable() {
            @Override
            public void run() {
                api.log(line, level, source, null, null, "Console");
            }
        }.runTaskAsynchronously(plugin);

        // Wyświetlenie w konsoli
        if ("System.out".equals(source)) {
            originalOut.println(line);
        } else {
            originalErr.println(line);
        }
    }

    public void restoreSystemOutAndErr() {
        if (originalOut != null) {
            System.setOut(originalOut);
        }
        if (originalErr != null) {
            System.setErr(originalErr);
        }
    }

    // Command block command event
    @EventHandler
    public void onCommandBlockCommand(ServerCommandEvent event) {
        if (event.getSender() instanceof BlockCommandSender) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log("Command Block executed command: " + event.getCommand(), "INFO", "ServerEventLogger", null, "CommandBlock", "N/A");
            });
        }
    }

    // Portal creation event
    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log("Portal created of type: " + event.getReason(), "INFO", "ServerEventLogger", null, "N/A", "N/A");
        });
    }

    // Monitoring method for RAM, TPS, CPU, and Disk Space
    private void startMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // RAM Usage
                long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
                long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
                long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024);
                long usedMemory = totalMemory - freeMemory;
                double usedMemoryPercentage = ((double) usedMemory / maxMemory) * 100;

                if (usedMemoryPercentage > configManager.getHighMemoryUsageThreshold()) {
                    // High memory usage alert
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        api.log("High Memory Usage Alert - Used: " + String.format("%.2f", usedMemoryPercentage) + "% (" + usedMemory + " MB of " + maxMemory + " MB)", "WARNING", "ServerEventLogger", null, "N/A", "N/A");
                    });
                } else {
                    // Regular memory usage log
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        api.log("Memory Usage - Used: " + String.format("%.2f", usedMemoryPercentage) + "% (" + usedMemory + " MB of " + maxMemory + " MB)", "INFO", "ServerEventLogger", null, "N/A", "N/A");
                    });
                }

                // TPS Monitoring
                double[] tps = Bukkit.getServer().getTPS();
                double currentTPS = tps[0];

                if (currentTPS < configManager.getLowTPSThreshold()) {
                    // Low TPS alert
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        api.log("Low TPS Alert - TPS: " + String.format("%.2f", currentTPS), "WARNING", "ServerEventLogger", null, "N/A", "N/A");
                    });
                } else {
                    // Regular TPS log
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        api.log("Server TPS - 1m: " + String.format("%.2f", tps[0]) + ", 5m: " + String.format("%.2f", tps[1]) + ", 15m: " + String.format("%.2f", tps[2]), "INFO", "ServerEventLogger", null, "N/A", "N/A");
                    });
                }

                /*
                // CPU Usage
                OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                double processCpuLoad = osBean.get * 100; // Process CPU load in percentage

                if (processCpuLoad > configManager.getHighCpuUsageThreshold()) {
                    // High CPU usage alert
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        api.log("High CPU Usage Alert - Process CPU Load: " + String.format("%.2f", processCpuLoad) + "%", "WARNING", "ServerEventLogger", null, "N/A", "N/A");
                    });
                } else {
                    // Regular CPU usage log
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        api.log("CPU Usage - Process CPU Load: " + String.format("%.2f", processCpuLoad) + "%", "INFO", "ServerEventLogger", null, "N/A", "N/A");
                    });
                }

                 */

                // Disk Space Usage
                long totalDiskSpace = new java.io.File("/").getTotalSpace() / (1024 * 1024 * 1024); // in GB
                long freeDiskSpace = new java.io.File("/").getFreeSpace() / (1024 * 1024 * 1024); // in GB
                long usedDiskSpace = totalDiskSpace - freeDiskSpace;
                double usedDiskSpacePercentage = ((double) usedDiskSpace / totalDiskSpace) * 100;

                if (usedDiskSpacePercentage > configManager.getHighDiskUsageThreshold()) {
                    // High disk usage alert
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        api.log("High Disk Usage Alert - Used: " + String.format("%.2f", usedDiskSpacePercentage) + "% (" + usedDiskSpace + " GB of " + totalDiskSpace + " GB)", "WARNING", "ServerEventLogger", null, "N/A", "N/A");
                    });
                } else {
                    // Regular disk usage log
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        api.log("Disk Usage - Used: " + String.format("%.2f", usedDiskSpacePercentage) + "% (" + usedDiskSpace + " GB of " + totalDiskSpace + " GB)", "INFO", "ServerEventLogger", null, "N/A", "N/A");
                    });
                }

                // Additional monitoring can be added here...

            }
        }.runTaskTimer(plugin, 0L, configManager.getMonitoringIntervalTicks()); // Run according to configured interval
    }
}
