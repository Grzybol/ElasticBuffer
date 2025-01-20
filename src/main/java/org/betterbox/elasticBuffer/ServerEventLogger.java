package org.betterbox.elasticBuffer;

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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.logging.*;

public class ServerEventLogger implements Listener {
    private final ElasticBufferAPI api;
    private final Plugin plugin;
    private final ElasticBufferConfigManager configManager;
    private final Logger logger = Logger.getLogger("Minecraft");

    public ServerEventLogger(ElasticBufferAPI api, Plugin plugin, ElasticBufferConfigManager configManager) {
        this.api = api;
        this.plugin = plugin;
        this.configManager = configManager;
        startMonitoring();
        startConsoleMonitoring();
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

    // Monitoring method for console logs
    private void startConsoleMonitoring() {
        // Znajdź głównego loggera Minecrafta
        Logger logger = Logger.getLogger("");

        // Utwórz niestandardowy handler
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                // Construct log message
                String logMessage = record.getMessage();
                String logLevel = record.getLevel().getName();
                String loggerName = record.getLoggerName();
                String sourceClassName = record.getSourceClassName();
                String sourceMethodName = record.getSourceMethodName();

                // Send log to ElasticBufferAPI asynchronously
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        api.log(logMessage, logLevel, loggerName, sourceClassName, sourceMethodName, "Console");
                    }
                }.runTaskAsynchronously(plugin);
            }

            @Override
            public void flush() {
                // No-op
            }

            @Override
            public void close() throws SecurityException {
                // No-op
            }
        };

        // Ustaw poziom handlera na ALL, aby przechwytywał wszystkie poziomy logowania
        handler.setLevel(Level.ALL);

        // Dodaj niestandardowy handler do loggera
        logger.addHandler(handler);
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
