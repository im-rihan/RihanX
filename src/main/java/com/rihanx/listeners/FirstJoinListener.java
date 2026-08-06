package com.rihanx.listeners;

import com.rihanx.RihanX;
import com.rihanx.kits.KitService;
import com.rihanx.scheduler.SchedulerUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Gives the configured {@code kits.first-join-kit} to brand-new players, once.
 */
public final class FirstJoinListener implements Listener {

    private final @NotNull RihanX plugin;
    private final @NotNull KitService kitService;
    private final @NotNull SchedulerUtil scheduler;

    public FirstJoinListener(
            @NotNull RihanX plugin,
            @NotNull KitService kitService,
            @NotNull SchedulerUtil scheduler
    ) {
        this.plugin = plugin;
        this.kitService = kitService;
        this.scheduler = scheduler;
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        if (event.getPlayer().hasPlayedBefore()) {
            return;
        }
        String kitName = plugin.getConfig().getString("kits.first-join-kit", "");
        if (kitName == null || kitName.isBlank()) {
            return;
        }
        Player player = event.getPlayer();
        scheduler.runSyncLater(() -> {
            if (player.isOnline()) {
                kitService.giveKit(player, kitName);
            }
        }, 20L);
    }
}
