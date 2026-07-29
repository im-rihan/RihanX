package com.rihanx.models;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * In-progress teleport warmup state for a player.
 */
public final class PendingTeleport {

    private final @NotNull UUID playerId;
    private final @NotNull Location destination;
    private final @NotNull Location startLocation;
    private final int delayTicks;
    private final long startedAt;
    private final @NotNull String reason;

    public PendingTeleport(
            @NotNull Player player,
            @NotNull Location destination,
            int delayTicks,
            @NotNull String reason
    ) {
        this.playerId = player.getUniqueId();
        this.destination = destination.clone();
        this.startLocation = player.getLocation().clone();
        this.delayTicks = delayTicks;
        this.startedAt = System.currentTimeMillis();
        this.reason = reason;
    }

    public @NotNull UUID getPlayerId() {
        return playerId;
    }

    public @NotNull Location getDestination() {
        return destination.clone();
    }

    public @NotNull Location getStartLocation() {
        return startLocation.clone();
    }

    public int getDelayTicks() {
        return delayTicks;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public @NotNull String getReason() {
        return reason;
    }

    public boolean hasMoved(@NotNull Location current, double threshold) {
        if (current.getWorld() == null || startLocation.getWorld() == null) {
            return true;
        }
        if (!current.getWorld().equals(startLocation.getWorld())) {
            return true;
        }
        return current.distanceSquared(startLocation) > threshold * threshold;
    }
}
