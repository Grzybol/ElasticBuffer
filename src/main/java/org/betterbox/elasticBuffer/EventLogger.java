package org.betterbox.elasticBuffer;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
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
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.Set;

public class EventLogger implements Listener {
    private final ElasticBufferAPI api;
    private final Plugin plugin;

    public EventLogger(ElasticBufferAPI api, Plugin plugin) {
        this.api = api;
        this.plugin = plugin;
    }

    // Obsługa zdarzeń Player*

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(event.getPlayer().getName() + " joined the game.", "INFO", "EventLogger", null,
                    event.getPlayer().getName(), event.getPlayer().getUniqueId().toString());
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(event.getPlayer().getName() + " left the game.", "INFO", "EventLogger", null,
                    event.getPlayer().getName(), event.getPlayer().getUniqueId().toString());
        });
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(event.getPlayer().getName() + " was kicked. Reason: " + event.getReason(), "WARNING",
                    "EventLogger", null, event.getPlayer().getName(), event.getPlayer().getUniqueId().toString());
        });
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(event.getEntity().getName() + " died. Cause: " + event.getDeathMessage(), "INFO",
                    "EventLogger", null, event.getEntity().getName(), event.getEntity().getUniqueId().toString());
        });
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(event.getPlayer().getName() + " respawned at " + event.getRespawnLocation().toString(), "INFO",
                    "EventLogger", null, event.getPlayer().getName(), event.getPlayer().getUniqueId().toString());
        });
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Ignoruj małe ruchy, aby zmniejszyć obciążenie
        if (event.getFrom().distanceSquared(event.getTo()) == 0) {
            return;
        }

        // Sprawdzenie, czy gracz porusza się zbyt szybko
        if (isMovingTooFast(player, event.getFrom(), event.getTo())) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " is moving too fast (possible speed hack).", "WARNING",
                        "CheatLogger", null, player.getName(), player.getUniqueId().toString());
            });
        }

        // Sprawdzenie, czy gracz lata bez uprawnień
        if (player.isFlying() && !player.hasPermission("cheatlogger.allowfly")) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " is flying without permission.", "WARNING",
                        "CheatLogger", null, player.getName(), player.getUniqueId().toString());
            });
        }

        // Sprawdzenie, czy gracz teleportuje się
        if (isTeleporting(player, event.getFrom(), event.getTo())) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " teleported unexpectedly.", "WARNING",
                        "CheatLogger", null, player.getName(), player.getUniqueId().toString());
            });
        }
    }

    // Metoda pomocnicza do sprawdzania szybkiego ruchu
    private boolean isMovingTooFast(Player player, Location from, Location to) {
        double distance = from.distance(to);
        double maxDistance = 0.7; // Maksymalna odległość na tick (dla sprintu i skoku)
        return distance > maxDistance;
    }

    // Metoda pomocnicza do sprawdzania nieoczekiwanych teleportacji
    private boolean isTeleporting(Player player, Location from, Location to) {
        double distance = from.distance(to);
        double maxTeleportDistance = 50.0; // Maksymalna odległość uznawana za normalny ruch
        return distance > maxTeleportDistance && !player.hasPermission("cheatlogger.allowteleport");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // AsyncPlayerChatEvent jest już asynchroniczne, więc nie trzeba używać runTaskAsynchronously
        api.log(event.getPlayer().getName() + ": " + event.getMessage(), "INFO", "EventLogger", null,
                event.getPlayer().getName(), event.getPlayer().getUniqueId().toString());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Sprawdzenie, czy gracz klika zbyt szybko (autoclicker)
        if (isClickingTooFast(player)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " is clicking too fast (possible autoclicker).", "WARNING",
                        "CheatLogger", null, player.getName(), player.getUniqueId().toString());
            });
        }

        // Sprawdzenie, czy gracz wchodzi w interakcję z niedozwolonym blokiem
        if (event.getClickedBlock() != null && isProhibitedBlock(event.getClickedBlock())) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " interacted with a prohibited block: " + event.getClickedBlock().getType(), "WARNING",
                        "CheatLogger", null, player.getName(), player.getUniqueId().toString());
            });
        }
    }

    // Metoda pomocnicza do sprawdzania szybkiego klikania
    private boolean isClickingTooFast(Player player) {
        // Implementuj logikę licznika kliknięć gracza w określonym czasie
        // Możesz użyć Map<Player, List<Long>> do przechowywania timestampów kliknięć
        return false; // Placeholder
    }

    // Metoda pomocnicza do sprawdzania, czy blok jest niedozwolony
    private boolean isProhibitedBlock(Block block) {
        // Definiuj listę niedozwolonych bloków
        Set<Material> prohibitedBlocks = EnumSet.of(Material.BEDROCK, Material.COMMAND_BLOCK);
        return prohibitedBlocks.contains(block.getType());
    }

    // Dodatkowe zdarzenia

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Sprawdzenie, czy gracz jest w trybie kreatywnym i nie ma uprawnień do niszczenia bloków
        if (player.getGameMode() == GameMode.CREATIVE && !player.hasPermission("cheatlogger.allowcreativebreak")) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " broke a block in creative mode without permission: " + event.getBlock().getType(), "WARNING",
                        "CheatLogger", null, player.getName(), player.getUniqueId().toString());
            });
            return;
        }

        // Sprawdzenie, czy gracz niszczy zbyt wiele bloków w krótkim czasie (np. nuker)
        if (isBreakingBlocksTooQuickly(player)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " is breaking blocks too quickly (possible nuker): " + event.getBlock().getType(), "WARNING",
                        "CheatLogger", null, player.getName(), player.getUniqueId().toString());
            });
        }

        // Sprawdzenie, czy blok jest chroniony (np. WorldGuard)
        if (isBlockProtected(event.getBlock(), player)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " attempted to break a protected block: " + event.getBlock().getType(), "WARNING",
                        "CheatLogger", null, player.getName(), player.getUniqueId().toString());
            });
        }
    }
    // Metoda pomocnicza do sprawdzania szybkiego niszczenia bloków
    private boolean isBreakingBlocksTooQuickly(Player player) {
        // Implementuj logikę licznika bloków niszczonych przez gracza w określonym czasie
        // Możesz użyć Map<Player, List<Long>> do przechowywania timestampów niszczeń
        return false; // Placeholder
    }

    // Metoda pomocnicza do sprawdzania, czy blok jest chroniony
    private boolean isBlockProtected(Block block, Player player) {
        // Implementuj integrację z pluginem ochrony, np. WorldGuard
        return false; // Placeholder
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(event.getPlayer().getName() + " placed block: " + event.getBlock().getType(), "INFO",
                    "EventLogger", null, event.getPlayer().getName(), event.getPlayer().getUniqueId().toString());
        });
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(event.getPlayer().getName() + " filled bucket with: " + event.getBlockClicked().getType(),
                    "INFO", "EventLogger", null, event.getPlayer().getName(),
                    event.getPlayer().getUniqueId().toString());
        });
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(event.getPlayer().getName() + " emptied bucket at: "
                            + event.getBlockClicked().getLocation().toString(), "INFO", "EventLogger", null,
                    event.getPlayer().getName(), event.getPlayer().getUniqueId().toString());
        });
    }

    @EventHandler
    public void onAnvilUse(PrepareAnvilEvent event) {
        if (event.getView().getPlayer() instanceof Player) {
            Player player = (Player) event.getView().getPlayer();
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
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " picked up: " + event.getItem().getItemStack().getType(), "INFO",
                        "EventLogger", null, player.getName(), player.getUniqueId().toString());
            });
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(event.getPlayer().getName() + " dropped: " + event.getItemDrop().getItemStack().getType(),
                    "INFO", "EventLogger", null, event.getPlayer().getName(),
                    event.getPlayer().getUniqueId().toString());
        });
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(event.getEnchanter().getName() + " enchanted item: " + event.getItem().getType(), "INFO",
                    "EventLogger", null, event.getEnchanter().getName(), event.getEnchanter().getUniqueId().toString());
        });
    }

    @EventHandler
    public void onBookEdit(PlayerEditBookEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(event.getPlayer().getName() + " edited a book titled: " + event.getNewBookMeta().getTitle(),
                    "INFO", "EventLogger", null, event.getPlayer().getName(),
                    event.getPlayer().getUniqueId().toString());
        });
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log("Item smelted: " + event.getSource().getType(), "INFO", "EventLogger", null, "N/A", "N/A");
        });
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(event.getPlayer().getName() + " changed game mode to: " + event.getNewGameMode().toString(),
                    "INFO", "EventLogger", null, event.getPlayer().getName(),
                    event.getPlayer().getUniqueId().toString());
        });
    }

    @EventHandler
    public void onPlayerCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(player.getName() + " crafted: " + event.getCurrentItem().getType(), "INFO", "EventLogger",
                        null, player.getName(), player.getUniqueId().toString());
            });
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (event.getDamager() instanceof Player) {
                Player attacker = (Player) event.getDamager();
                String victimName;
                String victimUUID = null;
                double distance = 0.0;
                String weaponUsed = "Unknown";

                // Obliczanie dystansu między atakującym a ofiarą
                if (event.getEntity() instanceof Player) {
                    Player victim = (Player) event.getEntity();
                    victimName = victim.getName();
                    victimUUID = victim.getUniqueId().toString();
                    distance = attacker.getLocation().distance(victim.getLocation());
                } else {
                    victimName = event.getEntity().getType().toString();
                    distance = attacker.getLocation().distance(event.getEntity().getLocation());
                }

                // Uzyskanie informacji o broni użytej przez atakującego
                if (attacker.getInventory().getItemInMainHand() != null) {
                    weaponUsed = attacker.getInventory().getItemInMainHand().getType().toString();
                }

                // Kompilacja wiadomości logowania
                String logMessage = String.format(
                        "%s damaged %s for %.2f damage using %s from a distance of %.2f blocks.",
                        attacker.getName(),
                        victimName,
                        event.getFinalDamage(),
                        weaponUsed,
                        distance
                );

                // Logowanie z dodatkowymi informacjami
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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (event.getEntity() instanceof org.bukkit.entity.TNTPrimed) {
                api.log("TNT exploded at: " + event.getLocation().toString(), "INFO", "EventLogger", null, "N/A",
                        "N/A");
            }
        });
    }

    @EventHandler
    public void onChestInteraction(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player && event.getInventory().getType() == org.bukkit.event.inventory.InventoryType.CHEST) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                api.log(event.getPlayer().getName() + " opened a chest at " + event.getPlayer().getLocation().toString(),
                        "INFO", "EventLogger", null, event.getPlayer().getName(),
                        event.getPlayer().getUniqueId().toString());
            });
        }
    }

    @EventHandler
    public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.log(event.getPlayer().getName() + " registered channel: " + event.getChannel(), "INFO",
                    "EventLogger", null, event.getPlayer().getName(), event.getPlayer().getUniqueId().toString());
        });
    }
}
