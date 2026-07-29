package com.rihanx.world;

import com.rihanx.managers.MessageManager;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * World information and control operations.
 */
public final class WorldService {

    private final @NotNull MessageManager messages;

    public WorldService(@NotNull MessageManager messages) {
        this.messages = messages;
    }

    public void sendInfo(@NotNull CommandSender sender, @NotNull World world) {
        messages.send(sender, "world-info-header", MessageManager.placeholders("world", world.getName()));
        sendLine(sender, "Environment", world.getEnvironment().name());
        sendLine(sender, "Difficulty", world.getDifficulty().name());
        sendLine(sender, "Time", String.valueOf(world.getTime()));
        sendLine(sender, "Players", String.valueOf(world.getPlayers().size()));
        sendLine(sender, "Loaded Chunks", String.valueOf(world.getLoadedChunks().length));
        sendLine(sender, "Seed", String.valueOf(world.getSeed()));
        sendLine(sender, "Border Size", String.format(Locale.US, "%.1f", world.getWorldBorder().getSize()));
    }

    public void sendSeed(@NotNull CommandSender sender, @NotNull World world) {
        messages.send(sender, "world-seed", MessageManager.placeholders("seed", world.getSeed()));
    }

    public void setWeather(@NotNull World world, @NotNull String weather) {
        String normalized = weather.toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "clear", "sun" -> {
                world.setStorm(false);
                world.setThundering(false);
            }
            case "rain" -> {
                world.setStorm(true);
                world.setThundering(false);
            }
            case "thunder", "storm" -> {
                world.setStorm(true);
                world.setThundering(true);
            }
            default -> throw new IllegalArgumentException("Invalid weather: " + weather);
        }
    }

    public void setDifficulty(@NotNull World world, @NotNull String difficulty) {
        world.setDifficulty(Difficulty.valueOf(difficulty.toUpperCase(Locale.ROOT)));
    }

    public void setTime(@NotNull World world, @NotNull String time) {
        String normalized = time.toLowerCase(Locale.ROOT);
        long ticks = switch (normalized) {
            case "day" -> 1000L;
            case "noon" -> 6000L;
            case "night" -> 13000L;
            case "midnight" -> 18000L;
            default -> {
                try {
                    yield Long.parseLong(normalized);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid time: " + time);
                }
            }
        };
        world.setTime(ticks);
    }

    public void sendBorder(@NotNull CommandSender sender, @NotNull World world) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        messages.send(sender, "world-border", MessageManager.placeholders(
                "x", center.getBlockX(),
                "z", center.getBlockZ(),
                "size", String.format(Locale.US, "%.1f", border.getSize())
        ));
    }

    public void sendSpawn(@NotNull CommandSender sender, @NotNull World world) {
        Location spawn = world.getSpawnLocation();
        messages.send(sender, "world-spawn", MessageManager.placeholders(
                "x", spawn.getBlockX(),
                "y", spawn.getBlockY(),
                "z", spawn.getBlockZ()
        ));
    }

    public void setSpawn(@NotNull Player player) {
        World world = player.getWorld();
        world.setSpawnLocation(player.getLocation());
        messages.send(player, "world-setspawn");
    }

    public @Nullable World resolveWorld(@NotNull Player player, @Nullable String name) {
        if (name == null || name.isBlank()) {
            return player.getWorld();
        }
        return org.bukkit.Bukkit.getWorld(name);
    }

    private void sendLine(@NotNull CommandSender sender, @NotNull String key, @NotNull String value) {
        messages.send(sender, "world-info-line", MessageManager.placeholders("key", key, "value", value));
    }
}
