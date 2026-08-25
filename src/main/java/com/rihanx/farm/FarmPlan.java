package com.rihanx.farm;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculated blueprint placements before any world mutation.
 */
public final class FarmPlan {

    private final @NotNull String farmId;
    private final @NotNull String description;
    private final int spawnDx;
    private final int spawnDy;
    private final int spawnDz;
    private final @NotNull List<BlockPlacement> placements;
    private final @NotNull Map<String, String> metadata;

    public FarmPlan(
            @NotNull String farmId,
            @NotNull String description,
            int spawnDx,
            int spawnDy,
            int spawnDz,
            @NotNull List<BlockPlacement> placements,
            @NotNull Map<String, String> metadata
    ) {
        this.farmId = farmId;
        this.description = description;
        this.spawnDx = spawnDx;
        this.spawnDy = spawnDy;
        this.spawnDz = spawnDz;
        this.placements = List.copyOf(placements);
        this.metadata = Map.copyOf(metadata);
    }

    public @NotNull String farmId() {
        return farmId;
    }

    public @NotNull String description() {
        return description;
    }

    public int spawnDx() {
        return spawnDx;
    }

    public int spawnDy() {
        return spawnDy;
    }

    public int spawnDz() {
        return spawnDz;
    }

    public @NotNull List<BlockPlacement> placements() {
        return placements;
    }

    public @NotNull Map<String, String> metadata() {
        return metadata;
    }

    public int width() {
        if (placements.isEmpty()) {
            return 0;
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (BlockPlacement p : placements) {
            min = Math.min(min, p.dx());
            max = Math.max(max, p.dx());
        }
        return max - min + 1;
    }

    public int length() {
        if (placements.isEmpty()) {
            return 0;
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (BlockPlacement p : placements) {
            min = Math.min(min, p.dz());
            max = Math.max(max, p.dz());
        }
        return max - min + 1;
    }

    public int height() {
        if (placements.isEmpty()) {
            return 0;
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (BlockPlacement p : placements) {
            min = Math.min(min, p.dy());
            max = Math.max(max, p.dy());
        }
        return max - min + 1;
    }

    public int minY() {
        return placements.stream().mapToInt(BlockPlacement::dy).min().orElse(0);
    }

    public int maxY() {
        return placements.stream().mapToInt(BlockPlacement::dy).max().orElse(0);
    }

    public @NotNull Map<Material, Integer> materialTotals() {
        Map<Material, Integer> totals = new EnumMap<>(Material.class);
        for (BlockPlacement p : placements) {
            totals.merge(p.material(), 1, Integer::sum);
        }
        return totals;
    }

    public int countMaterial(@NotNull Material material) {
        int n = 0;
        for (BlockPlacement p : placements) {
            if (p.material() == material) {
                n++;
            }
        }
        return n;
    }

    public int functionalCount() {
        int n = 0;
        for (BlockPlacement p : placements) {
            if (isFunctional(p.material())) {
                n++;
            }
        }
        return n;
    }

    public int containerCount() {
        int n = 0;
        for (BlockPlacement p : placements) {
            String name = p.material().name();
            if (name.contains("CHEST") || name.equals("BARREL") || name.equals("HOPPER")
                    || name.equals("DISPENSER") || name.equals("DROPPER")
                    || name.equals("SMOKER") || name.equals("FURNACE") || name.equals("BLAST_FURNACE")) {
                n++;
            }
        }
        return n;
    }

    public int redstoneCount() {
        int n = 0;
        for (BlockPlacement p : placements) {
            String name = p.material().name();
            if (name.contains("REDSTONE") || name.contains("REPEATER") || name.contains("COMPARATOR")
                    || name.contains("OBSERVER") || name.contains("PISTON")
                    || name.equals("LEVER") || name.contains("BUTTON")
                    || name.equals("DAYLIGHT_DETECTOR")) {
                n++;
            }
        }
        return n;
    }

    public static boolean isFunctional(@NotNull Material material) {
        String name = material.name();
        return name.equals("HOPPER") || name.contains("CHEST") || name.equals("BARREL")
                || name.contains("OBSERVER") || name.contains("PISTON")
                || name.equals("DISPENSER") || name.equals("DROPPER")
                || name.equals("COMPOSTER") || name.equals("SPAWNER")
                || name.equals("SMOKER") || name.equals("FURNACE")
                || name.equals("WATER") || name.equals("LAVA")
                || name.contains("RAIL") || name.equals("IRON_BARS");
    }

    public static @NotNull List<BlockPlacement> dedupeLastWins(@NotNull List<BlockPlacement> input) {
        Map<Long, BlockPlacement> map = new LinkedHashMap<>();
        for (BlockPlacement p : input) {
            map.put(p.packKey(), p);
        }
        return new ArrayList<>(map.values());
    }

    public @NotNull Map<Long, BlockPlacement> indexByCoord() {
        Map<Long, BlockPlacement> map = new HashMap<>();
        for (BlockPlacement p : placements) {
            map.put(p.packKey(), p);
        }
        return map;
    }

    public static @NotNull Builder builder(@NotNull String farmId, @NotNull String description) {
        return new Builder(farmId, description);
    }

    public static final class Builder {
        private final String farmId;
        private final String description;
        private int spawnDx;
        private int spawnDy;
        private int spawnDz;
        private final List<BlockPlacement> placements = new ArrayList<>();
        private final Map<String, String> metadata = new LinkedHashMap<>();

        private Builder(@NotNull String farmId, @NotNull String description) {
            this.farmId = farmId;
            this.description = description;
        }

        public @NotNull Builder spawn(int dx, int dy, int dz) {
            this.spawnDx = dx;
            this.spawnDy = dy;
            this.spawnDz = dz;
            return this;
        }

        public @NotNull Builder meta(@NotNull String key, @NotNull String value) {
            metadata.put(key, value);
            return this;
        }

        public @NotNull Builder add(@NotNull BlockPlacement placement) {
            placements.add(placement);
            return this;
        }

        public @NotNull FarmPlan build() {
            return new FarmPlan(farmId, description, spawnDx, spawnDy, spawnDz,
                    dedupeLastWins(placements), metadata);
        }
    }
}
