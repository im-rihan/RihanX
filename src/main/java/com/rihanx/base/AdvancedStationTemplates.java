package com.rihanx.base;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Slab;
import org.jetbrains.annotations.NotNull;

/**
 * GrabCraft-inspired advanced stations (original procedural builds, not blueprint copies).
 * <p>
 * Cues from:
 * <ul>
 *   <li>Dark Kingdom Train Station — andesite hall, blue accents, multi-track</li>
 *   <li>Western Train Station — spruce porch / ticket house</li>
 *   <li>Adacia Train Station — compact dark oak stop</li>
 * </ul>
 * Local space: tracks run along +Z (front). Each build includes a side yard throat
 * so {@code /station link} can exit sideways without a diagonal gap.
 */
public final class AdvancedStationTemplates {

    private AdvancedStationTemplates() {
    }

    public static void register(@NotNull java.util.Map<String, BaseTemplates.BaseBlueprint> map) {
        map.put("kingdom", kingdom());
        map.put("western", western());
        map.put("adacia", adacia());
        map.put("yard", yard());
    }

    /**
     * Modern andesite hall (Dark Kingdom vibe) — dual platforms, waiting room, canopy, side throat.
     */
    public static @NotNull BaseTemplates.BaseBlueprint kingdom() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int minX = -8;
        int maxX = 10;
        int minZ = -4;
        int maxZ = 16;

