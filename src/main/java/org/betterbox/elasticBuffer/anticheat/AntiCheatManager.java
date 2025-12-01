package org.betterbox.elasticBuffer.anticheat;

import org.betterbox.elasticBuffer.ElasticBufferConfigManager;
import org.betterbox.elasticBuffer.ElasticBufferPluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Coordinates registration of checks, violation tracking and reaction execution.
 */
public class AntiCheatManager {
    private final Map<String, AntiCheatCheck> registeredChecks = new HashMap<>();
    private final PlayerViolationTracker violationTracker;
    private final ElasticBufferConfigManager configManager;
    private final ElasticBufferPluginLogger logger;
    private final Plugin plugin;
    private final PlaceholderService placeholderService;
    private List<AntiCheatThresholdRule> rules = new ArrayList<>();
    private long historyWindowMillis;
    private boolean enabled;

    public AntiCheatManager(Plugin plugin,
                            ElasticBufferConfigManager configManager,
                            ElasticBufferPluginLogger logger) {
        this(plugin, configManager, logger, new PlayerViolationTracker(), new PlaceholderService());
    }

    public AntiCheatManager(Plugin plugin,
                            ElasticBufferConfigManager configManager,
                            ElasticBufferPluginLogger logger,
                            PlayerViolationTracker violationTracker,
                            PlaceholderService placeholderService) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.logger = logger;
        this.violationTracker = violationTracker;
        this.placeholderService = placeholderService;
        reload();
    }

    public void registerCheck(AntiCheatCheck check) {
        registeredChecks.put(check.getName().toLowerCase(), check);
        logger.log(ElasticBufferPluginLogger.LogLevel.DEBUG, "Registered anti-cheat check " + check.getName());
    }

    public Map<String, AntiCheatCheck> getRegisteredChecks() {
        return Collections.unmodifiableMap(registeredChecks);
    }

    public void reload() {
        this.enabled = configManager.isAntiCheatEnabled();
        this.rules = new ArrayList<>(configManager.getAntiCheatThresholdRules());
        this.historyWindowMillis = configManager.getViolationHistorySeconds() * 1000L;
        logger.log(ElasticBufferPluginLogger.LogLevel.INFO,
                "Anti-cheat module " + (enabled ? "enabled" : "disabled") + " with " + rules.size() + " rule(s)");
    }

    public void handleViolation(Player player, CheckType type, double severity) {
        if (!enabled) {
            return;
        }
        UUID playerId = player != null ? player.getUniqueId() : new UUID(0, 0);
        long now = System.currentTimeMillis();
        violationTracker.addViolation(playerId, new ViolationRecord(type, severity, now), historyWindowMillis);
        logger.log(ElasticBufferPluginLogger.LogLevel.DEBUG,
                "Recorded violation for " + (player != null ? player.getName() : "unknown") + " type=" + type + " severity=" + severity);
        evaluateRules(player, playerId, type, severity);
    }

    private void evaluateRules(Player player, UUID playerId, CheckType type, double severity) {
        for (AntiCheatThresholdRule rule : rules) {
            long window = rule.getWindowMillis() > 0 ? rule.getWindowMillis() : historyWindowMillis;
            int count = violationTracker.getWindowCount(playerId, window, rule.getSeverityMin());
            if (count >= rule.getCountMin() && severity >= rule.getSeverityMin()) {
                runCommand(rule, player, type, severity, count);
            }
        }
    }

    private void runCommand(AntiCheatThresholdRule rule, Player player, CheckType type, double severity, int count) {
        String resolved = placeholderService.applyPlaceholders(rule.getCommand(), player, type, severity, count);
        logger.log(ElasticBufferPluginLogger.LogLevel.INFO,
                "Executing anti-cheat command for " + (player != null ? player.getName() : "unknown") + ": " + resolved);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), resolved));
    }
}
