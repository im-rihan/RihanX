package com.rihanx.kingdom;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure Old Kingdom masonry planner. No world mutation — callers apply the plan.
 */
public final class KingdomStructurePlanner {

    public static final Set<String> BANNERS = Set.of(
            "WHITE_BANNER",
            "ORANGE_BANNER",
            "MAGENTA_BANNER",
            "LIGHT_BLUE_BANNER",
            "YELLOW_BANNER",
            "LIME_BANNER",
            "PINK_BANNER",
            "GRAY_BANNER",
            "LIGHT_GRAY_BANNER",
            "CYAN_BANNER",
            "PURPLE_BANNER",
            "BLUE_BANNER",
            "BROWN_BANNER",
            "GREEN_BANNER",
            "RED_BANNER",
            "BLACK_BANNER"
    );

    public static final Set<String> PALETTE;

    static {
        Set<String> palette = new java.util.HashSet<>(Set.of(
                "STONE_BRICKS",
                "CRACKED_STONE_BRICKS",
                "MOSSY_STONE_BRICKS",
                "WAXED_COPPER_BLOCK",
                "POLISHED_DEEPSLATE",
                "LANTERN",
                "SOUL_LANTERN",
                "EMERALD_BLOCK",
                "SEA_LANTERN",
                "IRON_BARS",
                "LADDER",
                "BARREL",
                "WAXED_COPPER_BULB"
        ));
        palette.addAll(BANNERS);
        PALETTE = Set.copyOf(palette);
    }

    public static final int CITADEL_KEEP_CLEARANCE = 15;

    public static boolean isAllowedMaterial(@NotNull String material) {
        return PALETTE.contains(material) || material.endsWith("BANNER");
    }

    public record Planned(int x, int y, int z, @NotNull String material) {
        public long key() {
            return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (y & 0xFFFL);
        }
    }

    private KingdomStructurePlanner() {
    }

    public static @NotNull List<Planned> planKingdom(
            @NotNull Kingdom kingdom,
            int inset,
            int tierSpacing,
            boolean autoLight,
            int worldMaxY
    ) {
        Map<Long, Planned> unique = new LinkedHashMap<>();
        for (KingdomTower tower : kingdom.towers().values()) {
            for (Planned block : planTower(tower, tierSpacing, autoLight, worldMaxY)) {
                unique.put(block.key(), block);
            }
        }
        KingdomTower central = kingdom.tower(TowerType.CENTRAL);
        if (central != null && !kingdom.hasTreasury()) {
            Planned barrel = new Planned(central.x(), central.groundY() + 1, central.z() + 2, "BARREL");
            unique.put(barrel.key(), barrel);
        }
        return new ArrayList<>(unique.values());
    }

