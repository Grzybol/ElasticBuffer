package org.betterbox.elasticBuffer;

import org.apache.logging.log4j.core.net.Priority;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import javax.management.monitor.Monitor;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public class EventLogger implements Listener {
    private final ElasticBufferAPI api;
    private final Plugin plugin;

    public EventLogger(ElasticBufferAPI api, Plugin plugin) {
        this.api = api;
        this.plugin = plugin;
    }

    // Helper method to set metadata and schedule its removal
    private void setTemporaryMetadata(Entity entity, String key, long durationTicks) {
        entity.setMetadata(key, new FixedMetadataValue(plugin, true));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            entity.removeMetadata(key, plugin);
        }, durationTicks);
    }

    // Helper method to check if metadata is present
    private boolean hasMetadata(Entity entity, String key) {
        return entity.hasMetadata(key);
    }

    //@EventHandler(priority = EventPriority.MONITOR)
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (hasMetadata(event.getPlayer(), "eventPlayerJoinHandled")) {
            return;
        }
        setTemporaryMetadata(event.getPlayer(), "eventPlayerJoinHandled", 1L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(event.getPlayer().getName() + " joined the game.", "INFO", "EventLogger", null,
                    event.getPlayer().getName(), event.getPlayer().getUniqueId().toString());
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (hasMetadata(event.getPlayer(), "eventPlayerQuitHandled")) {
            return;
        }
        setTemporaryMetadata(event.getPlayer(), "eventPlayerQuitHandled", 1L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(event.getPlayer().getName() + " left the game.", "INFO", "EventLogger", null,
                    event.getPlayer().getName(), event.getPlayer().getUniqueId().toString());
        });
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        if (hasMetadata(event.getPlayer(), "eventPlayerKickHandled")) {
            return;
        }
        setTemporaryMetadata(event.getPlayer(), "eventPlayerKickHandled", 1L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(event.getPlayer().getName() + " was kicked. Reason: " + event.getReason(), "WARNING",
                    "EventLogger", null, event.getPlayer().getName(), event.getPlayer().getUniqueId().toString());
        });
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (hasMetadata(event.getEntity(), "eventPlayerDeathHandled")) {
            return;
        }
        setTemporaryMetadata(event.getEntity(), "eventPlayerDeathHandled", 1L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(event.getEntity().getName() + " died. Cause: " + event.getDeathMessage(), "INFO",
                    "EventLogger", null, event.getEntity().getName(), event.getEntity().getUniqueId().toString());
        });
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (hasMetadata(event.getPlayer(), "eventPlayerRespawnHandled")) {
            return;
        }
        setTemporaryMetadata(event.getPlayer(), "eventPlayerRespawnHandled", 1L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(event.getPlayer().getName() + " respawned at " + event.getRespawnLocation().toString(), "INFO",
                    "EventLogger", null, event.getPlayer().getName(), event.getPlayer().getUniqueId().toString());
        });
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (hasMetadata(player, "eventPlayerMoveHandled")) {
            return;
        }
        setTemporaryMetadata(player, "eventPlayerMoveHandled", 1L);

        // Ignore small movements to reduce overhead
        if (event.getFrom().distanceSquared(event.getTo()) == 0) {
            return;
        }

        // Check if the player is moving too fast
        if (isMovingTooFast(player, event.getFrom(), event.getTo())) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " is moving too fast (possible speed hack).", "WARNING",
                        "CheatLogger", null, player.getName(), player.getUniqueId().toString());
            });
        }

        // Check if the player is flying without permission
        if (player.isFlying() && !player.hasPermission("cheatlogger.allowfly")) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " is flying without permission.", "WARNING",
                        "CheatLogger", null, player.getName(), player.getUniqueId().toString());
            });
        }

        // Check if the player is teleporting unexpectedly
        if (isTeleporting(player, event.getFrom(), event.getTo())) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " teleported unexpectedly.", "WARNING",
                        "CheatLogger", null, player.getName(), player.getUniqueId().toString());
            });
        }
    }

    private boolean isMovingTooFast(Player player, Location from, Location to) {
        double distance = from.distance(to);
        double maxDistance = 2.0;
        return distance > maxDistance;
    }

    private boolean isTeleporting(Player player, Location from, Location to) {
        double distance = from.distance(to);
        double maxTeleportDistance = 50.0; // Maximum normal movement distance
        return distance > maxTeleportDistance && !player.hasPermission("cheatlogger.allowteleport");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (hasMetadata(event.getPlayer(), "eventPlayerChatHandled")) {
            return;
        }
        setTemporaryMetadata(event.getPlayer(), "eventPlayerChatHandled", 1L);

        // AsyncPlayerChatEvent is already asynchronous
        api.log(event.getPlayer().getName() + ": " + event.getMessage(), "CHAT", "EventLogger", null,
                event.getPlayer().getName(), event.getPlayer().getUniqueId().toString());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (hasMetadata(player, "eventPlayerInteractHandled")) {
            return;
        }
        setTemporaryMetadata(player, "eventPlayerInteractHandled", 1L);

        // Check for autoclicker
        if (isClickingTooFast(player)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " is clicking too fast (possible autoclicker).", "WARNING",
                        "CheatLogger", null, player.getName(), player.getUniqueId().toString());
            });
        }

        // Check for prohibited block interaction
        if (event.getClickedBlock() != null && isProhibitedBlock(event.getClickedBlock())) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " interacted with a prohibited block: " + event.getClickedBlock().getType(), "WARNING",
                        "CheatLogger", null, player.getName(), player.getUniqueId().toString());
            });
        }
    }

    // Implement your click tracking logic here
    private boolean isClickingTooFast(Player player) {
        // Placeholder for actual implementation
        return false;
    }

    private boolean isProhibitedBlock(Block block) {
        Set<Material> prohibitedBlocks = EnumSet.of(Material.BEDROCK, Material.COMMAND_BLOCK);
        return prohibitedBlocks.contains(block.getType());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (hasMetadata(player, "eventBlockBreakHandled")) {
            return;
        }
        setTemporaryMetadata(player, "eventBlockBreakHandled", 1L);

        // Your existing logic...
        if (player.getGameMode() == GameMode.CREATIVE && !player.hasPermission("cheatlogger.allowcreativebreak")) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " broke a block in creative mode without permission: " + event.getBlock().getType(), "WARNING",
                        "CheatLogger", null, player.getName(), player.getUniqueId().toString());
            });
            return;
        }

        if (isBreakingBlocksTooQuickly(player)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " is breaking blocks too quickly (possible nuker): " + event.getBlock().getType(), "WARNING",
                        "CheatLogger", null, player.getName(), player.getUniqueId().toString());
            });
        }

        if (isBlockProtected(event.getBlock(), player)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " attempted to break a protected block: " + event.getBlock().getType(), "WARNING",
                        "CheatLogger", null, player.getName(), player.getUniqueId().toString());
            });
        }
    }

    // Implement your block breaking tracking logic here
    private boolean isBreakingBlocksTooQuickly(Player player) {
        // Placeholder for actual implementation
        return false;
    }

    private boolean isBlockProtected(Block block, Player player) {
        // Placeholder for actual implementation
        return false;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (hasMetadata(player, "eventBlockPlaceHandled")) {
            return;
        }
        setTemporaryMetadata(player, "eventBlockPlaceHandled", 1L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(player.getName() + " placed block: " + event.getBlock().getType(), "INFO",
                    "EventLogger", null, player.getName(), player.getUniqueId().toString());
        });
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();

        if (hasMetadata(player, "eventBucketFillHandled")) {
            return;
        }
        setTemporaryMetadata(player, "eventBucketFillHandled", 1L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(player.getName() + " filled bucket with: " + event.getBlockClicked().getType(),
                    "INFO", "EventLogger", null, player.getName(),
                    player.getUniqueId().toString());
        });
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();

        if (hasMetadata(player, "eventBucketEmptyHandled")) {
            return;
        }
        setTemporaryMetadata(player, "eventBucketEmptyHandled", 1L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(player.getName() + " emptied bucket at: "
                            + event.getBlockClicked().getLocation().toString(), "INFO", "EventLogger", null,
                    player.getName(), player.getUniqueId().toString());
        });
    }

    @EventHandler
    public void onAnvilUse(PrepareAnvilEvent event) {
        if (event.getView().getPlayer() instanceof Player) {
            Player player = (Player) event.getView().getPlayer();

            if (hasMetadata(player, "eventAnvilUseHandled")) {
                return;
            }
            setTemporaryMetadata(player, "eventAnvilUseHandled", 1L);

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " used anvil with result: " + event.getResult(), "INFO", "EventLogger",
                        null, player.getName(), player.getUniqueId().toString());
            });
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (hasMetadata(player, "eventItemPickupHandled")) {
                return;
            }
            setTemporaryMetadata(player, "eventItemPickupHandled", 1L);

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " picked up: " + event.getItem().getItemStack().getType(), "INFO",
                        "EventLogger", null, player.getName(), player.getUniqueId().toString());
            });
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (hasMetadata(player, "eventItemDropHandled")) {
            return;
        }
        setTemporaryMetadata(player, "eventItemDropHandled", 1L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(player.getName() + " dropped: " + event.getItemDrop().getItemStack().getType(),
                    "INFO", "EventLogger", null, player.getName(),
                    player.getUniqueId().toString());
        });
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();

        if (hasMetadata(player, "eventEnchantItemHandled")) {
            return;
        }
        setTemporaryMetadata(player, "eventEnchantItemHandled", 1L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(player.getName() + " enchanted item: " + event.getItem().getType(), "INFO",
                    "EventLogger", null, player.getName(), player.getUniqueId().toString());
        });
    }

    @EventHandler
    public void onBookEdit(PlayerEditBookEvent event) {
        Player player = event.getPlayer();

        if (hasMetadata(player, "eventBookEditHandled")) {
            return;
        }
        setTemporaryMetadata(player, "eventBookEditHandled", 1L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(player.getName() + " edited a book titled: " + event.getNewBookMeta().getTitle(),
                    "INFO", "EventLogger", null, player.getName(),
                    player.getUniqueId().toString());
        });
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        // Since FurnaceSmeltEvent doesn't have a player, we cannot set metadata here
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log("Item smelted: " + event.getSource().getType(), "INFO", "EventLogger", null, "N/A", "N/A");
        });
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();

        if (hasMetadata(player, "eventGameModeChangeHandled")) {
            return;
        }
        setTemporaryMetadata(player, "eventGameModeChangeHandled", 1L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(player.getName() + " changed game mode to: " + event.getNewGameMode().toString(),
                    "INFO", "EventLogger", null, player.getName(),
                    player.getUniqueId().toString());
        });
    }

    @EventHandler
    public void onPlayerCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();

            if (hasMetadata(player, "eventPlayerCraftHandled")) {
                return;
            }
            setTemporaryMetadata(player, "eventPlayerCraftHandled", 1L);

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " crafted: " + event.getCurrentItem().getType(), "INFO", "EventLogger",
                        null, player.getName(), player.getUniqueId().toString());
            });
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        if (hasMetadata(entity, "eventEntityDeathHandled")) {
            return;
        }
        setTemporaryMetadata(entity, "eventEntityDeathHandled", 1L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (event.getEntity().getKiller() != null) {
                Player killer = event.getEntity().getKiller();
                api.log(killer.getName() + " killed entity: " + event.getEntity().getType(), "INFO",
                        "EventLogger", null, killer.getName(), killer.getUniqueId().toString());
            } else {
                api.log("Entity died: " + event.getEntity().getType(), "INFO", "EventLogger", null, "N/A", "N/A");
            }
        });
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        if (damager.hasMetadata("eventEntityDamageHandled")) {
            return;
        }
        setTemporaryMetadata(damager, "eventEntityDamageHandled", 1L);

        // Calculate health after damage
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        LivingEntity victimEntity = (LivingEntity) event.getEntity();
        double healthAfterDamage = victimEntity.getHealth() - event.getFinalDamage();

        // If the entity will die from this damage, let onEntityDeath handle logging
        if (healthAfterDamage <= 0) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (damager instanceof Player) {
                Player attacker = (Player) damager;
                String victimName;
                String victimUUID = null;
                double distance = 0.0;
                String weaponUsed = "Unknown";

                if (event.getEntity() instanceof Player) {
                    Player victim = (Player) event.getEntity();
                    victimName = victim.getName();
                    victimUUID = victim.getUniqueId().toString();
                    distance = attacker.getLocation().distance(victim.getLocation());
                } else {
                    victimName = event.getEntity().getType().toString();
                    distance = attacker.getLocation().distance(event.getEntity().getLocation());
                }

                if (attacker.getInventory().getItemInMainHand() != null) {
                    weaponUsed = attacker.getInventory().getItemInMainHand().getType().toString();
                }

                String logMessage = String.format(
                        "%s damaged %s for %.2f damage using %s from a distance of %.2f blocks.",
                        attacker.getName(),
                        victimName,
                        event.getFinalDamage(),
                        weaponUsed,
                        distance
                );

                api.log(
                        logMessage,
                        "INFO",
                        "EventLogger",
                        null,
                        attacker.getName(),
                        attacker.getUniqueId().toString()
                );
            }
        });
    }

    @EventHandler
    public void onTNTExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();

        if (entity != null && entity.hasMetadata("eventTNTExplodeHandled")) {
            return;
        }
        if (entity != null) {
            setTemporaryMetadata(entity, "eventTNTExplodeHandled", 1L);
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (entity instanceof org.bukkit.entity.TNTPrimed) {
                api.log("TNT exploded at: " + event.getLocation().toString(), "INFO", "EventLogger", null, "N/A",
                        "N/A");
            }
        });
    }

    @EventHandler
    public void onChestInteraction(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();

            if (hasMetadata(player, "eventChestInteractionHandled")) {
                return;
            }
            setTemporaryMetadata(player, "eventChestInteractionHandled", 1L);

            if (event.getInventory().getType() == org.bukkit.event.inventory.InventoryType.CHEST) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    api.log(player.getName() + " opened a chest at " + player.getLocation().toString(),
                            "INFO", "EventLogger", null, player.getName(),
                            player.getUniqueId().toString());
                });
            }
        }
    }

    @EventHandler
    public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event) {
        Player player = event.getPlayer();

        if (hasMetadata(player, "eventPlayerRegisterChannelHandled")) {
            return;
        }
        setTemporaryMetadata(player, "eventPlayerRegisterChannelHandled", 1L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(player.getName() + " registered channel: " + event.getChannel(), "INFO",
                    "EventLogger", null, player.getName(), player.getUniqueId().toString());
        });
    }
}
