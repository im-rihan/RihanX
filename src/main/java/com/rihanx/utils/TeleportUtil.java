package com.rihanx.utils;

import com.rihanx.models.PendingTeleport;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Teleport helpers for particles, sounds, and permission checks.
 */
public final class TeleportUtil {

    private TeleportUtil() {
    }

    public static void playArriveEffects(
            @NotNull Player player,
            @NotNull Location location,
            boolean particles,
            boolean sound,
            @Nullable Particle particle,
            int particleCount,
            @Nullable Sound teleportSound,
            float volume,
            float pitch
    ) {
        if (particles && particle != null && location.getWorld() != null) {
            location.getWorld().spawnParticle(particle, location.clone().add(0, 1, 0), particleCount, 0.5, 0.5, 0.5, 0.01);
        }
        if (sound && teleportSound != null) {
            player.playSound(location, teleportSound, volume, pitch);
        }
    }

    public static void playWarmupParticles(
            @NotNull Player player,
            @Nullable Particle particle,
            int count
    ) {
        if (particle == null) {
            return;
        }
        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(particle, loc, count, 0.35, 0.5, 0.35, 0.01);
    }

    public static boolean hasMovedBeyond(@NotNull PendingTeleport pending, @NotNull Player player, double threshold) {
        return pending.hasMoved(player.getLocation(), threshold);
    }

    public static @Nullable Location prepareDestination(@NotNull Location raw, boolean safe, int maxYOffset) {
        if (!safe) {
            return raw.clone();
        }
        return LocationUtil.findSafe(raw, maxYOffset);
    }

    /**
     * Like {@link #prepareDestination} but keeps the exact coordinates when they are already safe.
     * Used for /spawn and warps so players are not shoved to the surface.
     */
    public static @Nullable Location prepareDestinationPreferExact(
            @NotNull Location raw,
            boolean safe,
            int maxYOffset
    ) {
        if (!safe) {
            return raw.clone();
        }
        World world = raw.getWorld();
        if (world == null) {
            return null;
        }
        if (LocationUtil.isSafe(world, raw.getBlockX(), raw.getBlockY(), raw.getBlockZ())) {
            return LocationUtil.centered(
                    world,
                    raw.getBlockX(),
                    raw.getBlockY(),
                    raw.getBlockZ(),
                    raw.getYaw(),
                    raw.getPitch()
            );
        }
        return LocationUtil.findSafe(raw, maxYOffset);
    }
}
