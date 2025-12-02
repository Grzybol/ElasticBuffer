package org.betterbox.elasticBuffer.anticheat;

import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility replacing supported placeholders in configured commands.
 */
public class PlaceholderService {
    public String applyPlaceholders(String command, Player player, CheckType type, double severity, int count) {
        if (command == null) {
            return "";
        }
        Map<String, String> values = new HashMap<>();
        values.put("%player_name%", player != null ? player.getName() : "unknown");
        UUID uuid = player != null ? player.getUniqueId() : null;
        values.put("%player_uuid%", uuid != null ? uuid.toString() : "unknown");
        values.put("%check_type%", type != null ? type.name() : "UNKNOWN");
        values.put("%severity%", Double.toString(severity));
        values.put("%count%", Integer.toString(count));
        values.put("%player_ip%", resolveAddress(player));

        String resolved = command;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            resolved = resolved.replace(entry.getKey(), entry.getValue());
        }
        return resolved;
    }

    private String resolveAddress(Player player) {
        if (player == null) {
            return "unknown";
        }
        InetSocketAddress address = player.getAddress();
        if (address == null || address.getAddress() == null) {
            return "unknown";
        }
        return address.getAddress().getHostAddress();
    }
}
