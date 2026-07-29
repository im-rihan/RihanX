package com.rihanx.utils;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.StructureType;
import org.bukkit.World;
import org.bukkit.generator.structure.Structure;
import org.bukkit.util.StructureSearchResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Structure registry resolution and locate helpers for Paper 26.2.
 */
public final class StructureUtil {

    private static final Map<String, String> ALIASES = new LinkedHashMap<>();

    static {
        ALIASES.put("village", "village_plains");
        ALIASES.put("villages", "village_plains");
        ALIASES.put("stronghold", "stronghold");
        ALIASES.put("ancient_city", "ancient_city");
        ALIASES.put("ancientcity", "ancient_city");
        ALIASES.put("trial_chamber", "trial_chambers");
        ALIASES.put("trial_chambers", "trial_chambers");
        ALIASES.put("trialchamber", "trial_chambers");
        ALIASES.put("trialchambers", "trial_chambers");
        ALIASES.put("bastion", "bastion_remnant");
        ALIASES.put("bastion_remnant", "bastion_remnant");
        ALIASES.put("fortress", "fortress");
        ALIASES.put("nether_fortress", "fortress");
        ALIASES.put("monument", "monument");
        ALIASES.put("ocean_monument", "monument");
        ALIASES.put("outpost", "pillager_outpost");
        ALIASES.put("pillager_outpost", "pillager_outpost");
        ALIASES.put("shipwreck", "shipwreck");
        ALIASES.put("ruined_portal", "ruined_portal");
        ALIASES.put("ruinedportal", "ruined_portal");
        ALIASES.put("end_city", "end_city");
        ALIASES.put("endcity", "end_city");
        ALIASES.put("mansion", "mansion");
        ALIASES.put("woodland_mansion", "mansion");
        ALIASES.put("ocean_ruins", "ocean_ruin_cold");
        ALIASES.put("ocean_ruin", "ocean_ruin_cold");
        ALIASES.put("buried_treasure", "buried_treasure");
        ALIASES.put("jungle_temple", "jungle_pyramid");
        ALIASES.put("jungle_pyramid", "jungle_pyramid");
        ALIASES.put("swamp_hut", "swamp_hut");
        ALIASES.put("witch_hut", "swamp_hut");
        ALIASES.put("igloo", "igloo");
        ALIASES.put("desert_pyramid", "desert_pyramid");
        ALIASES.put("desert_temple", "desert_pyramid");
        ALIASES.put("trail_ruins", "trail_ruins");
        ALIASES.put("mineshaft", "mineshaft");
    }

    private StructureUtil() {
    }

    public static @NotNull Registry<Structure> registry() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE);
    }

    public static @NotNull List<Structure> allStructures() {
        List<Structure> structures = new ArrayList<>();
        for (Structure structure : registry()) {
            structures.add(structure);
        }
        structures.sort(Comparator.comparing(StructureUtil::keyString));
        return structures;
    }

    public static @NotNull List<String> allStructureKeys() {
        return allStructures().stream().map(StructureUtil::keyString).collect(Collectors.toList());
    }

    public static @NotNull String keyString(@NotNull Structure structure) {
        NamespacedKey key = registry().getKey(structure);
        if (key == null) {
            return structure.toString().toLowerCase(Locale.ROOT);
        }
        return key.getKey();
    }

    public static @NotNull String displayName(@NotNull Structure structure) {
        return keyString(structure).replace('_', ' ');
    }

    public static @Nullable Structure match(@NotNull String input) {
        String normalized = BiomeUtil.normalize(input);
        String aliased = ALIASES.getOrDefault(normalized, normalized);

        Registry<Structure> registry = registry();
        Structure exact = registry.get(NamespacedKey.minecraft(aliased));
        if (exact != null) {
            return exact;
        }

        exact = registry.get(NamespacedKey.minecraft(normalized));
        if (exact != null) {
            return exact;
        }

        Structure partial = null;
        for (Structure structure : registry) {
            String key = keyString(structure);
            if (key.equals(normalized) || key.equals(aliased)) {
                return structure;
            }
            if (key.contains(normalized) || key.contains(aliased) || key.replace("_", "").contains(normalized.replace("_", ""))) {
                if (partial == null) {
                    partial = structure;
                } else if (!keyString(partial).equals(key)) {
                    partial = null;
                    break;
                }
            }
        }
        if (partial != null) {
            return partial;
        }

        if (normalized.equals("village") || aliased.startsWith("village")) {
            for (Structure structure : registry) {
                if (keyString(structure).startsWith("village")) {
                    return structure;
                }
            }
        }
        return null;
    }

    public static @NotNull List<String> suggestions(@Nullable String prefix) {
        String normalized = prefix == null ? "" : BiomeUtil.normalize(prefix);
        List<String> keys = new ArrayList<>(allStructureKeys());
        for (String alias : ALIASES.keySet()) {
            if (!keys.contains(alias)) {
                keys.add(alias);
            }
        }
        return keys.stream()
                .filter(key -> normalized.isEmpty() || key.contains(normalized) || key.replace("_", "").contains(normalized.replace("_", "")))
                .sorted()
                .distinct()
                .collect(Collectors.toList());
    }

    public static @Nullable Location locate(
            @NotNull Location origin,
            @NotNull Structure structure,
            int radiusBlocks
    ) {
        World world = origin.getWorld();
        if (world == null) {
            return null;
        }
        StructureSearchResult result = world.locateNearestStructure(origin, structure, radiusBlocks, false);
        if (result == null) {
            return null;
        }
        Location location = result.getLocation().clone();
        location.setY(world.getHighestBlockYAt(location) + 1.0);
        return location;
    }

    @SuppressWarnings("deprecation")
    public static @Nullable Location locateLegacy(
            @NotNull Location origin,
            @NotNull String name,
            int radiusBlocks
    ) {
        World world = origin.getWorld();
        if (world == null) {
            return null;
        }
        Map<String, StructureType> map = new HashMap<>();
        for (StructureType type : StructureType.getStructureTypes().values()) {
            map.put(type.getName().toLowerCase(Locale.ROOT).replace(' ', '_'), type);
            map.put(type.getKey().getKey(), type);
        }
        String normalized = BiomeUtil.normalize(name);
        String aliased = ALIASES.getOrDefault(normalized, normalized);
        StructureType type = map.get(aliased);
        if (type == null) {
            type = map.get(normalized);
        }
        if (type == null) {
            return null;
        }
        Location found = world.locateNearestStructure(origin, type, radiusBlocks, false);
        if (found == null) {
            return null;
        }
        found.setY(world.getHighestBlockYAt(found) + 1.0);
        return found;
    }
}
