package com.rihanx.spawn;

import com.rihanx.managers.MessageManager;
import com.rihanx.teleport.TeleportManager;
import com.rihanx.warp.WarpService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /spawn} and {@code /setspawn} — teleports to (and updates) a "spawn" warp,
 * falling back to the world spawn if no warp has been set yet.
 */
public final class SpawnService {

    public static final @NotNull String SPAWN_WARP = "spawn";

    private final @NotNull MessageManager messages;
    private final @NotNull TeleportManager teleportManager;
    private final @NotNull WarpService warpService;

    public SpawnService(
            @NotNull MessageManager messages,
            @NotNull TeleportManager teleportManager,
            @NotNull WarpService warpService
    ) {
        this.messages = messages;
        this.teleportManager = teleportManager;
        this.warpService = warpService;
    }

    public void teleportToSpawn(@NotNull Player player) {
        Location destination = warpService.getWarp(SPAWN_WARP);
        if (destination == null) {
            destination = player.getWorld().getSpawnLocation();
        }
        teleportManager.teleport(player, destination, "spawn");
        messages.send(player, "spawn-teleport");
    }

    public void setSpawn(@NotNull Player player) {
        player.getWorld().setSpawnLocation(player.getLocation());
        if (!warpService.getStore().set(SPAWN_WARP, player.getLocation())) {
            messages.send(player, "internal-error");
            return;
        }
        messages.send(player, "spawn-set");
    }
}
