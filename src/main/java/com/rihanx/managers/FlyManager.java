package com.rihanx.managers;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players have flight enabled by RihanX (per-UUID only).
 */
public final class FlyManager {

    private final Set<UUID> flying = ConcurrentHashMap.newKeySet();

    public boolean toggle(@NotNull Player player) {
        if (isFlying(player)) {
            disable(player);
            return false;
        }
        enable(player);
        return true;
    }

    public void enable(@NotNull Player player) {
        flying.add(player.getUniqueId());
        player.setAllowFlight(true);
        if (player.getGameMode() != GameMode.SPECTATOR) {
            player.setFlying(true);
        }
    }

    public void disable(@NotNull Player player) {
        flying.remove(player.getUniqueId());
        // Do not strip creative/spectator flight
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        player.setFlying(false);
        player.setAllowFlight(false);
    }

    public boolean isFlying(@NotNull Player player) {
        return flying.contains(player.getUniqueId());
    }

    public boolean isFlying(@NotNull UUID uuid) {
        return flying.contains(uuid);
    }

    public @NotNull Set<UUID> getFlying() {
        return Collections.unmodifiableSet(flying);
    }

    public void handleQuit(@NotNull Player player, boolean disableOnQuit) {
        if (disableOnQuit) {
            disable(player);
        } else {
            flying.remove(player.getUniqueId());
        }
    }

    public void handleJoin(@NotNull Player player) {
        if (isFlying(player)) {
            enable(player);
        }
    }

    public void clearAll() {
        flying.clear();
    }
}
