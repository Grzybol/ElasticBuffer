package org.betterbox.elasticBuffer;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandManager implements CommandExecutor {
    private final JavaPlugin plugin;
    private final ElasticBuffer elasticBuffer;
    private final ElasticBufferConfigManager configManager;

    public CommandManager(JavaPlugin plugin, ElasticBuffer elasticBuffer, ElasticBufferConfigManager configManager){
        this.configManager=configManager;
        this.plugin = plugin;
        this.elasticBuffer = elasticBuffer;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        elasticBuffer.receiveLog("CommandManager.onCommand called, sender: " + sender + ", args: " + String.join(", ", args),"DEBUG",plugin.getDescription().getName(),null,sender.getName(),null);
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("betterquests.reload")) {
                sender.sendMessage(ChatColor.GOLD+""+ChatColor.BOLD+"[BetterQuests]"+ChatColor.DARK_RED + " You don't have permission to do that!");
                elasticBuffer.receiveLog("Help command failed due to lack of permissions, sender: " + sender, "ERROR",plugin.getDescription().getName(),null,sender.getName(),null);
                return true;
            }
            configManager.ReloadConfig();
            sender.sendMessage(ChatColor.GOLD+""+ChatColor.BOLD+"[BetterQuests]"+ChatColor.AQUA + " Configuration reloaded!");
            return true;
        }
        return false;
    }
}
