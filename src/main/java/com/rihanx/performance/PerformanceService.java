package com.rihanx.performance;

import com.rihanx.managers.ConfigManager;
import com.rihanx.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.management.ManagementFactory;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Server and world performance reporting.
 */
public final class PerformanceService {

    private final @NotNull MessageManager messages;
    private final @NotNull ConfigManager config;
    public PerformanceService(@NotNull MessageManager messages, @NotNull ConfigManager config) {
        this.messages = messages;
        this.config = config;
    }

    public void sendServerInfo(@NotNull CommandSender sender) {
        messages.send(sender, "server-info-header");
        sendLine(sender, "Version", Bukkit.getVersion());
        sendLine(sender, "Bukkit", Bukkit.getBukkitVersion());
        sendLine(sender, "Players", Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        sendLine(sender, "Worlds", String.valueOf(Bukkit.getWorlds().size()));
        sendLine(sender, "Java", System.getProperty("java.version"));
    }

    public void sendTps(@NotNull CommandSender sender) {
        double[] tps = Bukkit.getTPS();
        messages.send(sender, "server-tps", MessageManager.placeholders(
                "tps1", formatTps(tps.length > 0 ? tps[0] : 20.0),
                "tps5", formatTps(tps.length > 1 ? tps[1] : 20.0),
                "tps15", formatTps(tps.length > 2 ? tps[2] : 20.0)
        ));
    }

    public void sendMspt(@NotNull CommandSender sender) {
        double mspt = Bukkit.getAverageTickTime();
        messages.send(sender, "server-mspt", MessageManager.placeholders("mspt", String.format(Locale.US, "%.2f", mspt)));
    }

    public void sendMemory(@NotNull CommandSender sender) {
        Runtime runtime = Runtime.getRuntime();
        long used = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long max = runtime.maxMemory() / (1024 * 1024);
        messages.send(sender, "server-memory", MessageManager.placeholders("used", used, "max", max));
    }

    public void sendUptime(@NotNull CommandSender sender) {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long days = TimeUnit.MILLISECONDS.toDays(uptimeMs);
        long hours = TimeUnit.MILLISECONDS.toHours(uptimeMs) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMs) % 60;
        String formatted = days + "d " + hours + "h " + minutes + "m";
        messages.send(sender, "server-uptime", MessageManager.placeholders("uptime", formatted));
    }

    public void sendNearbyChunks(@NotNull Player player) {
        int radius = config.getChunkReportRadius();
        World world = player.getWorld();
        int px = player.getLocation().getBlockX() >> 4;
        int pz = player.getLocation().getBlockZ() >> 4;
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (world.isChunkLoaded(px + dx, pz + dz)) {
                    count++;
                }
            }
        }
        messages.send(player, "performance-chunks", MessageManager.placeholders("count", count));
    }

    public void sendEntityBreakdown(@NotNull Player player) {
        int radius = config.getChunkReportRadius() * 16;
        Map<String, Integer> counts = new HashMap<>();
        int total = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            String type = entity.getType().name();
            counts.merge(type, 1, Integer::sum);
            total++;
        }
        List<Map.Entry<String, Integer>> top = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(config.getEntityReportLimit())
                .collect(Collectors.toList());
        messages.send(player, "performance-entities", MessageManager.placeholders(
                "count", total,
                "shown", top.size()
        ));
        for (Map.Entry<String, Integer> entry : top) {
            messages.send(player, "performance-entity-line", MessageManager.placeholders(
                    "type", entry.getKey(),
                    "count", entry.getValue()
            ));
        }
    }

    private void sendLine(@NotNull CommandSender sender, @NotNull String key, @NotNull String value) {
        messages.send(sender, "server-info-line", MessageManager.placeholders("key", key, "value", value));
    }

    private @NotNull String formatTps(double tps) {
        return String.format(Locale.US, "%.2f", Math.min(20.0, tps));
    }
}