        // Ground + platform deck
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                b.set(x, -1, z, Material.ANDESITE);
                b.set(x, 0, z, Material.POLISHED_ANDESITE);
            }
        }
        // Blue terracotta accents (stained clay → terracotta)
        for (int z = minZ; z <= maxZ; z++) {
            b.set(minX, 0, z, Material.BLUE_TERRACOTTA);
            b.set(maxX, 0, z, Material.CYAN_TERRACOTTA);
        }

        // Dual tracks at x=-2 and x=2 (center aisle)
        placeTrack(b, -2, minZ, maxZ - 2);
        placeTrack(b, 2, minZ, maxZ - 2);
        // Center aisle floor
        for (int z = minZ; z <= maxZ - 2; z++) {
            b.set(0, 0, z, Material.SMOOTH_STONE);
            b.set(-1, 0, z, Material.SMOOTH_STONE);
            b.set(1, 0, z, Material.SMOOTH_STONE);
        }

        // Side yard throat: rails from center track out east toward link / plain joins
        yardThroat(b, 2, maxZ - 4, 6);

        // Waiting hall on +X
        for (int x = 5; x <= 9; x++) {
            for (int z = 4; z <= 12; z++) {
                b.set(x, 0, z, Material.POLISHED_ANDESITE);
                boolean wall = x == 5 || x == 9 || z == 4 || z == 12;
                for (int y = 1; y <= 4; y++) {
                    if (wall) {
                        b.set(x, y, z, y == 4 ? Material.STONE_BRICKS : Material.ANDESITE);
                    } else {
                        b.set(x, y, z, Material.AIR);
                    }
                }
                b.set(x, 5, z, Material.STONE_BRICK_SLAB);
            }
        }
        // Windows + door
        for (int z = 6; z <= 10; z += 2) {
            b.set(5, 2, z, Material.WHITE_STAINED_GLASS_PANE);
            b.set(9, 2, z, Material.WHITE_STAINED_GLASS_PANE);
        }
        b.door(5, 1, 8, Material.SPRUCE_DOOR, BlockFace.WEST);
        b.facing(7, 1, 6, Material.CHEST, BlockFace.SOUTH);
        b.facing(8, 1, 6, Material.CHEST, BlockFace.SOUTH);
        b.set(7, 1, 10, Material.BOOKSHELF);
        b.set(8, 1, 10, Material.BOOKSHELF);
        b.set(7, 1, 8, Material.CRAFTING_TABLE);
        b.set(7, 5, 8, Material.STONE_BRICKS);
        b.hangingLantern(7, 4, 8, Material.LANTERN, 5);

        // Iron-bar canopy over tracks
        for (int x = -4; x <= 4; x++) {
            for (int z = 2; z <= 12; z++) {
                if (x == -4 || x == 4) {
                    b.set(x, 1, z, Material.IRON_BARS);
                    b.set(x, 2, z, Material.IRON_BARS);
                    b.set(x, 3, z, Material.IRON_BARS);
                }
                if ((z - 2) % 3 == 0) {
                    b.set(x, 4, z, Material.STONE_SLAB);
                }
            }
        }
        for (int z = 2; z <= 12; z++) {
            b.set(-4, 4, z, Material.STONE_BRICKS);
            b.set(4, 4, z, Material.STONE_BRICKS);
            b.set(0, 4, z, Material.STONE_SLAB);
        }

        // Cobble wall edge + glowstone posts
        for (int z = minZ; z <= maxZ; z++) {
            b.set(minX, 1, z, Material.COBBLESTONE_WALL);
            if (z % 4 == 0) {
                b.set(minX, 2, z, Material.GLOWSTONE);
            }
        }

        spawnPad(b, 0, maxZ + 1);
        spawnPad(b, 0, maxZ + 2);
        linkPad(b, 3, maxZ + 1);
        return b.build(
                "kingdom",
                "Advanced station (GrabCraft Dark Kingdom vibe) - dual track, andesite hall, side throat, link pad",
                0, 0, maxZ + 2
        );
    }

    /** Western spruce station house with porch and single powered track. */
    public static @NotNull BaseTemplates.BaseBlueprint western() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int minZ = -3;
        int maxZ = 14;

        for (int x = -6; x <= 8; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                b.set(x, -1, z, Material.COARSE_DIRT);
                b.set(x, 0, z, Material.SAND);
            }
        }
        // Platform planks
        for (int x = -3; x <= 3; x++) {
            for (int z = minZ; z <= maxZ - 2; z++) {
                b.set(x, 0, z, Material.SPRUCE_PLANKS);
            }
        }
        placeTrack(b, 0, minZ, maxZ - 2);

        // Station house (+X porch facing tracks)
        for (int x = 3; x <= 7; x++) {
            for (int z = 3; z <= 10; z++) {
                b.set(x, 0, z, Material.SPRUCE_PLANKS);
                boolean wall = x == 3 || x == 7 || z == 3 || z == 10;
                for (int y = 1; y <= 3; y++) {
                    if (wall) {
                        b.set(x, y, z, Material.SPRUCE_PLANKS);
                    } else {
                        b.set(x, y, z, Material.AIR);
                    }
                }
                b.set(x, 4, z, Material.SPRUCE_SLAB);
            }
        }
        // Porch (keep clear of side throat at z = maxZ-5)
        for (int x = 1; x <= 2; x++) {
            for (int z = 4; z <= 8; z++) {
                b.set(x, 0, z, Material.SPRUCE_SLAB);
            }
        }
        for (int z = 4; z <= 8; z += 4) {
            b.set(1, 1, z, Material.OAK_FENCE);
            b.set(1, 2, z, Material.OAK_FENCE);
            b.set(1, 3, z, Material.SPRUCE_STAIRS);
        }
        // Re-assert throat after porch so powered rails keep redstone beds
        yardThroat(b, 0, maxZ - 5, 5);
        b.door(3, 1, 6, Material.OAK_DOOR, BlockFace.WEST);
        b.facing(5, 1, 5, Material.CHEST, BlockFace.WEST);
        b.set(6, 1, 5, Material.BARREL);
        b.set(5, 1, 8, Material.CRAFTING_TABLE);
        b.slab(5, 1, 7, Material.SPRUCE_SLAB, Slab.Type.BOTTOM);
        b.set(5, 4, 6, Material.SPRUCE_PLANKS);
        b.hangingLantern(5, 3, 6, Material.LANTERN, 4);
        // Ticket window
        b.set(3, 2, 5, Material.GLASS_PANE);
        b.set(3, 2, 7, Material.GLASS_PANE);

        // Sand path + fence railing
        for (int z = minZ; z <= maxZ; z++) {
            b.set(-4, 1, z, Material.OAK_FENCE);
            b.set(8, 1, z, Material.OAK_FENCE);
        }

        spawnPad(b, 0, maxZ + 1);
        spawnPad(b, 0, maxZ + 2);
        linkPad(b, 2, maxZ + 1);
        return b.build(
                "western",
                "Advanced station (GrabCraft Western vibe) - spruce house, porch, powered track, side throat",
                0, 0, maxZ + 2
        );
    }

    /** Compact dark Adacia-style stop. */
    public static @NotNull BaseTemplates.BaseBlueprint adacia() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int minZ = -2;
        int maxZ = 10;

        for (int x = -4; x <= 5; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                b.set(x, -1, z, Material.BRICKS);
                b.set(x, 0, z, Material.DARK_OAK_PLANKS);
            }
        }
        placeTrack(b, 0, minZ, maxZ - 2);
        yardThroat(b, 0, maxZ - 4, 4);

        // Tiny waiting hut
        for (int x = 2; x <= 4; x++) {
            for (int z = 3; z <= 7; z++) {
                b.set(x, 0, z, Material.DARK_OAK_PLANKS);
                boolean wall = x == 2 || x == 4 || z == 3 || z == 7;
                for (int y = 1; y <= 3; y++) {
                    if (wall) {
                        b.set(x, y, z, Material.DARK_OAK_PLANKS);
                    } else {
                        b.set(x, y, z, Material.AIR);
                    }
                }
                b.set(x, 4, z, Material.DARK_OAK_SLAB);
            }
        }
        b.door(2, 1, 5, Material.DARK_OAK_DOOR, BlockFace.WEST);
        b.facing(3, 1, 4, Material.CHEST, BlockFace.WEST);
        b.set(3, 1, 6, Material.CRAFTING_TABLE);
        b.set(4, 2, 5, Material.WHITE_STAINED_GLASS_PANE);
        b.set(3, 4, 5, Material.DARK_OAK_PLANKS);
        b.hangingLantern(3, 3, 5, Material.LANTERN, 4);
        for (int z = minZ; z <= maxZ; z++) {
            b.set(-3, 1, z, Material.OAK_FENCE);
        }

        spawnPad(b, 0, maxZ + 1);
        spawnPad(b, 0, maxZ + 2);
        linkPad(b, 2, maxZ + 1);
        return b.build(
                "adacia",
                "Advanced station (GrabCraft Adacia vibe) - compact dark oak stop, powered track, side throat",
                0, 0, maxZ + 2
        );
    }

    /** Multi-track railyard with east/west throat for plain links. */
    public static @NotNull BaseTemplates.BaseBlueprint yard() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int minZ = -4;
        int maxZ = 18;

        for (int x = -10; x <= 10; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                b.set(x, -1, z, Material.STONE_BRICKS);
                b.set(x, 0, z, Material.GRAVEL);
            }
        }
        // Three parallel tracks
        placeTrack(b, -4, minZ, maxZ - 3);
        placeTrack(b, 0, minZ, maxZ - 3);
        placeTrack(b, 4, minZ, maxZ - 3);

        // Yard throat: merge center track to both sides near south end
        yardThroat(b, 0, maxZ - 6, 8);
        // West throat too
        for (int i = 1; i <= 6; i++) {
            int x = -i;
            int z = maxZ - 6;
            if (i % 2 == 0) {
                b.set(x, 0, z, Material.REDSTONE_BLOCK);
                b.set(x, 1, z, Material.POWERED_RAIL);
            } else {
                b.set(x, 0, z, Material.GRAVEL);
                b.set(x, 1, z, Material.RAIL);
            }
            b.set(x, 2, z, Material.AIR);
        }
        // Diagonal-safe corners from center onto throats (normal rails only)
        b.set(1, 0, maxZ - 7, Material.GRAVEL);
        b.set(1, 1, maxZ - 7, Material.RAIL);
        b.set(-1, 0, maxZ - 7, Material.GRAVEL);
        b.set(-1, 1, maxZ - 7, Material.RAIL);

        // Control tower
        for (int x = 7; x <= 9; x++) {
            for (int z = 6; z <= 10; z++) {
                b.set(x, 0, z, Material.STONE_BRICKS);
                for (int y = 1; y <= 5; y++) {
                    boolean wall = x == 7 || x == 9 || z == 6 || z == 10;
                    b.set(x, y, z, wall ? Material.STONE_BRICKS : Material.AIR);
                }
                b.set(x, 6, z, Material.STONE_BRICK_SLAB);
            }
        }
        b.door(7, 1, 8, Material.IRON_DOOR, BlockFace.WEST);
        b.set(8, 1, 8, Material.LEVER);
        b.facing(8, 1, 7, Material.CHEST, BlockFace.SOUTH);
        b.set(8, 2, 6, Material.GLASS_PANE);
        b.set(8, 2, 10, Material.GLASS_PANE);
        b.set(8, 6, 8, Material.STONE_BRICKS);
        b.hangingLantern(8, 5, 8, Material.LANTERN, 6);
        // Signal lamps
        for (int x : new int[]{-6, 6}) {
            b.set(x, 1, minZ, Material.OAK_FENCE);
            b.set(x, 2, minZ, Material.OAK_FENCE);
            b.set(x, 3, minZ, Material.REDSTONE_LAMP);
        }

        spawnPad(b, 0, maxZ + 1);
        spawnPad(b, 0, maxZ + 2);
        linkPad(b, 2, maxZ + 1);
        return b.build(
                "yard",
                "Advanced railyard - 3 tracks, east/west throats for plain links, control tower, link pad",
                0, 0, maxZ + 2
        );
    }

    /** Powered rail on redstone every other block along +Z. */
    private static void placeTrack(@NotNull BaseTemplates.Builder b, int x, int z0, int z1) {
        for (int z = z0; z <= z1; z++) {
            if ((z - z0) % 2 == 0) {
                b.set(x, 0, z, Material.REDSTONE_BLOCK);
                b.set(x, 1, z, Material.POWERED_RAIL);
            } else {
                b.set(x, 0, z, Material.GRAVEL);
                b.set(x, 1, z, Material.RAIL);
            }
            b.set(x, 2, z, Material.AIR);
        }
    }

    /**
     * East-bound spur from track at (trackX, throatZ) for sideways {@code /station link} joins.
     * Corner cell uses normal rail only (powered rails cannot curve).
     */
    private static void yardThroat(@NotNull BaseTemplates.Builder b, int trackX, int throatZ, int length) {
        // Corner off the main track (normal rail)
        b.set(trackX + 1, 0, throatZ, Material.GRAVEL);
        b.set(trackX + 1, 1, throatZ, Material.RAIL);
        b.set(trackX + 1, 2, throatZ, Material.AIR);
        for (int i = 2; i <= length; i++) {
            int x = trackX + i;
            if (i % 2 == 0) {
                b.set(x, 0, throatZ, Material.REDSTONE_BLOCK);
                b.set(x, 1, throatZ, Material.POWERED_RAIL);
            } else {
                b.set(x, 0, throatZ, Material.GRAVEL);
                b.set(x, 1, throatZ, Material.RAIL);
            }
            b.set(x, 2, throatZ, Material.AIR);
        }
    }

    private static void spawnPad(@NotNull BaseTemplates.Builder b, int x, int z) {
        b.set(x, -1, z, Material.DIRT_PATH);
        b.set(x, 0, z, Material.AIR);
        b.set(x, 1, z, Material.AIR);
    }

    private static void linkPad(@NotNull BaseTemplates.Builder b, int x, int z) {
        b.set(x, -1, z, Material.POLISHED_DEEPSLATE);
        b.set(x, 0, z, Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
        b.set(x, 1, z, Material.AIR);
        b.set(x + 1, -1, z, Material.AMETHYST_BLOCK);
        b.set(x - 1, -1, z, Material.AMETHYST_BLOCK);
    }
}
