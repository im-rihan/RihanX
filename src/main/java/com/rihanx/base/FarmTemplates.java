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

    /** Irrigated wheat field with hopper collection under water canals (not under farmland). */
    public static @NotNull BaseTemplates.BaseBlueprint wheat() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int size = 4; // -4..4

        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                b.set(x, -2, z, Material.STONE);
                boolean edge = x == -size || x == size || z == -size || z == size;
                if (edge) {
                    b.set(x, -1, z, Material.STONE_BRICKS);
                    b.set(x, 0, z, Material.OAK_FENCE);
                    b.set(x, 1, z, Material.OAK_FENCE);
                } else if (x == 0 || z == 0) {
                    // Water cross — hoppers stay on the cross: arms → center column → south chest
                    BlockFace drain;
                    if (x != 0) {
                        drain = x < 0 ? BlockFace.EAST : BlockFace.WEST;
                    } else if (z < size) {
                        drain = BlockFace.SOUTH;
                    } else if (z > size) {
                        drain = BlockFace.NORTH;
                    } else {
                        drain = BlockFace.DOWN;
                    }
                    b.facing(x, -1, z, Material.HOPPER, drain);
                    b.set(x, 0, z, Material.WATER);
                } else {
                    b.set(x, -1, z, Material.DIRT);
                    b.set(x, 0, z, Material.FARMLAND);
                    b.set(x, 1, z, Material.WHEAT);
                }
            }
        }

        for (int x = -size + 1; x <= size - 1; x++) {
            if (x != 0) {
                b.set(x, 0, size - 1, Material.WATER);
            }
        }
        hopperRowIntoChest(b, -1, size - 1, -size + 1, size - 1, size);

        b.set(-size, 1, size, Material.COMPOSTER);
        b.set(size, 1, size, Material.COMPOSTER);
        b.set(-size + 1, 1, size, Material.CRAFTING_TABLE);
        b.set(size - 1, 1, size, Material.BARREL);
        postHangingLantern(b, -size, 0, -size, Material.LANTERN);
        postHangingLantern(b, size, 0, -size, Material.LANTERN);

        b.set(0, 0, size, Material.OAK_FENCE_GATE);
        b.set(0, 1, size, Material.AIR);
        b.facing(0, 0, size, Material.OAK_FENCE_GATE, BlockFace.SOUTH);

        spawnPad(b, 0, size + 1);
        spawnPad(b, 0, size + 2);
        b.set(-1, 0, size + 2, Material.COMPOSTER);
        b.set(1, 0, size + 2, Material.BARREL);

        return b.build("wheat", "Auto wheat farm - water cross hoppers, chests, composters", 0, 0, size + 2);
    }

    /**
     * Observer + piston sugar cane farm.
     * Piston faces the cane; observer sits on the piston and faces the growth tip;
     * observer output → redstone dust behind it powers the piston (no torch/button needed).
     * Broken pieces fall into the hopper under the piston foot → chest.
     */
    public static @NotNull BaseTemplates.BaseBlueprint cane() {
        BaseTemplates.Builder c = new BaseTemplates.Builder();
        int rows = 6;
        for (int x = -rows; x <= rows; x++) {
            c.set(x, -1, 0, Material.STONE);
            c.set(x, 0, 0, Material.WATER);
            c.set(x, -1, 1, Material.SAND);
            c.set(x, 0, 1, Material.SUGAR_CANE);
            c.set(x, 1, 1, Material.SUGAR_CANE);
            c.set(x, 2, 1, Material.AIR);
            // Bedrock-safe (no quasi-connectivity): observer on piston faces tip;
            // dust sits on the solid DIRECTLY behind the piston so the pulse powers the piston body.
            // (Java also works — QC is not required with this wiring.)
            c.facing(x, 1, 2, Material.PISTON, BlockFace.NORTH);
            c.facing(x, 2, 2, Material.OBSERVER, BlockFace.NORTH);
            c.set(x, 0, 2, Material.AIR);
            c.facing(x, -1, 2, Material.HOPPER, BlockFace.SOUTH);
            c.set(x, 0, 3, Material.SMOOTH_STONE);
            c.set(x, 1, 3, Material.SMOOTH_STONE); // behind piston — dust on this powers piston
            c.set(x, 2, 3, Material.REDSTONE_WIRE); // observer output face
            c.set(x, -1, 3, Material.SMOOTH_STONE);
        }
        // Hopper trench continues into the chest line
        hopperRowIntoChest(c, -1, 3, -rows, rows, 5);
        for (int x = -rows - 1; x <= rows + 1; x++) {
            c.set(x, 0, -1, Material.STONE_BRICKS);
            c.slab(x, 3, 2, Material.STONE_BRICK_SLAB, Slab.Type.BOTTOM);
        }
        c.set(-rows - 1, 0, 1, Material.STONE_BRICKS);
        c.set(rows + 1, 0, 1, Material.STONE_BRICKS);
        postHangingLantern(c, -rows - 1, 0, 1, Material.LANTERN);
        postHangingLantern(c, rows + 1, 0, 1, Material.LANTERN);
        spawnPad(c, 0, 6);
        spawnPad(c, 0, 7);
        c.set(-1, 0, 7, Material.CRAFTING_TABLE);
        c.set(1, 0, 7, Material.BARREL);
        return c.build("cane", "Auto sugar-cane farm - observers power pistons, hopper under drops", 0, 0, 7);
    }

    /**
     * Bamboo observer farm — same circuit as cane.
     * Observer detects tip growth → pulse from its back into redstone dust → piston breaks stalks.
     * Hoppers sit under the drop (piston foot), facing the chest — not the opposite side.
     */
    public static @NotNull BaseTemplates.BaseBlueprint bamboo() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int rows = 5;
        for (int x = -rows; x <= rows; x++) {
            b.set(x, -1, 0, Material.PODZOL);
            b.set(x, 0, 0, Material.BAMBOO);
            b.set(x, 1, 0, Material.BAMBOO);
            b.set(x, 2, 0, Material.AIR);
            // Bedrock-safe: same Sportskeeda/Astroworld wiring — dust on solid behind piston
            b.facing(x, 1, 1, Material.PISTON, BlockFace.NORTH);
            b.facing(x, 2, 1, Material.OBSERVER, BlockFace.NORTH);
            b.set(x, 0, 1, Material.AIR);
            b.facing(x, -1, 1, Material.HOPPER, BlockFace.SOUTH);
            b.set(x, 0, 2, Material.SMOOTH_STONE);
            b.set(x, 1, 2, Material.SMOOTH_STONE); // behind piston — dust powers this → piston
            b.set(x, 2, 2, Material.REDSTONE_WIRE); // observer output
            b.set(x, -1, 2, Material.SMOOTH_STONE);
        }
        hopperRowIntoChest(b, -1, 2, -rows, rows, 4);
        for (int x = -rows - 1; x <= rows + 1; x++) {
            b.set(x, 0, -1, Material.JUNGLE_FENCE);
            b.slab(x, 3, 1, Material.JUNGLE_SLAB, Slab.Type.BOTTOM);
        }
        spawnPad(b, 0, 5);
        spawnPad(b, 0, 6);
        b.set(-1, 0, 6, Material.COMPOSTER);
        b.set(1, 0, 6, Material.CRAFTING_TABLE);
        postHangingLantern(b, -rows, 0, -1, Material.LANTERN);
        postHangingLantern(b, rows, 0, -1, Material.LANTERN);
        return b.build("bamboo", "Auto bamboo farm - observers power pistons, hopper under drops", 0, 0, 6);
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
                    // Hopper columns only — face south along the column into the chest line
                    b.facing(x, -1, z, Material.HOPPER, BlockFace.SOUTH);
                    b.set(x, 0, z, Material.DIRT);
                }
            }
            b.set(x, -1, -4, Material.STONE);
            b.set(x, 0, -4, Material.WATER);
        }
        hopperRowIntoChest(b, -1, 3, -5, 5, 4);
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
        hopperRowIntoChest(b, -1, 2, -3, 3, 5);
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
        b.facing(0, -1, 0, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -2, 0, Material.CHEST);
        b.facing(-1, -2, 0, Material.CHEST, BlockFace.SOUTH);
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

    /** Cactus farm with breaking edges and hopper collection beside sand (not under it). */
    public static @NotNull BaseTemplates.BaseBlueprint cactus() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        for (int x = -4; x <= 4; x += 2) {
            for (int z = -3; z <= 2; z += 2) {
                b.set(x, -1, z, Material.SAND);
                b.set(x, 0, z, Material.CACTUS);
                b.set(x, 1, z, Material.CACTUS);
                b.set(x + 1, 2, z, Material.OAK_FENCE);
                b.set(x + 1, 0, z, Material.AIR);
                b.set(x, -2, z, Material.SMOOTH_SANDSTONE);
            }
        }
        // Continuous hopper trenches beside each cactus column → chest line
        for (int x = -3; x <= 5; x += 2) {
            for (int z = -3; z <= 2; z++) {
                b.facing(x, -1, z, Material.HOPPER, BlockFace.SOUTH);
            }
        }
        // Cover trench columns x=-3..5 so every side hopper reaches storage
        hopperRowIntoChest(b, -1, 3, -3, 5, 4);
        for (int x = -5; x <= 5; x++) {
            b.set(x, 0, -4, Material.SANDSTONE_WALL);
            b.set(x, 0, 3, Material.SANDSTONE_WALL);
        }
        spawnPad(b, 0, 5);
        spawnPad(b, 0, 6);
        postHangingLantern(b, -4, 0, -4, Material.LANTERN);
        postHangingLantern(b, 4, 0, -4, Material.LANTERN);
        return b.build("cactus", "Cactus farm - auto-break fences, side hoppers, chests", 0, 0, 6);
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
                    BlockFace drain;
                    if (x != 0) {
                        drain = x < 0 ? BlockFace.EAST : BlockFace.WEST;
                    } else if (z < size) {
                        drain = BlockFace.SOUTH;
                    } else if (z > size) {
                        drain = BlockFace.NORTH;
                    } else {
                        drain = BlockFace.DOWN;
                    }
                    b.facing(x, -1, z, Material.HOPPER, drain);
                    b.set(x, 0, z, Material.WATER);
                } else {
                    b.set(x, -1, z, Material.DIRT);
                    b.set(x, 0, z, Material.FARMLAND);
                    b.set(x, 1, z, Material.POTATOES);
                }
            }
        }
        hopperRowIntoChest(b, -1, size - 1, -size + 1, size - 1, size);
        b.facing(0, 0, size, Material.SPRUCE_FENCE_GATE, BlockFace.SOUTH);
        postHangingLantern(b, -size, 0, -size, Material.LANTERN);
        postHangingLantern(b, size, 0, -size, Material.LANTERN);
        spawnPad(b, 0, size + 1);
        spawnPad(b, 0, size + 2);
        b.set(-1, 0, size + 2, Material.COMPOSTER);
        return b.build("potato", "Potato farm - water hoppers, chests, composters", 0, 0, size + 2);
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
        hopperRowIntoChest(b, -2, 3, -4, 4, 4);
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


    /**
     * Kelp aquarium farm (block-by-block):
     * glass tank holds water; kelp grows to tip at y=3 on front row z=2;
     * piston at tip height faces kelp; observer on piston watches tip cell;
     * broken pieces float up into top water → hoppers → chest.
     */
    public static @NotNull BaseTemplates.BaseBlueprint kelp() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();

        // Sand + water + kelp columns (tip growth space at y=3)
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 2; z++) {
                b.set(x, -1, z, Material.SAND);
                b.set(x, -2, z, Material.STONE);
                for (int y = 0; y <= 4; y++) {
                    b.set(x, y, z, Material.WATER);
                }
                b.set(x, 0, z, Material.KELP);
                b.set(x, 1, z, Material.KELP_PLANT);
                b.set(x, 2, z, Material.KELP_PLANT);
                b.set(x, 3, z, Material.WATER); // tip grows into this cell
            }
        }

        // Glass aquarium shell
        for (int y = -1; y <= 5; y++) {
            for (int x = -4; x <= 4; x++) {
                b.set(x, y, -4, Material.GLASS);
                b.set(x, y, 3, Material.GLASS);
            }
            for (int z = -4; z <= 3; z++) {
                b.set(-4, y, z, Material.GLASS);
                b.set(4, y, z, Material.GLASS);
            }
        }

        // Front-row harvest: piston at tip Y faces kelp at z=2; observer on piston faces tip
        // (was at z=4 / wrong Y so the head never hit kelp)
        for (int x = -3; x <= 3; x++) {
            b.set(x, 3, 3, Material.AIR); // open glass wall for piston body
            b.set(x, 4, 3, Material.AIR);
            b.facing(x, 3, 3, Material.PISTON, BlockFace.NORTH); // extends into (x,3,2) tip
            b.facing(x, 4, 3, Material.OBSERVER, BlockFace.NORTH); // watches (x,4,2); growth to y=4 also works
            // Also clear watch cell above tip so observer sees block updates as kelp grows past y=3
            b.set(x, 4, 2, Material.WATER);
            b.set(x, 3, 4, Material.SMOOTH_STONE);
            b.set(x, 4, 4, Material.REDSTONE_WIRE); // observer output
            b.set(x, 2, 3, Material.SMOOTH_STONE);
            b.set(x, 2, 4, Material.SMOOTH_STONE);
        }

        // Open top flush: floating kelp items → water → hoppers → chest
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 2; z++) {
                b.set(x, 5, z, Material.AIR);
            }
            b.set(x, 5, 2, Material.WATER);
            b.set(x, 5, 3, Material.WATER);
            b.set(x, 5, 4, Material.WATER);
        }
        hopperRowIntoChest(b, 5, 5, -3, 3, 6);

        b.set(-5, 5, 0, Material.STONE);
        b.set(5, 5, 0, Material.STONE);
        b.hangingLantern(-5, 4, 0, Material.LANTERN, 5);
        b.hangingLantern(5, 4, 0, Material.LANTERN, 5);

        spawnPad(b, 0, 8);
        spawnPad(b, 0, 9);
        b.set(-1, 0, 9, Material.CRAFTING_TABLE);
        b.set(1, 0, 9, Material.BARREL);
        return b.build(
                "kelp",
                "Kelp aquarium - glass holds water; pistons break tips; items float into hoppers",
                0, 0, 9
        );
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
        hopperRowIntoChest(b, -2, 3, -4, 4, 4);
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
     * Iron golem farm (Java 1.21 panic design, researched layout):
     * open-sky spawn deck → edge water streams (≤8 blocks) → center 2×2 drop →
     * lava blade on open trapdoors over hoppers → chest; magma kill-platform ring;
     * covered villager pods + covered zombie cage (LOS). Add 3 villagers/pod + nametag zombie.
     */
    public static @NotNull BaseTemplates.BaseBlueprint iron() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();

        // Open-sky spawn deck (NO roof — golems need this solid platform)
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                b.set(x, 8, z, Material.STONE_BRICKS);
                b.set(x, 9, z, Material.AIR);
                b.set(x, 10, z, Material.AIR);
                b.set(x, 11, z, Material.AIR);
            }
        }

        // Center 2×2 drop hole through the deck
        for (int x = -1; x <= 0; x++) {
            for (int z = -1; z <= 0; z++) {
                b.set(x, 8, z, Material.AIR);
            }
        }

        // Water sources on mid-edges ≤7 from hole (corner sources never reached center —
        // Java water only flows 8 blocks). Sources only — middles fill as flowing currents.
        b.set(-1, 9, -7, Material.WATER);
        b.set(0, 9, -7, Material.WATER);
        b.set(-1, 9, 6, Material.WATER);
        b.set(0, 9, 6, Material.WATER);
        b.set(-7, 9, -1, Material.WATER);
        b.set(-7, 9, 0, Material.WATER);
        b.set(6, 9, -1, Material.WATER);
        b.set(6, 9, 0, Material.WATER);

        // Continuous 2×2 drop tunnel from deck down to kill chamber
        for (int y = 1; y <= 8; y++) {
            for (int x = -2; x <= 1; x++) {
                for (int z = -2; z <= 1; z++) {
                    boolean wall = x == -2 || x == 1 || z == -2 || z == 1;
                    boolean shaft = x >= -1 && x <= 0 && z >= -1 && z <= 0;
                    if (wall) {
                        b.set(x, y, z, Material.STONE_BRICKS);
                    } else if (shaft) {
                        b.set(x, y, z, Material.AIR);
                    }
                }
            }
        }

        // Signs just under the deck hole — water cannot pass signs, golems can.
        b.facing(-1, 7, -1, Material.OAK_WALL_SIGN, BlockFace.EAST);
        b.facing(-1, 7, 0, Material.OAK_WALL_SIGN, BlockFace.EAST);
        b.facing(0, 7, -1, Material.OAK_WALL_SIGN, BlockFace.WEST);
        b.facing(0, 7, 0, Material.OAK_WALL_SIGN, BlockFace.WEST);
        b.facing(-1, 6, -1, Material.OAK_WALL_SIGN, BlockFace.SOUTH);
        b.facing(0, 6, -1, Material.OAK_WALL_SIGN, BlockFace.SOUTH);
        b.facing(-1, 6, 0, Material.OAK_WALL_SIGN, BlockFace.NORTH);
        b.facing(0, 6, 0, Material.OAK_WALL_SIGN, BlockFace.NORTH);

        // Kill platform: hoppers under open trapdoors; lava blade at golem head height;
        // magma ring is the visible burn floor around the drop (golems die in lava; drops into hoppers).
        for (int x = -1; x <= 0; x++) {
            for (int z = -1; z <= 0; z++) {
                b.facing(x, 1, z, Material.IRON_TRAPDOOR, BlockFace.SOUTH);
                b.set(x, 2, z, Material.LAVA);
                b.facing(x, 0, z, Material.HOPPER, BlockFace.SOUTH);
            }
        }
        // Magma burn platform around the drop (keep z<=0 so +Z collection hoppers stay clear)
        for (int x = -3; x <= 2; x++) {
            for (int z = -3; z <= 0; z++) {
                boolean shaft = x >= -1 && x <= 0 && z >= -1 && z <= 0;
                if (!shaft) {
                    b.set(x, 0, z, Material.MAGMA_BLOCK);
                    b.set(x, 1, z, Material.MAGMA_BLOCK);
                }
            }
        }
        // Fully glass-boxed lava markers (no leak into collection)
        for (int y = 0; y <= 2; y++) {
            b.set(-4, y, -1, Material.GLASS);
            b.set(-4, y, 0, Material.GLASS);
            b.set(-4, y, 1, Material.GLASS);
            b.set(-3, y, -1, Material.GLASS);
            b.set(-3, y, 1, Material.GLASS);
            b.set(3, y, -1, Material.GLASS);
            b.set(3, y, 0, Material.GLASS);
            b.set(3, y, 1, Material.GLASS);
            b.set(2, y, -1, Material.GLASS);
            b.set(2, y, 1, Material.GLASS);
        }
        b.set(-3, 1, 0, Material.LAVA);
        b.set(-3, 0, 0, Material.MAGMA_BLOCK);
        b.set(-3, 2, 0, Material.GLASS);
        b.set(2, 1, 0, Material.LAVA);
        b.set(2, 0, 0, Material.MAGMA_BLOCK);
        b.set(2, 2, 0, Material.GLASS);

        b.facing(-1, 0, 1, Material.HOPPER, BlockFace.EAST);
        b.facing(0, 0, 1, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -1, 1, Material.CHEST);
        b.facing(-1, -1, 1, Material.CHEST, BlockFace.SOUTH);
        b.facing(1, 0, 1, Material.HOPPER, BlockFace.WEST);
        b.facing(1, -1, 1, Material.BARREL, BlockFace.SOUTH);

        // Collection / AFK alcove on +Z of kill chamber (magma floor trim)
        for (int x = -2; x <= 2; x++) {
            for (int z = 2; z <= 5; z++) {
                b.set(x, -1, z, Material.STONE_BRICKS);
                b.set(x, 0, z, z == 2 ? Material.MAGMA_BLOCK : Material.STONE_BRICKS);
                b.set(x, 1, z, Material.AIR);
                b.set(x, 2, z, Material.AIR);
                b.set(x, 3, z, Material.STONE_BRICK_SLAB);
            }
        }
        b.set(0, 1, 2, Material.AIR);
        b.set(0, 2, 2, Material.AIR);
        b.set(0, 3, 4, Material.STONE_BRICKS);
        b.hangingLantern(0, 3, 4, Material.LANTERN, 3);

        // Covered villager pods (glass + roof) left/right — NOT open to sky
        for (int side : new int[]{-9, 9}) {
            int outer = side < 0 ? side - 1 : side + 1;
            for (int dz = -4; dz <= 3; dz++) {
                b.set(side, 8, dz, Material.STONE_BRICKS);
                b.set(outer, 8, dz, Material.STONE_BRICKS);
            }
            for (int i = 0; i < 3; i++) {
                int z = -3 + i * 2;
                BlockFace bedFace = side < 0 ? BlockFace.EAST : BlockFace.WEST;
                b.bed(side, 9, z, Material.RED_BED, bedFace);
                b.set(side, 9, z + 1, Material.COMPOSTER);
            }
            for (int y = 9; y <= 11; y++) {
                for (int dz = -4; dz <= 3; dz++) {
                    b.set(outer, y, dz, Material.GLASS);
                    if (dz == -4 || dz == 3) {
                        b.set(side, y, dz, Material.GLASS);
                    }
                }
                // roof
                for (int dz = -4; dz <= 3; dz++) {
                    b.set(side, 12, dz, Material.STONE_BRICK_SLAB);
                    b.set(outer, 12, dz, Material.STONE_BRICK_SLAB);
                }
            }
            // Iron bars window toward platform / zombie (line of sight)
            b.set(side, 9, 0, Material.IRON_BARS);
            b.set(side, 10, 0, Material.IRON_BARS);
        }

        // Covered zombie cage (roof so it never burns) with LOS to both pods
        for (int x = -1; x <= 1; x++) {
            for (int z = -10; z <= -8; z++) {
                b.set(x, 8, z, Material.STONE_BRICKS);
            }
        }
        b.set(0, 9, -9, Material.AIR);
        b.set(0, 10, -9, Material.AIR);
        for (int x = -1; x <= 1; x++) {
            b.set(x, 9, -8, Material.IRON_BARS);
            b.set(x, 10, -8, Material.IRON_BARS);
            b.set(x, 9, -10, Material.IRON_BARS);
            b.set(x, 10, -10, Material.IRON_BARS);
            b.set(x, 11, -9, Material.STONE_BRICKS); // roof
        }
        b.set(-1, 9, -9, Material.IRON_BARS);
        b.set(1, 9, -9, Material.IRON_BARS);
        b.set(-1, 10, -9, Material.IRON_BARS);
        b.set(1, 10, -9, Material.IRON_BARS);

        spawnPad(b, 0, 6);
        spawnPad(b, 0, 7);
        b.set(-1, 0, 7, Material.CRAFTING_TABLE);
        b.set(1, 0, 7, Material.BARREL);
        return b.build(
                "iron",
                "Iron farm - open deck, center drop tunnel, lava→hoppers→chest (add villagers + zombie)",
                0, 0, 7
        );
    }


    /**
     * Dark-room XP mob farm (researched 22-block drop design):
     * enclosed spawn pads → water SOURCE only at channel ends (flowing current) →
     * continuous 2×2 drop shaft through both floors (~22 blocks) →
     * hopper + bottom-slab kill floor (one-hit XP) with magma trim platform → chest.
     * Signs stop water at the hole; AFK room beside the kill chamber.
     */
    public static @NotNull BaseTemplates.BaseBlueprint xp() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();

        // ——— AFK + kill house at ground ———
        for (int x = -4; x <= 4; x++) {
            for (int z = 0; z <= 10; z++) {
                b.set(x, -2, z, Material.STONE_BRICKS);
                b.set(x, -1, z, Material.STONE_BRICKS);
                boolean wall = x == -4 || x == 4 || z == 0 || z == 10;
                if (wall) {
                    for (int y = 0; y <= 3; y++) {
                        b.set(x, y, z, Material.STONE_BRICKS);
                    }
                } else {
                    b.set(x, 0, z, Material.STONE_BRICKS);
                    b.set(x, 1, z, Material.AIR);
                    b.set(x, 2, z, Material.AIR);
                    b.set(x, 3, z, Material.STONE_BRICKS);
                }
            }
        }

        // Front door + wall buttons
        b.set(0, 1, 10, Material.AIR);
        b.set(0, 2, 10, Material.AIR);
        b.door(0, 1, 10, Material.IRON_DOOR, BlockFace.SOUTH);
        b.facing(-1, 2, 11, Material.STONE_BUTTON, BlockFace.SOUTH);
        b.facing(-1, 2, 9, Material.STONE_BUTTON, BlockFace.NORTH);

        // Kill floor: hoppers + bottom slabs (22-block fall → ~1 HP for XP).
        // Magma trim around the landing so any leftover mobs burn (items still hoppered).
        for (int x = -1; x <= 0; x++) {
            for (int z = 1; z <= 2; z++) {
                b.facing(x, 0, z, Material.HOPPER, BlockFace.SOUTH);
                b.slab(x, 1, z, Material.STONE_SLAB, Slab.Type.BOTTOM);
            }
        }
        for (int x = -3; x <= 2; x++) {
            for (int z = 0; z <= 3; z++) {
                boolean landing = (x == -1 || x == 0) && (z == 1 || z == 2);
                if (!landing) {
                    b.set(x, 0, z, Material.MAGMA_BLOCK);
                }
            }
        }
        b.facing(-1, 0, 3, Material.HOPPER, BlockFace.EAST);
        b.facing(0, 0, 3, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -1, 3, Material.CHEST);
        b.facing(-1, -1, 3, Material.CHEST, BlockFace.SOUTH);
        b.facing(1, 0, 3, Material.HOPPER, BlockFace.WEST);
        b.facing(1, -1, 3, Material.BARREL, BlockFace.SOUTH);

        // Safety bars between AFK (z=5+) and kill (z=1..2)
        for (int x = -3; x <= 3; x++) {
            b.set(x, 1, 4, Material.IRON_BARS);
            b.set(x, 2, 4, Material.IRON_BARS);
        }
        b.set(0, 1, 4, Material.AIR);
        b.set(0, 2, 4, Material.AIR);

        b.set(-3, 1, 7, Material.CRAFTING_TABLE);
        b.set(-3, 1, 8, Material.ANVIL);
        b.set(3, 1, 7, Material.BARREL);
        b.set(3, 1, 8, Material.CHEST);
        b.hangingLantern(0, 3, 7, Material.LANTERN, 3);

        // ——— Continuous 2×2 drop shaft y=2..31 (through BOTH spawn floors) ———
        for (int y = 2; y <= 31; y++) {
            for (int x = -2; x <= 1; x++) {
                for (int z = 0; z <= 3; z++) {
                    boolean wall = x == -2 || x == 1 || z == 0 || z == 3;
                    boolean shaft = (x == -1 || x == 0) && (z == 1 || z == 2);
                    if (wall) {
                        b.set(x, y, z, Material.COBBLESTONE);
                    } else if (shaft) {
                        b.set(x, y, z, Material.AIR);
                    }
                }
            }
        }
        // Re-apply kill landing after shaft walls
        for (int x = -1; x <= 0; x++) {
            for (int z = 1; z <= 2; z++) {
                b.facing(x, 0, z, Material.HOPPER, BlockFace.SOUTH);
                b.slab(x, 1, z, Material.STONE_SLAB, Slab.Type.BOTTOM);
                for (int y = 2; y <= 31; y++) {
                    b.set(x, y, z, Material.AIR);
                }
            }
        }

        // Outside ladder for maintenance
        for (int y = 1; y <= 30; y++) {
            b.facing(-3, y, 1, Material.LADDER, BlockFace.WEST);
        }

        // ——— Two dark enclosed spawn floors (roofed — not open sky) ———
        // Floor A at y=24, Floor B at y=28. Water: SOURCES only at outer ends so currents push mobs.
        for (int floor = 0; floor < 2; floor++) {
            int y = 24 + floor * 4;
            for (int x = -8; x <= 7; x++) {
                for (int z = -6; z <= 5; z++) {
                    b.set(x, y, z, Material.COBBLESTONE);
                    b.set(x, y + 1, z, Material.AIR);
                    b.set(x, y + 2, z, Material.AIR);
                    b.set(x, y + 3, z, Material.COBBLESTONE); // solid dark roof
                }
            }
            for (int yy = y + 1; yy <= y + 2; yy++) {
                for (int x = -8; x <= 7; x++) {
                    b.set(x, yy, -6, Material.COBBLESTONE);
                    b.set(x, yy, 5, Material.COBBLESTONE);
                }
                for (int z = -6; z <= 5; z++) {
                    b.set(-8, yy, z, Material.COBBLESTONE);
                    b.set(7, yy, z, Material.COBBLESTONE);
                }
            }
            // 2×2 hole into continuous shaft
            for (int x = -1; x <= 0; x++) {
                for (int z = 1; z <= 2; z++) {
                    b.set(x, y, z, Material.AIR);
                    b.set(x, y + 1, z, Material.AIR);
                    b.set(x, y + 2, z, Material.AIR);
                    b.set(x, y + 3, z, Material.AIR); // open roof over shaft so Floor B can drop through
                }
            }
            // Four channels: sources ONLY at outer ends (still water if every cell is a source)
            b.set(-1, y + 1, -5, Material.WATER);
            b.set(0, y + 1, -5, Material.WATER);
            b.set(-1, y + 1, 4, Material.WATER);
            b.set(0, y + 1, 4, Material.WATER);
            b.set(-7, y + 1, 1, Material.WATER);
            b.set(-7, y + 1, 2, Material.WATER);
            b.set(6, y + 1, 1, Material.WATER);
            b.set(6, y + 1, 2, Material.WATER);
            // Dry hole rim
            for (int x = -1; x <= 0; x++) {
                for (int z = 1; z <= 2; z++) {
                    b.set(x, y + 1, z, Material.AIR);
                }
            }
            // Dry corner spawn pads
            for (int x = -7; x <= -3; x++) {
                for (int z = -5; z <= -1; z++) {
                    b.set(x, y + 1, z, Material.AIR);
                }
                for (int z = 3; z <= 4; z++) {
                    b.set(x, y + 1, z, Material.AIR);
                }
            }
            for (int x = 2; x <= 6; x++) {
                for (int z = -5; z <= -1; z++) {
                    b.set(x, y + 1, z, Material.AIR);
                }
                for (int z = 3; z <= 4; z++) {
                    b.set(x, y + 1, z, Material.AIR);
                }
            }
        }

        // Re-open shaft through both floors (floors rewrite solid roofs over the hole)
        for (int y = 24; y <= 31; y++) {
            for (int x = -1; x <= 0; x++) {
                for (int z = 1; z <= 2; z++) {
                    b.set(x, y, z, Material.AIR);
                }
            }
        }

        // Signs under each floor hole — block water cascade, allow mobs through
        for (int signY : new int[]{23, 27}) {
            b.facing(-1, signY, 1, Material.OAK_WALL_SIGN, BlockFace.EAST);
            b.facing(-1, signY, 2, Material.OAK_WALL_SIGN, BlockFace.EAST);
            b.facing(0, signY, 1, Material.OAK_WALL_SIGN, BlockFace.WEST);
            b.facing(0, signY, 2, Material.OAK_WALL_SIGN, BlockFace.WEST);
        }

        spawnPad(b, 0, 11);
        spawnPad(b, 0, 12);
        b.set(-1, 0, 12, Material.CRAFTING_TABLE);
        b.set(1, 0, 12, Material.BARREL);
        return b.build(
                "xp",
                "XP mob farm - flowing water channels, continuous drop shaft, slab+magma kill platform",
                0, 0, 12
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

    /**
     * Face toward a collection cell (used so canal hoppers actually drain into the chest line).
     */
    private static @NotNull BlockFace hopperTowardPoint(int x, int z, int tx, int tz) {
        int dx = tx - x;
        int dz = tz - z;
        if (dx == 0 && dz == 0) {
            return BlockFace.DOWN;
        }
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx > 0 ? BlockFace.EAST : BlockFace.WEST;
        }
        return dz > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }


    /**
     * Connected hopper row → DOWN hopper → double chest.
     * Row hoppers feed toward x=0; center walks along Z to chestZ, then drops into the chest.
     * Barrel sits beside the double chest (extra storage).
     */
    private static void hopperRowIntoChest(
            @NotNull BaseTemplates.Builder b,
            int hopperY,
            int lineZ,
            int xMin,
            int xMax,
            int chestZ
    ) {
        for (int x = xMin; x <= xMax; x++) {
            BlockFace face;
            if (x < 0) {
                face = BlockFace.EAST;
            } else if (x > 0) {
                face = BlockFace.WEST;
            } else if (lineZ < chestZ) {
                face = BlockFace.SOUTH;
            } else if (lineZ > chestZ) {
                face = BlockFace.NORTH;
            } else {
                face = BlockFace.DOWN;
            }
            b.facing(x, hopperY, lineZ, Material.HOPPER, face);
        }
        if (lineZ != chestZ) {
            int step = lineZ < chestZ ? 1 : -1;
            BlockFace along = step > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
            for (int z = lineZ + step; z != chestZ; z += step) {
                b.facing(0, hopperY, z, Material.HOPPER, along);
            }
            b.facing(0, hopperY, chestZ, Material.HOPPER, BlockFace.DOWN);
        } else {
            b.facing(0, hopperY, chestZ, Material.HOPPER, BlockFace.DOWN);
        }
        b.set(0, hopperY - 1, chestZ, Material.CHEST);
        b.facing(-1, hopperY - 1, chestZ, Material.CHEST, BlockFace.SOUTH);
        b.facing(1, hopperY - 1, chestZ, Material.BARREL, BlockFace.SOUTH);
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
