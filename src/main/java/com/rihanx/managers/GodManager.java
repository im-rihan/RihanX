package com.rihanx.managers;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players in god mode by UUID only (never applies to other players).
 */
public final class GodManager {

    private final Set<UUID> gods = ConcurrentHashMap.newKeySet();

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
        Player online = org.bukkit.Bukkit.getPlayer(id);
        if (online == null) {
            online = player;
        }
        online.setInvulnerable(true);
        online.setFireTicks(0);
    }

    public void disable(@NotNull Player player) {
        UUID id = player.getUniqueId();
        if (!gods.remove(id)) {
            return;
        }
        Player online = org.bukkit.Bukkit.getPlayer(id);
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

    public void handleQuit(@NotNull Player player, boolean disableOnQuit) {
        if (disableOnQuit) {
            disable(player);
        } else {
            // Keep UUID marked but clear invulnerability flag on the leaving entity
            player.setInvulnerable(false);
        }
    }

    public void handleJoin(@NotNull Player player) {
        if (isGod(player)) {
            player.setInvulnerable(true);
            player.setFireTicks(0);
        } else {
            // Ensure leftover invulnerability from other plugins/sessions does not stick
            // only clear if we are not tracking them as god
            if (player.isInvulnerable() && !player.isOp()) {
                // leave ops alone if another plugin set invulnerable
            }
        }
    }

    public void disableAllOnline() {
        for (UUID uuid : Set.copyOf(gods)) {
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player != null) {
                disable(player);
            } else {
                gods.remove(uuid);
            }
        }
    }
}
