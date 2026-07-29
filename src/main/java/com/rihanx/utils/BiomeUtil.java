package com.rihanx.utils;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/**
 * Biome resolution and async-friendly search helpers.
 */
public final class BiomeUtil {

    private BiomeUtil() {
    }

    public static @NotNull Registry<Biome> registry() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
    }

    public static @NotNull List<Biome> allBiomes() {
        List<Biome> biomes = new ArrayList<>();
        for (Biome biome : registry()) {
            biomes.add(biome);
        }
        biomes.sort(Comparator.comparing(BiomeUtil::keyString));
        return biomes;
    }

    public static @NotNull List<String> allBiomeKeys() {
        return allBiomes().stream().map(BiomeUtil::keyString).collect(Collectors.toList());
    }

    public static @NotNull String keyString(@NotNull Biome biome) {
        NamespacedKey key = registry().getKey(biome);
        if (key == null) {
            return biome.toString().toLowerCase(Locale.ROOT);
        }
        return key.getKey();
    }

    public static @NotNull String displayName(@NotNull Biome biome) {
        return keyString(biome).replace('_', ' ');
    }

    public static @Nullable Biome match(@NotNull String input) {
        String normalized = normalize(input);
        Registry<Biome> registry = registry();

        Biome exact = registry.get(NamespacedKey.minecraft(normalized));
        if (exact != null) {
            return exact;
        }

        for (Biome biome : registry) {
            String key = keyString(biome);
            if (key.equals(normalized) || key.replace("_", "").equals(normalized.replace("_", ""))) {
                return biome;
            }
        }

        Biome partial = null;
        for (Biome biome : registry) {
            String key = keyString(biome);
            if (key.contains(normalized) || normalize(displayName(biome)).contains(normalized)) {
                if (partial != null) {
                    return null;
                }
                partial = biome;
            }
        }
        return partial;
    }

    public static @NotNull List<String> suggestions(@Nullable String prefix) {
        String normalized = prefix == null ? "" : normalize(prefix);
        return allBiomeKeys().stream()
                .filter(key -> normalized.isEmpty() || key.contains(normalized) || key.replace("_", "").contains(normalized.replace("_", "")))
                .sorted()
                .collect(Collectors.toList());
    }

    public static @NotNull Biome current(@NotNull Player player) {
        return player.getLocation().getBlock().getBiome();
    }

    public static @NotNull Biome at(@NotNull Location location) {
        return location.getBlock().getBiome();
    }

    public static @Nullable Location locate(
            @NotNull Location origin,
            @NotNull Biome target,
            int maxRadiusBlocks,
            int stepBlocks,
            @Nullable BooleanSupplier cancelled,
            @Nullable ProgressCallback progress
    ) {
        if (origin.getWorld() == null) {
            return null;
        }
        if (biomesEqual(at(origin), target)) {
            return origin.clone();
        }

        int totalRings = Math.max(1, maxRadiusBlocks / Math.max(1, stepBlocks));
        int scanned = 0;
        int totalEstimate = 1;
        for (int r = 1; r <= totalRings; r++) {
            totalEstimate += r * 8;
        }

        for (int ring = 1; ring <= totalRings; ring++) {
            int radius = ring * stepBlocks;
            for (int i = -ring; i <= ring; i++) {
                if (cancelled != null && cancelled.getAsBoolean()) {
                    return null;
                }
                Location[] samples = new Location[]{
                        origin.clone().add(i * stepBlocks, 0, -radius),
                        origin.clone().add(i * stepBlocks, 0, radius),
                        origin.clone().add(-radius, 0, i * stepBlocks),
                        origin.clone().add(radius, 0, i * stepBlocks)
                };
                for (Location sample : samples) {
                    scanned++;
                    if (progress != null && scanned % 32 == 0) {
                        progress.onProgress(scanned, totalEstimate);
                    }
                    if (biomesEqual(at(sample), target)) {
                        sample.setY(sample.getWorld().getHighestBlockYAt(sample) + 1.0);
                        return sample;
                    }
                }
            }
        }
        return null;
    }

    public static boolean biomesEqual(@NotNull Biome a, @NotNull Biome b) {
        NamespacedKey keyA = registry().getKey(a);
        NamespacedKey keyB = registry().getKey(b);
        if (keyA != null && keyB != null) {
            return keyA.equals(keyB);
        }
        return a.equals(b);
    }

    public static @NotNull String normalize(@NotNull String input) {
        return input.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int scanned, int total);
    }
}