    public static @NotNull List<Planned> planTower(
            @NotNull KingdomTower tower,
            int tierSpacing,
            boolean autoLight,
            int worldMaxY
    ) {
        Map<Long, Planned> unique = new LinkedHashMap<>();
        int size = tower.type().isCentral() ? 3 : 2;
        int ground = tower.groundY();
        int top = Math.min(worldMaxY - 2, tower.topY());
        int spacing = Math.max(8, tierSpacing);

        for (int y = ground; y <= top; y++) {
            boolean deck = (y - ground) % spacing == 0;
            boolean railing = (y - ground) % spacing == 1;
            for (int dx = -size; dx <= size; dx++) {
                for (int dz = -size; dz <= size; dz++) {
                    int x = tower.x() + dx;
                    int z = tower.z() + dz;
                    boolean edge = Math.abs(dx) == size || Math.abs(dz) == size;
                    boolean corner = Math.abs(dx) == size && Math.abs(dz) == size;
                    if (y == ground) {
                        put(unique, x, y, z, "POLISHED_DEEPSLATE");
                        continue;
                    }
                    if (railing && edge) {
                        int tier = Math.max(0, (y - ground - 1) / spacing);
                        if (autoLight && tower.isTierLit(tier)) {
                            put(unique, x, y, z, corner ? "SOUL_LANTERN" : "IRON_BARS");
                        } else {
                            put(unique, x, y, z, "IRON_BARS");
                        }
                        continue;
                    }
                    if (deck) {
                        if (edge) {
                            put(unique, x, y, z, "WAXED_COPPER_BLOCK");
                        } else if (dx != 0 || dz != 0) {
                            put(unique, x, y, z, "POLISHED_DEEPSLATE");
                        }
                        continue;
                    }
                    if (edge) {
                        String data = corner
                                ? "WAXED_COPPER_BLOCK"
                                : (((x + y + z) & 3) == 0
                                ? "CRACKED_STONE_BRICKS"
                                : ((x + z) & 1) == 0 ? "MOSSY_STONE_BRICKS" : "STONE_BRICKS");
                        put(unique, x, y, z, data);
                    }
                }
            }
            if (y > ground && y < top && !railing) {
                put(unique, tower.x(), y, tower.z() - size + 1, "LADDER");
            }
        }
        put(unique, tower.x(), top, tower.z(), "SEA_LANTERN");
        put(unique, tower.x(), top + 1, tower.z(), "EMERALD_BLOCK");
        put(unique, tower.x() + 1, top, tower.z(), "WAXED_COPPER_BLOCK");
        put(unique, tower.x() - 1, top, tower.z(), "WAXED_COPPER_BLOCK");
        put(unique, tower.x(), top, tower.z() + 1, "WAXED_COPPER_BLOCK");
        put(unique, tower.x(), top, tower.z() - 1, "WAXED_COPPER_BLOCK");
        return new ArrayList<>(unique.values());
    }

    public static @NotNull List<Planned> planGateLanterns(@NotNull KingdomGate gate, @NotNull String bannerMaterial) {
        Map<Long, Planned> unique = new LinkedHashMap<>();
        put(unique, gate.x() + 2, gate.y() + 2, gate.z(), "LANTERN");
        put(unique, gate.x() - 2, gate.y() + 2, gate.z(), "LANTERN");
        put(unique, gate.x(), gate.y() + 2, gate.z() + 2, "LANTERN");
        put(unique, gate.x(), gate.y() + 2, gate.z() - 2, "LANTERN");
        put(unique, gate.x(), gate.y() - 1, gate.z(), "WAXED_COPPER_BULB");
        String banner = bannerMaterial == null || bannerMaterial.isBlank() ? "ORANGE_BANNER" : bannerMaterial;
        put(unique, gate.x(), gate.y() + 3, gate.z(), banner);
        return new ArrayList<>(unique.values());
    }

    public static void registerDefaultTowers(@NotNull Kingdom kingdom, int inset, int groundY) {
        registerDefaultTowers(kingdom, inset, groundY, true);
    }

    public static void registerDefaultTowers(
            @NotNull Kingdom kingdom,
            int inset,
            int groundY,
            boolean includeCentral
    ) {
        int height = kingdom.towerHeight();
        List<TowerType> types = new ArrayList<>(List.of(
                TowerType.NORTH_WEST,
                TowerType.NORTH_EAST,
                TowerType.SOUTH_WEST,
                TowerType.SOUTH_EAST
        ));
        if (includeCentral) {
            types.add(TowerType.CENTRAL);
        }
        for (TowerType type : types) {
            int[] xz = kingdom.bounds().towerAnchor(type, inset);
            int towerHeight = type.isCentral() ? height + 16 : height;
            KingdomTower tower = new KingdomTower(type, xz[0], xz[1], groundY, towerHeight);
            tower.setAllLit(true);
            kingdom.putTower(tower);
        }
    }

    private static void put(@NotNull Map<Long, Planned> unique, int x, int y, int z, @NotNull String material) {
        Planned planned = new Planned(x, y, z, material);
        unique.put(planned.key(), planned);
    }
}
