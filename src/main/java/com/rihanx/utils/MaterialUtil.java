package com.rihanx.utils;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Material name resolution helpers.
 */
public final class MaterialUtil {

    private MaterialUtil() {
    }

    public static @Nullable Material match(@NotNull String input) {
        String normalized = input.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        Material material = Material.matchMaterial(normalized);
        if (material != null) {
            return material;
        }
        material = Material.matchMaterial(normalized, false);
        if (material != null) {
            return material;
        }
        try {
            return Material.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static @NotNull String key(@NotNull Material material) {
        return material.getKey().getKey();
    }

    public static @NotNull List<String> suggestions(@Nullable String prefix) {
        String normalized = prefix == null ? "" : prefix.trim().toLowerCase(Locale.ROOT);
        List<String> results = new ArrayList<>();
        for (Material material : Material.values()) {
            if (!material.isItem() || material.isAir()) {
                continue;
            }
            String key = material.getKey().getKey();
            if (normalized.isEmpty() || key.startsWith(normalized) || key.contains(normalized)) {
                results.add(key);
            }
            if (results.size() >= 50) {
                break;
            }
        }
        return results;
    }

    public static boolean isDeepslateOre(@NotNull Material material) {
        String name = material.name();
        return name.startsWith("DEEPSLATE_") && name.endsWith("_ORE");
    }
}
