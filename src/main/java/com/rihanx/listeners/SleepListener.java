package com.rihanx.listeners;

import com.rihanx.managers.MessageManager;
import com.rihanx.managers.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Night skips to sunrise only when every required online player is sleeping.
 */
public final class SleepListener implements Listener {

    private final @NotNull JavaPlugin plugin;
    private final @NotNull MessageManager messages;
    private final @NotNull VanishManager vanishManager;

    public SleepListener(
            @NotNull JavaPlugin plugin,
            @NotNull MessageManager messages,
            @NotNull VanishManager vanishManager
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.vanishManager = vanishManager;
        refresh();
    }

    /** Re-apply gamerule + sleepingIgnored after enable/reload. */
    public void refresh() {
        enforceSleepPercentage();
        for (Player player : Bukkit.getOnlinePlayers()) {
            syncSleepingIgnored(player);
        }
    }

    public void enforceSleepPercentage() {
        if (!plugin.getConfig().getBoolean("sleep.enabled", true)) {
            return;
        }
        int percent = plugin.getConfig().getInt("sleep.players-sleeping-percentage", 100);
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL) {
                world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, percent);
            }
        }
    }

    @EventHandler
    public void onWorldLoad(@NotNull WorldLoadEvent event) {
        if (!plugin.getConfig().getBoolean("sleep.enabled", true)) {
            return;
        }
        World world = event.getWorld();
        if (world.getEnvironment() == World.Environment.NORMAL) {
            world.setGameRule(
                    GameRule.PLAYERS_SLEEPING_PERCENTAGE,
                    plugin.getConfig().getInt("sleep.players-sleeping-percentage", 100)
            );
        }
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        syncSleepingIgnored(event.getPlayer());
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        // Reset so rejoining elsewhere isn't stuck ignored
        event.getPlayer().setSleepingIgnored(false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameMode(@NotNull PlayerGameModeChangeEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> syncSleepingIgnored(event.getPlayer()));
    }

    @EventHandler
    public void onWorldChange(@NotNull PlayerChangedWorldEvent event) {
        syncSleepingIgnored(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBedEnter(@NotNull PlayerBedEnterEvent event) {
        if (!plugin.getConfig().getBoolean("sleep.enabled", true)) {
            return;
        }
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }
        Player sleeper = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!sleeper.isOnline() || !sleeper.isSleeping()) {
                return;
            }
            List<Player> required = requiredPlayers(sleeper.getWorld());
            List<Player> sleeping = required.stream().filter(Player::isSleeping).collect(Collectors.toList());
            List<Player> awake = required.stream().filter(p -> !p.isSleeping()).collect(Collectors.toList());

            messages.send(sleeper, "sleep-progress", MessageManager.placeholders(
                    "sleeping", sleeping.size(),
                    "total", required.size()
            ));
            if (!awake.isEmpty()) {
                String names = awake.stream().map(Player::getName).collect(Collectors.joining(", "));
                for (Player online : required) {
                    messages.send(online, "sleep-waiting", MessageManager.placeholders(
                            "sleeping", sleeping.size(),
                            "total", required.size(),
                            "players", names
                    ));
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTimeSkip(@NotNull TimeSkipEvent event) {
        if (!plugin.getConfig().getBoolean("sleep.enabled", true)) {
            return;
        }
        if (event.getSkipReason() != TimeSkipEvent.SkipReason.NIGHT_SKIP) {
            return;
        }
        World world = event.getWorld();
        List<Player> required = requiredPlayers(world);
        if (required.isEmpty()) {
            return;
        }
        List<Player> awake = new ArrayList<>();
        for (Player player : required) {
            if (!player.isSleeping()) {
                awake.add(player);
            }
        }
        if (!awake.isEmpty()) {
            event.setCancelled(true);
            String names = awake.stream().map(Player::getName).collect(Collectors.joining(", "));
            for (Player player : required) {
                messages.send(player, "sleep-blocked", MessageManager.placeholders(
                        "players", names,
                        "count", awake.size()
                ));
            }
        }
    }

    /**
     * Mark excluded players as sleeping-ignored so vanilla percentage math matches our rules.
     */
    public void syncSleepingIgnored(@NotNull Player player) {
        if (!plugin.getConfig().getBoolean("sleep.enabled", true)) {
            player.setSleepingIgnored(false);
            return;
        }
        player.setSleepingIgnored(isExcluded(player));
    }

    private boolean isExcluded(@NotNull Player player) {
        if (player.getWorld().getEnvironment() != World.Environment.NORMAL
                && plugin.getConfig().getBoolean("sleep.ignore-other-dimensions", true)) {
            return true;
        }
        if (plugin.getConfig().getBoolean("sleep.ignore-spectators", true)
                && player.getGameMode() == GameMode.SPECTATOR) {
            return true;
        }
        if (plugin.getConfig().getBoolean("sleep.ignore-vanished", true)
                && vanishManager.isVanished(player)) {
            return true;
        }
        if (plugin.getConfig().getBoolean("sleep.ignore-creative", false)
                && player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }
        return false;
    }

    /**
     * Players who must sleep before sunrise in this world.
     */
    private @NotNull List<Player> requiredPlayers(@NotNull World world) {
        boolean sameWorldOnly = plugin.getConfig().getBoolean("sleep.same-world-only", true);
        boolean ignoreOtherDimensions = plugin.getConfig().getBoolean("sleep.ignore-other-dimensions", true);

        List<Player> required = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (sameWorldOnly && !player.getWorld().equals(world)) {
                continue;
            }
            if (!sameWorldOnly && ignoreOtherDimensions
                    && player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                continue;
            }
            if (isExcluded(player)) {
                continue;
            }
            required.add(player);
        }
        return required;
    }
}
