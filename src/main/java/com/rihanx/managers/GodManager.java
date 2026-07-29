package com.rihanx.managers;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players in god mode (invulnerable).
 */
public final class GodManager {

    private final Set<UUID> gods = ConcurrentHashMap.newKeySet();

    public boolean toggle(@NotNull Player player) {
        if (gods.contains(player.getUniqueId())) {
            disable(player);
            return false;
        }
        enable(player);
        return true;
    }

    public void enable(@NotNull Player player) {
        gods.add(player.getUniqueId());
        player.setInvulnerable(true);
        player.setFireTicks(0);
    }

    public void disable(@NotNull Player player) {
        gods.remove(player.getUniqueId());
        player.setInvulnerable(false);
    }

    public boolean isGod(@NotNull Player player) {
        return gods.contains(player.getUniqueId());
    }

    public boolean isGod(@NotNull UUID uuid) {
        return gods.contains(uuid);
    }

    public @NotNull Set<UUID> getGods() {
        return Collections.unmodifiableSet(gods);
    }

    public void handleQuit(@NotNull Player player, boolean disableOnQuit) {
        if (disableOnQuit) {
            disable(player);
        }
    }
}
