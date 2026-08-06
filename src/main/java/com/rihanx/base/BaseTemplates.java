package com.rihanx.base;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Built-in pasteable base / house templates (relative coords, origin at floor center).
 */
public final class BaseTemplates {

    private BaseTemplates() {
    }

    public static @NotNull Map<String, BaseBlueprint> all() {
        Map<String, BaseBlueprint> map = new LinkedHashMap<>();
        map.put("hut", hut());
        map.put("cottage", cottage());
        map.put("bungalow", bungalow());
        map.put("villa", villa());
        map.put("village", village());
        return map;
    }

    public static @NotNull BaseBlueprint hut() {
        List<RelBlock> blocks = new ArrayList<>();
        // 5x5 oak cabin
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                blocks.add(rel(x, 0, z, Material.OAK_PLANKS)); // floor
                boolean wall = x == -2 || x == 2 || z == -2 || z == 2;
                if (wall) {
                    // door opening on +z
                    if (!(z == 2 && x == 0)) {
                        blocks.add(rel(x, 1, z, Material.OAK_LOG));
                        blocks.add(rel(x, 2, z, Material.OAK_LOG));
                    }
                }
                blocks.add(rel(x, 3, z, Material.OAK_SLAB)); // roof
            }
        }
        blocks.add(rel(0, 1, 2, Material.AIR));
        blocks.add(rel(0, 2, 2, Material.AIR));
        blocks.add(rel(0, 1, 1, Material.CRAFTING_TABLE));
        blocks.add(rel(1, 1, 1, Material.CHEST));
        blocks.add(rel(-1, 1, -1, Material.WHITE_BED));
        blocks.add(rel(0, 2, 0, Material.LANTERN));
        return new BaseBlueprint("hut", "Small oak starter hut", blocks);
    }

    public static @NotNull BaseBlueprint cottage() {
        List<RelBlock> blocks = new ArrayList<>();
        // 7x7 spruce cottage + chimney
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                blocks.add(rel(x, 0, z, Material.SPRUCE_PLANKS));
                boolean wall = x == -3 || x == 3 || z == -3 || z == 3;
                if (wall && !(z == 3 && (x == 0 || x == 1))) {
                    blocks.add(rel(x, 1, z, Material.SPRUCE_LOG));
                    blocks.add(rel(x, 2, z, Material.SPRUCE_LOG));
                    if (x == -3 || x == 3 || z == -3) {
                        blocks.add(rel(x, 3, z, Material.SPRUCE_STAIRS));
                    }
                }
                if (Math.abs(x) <= 2 && Math.abs(z) <= 2) {
                    blocks.add(rel(x, 4, z, Material.SPRUCE_SLAB));
                }
            }
        }
        // windows
        blocks.add(rel(-3, 2, 0, Material.GLASS_PANE));
        blocks.add(rel(3, 2, 0, Material.GLASS_PANE));
        blocks.add(rel(0, 2, -3, Material.GLASS_PANE));
        // door clearance
        blocks.add(rel(0, 1, 3, Material.AIR));
        blocks.add(rel(0, 2, 3, Material.AIR));
        blocks.add(rel(1, 1, 3, Material.AIR));
        blocks.add(rel(1, 2, 3, Material.AIR));
        // chimney
        blocks.add(rel(2, 1, -2, Material.BRICKS));
        blocks.add(rel(2, 2, -2, Material.BRICKS));
        blocks.add(rel(2, 3, -2, Material.BRICKS));
        blocks.add(rel(2, 4, -2, Material.BRICKS));
        blocks.add(rel(2, 5, -2, Material.CAMPFIRE));
        // furniture
        blocks.add(rel(-1, 1, -1, Material.RED_BED));
        blocks.add(rel(1, 1, 1, Material.CRAFTING_TABLE));
        blocks.add(rel(2, 1, 1, Material.FURNACE));
        blocks.add(rel(-2, 1, 1, Material.CHEST));
        blocks.add(rel(0, 1, -2, Material.BOOKSHELF));
        blocks.add(rel(0, 2, 0, Material.LANTERN));
        return new BaseBlueprint("cottage", "Cozy spruce cottage with chimney", blocks);
    }

    public static @NotNull BaseBlueprint bungalow() {
        List<RelBlock> blocks = new ArrayList<>();
        // 9x9 stone brick bungalow, open living room
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                blocks.add(rel(x, -1, z, Material.STONE_BRICKS)); // foundation
                blocks.add(rel(x, 0, z, Material.SMOOTH_STONE));
                boolean wall = x == -4 || x == 4 || z == -4 || z == 4;
                if (wall && !(z == 4 && x >= -1 && x <= 1)) {
                    blocks.add(rel(x, 1, z, Material.STONE_BRICKS));
                    blocks.add(rel(x, 2, z, Material.STONE_BRICKS));
                    blocks.add(rel(x, 3, z, Material.STONE_BRICK_SLAB));
                } else if (!wall) {
                    blocks.add(rel(x, 3, z, Material.OAK_PLANKS)); // ceiling
                }
            }
        }
        // porch
        for (int x = -2; x <= 2; x++) {
            blocks.add(rel(x, 0, 5, Material.OAK_SLAB));
            blocks.add(rel(x, 1, 5, Material.OAK_FENCE));
        }
        // windows
        for (int side : new int[]{-4, 4}) {
            blocks.add(rel(side, 2, -1, Material.GLASS));
            blocks.add(rel(side, 2, 1, Material.GLASS));
        }
        blocks.add(rel(-1, 2, -4, Material.GLASS));
        blocks.add(rel(1, 2, -4, Material.GLASS));
        // door gap
        for (int x = -1; x <= 1; x++) {
            blocks.add(rel(x, 1, 4, Material.AIR));
            blocks.add(rel(x, 2, 4, Material.AIR));
        }
        // interior
        blocks.add(rel(-3, 1, -3, Material.WHITE_BED));
        blocks.add(rel(-2, 1, -3, Material.WHITE_BED));
        blocks.add(rel(3, 1, -3, Material.CHEST));
        blocks.add(rel(3, 1, -2, Material.CHEST));
        blocks.add(rel(2, 1, 2, Material.CRAFTING_TABLE));
        blocks.add(rel(3, 1, 2, Material.FURNACE));
        blocks.add(rel(3, 1, 3, Material.BLAST_FURNACE));
        blocks.add(rel(-3, 1, 2, Material.BOOKSHELF));
        blocks.add(rel(-3, 1, 3, Material.ENCHANTING_TABLE));
        blocks.add(rel(0, 2, 0, Material.LANTERN));
        blocks.add(rel(-2, 2, 2, Material.LANTERN));
        blocks.add(rel(2, 2, -2, Material.LANTERN));
        return new BaseBlueprint("bungalow", "Stone bungalow with porch and rooms", blocks);
    }

    public static @NotNull BaseBlueprint villa() {
        List<RelBlock> blocks = new ArrayList<>();
        // 11x11 two-level villa (ground + loft)
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                blocks.add(rel(x, -1, z, Material.DEEPSLATE_BRICKS));
                blocks.add(rel(x, 0, z, Material.DARK_OAK_PLANKS));
                boolean wall = x == -5 || x == 5 || z == -5 || z == 5;
                if (wall && !(z == 5 && x >= -1 && x <= 1)) {
                    blocks.add(rel(x, 1, z, Material.QUARTZ_BLOCK));
                    blocks.add(rel(x, 2, z, Material.QUARTZ_BLOCK));
                    blocks.add(rel(x, 3, z, Material.QUARTZ_BLOCK));
                    blocks.add(rel(x, 4, z, Material.QUARTZ_STAIRS));
                }
                // loft floor partial
                if (Math.abs(x) <= 4 && Math.abs(z) <= 4 && (x <= -1 || z <= -1)) {
                    blocks.add(rel(x, 3, z, Material.DARK_OAK_PLANKS));
                }
            }
        }
        // balcony
        for (int x = -3; x <= 3; x++) {
            blocks.add(rel(x, 3, 5, Material.QUARTZ_SLAB));
            blocks.add(rel(x, 4, 5, Material.OAK_FENCE));
        }
        // windows
        blocks.add(rel(-5, 2, 0, Material.GLASS));
        blocks.add(rel(5, 2, 0, Material.GLASS));
        blocks.add(rel(0, 2, -5, Material.GLASS));
        blocks.add(rel(-3, 2, -5, Material.GLASS));
        blocks.add(rel(3, 2, -5, Material.GLASS));
        // door
        for (int x = -1; x <= 1; x++) {
            blocks.add(rel(x, 1, 5, Material.AIR));
            blocks.add(rel(x, 2, 5, Material.AIR));
        }
        // stairs to loft
        blocks.add(rel(4, 1, 3, Material.DARK_OAK_STAIRS));
        blocks.add(rel(4, 2, 2, Material.DARK_OAK_STAIRS));
        blocks.add(rel(4, 3, 1, Material.DARK_OAK_STAIRS));
        // furniture
        blocks.add(rel(-4, 1, -4, Material.RED_BED));
        blocks.add(rel(-3, 1, -4, Material.RED_BED));
        blocks.add(rel(4, 1, -4, Material.ENDER_CHEST));
        blocks.add(rel(3, 1, -4, Material.CHEST));
        blocks.add(rel(3, 1, -3, Material.CHEST));
        blocks.add(rel(-4, 1, 3, Material.BOOKSHELF));
        blocks.add(rel(-4, 1, 4, Material.ENCHANTING_TABLE));
        blocks.add(rel(2, 1, 3, Material.CRAFTING_TABLE));
        blocks.add(rel(3, 1, 3, Material.FURNACE));
        blocks.add(rel(3, 1, 4, Material.SMOKER));
        blocks.add(rel(0, 1, -3, Material.OAK_STAIRS)); // seating
        blocks.add(rel(1, 1, -3, Material.OAK_STAIRS));
        blocks.add(rel(-1, 1, -3, Material.OAK_STAIRS));
        blocks.add(rel(0, 2, 0, Material.LANTERN));
        blocks.add(rel(-3, 4, -3, Material.LANTERN));
        blocks.add(rel(3, 2, 0, Material.LANTERN));
        // garden posts
        blocks.add(rel(-5, 1, 6, Material.OAK_FENCE));
        blocks.add(rel(5, 1, 6, Material.OAK_FENCE));
        blocks.add(rel(-5, 0, 6, Material.GRASS_BLOCK));
        blocks.add(rel(5, 0, 6, Material.GRASS_BLOCK));
        blocks.add(rel(-5, 1, 7, Material.OAK_LEAVES));
        blocks.add(rel(5, 1, 7, Material.OAK_LEAVES));
        return new BaseBlueprint("villa", "Large quartz villa with loft and balcony", blocks);
    }

    public static @NotNull BaseBlueprint village() {
        List<RelBlock> blocks = new ArrayList<>();
        // Classic villager-style house
        for (int x = -3; x <= 3; x++) {
            for (int z = -2; z <= 3; z++) {
                blocks.add(rel(x, 0, z, Material.COBBLESTONE));
                boolean wall = x == -3 || x == 3 || z == -2 || z == 3;
                if (wall && !(z == 3 && x == 0)) {
                    blocks.add(rel(x, 1, z, Material.OAK_PLANKS));
                    blocks.add(rel(x, 2, z, Material.OAK_PLANKS));
                }
            }
        }
        // roof ridge
        for (int x = -3; x <= 3; x++) {
            blocks.add(rel(x, 3, -1, Material.OAK_STAIRS));
            blocks.add(rel(x, 3, 0, Material.OAK_PLANKS));
            blocks.add(rel(x, 3, 1, Material.OAK_PLANKS));
            blocks.add(rel(x, 3, 2, Material.OAK_STAIRS));
            blocks.add(rel(x, 4, 0, Material.OAK_SLAB));
            blocks.add(rel(x, 4, 1, Material.OAK_SLAB));
        }
        blocks.add(rel(0, 1, 3, Material.AIR));
        blocks.add(rel(0, 2, 3, Material.AIR));
        blocks.add(rel(-3, 2, 0, Material.GLASS_PANE));
        blocks.add(rel(3, 2, 0, Material.GLASS_PANE));
        blocks.add(rel(-1, 1, 1, Material.WHITE_BED));
        blocks.add(rel(1, 1, 1, Material.CRAFTING_TABLE));
        blocks.add(rel(2, 1, 1, Material.CHEST));
        blocks.add(rel(0, 2, 1, Material.LANTERN));
        return new BaseBlueprint("village", "Villager-style house", blocks);
    }

    private static @NotNull RelBlock rel(int dx, int dy, int dz, @NotNull Material material) {
        return new RelBlock(dx, dy, dz, material);
    }

    public record RelBlock(int dx, int dy, int dz, @NotNull Material material) {
    }

    public record BaseBlueprint(
            @NotNull String id,
            @NotNull String description,
            @NotNull List<RelBlock> blocks
    ) {
        public @NotNull String id() {
            return id.toLowerCase(Locale.ROOT);
        }
    }
}
