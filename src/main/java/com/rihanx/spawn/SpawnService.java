package com.rihanx.spawn;

import com.rihanx.managers.MessageManager;
import com.rihanx.teleport.TeleportManager;
import com.rihanx.utils.LocationUtil;
import com.rihanx.warp.WarpService;
import org.bukkit.Location;
import org.bukkit.World;
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
        Location destination;
        if (warpService.getStore().has(SPAWN_WARP)) {
            destination = warpService.getWarp(SPAWN_WARP);
            if (destination == null) {
                // Warp exists but its world is not loaded — never silently fall back
                messages.send(player, "spawn-world-unloaded", MessageManager.placeholders(
                        "warp", SPAWN_WARP
                ));
                return;
            }
        } else {
            destination = player.getWorld().getSpawnLocation().clone();
            // Center on the block and keep facing
            destination.setX(destination.getBlockX() + 0.5);
            destination.setZ(destination.getBlockZ() + 0.5);
            destination.setYaw(player.getLocation().getYaw());
            destination.setPitch(0f);
        }

        // Prefer the exact spawn point when it is already safe (do not shove to surface)
        teleportManager.teleportPreferExact(player, destination, "spawn");
    }

    public void setSpawn(@NotNull Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world != null) {
            world.setSpawnLocation(loc);
        }
        if (!warpService.getStore().set(SPAWN_WARP, loc)) {
            messages.send(player, "internal-error");
            return;
        }
        messages.send(player, "spawn-set");
    }

    /** True when the spawn warp's world is loaded (or no warp is set yet). */
    public boolean isSpawnReady() {
        if (!warpService.getStore().has(SPAWN_WARP)) {
            return true;
        }
        return warpService.getWarp(SPAWN_WARP) != null;
    }

    public static boolean isExactSpawnSafe(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        return LocationUtil.isSafe(world, location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
