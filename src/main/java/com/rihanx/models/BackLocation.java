package com.rihanx.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Stored previous location for the /back command.
 */
public final class BackLocation {

    private final @NotNull UUID playerId;
    private final @NotNull String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final long timestamp;

    public BackLocation(@NotNull UUID playerId, @NotNull Location location, long timestamp) {
        this.playerId = playerId;
        World world = location.getWorld();
        this.worldName = world != null ? world.getName() : "world";
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.timestamp = timestamp;
    }

    public BackLocation(
            @NotNull UUID playerId,
            @NotNull String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            long timestamp
    ) {
        this.playerId = playerId;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.timestamp = timestamp;
    }

    public @NotNull UUID getPlayerId() {
        return playerId;
    }

    public @NotNull String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public @Nullable Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    public void write(@NotNull ConfigurationSection section) {
        section.set("uuid", playerId.toString());
        section.set("world", worldName);
        section.set("x", x);
        section.set("y", y);
        section.set("z", z);
        section.set("yaw", yaw);
        section.set("pitch", pitch);
        section.set("timestamp", timestamp);
    }

    public static @Nullable BackLocation read(@NotNull ConfigurationSection section) {
        String uuidString = section.getString("uuid");
        String world = section.getString("world");
        if (uuidString == null || world == null) {
            return null;
        }
        try {
            return new BackLocation(
                    UUID.fromString(uuidString),
                    world,
                    section.getDouble("x"),
                    section.getDouble("y"),
                    section.getDouble("z"),
                    (float) section.getDouble("yaw"),
                    (float) section.getDouble("pitch"),
                    section.getLong("timestamp")
            );
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
