package com.rihanx.managers;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players have flight enabled by RihanX (per-UUID only).
 * State persists across disconnect/rejoin (and restart via {@link PlayerStateStore}).
 */
public final class FlyManager {

    private final Set<UUID> flying = ConcurrentHashMap.newKeySet();
    private @Nullable PlayerStateStore store;

    public void attachStore(@NotNull PlayerStateStore store) {
        this.store = store;
        flying.addAll(store.getFly());
    }

    public boolean toggle(@NotNull Player player) {
        if (isFlying(player)) {
            disable(player);
            return false;
        }
        enable(player);
        return true;
    }

    public void enable(@NotNull Player player) {
        UUID id = player.getUniqueId();
        flying.add(id);
        if (store != null) {
            store.setFly(id, true);
        }
        Player online = Bukkit.getPlayer(id);
        if (online == null || !online.equals(player)) {
            online = player;
        }
        online.setAllowFlight(true);
        if (online.getGameMode() != GameMode.SPECTATOR) {
            online.setFlying(true);
        }
    }

    public void disable(@NotNull Player player) {
        UUID id = player.getUniqueId();
        flying.remove(id);
        if (store != null) {
            store.setFly(id, false);
        }
        Player online = Bukkit.getPlayer(id);
        if (online == null) {
            online = player;
        }
        if (online.getGameMode() == GameMode.CREATIVE || online.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        online.setFlying(false);
        online.setAllowFlight(false);
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

    /**
     * @param disableOnQuit if true, turns fly off and forgets it; if false, keeps it for rejoin
     */
    public void handleQuit(@NotNull Player player, boolean disableOnQuit) {
        if (disableOnQuit) {
            disable(player);
            return;
        }
        // Keep UUID marked — do not remove — so handleJoin restores flight
    }

    public void handleJoin(@NotNull Player player) {
        if (isFlying(player) || (store != null && store.hasFly(player.getUniqueId()))) {
            flying.add(player.getUniqueId());
            enable(player);
        }
    }

    public void clearAll() {
        for (UUID id : Set.copyOf(flying)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                }
            }
        }
        flying.clear();
    }
}
