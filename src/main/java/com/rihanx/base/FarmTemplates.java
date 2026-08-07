package com.rihanx.base;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Slab;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Automatic farms with gadgets (hoppers, chests, water, observers, pistons, composters).
 * Local space: front / collection face = +Z.
 */
public final class FarmTemplates {

    private FarmTemplates() {
    }

    public static @NotNull Map<String, BaseTemplates.BaseBlueprint> all() {
        Map<String, BaseTemplates.BaseBlueprint> map = new LinkedHashMap<>();
        map.put("wheat", wheat());
        map.put("potato", potato());
        map.put("cane", cane());
        map.put("bamboo", bamboo());
        map.put("melon", melon());
        map.put("cocoa", cocoa());
        map.put("kelp", kelp());
        map.put("mushroom", mushroom());
        map.put("nether", nether());
        map.put("animal", animal());
        map.put("cactus", cactus());
        map.put("iron", iron());
        map.put("xp", xp());
        return map;
    }

    /** Irrigated wheat field with hopper collection under the soil and composters. */
    public static @NotNull BaseTemplates.BaseBlueprint wheat() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int size = 4; // -4..4

        // Stone platform + hopper layer under farmland
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                b.set(x, -2, z, Material.STONE);
                boolean edge = x == -size || x == size || z == -size || z == size;
                if (edge) {
                    b.set(x, -1, z, Material.STONE_BRICKS);
                    b.set(x, 0, z, Material.OAK_FENCE);
                    b.set(x, 1, z, Material.OAK_FENCE);
                } else if (x == 0 || z == 0) {
                    // Water cross irrigation
                    b.set(x, -1, z, Material.STONE);
                    b.set(x, 0, z, Material.WATER);
                } else {
                    // Hopper under soil → points toward center chest line
                    BlockFace hop = hopperToward(x, z);
                    b.facing(x, -1, z, Material.HOPPER, hop);
                    b.set(x, 0, z, Material.FARMLAND);
                    b.set(x, 1, z, Material.WHEAT);
                }
            }
        }

        // Collection row into barrels/chests at front
        for (int x = -size + 1; x <= size - 1; x++) {
            if (x == 0) {
                continue;
            }
            b.facing(x, -1, size - 1, Material.HOPPER, BlockFace.SOUTH);
        }
        b.facing(0, -1, size - 1, Material.HOPPER, BlockFace.SOUTH);
        b.facing(0, -1, size, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -2, size, Material.CHEST);
        b.facing(-1, -2, size, Material.CHEST, BlockFace.SOUTH);
        b.facing(1, -2, size, Material.BARREL, BlockFace.SOUTH);

        // Gadgets (corners) then safe ceiling lanterns on back posts only
        b.set(-size, 1, size, Material.COMPOSTER);
        b.set(size, 1, size, Material.COMPOSTER);
        b.set(-size + 1, 1, size, Material.CRAFTING_TABLE);
        b.set(size - 1, 1, size, Material.BARREL);
        // Never post a lantern on the water cross center
        postHangingLantern(b, -size, 0, -size, Material.LANTERN);
        postHangingLantern(b, size, 0, -size, Material.LANTERN);

        // Gate entrance
        b.set(0, 0, size, Material.OAK_FENCE_GATE);
        b.set(0, 1, size, Material.AIR);
        b.facing(0, 0, size, Material.OAK_FENCE_GATE, BlockFace.SOUTH);

        // Path under feet at spawn (player stands on AIR above path)
        spawnPad(b, 0, size + 1);
        spawnPad(b, 0, size + 2);
        b.set(-1, 0, size + 2, Material.COMPOSTER);
        b.set(1, 0, size + 2, Material.BARREL);

        return b.build("wheat", "Auto wheat farm - water cross, hoppers, chests, composters", 0, 0, size + 2);
    }

    /** Observer + piston sugar cane farm with hopper collection. */
    public static @NotNull BaseTemplates.BaseBlueprint cane() {
        BaseTemplates.Builder c = new BaseTemplates.Builder();
        int rows = 6;
        for (int x = -rows; x <= rows; x++) {
            c.set(x, -1, 0, Material.STONE);
            c.set(x, 0, 0, Material.WATER);
            c.set(x, -1, 1, Material.SAND);
            c.set(x, 0, 1, Material.SUGAR_CANE);
            c.set(x, 1, 1, Material.SUGAR_CANE);
            c.set(x, 2, 1, Material.SUGAR_CANE);
            c.facing(x, 2, 2, Material.OBSERVER, BlockFace.NORTH);
            c.facing(x, 2, 3, Material.PISTON, BlockFace.NORTH);
            c.facing(x, -2, 1, Material.HOPPER, BlockFace.SOUTH);
            c.set(x, -3, 1, Material.SMOOTH_STONE);
            c.facing(x, -2, 2, Material.HOPPER, x < 0 ? BlockFace.EAST : (x > 0 ? BlockFace.WEST : BlockFace.SOUTH));
        }
        c.facing(0, -2, 3, Material.HOPPER, BlockFace.DOWN);
        c.set(0, -3, 3, Material.CHEST);
        c.facing(-1, -3, 3, Material.CHEST, BlockFace.SOUTH);
        c.set(1, -3, 3, Material.BARREL);
        for (int x = -rows - 1; x <= rows + 1; x++) {
            c.set(x, 0, -1, Material.STONE_BRICKS);
            c.slab(x, 3, 2, Material.STONE_BRICK_SLAB, Slab.Type.BOTTOM);
        }
        c.set(-rows - 1, 0, 1, Material.STONE_BRICKS);
        c.set(rows + 1, 0, 1, Material.STONE_BRICKS);
        postHangingLantern(c, -rows - 1, 0, 1, Material.LANTERN);
        postHangingLantern(c, rows + 1, 0, 1, Material.LANTERN);
        spawnPad(c, 0, 4);
        spawnPad(c, 0, 5);
        c.set(-1, 0, 5, Material.CRAFTING_TABLE);
        c.set(1, 0, 5, Material.BARREL);
        return c.build("cane", "Auto sugar-cane farm - observers, pistons, hoppers, chests", 0, 0, 5);
    }

    /** Bamboo observer farm with hoppers. */
    public static @NotNull BaseTemplates.BaseBlueprint bamboo() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int rows = 5;
        for (int x = -rows; x <= rows; x++) {
            b.set(x, -1, 0, Material.PODZOL);
            b.set(x, 0, 0, Material.BAMBOO);
            b.set(x, 1, 0, Material.BAMBOO);
            b.set(x, 2, 0, Material.BAMBOO);
            b.facing(x, 2, 1, Material.OBSERVER, BlockFace.NORTH);
            b.facing(x, 2, 2, Material.PISTON, BlockFace.NORTH);
            b.facing(x, -2, 0, Material.HOPPER, BlockFace.SOUTH);
            b.set(x, -3, 0, Material.SMOOTH_STONE);
            b.facing(x, -2, 1, Material.HOPPER, x < 0 ? BlockFace.EAST : (x > 0 ? BlockFace.WEST : BlockFace.SOUTH));
        }
        b.facing(0, -2, 2, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -3, 2, Material.CHEST);
        b.facing(-1, -3, 2, Material.BARREL, BlockFace.SOUTH);
        for (int x = -rows - 1; x <= rows + 1; x++) {
            b.set(x, 0, -1, Material.JUNGLE_FENCE);
            b.set(x, 3, 1, Material.JUNGLE_SLAB);
        }
        spawnPad(b, 0, 3);
        spawnPad(b, 0, 4);
        b.set(-1, 0, 4, Material.COMPOSTER);
        b.set(1, 0, 4, Material.CRAFTING_TABLE);
        postHangingLantern(b, -rows, 0, -1, Material.LANTERN);
        postHangingLantern(b, rows, 0, -1, Material.LANTERN);
        return b.build("bamboo", "Auto bamboo farm - observers, pistons, hoppers", 0, 0, 4);
    }

    /** Melon/pumpkin stem farm with hopper collection under harvest pads. */
    public static @NotNull BaseTemplates.BaseBlueprint melon() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        for (int x = -5; x <= 5; x++) {
            for (int z = -3; z <= 2; z++) {
                b.set(x, -2, z, Material.STONE);
                if ((x & 1) == 0) {
                    b.set(x, -1, z, Material.FARMLAND);
                    b.set(x, 0, z, Material.MELON_STEM);
                } else {
                    b.facing(x, -1, z, Material.HOPPER, BlockFace.SOUTH);
                    b.set(x, 0, z, Material.DIRT);
                }
            }
            b.set(x, -1, -4, Material.STONE);
            b.set(x, 0, -4, Material.WATER);
            b.facing(x, -1, 3, Material.HOPPER, BlockFace.SOUTH);
        }
        b.facing(0, -1, 4, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -2, 4, Material.CHEST);
        b.facing(-1, -2, 4, Material.CHEST, BlockFace.SOUTH);
        b.set(1, -2, 4, Material.BARREL);
        for (int x = -6; x <= 6; x++) {
            b.set(x, 0, -5, Material.OAK_FENCE);
            b.set(x, 0, 3, Material.OAK_FENCE);
        }
        for (int z = -5; z <= 3; z++) {
            b.set(-6, 0, z, Material.OAK_FENCE);
            b.set(6, 0, z, Material.OAK_FENCE);
        }
        b.facing(0, 0, 3, Material.OAK_FENCE_GATE, BlockFace.SOUTH);
        spawnPad(b, 0, 5);
        spawnPad(b, 0, 6);
        postHangingLantern(b, -5, 0, -5, Material.LANTERN);
        postHangingLantern(b, 5, 0, -5, Material.LANTERN);
        b.set(-2, 0, 6, Material.CRAFTING_TABLE);
        b.set(2, 0, 6, Material.COMPOSTER);
        return b.build("melon", "Melon farm - stems, dirt pads, hopper collection", 0, 0, 6);
    }

    /** Nether wart farm on soul sand with hoppers. */
    public static @NotNull BaseTemplates.BaseBlueprint nether() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 3; z++) {
                b.set(x, -2, z, Material.BLACKSTONE);
                boolean edge = x == -4 || x == 4 || z == -4 || z == 3;
                if (edge) {
                    b.set(x, -1, z, Material.POLISHED_BLACKSTONE_BRICKS);
                    b.set(x, 0, z, Material.CRIMSON_FENCE);
                } else {
                    b.facing(x, -1, z, Material.HOPPER, BlockFace.SOUTH);
                    b.set(x, 0, z, Material.SOUL_SAND);
                    b.set(x, 1, z, Material.NETHER_WART);
                }
            }
        }
        for (int x = -3; x <= 3; x++) {
            b.facing(x, -1, 4, Material.HOPPER, BlockFace.SOUTH);
        }
        b.facing(0, -1, 5, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -2, 5, Material.CHEST);
        b.facing(-1, -2, 5, Material.BARREL, BlockFace.SOUTH);
        b.facing(0, 0, 3, Material.CRIMSON_FENCE_GATE, BlockFace.SOUTH);
        b.set(0, 0, 6, Material.BLACKSTONE);
        b.set(0, 0, 7, Material.BLACKSTONE);
        postHangingLantern(b, -4, 0, -4, Material.SOUL_LANTERN);
        postHangingLantern(b, 4, 0, -4, Material.SOUL_LANTERN);
        postHangingLantern(b, 4, 0, 3, Material.SOUL_LANTERN);
        postHangingLantern(b, -4, 0, 3, Material.SOUL_LANTERN);
        spawnPad(b, 0, 7);
        b.set(-1, 0, 7, Material.CRAFTING_TABLE);
        b.set(1, 0, 7, Material.BARREL);
        return b.build("nether", "Nether wart farm - soul sand, hoppers, crimson trim", 0, 0, 7);
    }

    /** Animal pens with water, feed chests, lanterns, gates. */
    public static @NotNull BaseTemplates.BaseBlueprint animal() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        // Four pens in a 2x2
        pen(b, -7, -2, -7, -2, Material.OAK_FENCE, Material.OAK_FENCE_GATE);
        pen(b, 2, 7, -7, -2, Material.SPRUCE_FENCE, Material.SPRUCE_FENCE_GATE);
        pen(b, -7, -2, 2, 7, Material.BIRCH_FENCE, Material.BIRCH_FENCE_GATE);
        pen(b, 2, 7, 2, 7, Material.JUNGLE_FENCE, Material.JUNGLE_FENCE_GATE);

        // Center courtyard gadgets + hopper feed into storage
        b.set(0, -1, 0, Material.HOPPER);
        b.facing(0, -1, 0, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -2, 0, Material.CHEST);
        b.set(0, 0, 0, Material.CRAFTING_TABLE);
        b.set(-1, 0, 0, Material.BARREL);
        b.set(1, 0, 0, Material.BARREL);
        b.set(0, 0, 1, Material.CHEST);
        b.set(0, 0, -1, Material.HAY_BLOCK);
        b.set(0, 4, 0, Material.OAK_PLANKS);
        b.hangingLantern(0, 3, 0, Material.LANTERN, 4);
        spawnPad(b, 0, 9);
        spawnPad(b, 0, 10);
        return b.build("animal", "4 animal pens - water, hay, chests, gates, hopper storage", 0, 0, 10);
    }

    /** Cactus farm with breaking edges and hopper collection. */
    public static @NotNull BaseTemplates.BaseBlueprint cactus() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        for (int x = -4; x <= 4; x += 2) {
            for (int z = -3; z <= 2; z += 2) {
                b.set(x, -1, z, Material.SAND);
                b.set(x, 0, z, Material.CACTUS);
                b.set(x, 1, z, Material.CACTUS);
                // Break fence one block above base cactus so paste doesn't self-break
                b.set(x + 1, 2, z, Material.OAK_FENCE);
                b.facing(x, -2, z, Material.HOPPER, BlockFace.SOUTH);
                b.set(x, -3, z, Material.SMOOTH_SANDSTONE);
            }
        }
        for (int x = -4; x <= 4; x++) {
            b.facing(x, -2, 3, Material.HOPPER, BlockFace.SOUTH);
        }
        b.facing(0, -2, 4, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -3, 4, Material.CHEST);
        b.facing(-1, -3, 4, Material.BARREL, BlockFace.SOUTH);
        for (int x = -5; x <= 5; x++) {
            b.set(x, 0, -4, Material.SANDSTONE_WALL);
            b.set(x, 0, 3, Material.SANDSTONE_WALL);
        }
        spawnPad(b, 0, 5);
        spawnPad(b, 0, 6);
        postHangingLantern(b, -4, 0, -4, Material.LANTERN);
        postHangingLantern(b, 4, 0, -4, Material.LANTERN);
        return b.build("cactus", "Cactus farm - auto-break fences, hoppers, chests", 0, 0, 6);
    }

    /** Potato/carrot style crop farm (same gadgets as wheat). */
    public static @NotNull BaseTemplates.BaseBlueprint potato() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int size = 4;
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                b.set(x, -2, z, Material.STONE);
                boolean edge = x == -size || x == size || z == -size || z == size;
                if (edge) {
                    b.set(x, -1, z, Material.STONE_BRICKS);
                    b.set(x, 0, z, Material.SPRUCE_FENCE);
                    b.set(x, 1, z, Material.SPRUCE_FENCE);
                } else if (x == 0 || z == 0) {
                    b.set(x, -1, z, Material.STONE);
                    b.set(x, 0, z, Material.WATER);
                } else {
                    b.facing(x, -1, z, Material.HOPPER, hopperToward(x, z));
                    b.set(x, 0, z, Material.FARMLAND);
                    b.set(x, 1, z, Material.POTATOES);
                }
            }
        }
        b.facing(0, -1, size, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -2, size, Material.CHEST);
        b.facing(-1, -2, size, Material.BARREL, BlockFace.SOUTH);
        b.facing(0, 0, size, Material.SPRUCE_FENCE_GATE, BlockFace.SOUTH);
        postHangingLantern(b, -size, 0, -size, Material.LANTERN);
        postHangingLantern(b, size, 0, -size, Material.LANTERN);
        spawnPad(b, 0, size + 1);
        spawnPad(b, 0, size + 2);
        b.set(-1, 0, size + 2, Material.COMPOSTER);
        return b.build("potato", "Potato farm - water, hoppers, chests, composters", 0, 0, size + 2);
    }

    /** Jungle cocoa farm on logs with hopper floors. */
    public static @NotNull BaseTemplates.BaseBlueprint cocoa() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        for (int x = -4; x <= 4; x += 2) {
            for (int z = -3; z <= 2; z++) {
                b.set(x, -1, z, Material.JUNGLE_LOG);
                b.set(x, 0, z, Material.JUNGLE_LOG);
                b.set(x, 1, z, Material.JUNGLE_LOG);
                b.set(x, 2, z, Material.JUNGLE_LOG);
                if (x > -4) {
                    b.facing(x - 1, 1, z, Material.COCOA, BlockFace.EAST);
                }
                if (x < 4) {
                    b.facing(x + 1, 1, z, Material.COCOA, BlockFace.WEST);
                }
                b.facing(x, -2, z, Material.HOPPER, BlockFace.SOUTH);
            }
        }
        for (int x = -4; x <= 4; x++) {
            b.facing(x, -2, 3, Material.HOPPER, BlockFace.SOUTH);
        }
        b.facing(0, -2, 4, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -3, 4, Material.CHEST);
        b.facing(-1, -3, 4, Material.BARREL, BlockFace.SOUTH);
        for (int x = -5; x <= 5; x++) {
            b.set(x, 3, -3, Material.JUNGLE_SLAB);
            b.set(x, 3, 2, Material.JUNGLE_SLAB);
        }
        b.set(-4, 4, -3, Material.JUNGLE_PLANKS);
        b.set(4, 4, -3, Material.JUNGLE_PLANKS);
        b.hangingLantern(-4, 3, -3, Material.LANTERN, 4);
        b.hangingLantern(4, 3, -3, Material.LANTERN, 4);
        spawnPad(b, 0, 5);
        spawnPad(b, 0, 6);
        return b.build("cocoa", "Cocoa farm - jungle logs, hoppers, hanging lanterns", 0, 0, 6);
    }

    /** Kelp water column farm with hopper drains. */
    public static @NotNull BaseTemplates.BaseBlueprint kelp() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 2; z++) {
                b.set(x, -1, z, Material.SAND);
                b.facing(x, -2, z, Material.HOPPER, BlockFace.SOUTH);
                for (int y = 0; y <= 4; y++) {
                    b.set(x, y, z, Material.WATER);
                }
                b.set(x, 0, z, Material.KELP);
                b.set(x, 1, z, Material.KELP_PLANT);
                b.set(x, 2, z, Material.KELP_PLANT);
                b.set(x, 3, z, Material.KELP);
            }
        }
        // Glass tank walls
        for (int y = 0; y <= 5; y++) {
            for (int x = -4; x <= 4; x++) {
                b.set(x, y, -4, Material.GLASS);
                b.set(x, y, 3, Material.GLASS);
            }
            for (int z = -4; z <= 3; z++) {
                b.set(-4, y, z, Material.GLASS);
                b.set(4, y, z, Material.GLASS);
            }
        }
        fillRectRoof(b, -4, 4, -4, 3, 5, Material.GLASS);
        b.facing(0, -2, 4, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -3, 4, Material.CHEST);
        b.facing(-1, -3, 4, Material.BARREL, BlockFace.SOUTH);
        b.hangingLantern(0, 4, 0, Material.LANTERN, 5);
        b.hangingLantern(-3, 4, -3, Material.LANTERN, 5);
        b.hangingLantern(3, 4, 2, Material.LANTERN, 5);
        spawnPad(b, 0, 5);
        spawnPad(b, 0, 6);
        return b.build("kelp", "Kelp tank farm - glass, water, hoppers, hanging lanterns", 0, 0, 6);
    }

    /** Mooshroom / mushroom farm hut with hoppers. */
    public static @NotNull BaseTemplates.BaseBlueprint mushroom() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 3; z++) {
                b.set(x, -1, z, Material.MYCELIUM);
                b.facing(x, -2, z, Material.HOPPER, BlockFace.SOUTH);
                boolean edge = x == -4 || x == 4 || z == -4 || z == 3;
                if (edge) {
                    b.set(x, 0, z, Material.DARK_OAK_LOG);
                    b.set(x, 1, z, Material.DARK_OAK_LOG);
                    b.set(x, 2, z, Material.DARK_OAK_LOG);
                } else {
                    b.set(x, 0, z, (x + z) % 2 == 0 ? Material.RED_MUSHROOM : Material.BROWN_MUSHROOM);
                }
            }
        }
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 3; z++) {
                b.set(x, 3, z, Material.DARK_OAK_PLANKS);
            }
        }
        b.set(0, 0, 3, Material.AIR);
        b.set(0, 1, 3, Material.AIR);
        b.door(0, 0, 3, Material.DARK_OAK_DOOR, BlockFace.SOUTH);
        b.facing(0, -2, 4, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -3, 4, Material.CHEST);
        b.set(0, 4, 0, Material.DARK_OAK_PLANKS);
        b.set(-3, 4, -3, Material.DARK_OAK_PLANKS);
        b.set(3, 4, 1, Material.DARK_OAK_PLANKS);
        b.hangingLantern(0, 3, 0, Material.LANTERN, 4);
        b.hangingLantern(-3, 3, -3, Material.LANTERN, 4);
        b.hangingLantern(3, 3, 1, Material.LANTERN, 4);
        spawnPad(b, 0, 5);
        spawnPad(b, 0, 6);
        return b.build("mushroom", "Mushroom hut farm - mycelium, hoppers, hanging lanterns", 0, 0, 6);
    }

    /**
     * Iron golem farm shell — beds + composters for villagers, water streams, lava kill, hoppers.
     * Place 3 villagers per pod and a zombie (nametag) in the cage after paste.
     */
    public static @NotNull BaseTemplates.BaseBlueprint iron() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        // Spawn platform
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 4; z++) {
                b.set(x, 8, z, Material.STONE_BRICKS);
                b.set(x, 9, z, Material.AIR);
                b.set(x, 10, z, Material.AIR);
                b.set(x, 11, z, Material.AIR);
            }
        }
        // Water push to +Z drop
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 2; z++) {
                b.set(x, 9, z, Material.WATER);
            }
            b.set(x, 8, 3, Material.AIR); // drop hole row
            b.set(x, 8, 4, Material.STONE_BRICKS);
        }
        // Drop tube
        for (int y = 0; y <= 8; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = 3; z <= 5; z++) {
                    boolean wall = x == -2 || x == 2 || z == 3 || z == 5 || y == 0;
                    if (wall && !(z == 5 && x == 0 && y >= 1 && y <= 2)) {
                        b.set(x, y, z, Material.STONE_BRICKS);
                    } else if (!wall) {
                        b.set(x, y, z, Material.AIR);
                    }
                }
            }
        }
        // Lava blade kill + hoppers
        b.set(0, 1, 4, Material.LAVA);
        b.set(0, 0, 4, Material.STONE_BRICKS);
        for (int x = -1; x <= 1; x++) {
            b.facing(x, -1, 4, Material.HOPPER, BlockFace.SOUTH);
        }
        b.facing(0, -1, 5, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -2, 5, Material.CHEST);
        b.facing(-1, -2, 5, Material.CHEST, BlockFace.SOUTH);
        b.set(1, -2, 5, Material.BARREL);

        // Villager pods (beds + workstations) left/right — keep bed head free of composters
        for (int side : new int[]{-8, 8}) {
            for (int i = 0; i < 3; i++) {
                int z = -4 + i * 2;
                BlockFace bedFace = side < 0 ? BlockFace.EAST : BlockFace.WEST;
                b.bed(side, 9, z, Material.RED_BED, bedFace);
                // Workstation beside the bed (same side wall), not on the bed head
                b.set(side, 9, z + 1, Material.COMPOSTER);
            }
            // Glass box
            for (int y = 9; y <= 11; y++) {
                for (int dz = -5; dz <= 2; dz++) {
                    b.set(side - (side < 0 ? 1 : -1), y, dz, Material.GLASS);
                }
            }
        }
        // Zombie cue cell (player places named zombie)
        b.set(0, 9, -7, Material.IRON_BARS);
        b.set(0, 10, -7, Material.IRON_BARS);
        b.set(0, 9, -8, Material.STONE_BRICKS);
        b.set(1, 9, -7, Material.IRON_BARS);
        b.set(-1, 9, -7, Material.IRON_BARS);

        // Roof over platform + hanging lanterns
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 4; z++) {
                b.set(x, 12, z, Material.STONE_BRICK_SLAB);
            }
        }
        b.set(0, 12, 0, Material.STONE_BRICKS);
        b.set(-4, 12, -3, Material.STONE_BRICKS);
        b.set(4, 12, -3, Material.STONE_BRICKS);
        b.hangingLantern(0, 10, 0, Material.LANTERN, 12);
        b.hangingLantern(-4, 10, -3, Material.LANTERN, 12);
        b.hangingLantern(4, 10, -3, Material.LANTERN, 12);
        // Kill-chamber light above head
        b.set(0, 4, 4, Material.STONE_BRICKS);
        b.hangingLantern(0, 3, 4, Material.LANTERN, 4);

        // AFK / collection entrance
        spawnPad(b, 0, 7);
        spawnPad(b, 0, 8);
        b.set(-1, 0, 8, Material.CRAFTING_TABLE);
        b.set(1, 0, 8, Material.BARREL);
        return b.build(
                "iron",
                "Iron farm shell - water, lava kill, hoppers (add villagers + zombie)",
                0, 0, 8
        );
    }

    /**
     * Mob XP farm — one connected building: AFK house ↔ kill room ↔ drop chute ↔ dark pads.
     * Hopper chain under magma drains into a double chest under the AFK floor.
     */
    public static @NotNull BaseTemplates.BaseBlueprint xp() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();

        // ——— Unified ground building: chute (z=0..2) + kill (z=3..5) + AFK (z=6..9) ———
        for (int x = -3; x <= 3; x++) {
            for (int z = 0; z <= 9; z++) {
                b.set(x, -2, z, Material.STONE_BRICKS);
                b.set(x, -1, z, Material.STONE_BRICKS);
                boolean wall = x == -3 || x == 3 || z == 0 || z == 9;
                if (wall) {
                    b.set(x, 0, z, Material.STONE_BRICKS);
                    b.set(x, 1, z, Material.STONE_BRICKS);
                    b.set(x, 2, z, Material.STONE_BRICKS);
                    b.set(x, 3, z, Material.STONE_BRICKS);
                } else {
                    b.set(x, 0, z, Material.STONE_BRICKS);
                    b.set(x, 1, z, Material.AIR);
                    b.set(x, 2, z, Material.AIR);
                    b.set(x, 3, z, Material.STONE_BRICK_SLAB);
                }
            }
        }
        // Roof over whole ground house
        for (int x = -3; x <= 3; x++) {
            for (int z = 0; z <= 9; z++) {
                b.set(x, 4, z, Material.STONE_BRICKS);
            }
        }

        // Front door (player spawns just outside)
        b.set(0, 1, 9, Material.AIR);
        b.set(0, 2, 9, Material.AIR);
        b.door(0, 1, 9, Material.IRON_DOOR, BlockFace.SOUTH);
        b.facing(0, 2, 10, Material.STONE_BUTTON, BlockFace.SOUTH);
        b.facing(0, 2, 8, Material.STONE_BUTTON, BlockFace.NORTH);

        // Kill + drop floor = hopper pipeline (items land on hoppers after the fall)
        // Layout: chute z=1..2 → kill z=3..5 → collector z=6 → chest below
        for (int z = 1; z <= 5; z++) {
            b.facing(-1, 0, z, Material.HOPPER, BlockFace.EAST);
            b.facing(1, 0, z, Material.HOPPER, BlockFace.WEST);
            b.facing(0, 0, z, Material.HOPPER, BlockFace.SOUTH);
        }
        b.facing(0, 0, 6, Material.HOPPER, BlockFace.DOWN);
        b.facing(-1, 0, 6, Material.HOPPER, BlockFace.EAST);
        b.facing(1, 0, 6, Material.HOPPER, BlockFace.WEST);
        b.set(0, -1, 6, Material.CHEST);
        b.facing(-1, -1, 6, Material.CHEST, BlockFace.SOUTH);
        b.set(1, -1, 6, Material.BARREL);
        // Safety bars so AFK player doesn't walk onto the kill hoppers
        for (int x = -2; x <= 2; x++) {
            b.set(x, 1, 5, Material.IRON_BARS);
        }
        b.set(0, 1, 5, Material.AIR);
        b.set(0, 2, 5, Material.AIR);

        // Drop chute tower (same footprint as house north end, rises from roof)
        for (int y = 4; y <= 24; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = 0; z <= 2; z++) {
                    boolean wall = x == -2 || x == 2 || z == 0 || z == 2;
                    if (wall) {
                        b.set(x, y, z, Material.COBBLESTONE);
                    } else {
                        b.set(x, y, z, Material.AIR);
                    }
                }
            }
        }
        // Keep chute shaft open down onto hopper floor (z=1..2)
        for (int x = -1; x <= 1; x++) {
            for (int y = 1; y <= 3; y++) {
                b.set(x, y, 1, Material.AIR);
                b.set(x, y, 2, Material.AIR);
            }
        }

        // AFK furniture
        b.set(-2, 1, 7, Material.CRAFTING_TABLE);
        b.set(-2, 1, 8, Material.ANVIL);
        b.set(2, 1, 7, Material.BARREL);
        b.set(2, 1, 8, Material.CHEST);
        b.hangingLantern(0, 3, 7, Material.LANTERN, 4);

        // Outside ladder up the chute for maintenance
        for (int y = 1; y <= 20; y++) {
            b.facing(-3, y, 1, Material.LADDER, BlockFace.WEST);
        }

        // Spawn pads at top (2 floors) — attached to chute
        for (int floor = 0; floor < 2; floor++) {
            int y = 20 + floor * 4;
            for (int x = -6; x <= 6; x++) {
                for (int z = -6; z <= 2; z++) {
                    b.set(x, y, z, Material.COBBLESTONE);
                    b.set(x, y + 1, z, Material.AIR);
                    b.set(x, y + 2, z, Material.AIR);
                    b.set(x, y + 3, z, Material.COBBLESTONE);
                }
            }
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= -1; z++) {
                    b.set(x, y + 1, z, Material.WATER);
                }
            }
            for (int x = -1; x <= 1; x++) {
                b.set(x, y, 1, Material.AIR);
                b.set(x, y, 0, Material.AIR);
            }
        }

        spawnPad(b, 0, 10);
        spawnPad(b, 0, 11);
        b.set(-1, 0, 11, Material.CRAFTING_TABLE);
        b.set(1, 0, 11, Material.BARREL);
        return b.build(
                "xp",
                "XP mob farm - connected AFK house, magma kill, hopper→chest, dark pads",
                0, 0, 11
        );
    }

    private static void fillRectRoof(
            @NotNull BaseTemplates.Builder b,
            int minX, int maxX, int minZ, int maxZ,
            int y,
            @NotNull Material material
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                b.set(x, y, z, material);
            }
        }
    }

    /**
     * Fence post with plank cap; lantern at groundY+3 (above head), never in walk volume.
     */
    private static void postHangingLantern(
            @NotNull BaseTemplates.Builder b,
            int x,
            int groundY,
            int z,
            @NotNull Material lantern
    ) {
        b.set(x, groundY, z, Material.OAK_FENCE);
        b.set(x, groundY + 1, z, Material.OAK_FENCE);
        b.set(x, groundY + 2, z, Material.OAK_FENCE);
        b.set(x, groundY + 4, z, Material.OAK_PLANKS);
        b.hangingLantern(x, groundY + 3, z, lantern, groundY + 4);
    }

    /** Dirt path under the spawn cell; feet/head stay air so the player is never sealed in a path block. */
    private static void spawnPad(@NotNull BaseTemplates.Builder b, int x, int z) {
        b.set(x, -1, z, Material.DIRT_PATH);
        b.set(x, 0, z, Material.AIR);
        b.set(x, 1, z, Material.AIR);
    }

    private static void pen(
            @NotNull BaseTemplates.Builder b,
            int x1, int x2, int z1, int z2,
            @NotNull Material fence,
            @NotNull Material gate
    ) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                b.set(x, -1, z, Material.DIRT);
                b.set(x, 0, z, Material.GRASS_BLOCK);
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                if (edge) {
                    b.set(x, 1, z, fence);
                }
            }
        }
        // Water corner + feed chest
        b.set(x1 + 1, 0, z1 + 1, Material.WATER);
        b.set(x2 - 1, 1, z1 + 1, Material.CHEST);
        b.set(x1 + 1, 1, z2 - 1, Material.HAY_BLOCK);
        b.set((x1 + x2) / 2, 1, z2, Material.AIR);
        b.facing((x1 + x2) / 2, 1, z2, gate, BlockFace.SOUTH);
        b.set((x1 + x2) / 2, 4, (z1 + z2) / 2, Material.OAK_PLANKS);
        b.hangingLantern((x1 + x2) / 2, 3, (z1 + z2) / 2, Material.LANTERN, 4);
    }

    private static @NotNull BlockFace hopperToward(int x, int z) {
        if (Math.abs(x) >= Math.abs(z)) {
            return x > 0 ? BlockFace.WEST : BlockFace.EAST;
        }
        return z > 0 ? BlockFace.NORTH : BlockFace.SOUTH;
    }

    /** Materials that count as "gadgets" for validation tests. */
    public static @NotNull Set<Material> gadgetMaterials() {
        return Set.of(
                Material.HOPPER, Material.CHEST, Material.BARREL, Material.COMPOSTER,
                Material.OBSERVER, Material.PISTON, Material.STICKY_PISTON,
                Material.DISPENSER, Material.DROPPER, Material.CRAFTING_TABLE,
                Material.WATER, Material.LANTERN, Material.SOUL_LANTERN, Material.IRON_CHAIN,
                Material.MAGMA_BLOCK, Material.LAVA, Material.IRON_BARS
        );
    }
}
