package com.rihanx.listeners;

import com.rihanx.managers.BackLocationManager;
import com.rihanx.managers.ConfigManager;
import com.rihanx.managers.CooldownManager;
import com.rihanx.managers.FreezeManager;
import com.rihanx.managers.GodManager;
import com.rihanx.managers.VanishManager;
import com.rihanx.scheduler.AsyncTaskTracker;
import com.rihanx.teleport.TeleportManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Handles freeze, god mode, teleport cancellation, and cleanup on quit.
 */
public final class PlayerListener implements Listener {

    private final @NotNull FreezeManager freezeManager;
    private final @NotNull VanishManager vanishManager;
    private final @NotNull GodManager godManager;
    private final @NotNull TeleportManager teleportManager;
    private final @NotNull ConfigManager configManager;
    private final @NotNull CooldownManager cooldownManager;
    private final @NotNull AsyncTaskTracker taskTracker;
    private final @NotNull BackLocationManager backLocationManager;

    public PlayerListener(
            @NotNull FreezeManager freezeManager,
            @NotNull VanishManager vanishManager,
            @NotNull GodManager godManager,
            @NotNull TeleportManager teleportManager,
            @NotNull ConfigManager configManager,
            @NotNull CooldownManager cooldownManager,
            @NotNull AsyncTaskTracker taskTracker,
            @NotNull BackLocationManager backLocationManager
    ) {
        this.freezeManager = freezeManager;
        this.vanishManager = vanishManager;
        this.godManager = godManager;
        this.teleportManager = teleportManager;
        this.configManager = configManager;
        this.cooldownManager = cooldownManager;
        this.taskTracker = taskTracker;
        this.backLocationManager = backLocationManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(@NotNull PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!freezeManager.isFrozen(player)) {
            return;
        }
        if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ()) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(@NotNull EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (godManager.isGod(player) && player.isOp()) {
            event.setCancelled(true);
            player.setFireTicks(0);
            return;
        }
        teleportManager.cancelOnDamage(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombust(@NotNull EntityCombustEvent event) {
        if (event.getEntity() instanceof Player player && godManager.isGod(player) && player.isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFood(@NotNull FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && godManager.isGod(player) && player.isOp()) {
            event.setCancelled(true);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(@NotNull PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            return;
        }
        Player player = event.getPlayer();
        if (event.getFrom() != null && event.getTo() != null
                && event.getFrom().getWorld() != null
                && !event.getFrom().equals(event.getTo())) {
            backLocationManager.push(player, event.getFrom());
        }
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        freezeManager.handleQuit(player, configManager.unfreezeOnQuit());
        vanishManager.handleQuit(player, configManager.unvanishOnQuit());
        godManager.handleQuit(player, configManager.raw().getBoolean("general.ungod-on-quit", true));
        teleportManager.cancel(player.getUniqueId(), false);
        cooldownManager.clear(player.getUniqueId());
        taskTracker.cancel(player.getUniqueId());
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        vanishManager.handleJoin(player);
        if (freezeManager.isFrozen(player)) {
            freezeManager.freeze(player);
        }
        if (godManager.isGod(player)) {
            godManager.enable(player);
        }
    }
}
