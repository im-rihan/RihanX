package com.rihanx.base;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Slab;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Train station / railway blueprints (pasted like farms via {@link BaseService}).
 * Local space: tracks run along +Z (front).
 */
public final class StationTemplates {

    private StationTemplates() {
    }

    public static @NotNull Map<String, BaseTemplates.BaseBlueprint> all() {
        Map<String, BaseTemplates.BaseBlueprint> map = new LinkedHashMap<>();
        map.put("station", station());
        map.put("depot", depot());
        map.put("crossing", crossing());
        map.put("rail", rail());
        map.put("terminal", terminal());
        map.put("mine", mine());
        return map;
    }

    /** Small passenger platform with waiting room, rails, and chest. */
    public static @NotNull BaseTemplates.BaseBlueprint station() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();

        // Platform deck
        for (int x = -4; x <= 4; x++) {
            for (int z = -2; z <= 8; z++) {
                b.set(x, -1, z, Material.STONE_BRICKS);
                b.set(x, 0, z, Material.SMOOTH_STONE);
            }
        }
        // Track bed + rails down the center
        for (int z = -2; z <= 8; z++) {
            b.set(-1, 0, z, Material.GRAVEL);
            b.set(0, 0, z, Material.GRAVEL);
            b.set(1, 0, z, Material.GRAVEL);
            b.set(0, 1, z, Material.RAIL);
            if ((z & 1) == 0) {
                b.set(-1, 1, z, Material.POWERED_RAIL);
                b.set(1, 1, z, Material.RAIL);
            } else {
                b.set(-1, 1, z, Material.RAIL);
                b.set(1, 1, z, Material.POWERED_RAIL);
            }
        }
        // Redstone under every other powered rail
        for (int z = -2; z <= 8; z += 2) {
            b.set(-1, -1, z, Material.REDSTONE_BLOCK);
            b.set(1, -1, z, Material.REDSTONE_BLOCK);
        }

        // Waiting pavilion on +X side
        for (int x = 2; x <= 4; x++) {
            for (int z = 2; z <= 6; z++) {
                b.set(x, 0, z, Material.SPRUCE_PLANKS);
            }
        }
        for (int y = 1; y <= 3; y++) {
            b.set(2, y, 2, Material.SPRUCE_LOG);
            b.set(4, y, 2, Material.SPRUCE_LOG);
            b.set(2, y, 6, Material.SPRUCE_LOG);
            b.set(4, y, 6, Material.SPRUCE_LOG);
        }
        for (int x = 2; x <= 4; x++) {
            for (int z = 2; z <= 6; z++) {
                b.set(x, 4, z, Material.SPRUCE_SLAB);
            }
        }
        b.stairs(3, 1, 3, Material.OAK_STAIRS, BlockFace.EAST);
        b.facing(3, 1, 5, Material.CHEST, BlockFace.WEST);
        b.set(4, 1, 4, Material.CRAFTING_TABLE);
        b.set(3, 4, 4, Material.SPRUCE_PLANKS);
        b.hangingLantern(3, 3, 4, Material.LANTERN, 4);

