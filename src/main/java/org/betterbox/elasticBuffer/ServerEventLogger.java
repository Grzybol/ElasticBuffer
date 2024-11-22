package org.betterbox.elasticBuffer;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ServerEventLogger implements Listener {
    private final ElasticBufferAPI api;
    private final Plugin plugin;

    public ServerEventLogger(ElasticBufferAPI api, Plugin plugin) {
        this.api = api;
        this.plugin = plugin;
        startMonitoring();
    }

    // Zdarzenie startu serwera
    @EventHandler
    public void onServerStart(ServerLoadEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log("Server started.", "INFO", "ServerEventLogger", null, "N/A", "N/A");
        });
    }

    // Zdarzenie komendy z konsoli
    @EventHandler
    public void onConsoleCommand(ServerCommandEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log("Console command executed: " + event.getCommand(), "INFO", "ServerEventLogger", null, "Console", "N/A");
        });
    }

    // Zdarzenie komendy RCON
    @EventHandler
    public void onRconCommand(RemoteServerCommandEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log("RCON command executed: " + event.getCommand(), "INFO", "ServerEventLogger", null, "RCON", "N/A");
        });
    }

    // Zdarzenie komendy z command blocka
    @EventHandler
    public void onCommandBlockCommand(ServerCommandEvent event) {
        if (event.getSender() instanceof BlockCommandSender) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log("Command Block executed command: " + event.getCommand(), "INFO", "ServerEventLogger", null, "CommandBlock", "N/A");
            });
        }
    }

    // Zdarzenie tworzenia portalu
    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log("Portal created of type: " + event.getReason(), "INFO", "ServerEventLogger", null, "N/A", "N/A");
        });
    }

    // Metoda monitorująca RAM i TPS
    private void startMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // RAM Usage
                long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
                long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024);
                long usedMemory = totalMemory - freeMemory;

                // Logowanie użycia RAM w sposób asynchroniczny
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    api.log("RAM Usage - Used: " + usedMemory + " MB, Free: " + freeMemory + " MB, Total: " + totalMemory + " MB", "INFO", "ServerEventLogger", null, "N/A", "N/A");
                });

                // TPS (jeśli używasz PaperMC)
                double[] tps = Bukkit.getServer().getTPS();
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    api.log("Server TPS - 1m: " + tps[0] + ", 5m: " + tps[1] + ", 15m: " + tps[2], "INFO", "ServerEventLogger", null, "N/A", "N/A");
                });
            }
        }.runTaskTimer(plugin, 0L, 1200L); // Co 60 sekund (1200 ticków)
    }
}
