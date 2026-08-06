package com.rihanx.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players in god mode by UUID only.
 * State persists across disconnect/rejoin (and restart via {@link PlayerStateStore}).
 */
public final class GodManager {

    private final Set<UUID> gods = ConcurrentHashMap.newKeySet();
    private @Nullable PlayerStateStore store;

    public void attachStore(@NotNull PlayerStateStore store) {
        this.store = store;
        gods.addAll(store.getGod());
    }

    public boolean toggle(@NotNull Player player) {
        if (isGod(player)) {
            disable(player);
            return false;
        }
        enable(player);
        return true;
    }

    public void enable(@NotNull Player player) {
        UUID id = player.getUniqueId();
        gods.add(id);
        if (store != null) {
            store.setGod(id, true);
        }
        Player online = Bukkit.getPlayer(id);
        if (online == null) {
            online = player;
        }
        online.setInvulnerable(true);
        online.setFireTicks(0);
    }

    public void disable(@NotNull Player player) {
        UUID id = player.getUniqueId();
        if (!gods.remove(id)) {
            if (store != null) {
                store.setGod(id, false);
            }
            return;
        }
        if (store != null) {
            store.setGod(id, false);
        }
        Player online = Bukkit.getPlayer(id);
        if (online == null) {
            online = player;
        }
        online.setInvulnerable(false);
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

    /**
     * @param disableOnQuit if true, turns god off and forgets it; if false, keeps it for rejoin
     */
    public void handleQuit(@NotNull Player player, boolean disableOnQuit) {
        if (disableOnQuit) {
            disable(player);
            return;
        }
        // Keep UUID marked; clear entity flag until they rejoin
        player.setInvulnerable(false);
    }

    public void handleJoin(@NotNull Player player) {
        if (isGod(player) || (store != null && store.hasGod(player.getUniqueId()))) {
            gods.add(player.getUniqueId());
            player.setInvulnerable(true);
            player.setFireTicks(0);
        }
    }

    public void disableAllOnline() {
        for (UUID uuid : Set.copyOf(gods)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                disable(player);
            } else {
                gods.remove(uuid);
                if (store != null) {
                    store.setGod(uuid, false);
                }
            }
        }
    }
}
