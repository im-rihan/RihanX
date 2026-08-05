package com.rihanx.managers;

import com.rihanx.utils.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Tracks vanished players and hides them from others.
 */
public final class VanishManager {

    private final @NotNull Plugin plugin;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();
    private @NotNull Consumer<Player> stateListener = player -> { };

    public VanishManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    public void setStateListener(@NotNull Consumer<Player> stateListener) {
        this.stateListener = stateListener;
    }

    public boolean vanish(@NotNull Player player) {
        if (!vanished.add(player.getUniqueId())) {
            return false;
        }
        applyVanish(player);
        stateListener.accept(player);
        return true;
    }

    public boolean unvanish(@NotNull Player player) {
        if (!vanished.remove(player.getUniqueId())) {
            return false;
        }
        reveal(player);
        stateListener.accept(player);
        return true;
    }

    public boolean toggle(@NotNull Player player) {
        if (isVanished(player)) {
            unvanish(player);
            return false;
        }
        vanish(player);
        return true;
    }

    public boolean isVanished(@NotNull UUID playerId) {
        return vanished.contains(playerId);
    }

    public boolean isVanished(@NotNull Player player) {
        return isVanished(player.getUniqueId());
    }

    public @NotNull Set<UUID> getVanished() {
        return Collections.unmodifiableSet(vanished);
    }

    public void applyVanish(@NotNull Player player) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) {
                continue;
            }
            if (!PermissionUtil.canSeeVanished(online)) {
                online.hidePlayer(plugin, player);
            }
        }
    }

    public void reveal(@NotNull Player player) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }
    }

    public void handleJoin(@NotNull Player joining) {
        for (UUID uuid : vanished) {
            Player vanishedPlayer = Bukkit.getPlayer(uuid);
            if (vanishedPlayer == null || vanishedPlayer.equals(joining)) {
                continue;
            }
            if (!PermissionUtil.canSeeVanished(joining)) {
                joining.hidePlayer(plugin, vanishedPlayer);
            }
        }
        if (isVanished(joining)) {
            applyVanish(joining);
        }
    }

    public void handleQuit(@NotNull Player player, boolean unvanishOnQuit) {
        if (unvanishOnQuit) {
            unvanish(player);
        }
    }

    public void unvanishAll() {
        for (UUID uuid : Set.copyOf(vanished)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                unvanish(player);
            } else {
                vanished.remove(uuid);
            }
        }
    }
}