        // Platform edge fences
        for (int z = -2; z <= 8; z++) {
            b.set(-4, 1, z, Material.OAK_FENCE);
            b.set(4, 1, z, Material.OAK_FENCE);
        }
        b.set(0, 1, -2, Material.AIR); // keep rail clear
        spawnPad(b, 0, 10);
        spawnPad(b, 0, 11);
        linkPad(b, 2, 10);
        b.set(-2, 0, 11, Material.BARREL);
        b.set(2, 0, 11, Material.COMPOSTER);
        return b.build("station", "Train station - platform, powered rails, waiting pavilion, link pad", 0, 0, 11);
    }

    /** Larger depot with storage, furnace, and minecart bay. */
    public static @NotNull BaseTemplates.BaseBlueprint depot() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        for (int x = -6; x <= 6; x++) {
            for (int z = -3; z <= 10; z++) {
                b.set(x, -1, z, Material.COBBLESTONE);
                b.set(x, 0, z, Material.STONE_BRICKS);
            }
        }
        for (int z = -3; z <= 10; z++) {
            b.set(0, 0, z, Material.GRAVEL);
            b.set(0, 1, z, z % 2 == 0 ? Material.POWERED_RAIL : Material.RAIL);
            if (z % 2 == 0) {
                b.set(0, -1, z, Material.REDSTONE_BLOCK);
            }
        }
        // Depot shed
        for (int x = -6; x <= -2; x++) {
            for (int z = 1; z <= 7; z++) {
                b.set(x, 0, z, Material.SPRUCE_PLANKS);
                boolean wall = x == -6 || x == -2 || z == 1 || z == 7;
                if (wall) {
                    b.set(x, 1, z, Material.SPRUCE_LOG);
                    b.set(x, 2, z, Material.SPRUCE_PLANKS);
                    b.set(x, 3, z, Material.SPRUCE_PLANKS);
                } else {
                    b.set(x, 1, z, Material.AIR);
                    b.set(x, 2, z, Material.AIR);
                }
                b.set(x, 4, z, Material.SPRUCE_PLANKS);
            }
        }
        b.door(-2, 1, 4, Material.SPRUCE_DOOR, BlockFace.EAST);
        b.facing(-4, 1, 3, Material.CHEST, BlockFace.SOUTH);
        b.facing(-5, 1, 3, Material.CHEST, BlockFace.SOUTH);
        b.set(-4, 1, 5, Material.BARREL);
        b.set(-5, 1, 5, Material.BARREL);
        b.facing(-4, 1, 6, Material.FURNACE, BlockFace.SOUTH);
        b.set(-5, 1, 6, Material.CRAFTING_TABLE);
        b.set(-4, 4, 4, Material.SPRUCE_PLANKS);
        b.hangingLantern(-4, 3, 4, Material.LANTERN, 4);

        // Minecart bay markers (place minecarts after paste)
        b.set(2, 1, 4, Material.RAIL);
        b.set(3, 1, 4, Material.DETECTOR_RAIL);
        b.set(4, 1, 4, Material.RAIL);
        b.set(3, 0, 4, Material.GRAVEL);
        b.set(2, 0, 4, Material.GRAVEL);
        b.set(4, 0, 4, Material.GRAVEL);

        spawnPad(b, 0, 12);
        spawnPad(b, 0, 13);
        linkPad(b, 2, 12);
        return b.build("depot", "Rail depot - shed storage, powered track, minecart bay, link pad", 0, 0, 13);
    }

    /** Four-way rail crossing with signal posts. */
    public static @NotNull BaseTemplates.BaseBlueprint crossing() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                b.set(x, -1, z, Material.STONE);
                b.set(x, 0, z, Material.GRAVEL);
            }
        }
        for (int i = -5; i <= 5; i++) {
            b.set(0, 1, i, Material.RAIL);
            b.set(i, 1, 0, Material.RAIL);
            if (Math.abs(i) >= 2 && (i & 1) == 0) {
                b.set(0, 1, i, Material.POWERED_RAIL);
                b.set(0, -1, i, Material.REDSTONE_BLOCK);
                b.set(i, 1, 0, Material.POWERED_RAIL);
                b.set(i, -1, 0, Material.REDSTONE_BLOCK);
            }
        }
        // Crossing center
        b.set(0, 1, 0, Material.RAIL);
        for (int side : new int[]{-3, 3}) {
            b.set(side, 1, side, Material.OAK_FENCE);
            b.set(side, 2, side, Material.OAK_FENCE);
            b.set(side, 3, side, Material.REDSTONE_LAMP);
            b.set(side, 4, side, Material.STONE_BUTTON);
        }
        spawnPad(b, 0, 7);
        spawnPad(b, 0, 8);
        b.facing(1, 0, 8, Material.CHEST, BlockFace.SOUTH);
        return b.build("crossing", "Rail crossing - 4-way tracks, signal lamps", 0, 0, 8);
    }

    /** Straight powered rail segment for linking stations. */
    public static @NotNull BaseTemplates.BaseBlueprint rail() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int len = 24;
        for (int z = 0; z <= len; z++) {
            for (int x = -1; x <= 1; x++) {
                b.set(x, -1, z, Material.STONE);
                b.set(x, 0, z, Material.GRAVEL);
            }
            b.set(0, 1, z, z % 2 == 0 ? Material.POWERED_RAIL : Material.RAIL);
            if (z % 2 == 0) {
                b.set(0, -1, z, Material.REDSTONE_BLOCK);
            }
            if (z % 4 == 0) {
                b.set(-2, 0, z, Material.OAK_FENCE);
                b.set(-2, 1, z, Material.TORCH);
                b.set(2, 0, z, Material.OAK_FENCE);
                b.set(2, 1, z, Material.TORCH);
            }
        }
        spawnPad(b, 0, len + 2);
        spawnPad(b, 0, len + 3);
        b.facing(1, 0, len + 3, Material.BARREL, BlockFace.SOUTH);
        return b.build("rail", "Powered railway segment (~25 blocks) with torch posts", 0, 0, len + 3);
    }

    /** End-of-line terminal with buffers and ticket booth. */
    public static @NotNull BaseTemplates.BaseBlueprint terminal() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        for (int x = -5; x <= 5; x++) {
            for (int z = -2; z <= 10; z++) {
                b.set(x, -1, z, Material.DEEPSLATE_BRICKS);
                b.set(x, 0, z, Material.POLISHED_DEEPSLATE);
            }
        }
        for (int z = -2; z <= 8; z++) {
            b.set(0, 0, z, Material.GRAVEL);
            b.set(0, 1, z, z % 2 == 0 ? Material.POWERED_RAIL : Material.RAIL);
            if (z % 2 == 0) {
                b.set(0, -1, z, Material.REDSTONE_BLOCK);
            }
        }
        // Buffer stop
        b.set(-1, 1, 9, Material.IRON_BLOCK);
        b.set(0, 1, 9, Material.IRON_BLOCK);
        b.set(1, 1, 9, Material.IRON_BLOCK);
        b.set(0, 2, 9, Material.IRON_BARS);
        // Ticket booth
        for (int x = 2; x <= 5; x++) {
            for (int z = 3; z <= 7; z++) {
                b.set(x, 0, z, Material.SPRUCE_PLANKS);
                boolean wall = x == 2 || x == 5 || z == 3 || z == 7;
                if (wall) {
                    b.set(x, 1, z, Material.SPRUCE_PLANKS);
                    b.set(x, 2, z, Material.SPRUCE_PLANKS);
                }
                b.set(x, 3, z, Material.SPRUCE_SLAB);
            }
        }
        b.door(2, 1, 5, Material.SPRUCE_DOOR, BlockFace.WEST);
        b.facing(4, 1, 5, Material.CHEST, BlockFace.WEST);
        b.set(3, 1, 4, Material.BARREL);
        b.set(4, 1, 4, Material.CRAFTING_TABLE);
        b.slab(3, 1, 6, Material.SPRUCE_SLAB, Slab.Type.BOTTOM);
        b.set(3, 3, 5, Material.SPRUCE_PLANKS);
        b.hangingLantern(3, 2, 5, Material.LANTERN, 3);

        spawnPad(b, 0, 12);
        spawnPad(b, 0, 13);
        linkPad(b, 2, 12);
        return b.build("terminal", "Rail terminal - buffer stop, ticket booth, powered track, link pad", 0, 0, 13);
    }

    /**
     * Surface station + descending lined tunnel to a mining outpost.
     * Face the mountain / dig direction when pasting; tunnel runs along +Z and drops ~1Y / 3 blocks.
     */
    public static @NotNull BaseTemplates.BaseBlueprint mine() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();

        // Surface platform
        for (int x = -4; x <= 4; x++) {
            for (int z = -2; z <= 6; z++) {
                b.set(x, -1, z, Material.DEEPSLATE_BRICKS);
                b.set(x, 0, z, Material.POLISHED_DEEPSLATE);
            }
        }
        for (int z = -2; z <= 6; z++) {
            b.set(0, 0, z, Material.GRAVEL);
            b.set(0, 1, z, z % 2 == 0 ? Material.POWERED_RAIL : Material.RAIL);
            if (z % 2 == 0) {
                b.set(0, -1, z, Material.REDSTONE_BLOCK);
            }
        }
        // Waiting shed
        for (int x = 2; x <= 4; x++) {
            for (int z = 1; z <= 4; z++) {
                b.set(x, 0, z, Material.SPRUCE_PLANKS);
                boolean wall = x == 2 || x == 4 || z == 1 || z == 4;
                if (wall) {
                    b.set(x, 1, z, Material.SPRUCE_LOG);
                    b.set(x, 2, z, Material.SPRUCE_LOG);
                }
                b.set(x, 3, z, Material.SPRUCE_SLAB);
            }
        }
        b.door(2, 1, 2, Material.SPRUCE_DOOR, BlockFace.WEST);
        b.facing(3, 1, 3, Material.CHEST, BlockFace.WEST);
        b.set(3, 3, 2, Material.SPRUCE_PLANKS);
        b.hangingLantern(3, 2, 2, Material.LANTERN, 3);

        // Descending mining tunnel along +Z (3×3 clear, deepslate lining)
        int tunnelStart = 7;
        int tunnelLen = 42;
        int dropEvery = 3;
        for (int i = 0; i <= tunnelLen; i++) {
            int z = tunnelStart + i;
            int y = -(i / dropEvery); // drop 1 block every 3 along the tunnel
            for (int x = -2; x <= 2; x++) {
                for (int dy = -1; dy <= 3; dy++) {
                    boolean shell = x == -2 || x == 2 || dy == -1 || dy == 3;
                    boolean railBed = x == 0 && dy == 0;
                    boolean clear = Math.abs(x) <= 1 && dy >= 0 && dy <= 2;
                    if (shell) {
                        b.set(x, y + dy, z, Material.DEEPSLATE);
                    } else if (railBed) {
                        b.set(x, y + dy, z, Material.GRAVEL);
                    } else if (clear) {
                        b.set(x, y + dy, z, Material.AIR);
                    }
                }
            }
            b.set(0, y + 1, z, i % 2 == 0 ? Material.POWERED_RAIL : Material.RAIL);
            if (i % 2 == 0) {
                b.set(0, y - 1, z, Material.REDSTONE_BLOCK);
            }
            if (i % 6 == 0) {
                b.facing(-1, y + 1, z, Material.WALL_TORCH, BlockFace.EAST);
                b.facing(1, y + 1, z, Material.WALL_TORCH, BlockFace.WEST);
            }
        }

        int endZ = tunnelStart + tunnelLen;
        int endY = -(tunnelLen / dropEvery);
        // Mining outpost chamber
        for (int x = -5; x <= 5; x++) {
            for (int z = endZ; z <= endZ + 8; z++) {
                for (int dy = -1; dy <= 4; dy++) {
                    boolean shell = x == -5 || x == 5 || z == endZ || z == endZ + 8 || dy == -1 || dy == 4;
                    if (shell) {
                        b.set(x, endY + dy, z, Material.DEEPSLATE_BRICKS);
                    } else {
                        b.set(x, endY + dy, z, Material.AIR);
                    }
                }
                b.set(x, endY, z, Material.COBBLESTONE);
            }
        }
        // Open tunnel mouth into chamber
        for (int dy = 0; dy <= 2; dy++) {
            for (int x = -1; x <= 1; x++) {
                b.set(x, endY + dy, endZ, Material.AIR);
            }
        }
        // Rails into chamber + buffer
        for (int z = endZ; z <= endZ + 4; z++) {
            b.set(0, endY, z, Material.GRAVEL);
            b.set(0, endY + 1, z, z % 2 == 0 ? Material.POWERED_RAIL : Material.RAIL);
        }
        b.set(-1, endY + 1, endZ + 5, Material.IRON_BLOCK);
        b.set(0, endY + 1, endZ + 5, Material.IRON_BLOCK);
        b.set(1, endY + 1, endZ + 5, Material.IRON_BLOCK);

        // Mining gadgets
        b.facing(-3, endY + 1, endZ + 3, Material.CHEST, BlockFace.EAST);
        b.facing(-3, endY + 1, endZ + 4, Material.CHEST, BlockFace.EAST);
        b.facing(-3, endY + 1, endZ + 5, Material.BARREL, BlockFace.EAST);
        b.set(3, endY + 1, endZ + 3, Material.BLAST_FURNACE);
        b.set(3, endY + 1, endZ + 4, Material.FURNACE);
        b.set(3, endY + 1, endZ + 5, Material.CRAFTING_TABLE);
        b.set(2, endY + 1, endZ + 6, Material.ANVIL);
        b.set(0, endY + 4, endZ + 4, Material.DEEPSLATE_BRICKS);
        b.hangingLantern(0, endY + 3, endZ + 4, Material.LANTERN, endY + 4);
        b.set(-2, endY + 1, endZ + 6, Material.TORCH);
        b.set(2, endY + 1, endZ + 6, Material.TORCH);

        spawnPad(b, 0, 5);
        spawnPad(b, 0, 6);
        linkPad(b, 2, 5);
        b.set(-2, 0, 6, Material.BARREL);
        return b.build(
                "mine",
                "Mine station - surface stop, descending rail tunnel, mining outpost with chests/furnaces",
                0, 0, 6
        );
    }

    private static void spawnPad(@NotNull BaseTemplates.Builder b, int x, int z) {
        b.set(x, -1, z, Material.DIRT_PATH);
        b.set(x, 0, z, Material.AIR);
        b.set(x, 1, z, Material.AIR);
    }

    /**
     * Deepslate + gold pressure plate — stand here after {@code /station set <name>} / paste-with-name
     * and once linked with {@code /station link}, it teleports to the other stop.
     */
    private static void linkPad(@NotNull BaseTemplates.Builder b, int x, int z) {
        b.set(x, -1, z, Material.POLISHED_DEEPSLATE);
        b.set(x, 0, z, Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
        b.set(x, 1, z, Material.AIR);
        b.set(x + 1, -1, z, Material.AMETHYST_BLOCK);
        b.set(x - 1, -1, z, Material.AMETHYST_BLOCK);
    }
}
