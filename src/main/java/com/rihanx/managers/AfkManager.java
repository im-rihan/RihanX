package com.rihanx.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks AFK players and applies a tab-list name suffix.
 */
public final class AfkManager {

    private static final @NotNull Component AFK_SUFFIX = Component.text(" [AFK]", NamedTextColor.GRAY);

    private final Set<UUID> afk = ConcurrentHashMap.newKeySet();

    /** Returns the new AFK state after toggling. */
    public boolean toggle(@NotNull Player player) {
        if (isAfk(player)) {
            setAfk(player, false);
            return false;
        }
        setAfk(player, true);
        return true;
    }

    public void setAfk(@NotNull Player player, boolean value) {
        boolean changed = value ? afk.add(player.getUniqueId()) : afk.remove(player.getUniqueId());
        if (changed) {
            applyListName(player, value);
        }
    }

    /**
     * Clears AFK due to activity (move/interact/chat) without a manual toggle.
     * Returns {@code true} if the player was AFK and is now cleared.
     */
    public boolean clearOnActivity(@NotNull Player player) {
        if (!afk.remove(player.getUniqueId())) {
            return false;
        }
        applyListName(player, false);
        return true;
    }

    public boolean isAfk(@NotNull UUID playerId) {
        return afk.contains(playerId);
    }

    public boolean isAfk(@NotNull Player player) {
        return isAfk(player.getUniqueId());
    }

    public @NotNull Set<UUID> getAfk() {
        return Collections.unmodifiableSet(afk);
    }

    public void handleQuit(@NotNull Player player) {
        afk.remove(player.getUniqueId());
    }

    public void clearAll() {
        for (UUID uuid : Set.copyOf(afk)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                setAfk(player, false);
            } else {
                afk.remove(uuid);
            }
        }
    }

    private void applyListName(@NotNull Player player, boolean value) {
        Component name = Component.text(player.getName());
        player.playerListName(value ? name.append(AFK_SUFFIX) : name);
    }
}
