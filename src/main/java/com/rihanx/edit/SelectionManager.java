package com.rihanx.edit;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks pos1/pos2 selections for protect and edit wands.
 */
public final class SelectionManager {

    public enum Channel {
        PROTECT,
        EDIT
    }

    private final Map<UUID, Location> protectPos1 = new ConcurrentHashMap<>();
    private final Map<UUID, Location> protectPos2 = new ConcurrentHashMap<>();
    private final Map<UUID, Location> editPos1 = new ConcurrentHashMap<>();
    private final Map<UUID, Location> editPos2 = new ConcurrentHashMap<>();

    public void setPos1(@NotNull Player player, @NotNull Channel channel, @NotNull Location location) {
        map(channel, true).put(player.getUniqueId(), location.clone());
    }

    public void setPos2(@NotNull Player player, @NotNull Channel channel, @NotNull Location location) {
        map(channel, false).put(player.getUniqueId(), location.clone());
    }

    public @Nullable Location getPos1(@NotNull Player player, @NotNull Channel channel) {
        Location loc = map(channel, true).get(player.getUniqueId());
        return loc == null ? null : loc.clone();
    }

    public @Nullable Location getPos2(@NotNull Player player, @NotNull Channel channel) {
        Location loc = map(channel, false).get(player.getUniqueId());
        return loc == null ? null : loc.clone();
    }

    public @Nullable Cuboid getCuboid(@NotNull Player player, @NotNull Channel channel) {
        Location a = getPos1(player, channel);
        Location b = getPos2(player, channel);
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) {
            return null;
        }
        if (!a.getWorld().equals(b.getWorld())) {
            return null;
        }
        return new Cuboid(a, b);
    }

    public void clear(@NotNull Player player, @NotNull Channel channel) {
        map(channel, true).remove(player.getUniqueId());
        map(channel, false).remove(player.getUniqueId());
    }

    public void clearAll(@NotNull Player player) {
        clear(player, Channel.PROTECT);
        clear(player, Channel.EDIT);
    }

    private @NotNull Map<UUID, Location> map(@NotNull Channel channel, boolean pos1) {
        if (channel == Channel.PROTECT) {
            return pos1 ? protectPos1 : protectPos2;
        }
        return pos1 ? editPos1 : editPos2;
    }
}
