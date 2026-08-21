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

    /**
     * Hydrated crop plot + farmer villager station (true auto re-farm for wheat/potato).
     * Villagers harvest mature crops and replant; crops go into the composter -> hopper -> chest.
     * Put one farmer villager in the bed/composter pod after paste.
     */
    public static @NotNull BaseTemplates.BaseBlueprint wheat() {
        return cropVillagerFarm(
                "wheat",
                Material.WHEAT,
                "Auto wheat - farmer villager harvests+replants; composter to chest"
        );
    }

    /**
     * Observer + piston sugar cane farm (classic leave-base design).
     * Piston breaks the 2nd cane when the 3rd grows into the observer; bottom cane stays and regrows.
     * Broken pieces fall into the water channel -> hoppers -> chest.
     */
    public static @NotNull BaseTemplates.BaseBlueprint cane() {
        BaseTemplates.Builder c = new BaseTemplates.Builder();
        int rows = 6;
        for (int x = -rows; x <= rows; x++) {
            c.set(x, 0, 1, Material.SAND);
            c.set(x, 1, 1, Material.SUGAR_CANE);
            c.set(x, 2, 1, Material.AIR);
            c.set(x, 3, 1, Material.AIR);
            c.facing(x, 2, 2, Material.PISTON, BlockFace.NORTH);
            c.facing(x, 3, 2, Material.OBSERVER, BlockFace.NORTH);
            c.set(x, 1, 2, Material.AIR);
            c.set(x, 1, 3, Material.SMOOTH_STONE);
            c.set(x, 2, 3, Material.SMOOTH_STONE);
            c.set(x, 3, 3, Material.REDSTONE_WIRE);
            c.set(x, 0, 3, Material.SMOOTH_STONE);
            c.set(x, 0, 2, Material.SMOOTH_STONE);
            // Water over hoppers (hydration). Carpet row feeds south into water hoppers.
            c.facing(x, -1, -1, Material.HOPPER, BlockFace.SOUTH);
            c.set(x, 0, -1, Material.WHITE_CARPET);
            c.facing(x, -1, 0, Material.HOPPER, BlockFace.SOUTH);
            c.set(x, 0, 0, Material.WATER);
        }
        for (int x = -rows - 1; x <= rows + 1; x++) {
            c.set(x, 0, -2, Material.STONE_BRICKS);
            c.set(x, 1, -2, Material.STONE_BRICKS);
            c.slab(x, 4, 2, Material.STONE_BRICK_SLAB, Slab.Type.BOTTOM);
        }
        c.set(-rows - 1, 0, -1, Material.STONE_BRICKS);
        c.set(rows + 1, 0, -1, Material.STONE_BRICKS);
        c.set(-rows - 1, 0, 0, Material.STONE_BRICKS);
        c.set(rows + 1, 0, 0, Material.STONE_BRICKS);
        // Loot bay from water hopper row (under hydration canal)
        hopperRowIntoChest(c, -1, 0, -rows, rows, 5);
        for (int x = -rows; x <= rows; x++) {
            // Do not overwrite hopperRow facings at z=0 — only restore overlays + carpet feeders
            c.facing(x, -1, -1, Material.HOPPER, BlockFace.SOUTH);
            c.set(x, 0, -1, Material.WHITE_CARPET);
            c.set(x, 0, 0, Material.WATER);
            c.set(x, 0, 1, Material.SAND);
            c.set(x, 1, 1, Material.SUGAR_CANE);
        }
        c.set(-rows - 1, 1, 1, Material.STONE_BRICKS);
        c.set(rows + 1, 1, 1, Material.STONE_BRICKS);
        postHangingLantern(c, -rows - 1, 1, 1, Material.LANTERN);
        postHangingLantern(c, rows + 1, 1, 1, Material.LANTERN);
        spawnPad(c, 0, 6);
        spawnPad(c, 0, 7);
        c.set(-1, 0, 7, Material.CRAFTING_TABLE);
        c.set(1, 0, 7, Material.BARREL);
        return c.build("cane", "Auto sugar-cane - tip break; carpet hoppers + water collect", 0, 0, 7);
    }

    /**
     * Bamboo observer farm — same leave-base / tip-break cycle as cane.
     * Water is NOT adjacent to bamboo (would break it). Collection uses carpet→hopper.
     */
    public static @NotNull BaseTemplates.BaseBlueprint bamboo() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int rows = 5;
        for (int x = -rows; x <= rows; x++) {
            b.set(x, 0, 2, Material.SMOOTH_STONE);
            b.set(x, 1, 2, Material.SMOOTH_STONE);
            b.set(x, 2, 2, Material.REDSTONE_WIRE);
            b.set(x, -1, 2, Material.SMOOTH_STONE);
            b.set(x, -1, 1, Material.SMOOTH_STONE);
            if (x == 0) {
                // Center column reserved for hopper pipe to loot bay
                b.set(x, -1, 0, Material.SMOOTH_STONE);
                b.set(x, 0, 0, Material.AIR);
                b.set(x, 1, 0, Material.AIR);
                b.set(x, 2, 0, Material.AIR);
                continue;
            }
            b.set(x, -1, 0, Material.PODZOL);
            b.set(x, 0, 0, Material.BAMBOO);
            b.set(x, 1, 0, Material.AIR);
            b.set(x, 2, 0, Material.AIR);
            b.facing(x, 1, 1, Material.PISTON, BlockFace.NORTH);
            b.facing(x, 2, 1, Material.OBSERVER, BlockFace.NORTH);
            b.set(x, 0, 1, Material.AIR);
            // Carpet over hoppers beside stalks (NOT water — water breaks bamboo)
            b.facing(x, -1, -1, Material.HOPPER, BlockFace.SOUTH);
            b.set(x, 0, -1, Material.WHITE_CARPET);
        }
        // Solid walls contain the farm (fences do not block items/water)
        for (int x = -rows - 1; x <= rows + 1; x++) {
            b.set(x, 0, -2, Material.JUNGLE_PLANKS);
            b.set(x, 1, -2, Material.JUNGLE_PLANKS);
            b.slab(x, 3, 1, Material.JUNGLE_SLAB, Slab.Type.BOTTOM);
        }
        b.set(-rows - 1, 0, -1, Material.JUNGLE_PLANKS);
        b.set(rows + 1, 0, -1, Material.JUNGLE_PLANKS);
        hopperRowIntoChest(b, -1, -1, -rows, rows, 4);
        for (int x = -rows; x <= rows; x++) {
            if (x == 0) {
                continue;
            }
            b.set(x, 0, -1, Material.WHITE_CARPET);
            b.set(x, -1, 0, Material.PODZOL);
            b.set(x, 0, 0, Material.BAMBOO);
        }
        spawnPad(b, 0, 5);
        spawnPad(b, 0, 6);
        b.set(-1, 0, 6, Material.COMPOSTER);
        b.set(1, 0, 6, Material.CRAFTING_TABLE);
        postHangingLantern(b, -rows, 0, -2, Material.LANTERN);
        postHangingLantern(b, rows, 0, -2, Material.LANTERN);
        return b.build("bamboo", "Auto bamboo - tip break; carpet hoppers collect (no water on stalks)", 0, 0, 6);
    }

    /**
     * Melon stem farm — dry fruit pads (water in 1-deep trenches only so stems are not flooded).
     */
    public static @NotNull BaseTemplates.BaseBlueprint melon() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        for (int x = -5; x <= 5; x++) {
            for (int z = -2; z <= 2; z++) {
                b.set(x, -2, z, Material.STONE);
                if ((x & 1) != 0) {
                    // Trench: water at y=-1 over hoppers at y=-2 — pads at y=0 stay DRY
                    b.facing(x, -2, z, Material.HOPPER, BlockFace.SOUTH);
                    b.set(x, -1, z, Material.WATER);
                    b.set(x, 0, z, Material.AIR);
                } else if (((x / 2) + z) % 2 == 0) {
                    b.set(x, -1, z, Material.FARMLAND);
                    b.set(x, 0, z, Material.MELON_STEM);
                } else {
                    b.set(x, -1, z, Material.DIRT);
                    b.set(x, 0, z, Material.AIR); // fruit pad
                }
            }
            // End water sources for trench flow (odd columns only — match hopper trenches)
            b.set(x, -2, -3, Material.STONE);
            if ((x & 1) != 0) {
                b.facing(x, -2, -3, Material.HOPPER, BlockFace.SOUTH);
                b.set(x, -1, -3, Material.WATER);
            } else {
                b.set(x, -1, -3, Material.STONE);
            }
        }
        for (int x = -6; x <= 6; x++) {
            b.set(x, 0, -4, Material.STONE_BRICKS);
            b.set(x, 0, 4, Material.STONE_BRICKS);
            b.set(x, 1, -4, Material.STONE_BRICKS);
            b.set(x, 1, 4, Material.STONE_BRICKS);
        }
        for (int z = -4; z <= 4; z++) {
            b.set(-6, 0, z, Material.STONE_BRICKS);
            b.set(6, 0, z, Material.STONE_BRICKS);
            b.set(-6, 1, z, Material.STONE_BRICKS);
            b.set(6, 1, z, Material.STONE_BRICKS);
        }
        b.set(0, 0, 4, Material.AIR);
        b.set(0, 1, 4, Material.AIR);
        b.facing(0, 0, 4, Material.OAK_FENCE_GATE, BlockFace.SOUTH);
        // Loot bay PAST the plot so hopperRow does not wipe farmland (lineZ=3 was on stems)
        hopperRowIntoChest(b, -2, 3, -5, 5, 6);
        // Re-assert grow soil after helper
        for (int x = -5; x <= 5; x++) {
            for (int z = -2; z <= 2; z++) {
                if ((x & 1) != 0) {
                    b.facing(x, -2, z, Material.HOPPER, BlockFace.SOUTH);
                    b.set(x, -1, z, Material.WATER);
                    b.set(x, 0, z, Material.AIR);
                } else if (((x / 2) + z) % 2 == 0) {
                    b.set(x, -1, z, Material.FARMLAND);
                    b.set(x, 0, z, Material.MELON_STEM);
                } else {
                    b.set(x, -1, z, Material.DIRT);
                    b.set(x, 0, z, Material.AIR);
                }
            }
        }
        spawnPad(b, 0, 8);
        spawnPad(b, 0, 9);
        postHangingLantern(b, -5, 0, -4, Material.LANTERN);
        postHangingLantern(b, 5, 0, -4, Material.LANTERN);
        b.set(-2, 0, 9, Material.CRAFTING_TABLE);
        b.set(2, 0, 9, Material.COMPOSTER);
        return b.build("melon", "Melon farm - dry pads; trench water/hoppers; stems stay", 0, 0, 9);
    }

    /** Nether wart farm on soul sand with hopper trenches between columns. */
    public static @NotNull BaseTemplates.BaseBlueprint nether() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 3; z++) {
                b.set(x, -2, z, Material.BLACKSTONE);
                boolean edge = x == -4 || x == 4 || z == -4 || z == 3;
                if (edge) {
                    b.set(x, -1, z, Material.POLISHED_BLACKSTONE_BRICKS);
                    b.set(x, 0, z, Material.CRIMSON_FENCE);
                } else if ((x & 1) == 0) {
                    b.set(x, -1, z, Material.SOUL_SAND);
                    b.set(x, 0, z, Material.NETHER_WART);
                } else {
                    BlockFace face = z < 3 ? BlockFace.SOUTH : BlockFace.DOWN;
                    b.facing(x, -1, z, Material.HOPPER, face);
                    b.set(x, 0, z, Material.AIR);
                }
            }
        }
        b.facing(0, 0, 3, Material.CRIMSON_FENCE_GATE, BlockFace.SOUTH);
        postHangingLantern(b, -4, 0, -4, Material.SOUL_LANTERN);
        postHangingLantern(b, 4, 0, -4, Material.SOUL_LANTERN);
        postHangingLantern(b, 4, 0, 3, Material.SOUL_LANTERN);
        postHangingLantern(b, -4, 0, 3, Material.SOUL_LANTERN);
        // lineZ=3 past soul-sand rows — do NOT wipe wart soil at z=2
        hopperRowIntoChest(b, -1, 3, -3, 3, 6);
        // Re-assert soul sand + wart after helper
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 2; z++) {
                if (x == -4 || x == 4 || z == -4) {
                    continue;
                }
                if ((x & 1) == 0) {
                    b.set(x, -1, z, Material.SOUL_SAND);
                    b.set(x, 0, z, Material.NETHER_WART);
                }
            }
        }
        spawnPad(b, 0, 7);
        spawnPad(b, 0, 8);
        b.set(-1, 0, 8, Material.CRAFTING_TABLE);
        b.set(1, 0, 8, Material.BARREL);
        return b.build("nether", "Nether wart farm - soul sand, hoppers, crimson trim", 0, 0, 8);
    }

    /**
     * Four animal pens with feed chests + hay. No hopper lines — animals do not auto-drop
     * into hoppers; chests are for player feed/loot storage only.
     */
    public static @NotNull BaseTemplates.BaseBlueprint animal() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        pen(b, -7, -2, -7, -2, Material.OAK_FENCE, Material.OAK_FENCE_GATE);
        pen(b, 2, 7, -7, -2, Material.SPRUCE_FENCE, Material.SPRUCE_FENCE_GATE);
        pen(b, -7, -2, 2, 7, Material.BIRCH_FENCE, Material.BIRCH_FENCE_GATE);
        pen(b, 2, 7, 2, 7, Material.JUNGLE_FENCE, Material.JUNGLE_FENCE_GATE);

        // Seal outer mid-corridors so animals cannot walk out between pens
        for (int z = -1; z <= 1; z++) {
            b.set(-7, 1, z, Material.OAK_FENCE);
            b.set(7, 1, z, Material.JUNGLE_FENCE);
            b.set(-7, 0, z, Material.GRASS_BLOCK);
            b.set(7, 0, z, Material.GRASS_BLOCK);
        }
        for (int x = -1; x <= 1; x++) {
            b.set(x, 1, -7, Material.SPRUCE_FENCE);
            b.set(x, 1, 7, Material.BIRCH_FENCE);
            b.set(x, 0, -7, Material.GRASS_BLOCK);
            b.set(x, 0, 7, Material.GRASS_BLOCK);
        }
        for (int z = -2; z <= 2; z++) {
            if (z >= -1 && z <= 1) {
                continue;
            }
            b.set(-2, 1, z, Material.OAK_FENCE);
            b.set(2, 1, z, Material.SPRUCE_FENCE);
        }
        for (int x = -2; x <= 2; x++) {
            if (x >= -1 && x <= 1) {
                continue;
            }
            b.set(x, 1, -2, Material.OAK_FENCE);
            b.set(x, 1, 2, Material.BIRCH_FENCE);
        }

        // Courtyard: crafting + feed storage (no hoppers — nothing auto-collects here)
        b.set(0, 0, 0, Material.CRAFTING_TABLE);
        b.facing(-1, 0, 0, Material.CHEST, BlockFace.SOUTH);
        b.facing(1, 0, 0, Material.CHEST, BlockFace.SOUTH);
        b.facing(0, 0, 1, Material.BARREL, BlockFace.SOUTH);
        b.set(0, 0, -1, Material.HAY_BLOCK);
        b.set(0, 4, 0, Material.OAK_PLANKS);
        b.hangingLantern(0, 3, 0, Material.LANTERN, 4);
        spawnPad(b, 0, 9);
        spawnPad(b, 0, 10);
        return b.build("animal", "4 pens with cows/sheep/pigs/chickens - feed chests, hay, gates", 0, 0, 10);
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
        for (int x = -5; x <= 5; x++) {
            b.set(x, 0, -4, Material.SANDSTONE_WALL);
            b.set(x, 0, 3, Material.SANDSTONE_WALL);
        }
        hopperRowIntoChest(b, -1, 3, -3, 5, 6);
        spawnPad(b, 0, 7);
        spawnPad(b, 0, 8);
        postHangingLantern(b, -4, 0, -4, Material.LANTERN);
        postHangingLantern(b, 4, 0, -4, Material.LANTERN);
        return b.build("cactus", "Cactus farm - auto-break fences, side hoppers, chests", 0, 0, 8);
    }

    /** Potato farm — same farmer-villager auto re-plant as wheat. */
    public static @NotNull BaseTemplates.BaseBlueprint potato() {
        return cropVillagerFarm(
                "potato",
                Material.POTATOES,
                "Auto potato - farmer villager harvests+replants; composter to chest"
        );
    }

    /** Jungle cocoa farm — pods hang over open hopper floors (not under solid logs). */
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
            }
        }
        // Open hopper floor under the hanging cocoa (air at pod feet, hoppers below)
        for (int x = -4; x <= 4; x++) {
            for (int z = -3; z <= 2; z++) {
                if ((x & 1) != 0) {
                    b.set(x, 0, z, Material.AIR);
                    b.facing(x, -1, z, Material.HOPPER, BlockFace.SOUTH);
                }
            }
        }
        hopperRowIntoChest(b, -1, 3, -4, 4, 6);
        for (int x = -5; x <= 5; x++) {
            b.set(x, 3, -3, Material.JUNGLE_SLAB);
            b.set(x, 3, 2, Material.JUNGLE_SLAB);
        }
        b.set(-4, 4, -3, Material.JUNGLE_PLANKS);
        b.set(4, 4, -3, Material.JUNGLE_PLANKS);
        b.hangingLantern(-4, 3, -3, Material.LANTERN, 4);
        b.hangingLantern(4, 3, -3, Material.LANTERN, 4);
        // Re-assert loot bay after decorative slabs (must stay last for hoppers)
        hopperRowIntoChest(b, -1, 3, -4, 4, 6);
        spawnPad(b, 0, 7);
        spawnPad(b, 0, 8);
        return b.build("cocoa", "Cocoa farm - jungle logs, hoppers, hanging lanterns", 0, 0, 8);
    }


    /**
     * Kelp aquarium with a real item path to hoppers:
     * <ol>
     *   <li>Pistons break tips in the tank</li>
     *   <li>Items float up to the open water surface (no solid roof / stone slab in the way)</li>
     *   <li>North-only water sources at y=5 create a south-flowing stream</li>
     *   <li>Stream runs over hoppers at y=4 — hoppers suck items, then pipe to ground chests</li>
     * </ol>
     * Static water everywhere does not push items; solid stone above observers blocks float-up.
     */
    public static @NotNull BaseTemplates.BaseBlueprint kelp() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();

        // Grow tank: sand + water + kelp (surface at y=4 — open to sky inside walls)
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 2; z++) {
                b.set(x, -1, z, Material.SAND);
                b.set(x, -2, z, Material.STONE);
                for (int y = 0; y <= 4; y++) {
                    b.set(x, y, z, Material.WATER);
                }
                b.set(x, 0, z, Material.KELP);
                b.set(x, 1, z, Material.WATER);
                b.set(x, 2, z, Material.WATER);
                b.set(x, 3, z, Material.WATER);
            }
        }

        // Glass walls only — NO glass roof over the grow tank (items must float to surface)
        for (int y = -1; y <= 6; y++) {
            for (int x = -4; x <= 4; x++) {
                b.set(x, y, -4, Material.GLASS);
                b.set(x, y, 6, Material.GLASS);
            }
            for (int z = -4; z <= 6; z++) {
                b.set(-4, y, z, Material.GLASS);
                b.set(4, y, z, Material.GLASS);
            }
        }

        // Harvest: piston at tip Y; observer on top faces kelp; dust south of observer (not on hoppers)
        for (int x = -3; x <= 3; x++) {
            b.set(x, 3, 3, Material.AIR);
            b.facing(x, 3, 3, Material.PISTON, BlockFace.NORTH);
            b.facing(x, 4, 3, Material.OBSERVER, BlockFace.NORTH);
            b.set(x, 3, 4, Material.SMOOTH_STONE);
            b.set(x, 4, 4, Material.REDSTONE_WIRE); // observer output → powers piston via stone
            b.set(x, 2, 3, Material.SMOOTH_STONE);
            b.set(x, 2, 4, Material.SMOOTH_STONE);
        }

        // Hoppers only at z=5 under the stream (z=4 kept for redstone)
        for (int x = -3; x <= 3; x++) {
            b.facing(x, 4, 5, Material.HOPPER, BlockFace.SOUTH);
        }
        hopperRowIntoChest(b, 4, 5, -3, 3, 8);
        // Re-assert redstone after hopper helper (must not wipe observer circuit)
        for (int x = -3; x <= 3; x++) {
            b.set(x, 3, 4, Material.SMOOTH_STONE);
            b.set(x, 4, 4, Material.REDSTONE_WIRE);
            b.facing(x, 4, 3, Material.OBSERVER, BlockFace.NORTH);
            b.facing(x, 3, 3, Material.PISTON, BlockFace.NORTH);
        }

        // Flowing stream at y=5: SOURCE only on the NORTH edge of the tank.
        // AIR toward the south so water actually flows over hoppers after paste ticks.
        for (int x = -3; x <= 3; x++) {
            for (int z = -2; z <= 5; z++) {
                b.set(x, 5, z, Material.AIR);
            }
            b.set(x, 5, -3, Material.WATER);
        }
        // Glass lid contains the stream
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 5; z++) {
                b.set(x, 6, z, Material.GLASS);
            }
        }
        // South glass wall — leave hopper pipe column open
        for (int y = -1; y <= 6; y++) {
            for (int x = -4; x <= 4; x++) {
                if (x == 0 && y >= 1 && y <= 5) {
                    continue;
                }
                b.set(x, y, 6, Material.GLASS);
            }
        }

        b.set(-5, 4, 0, Material.STONE);
        b.set(5, 4, 0, Material.STONE);
        b.hangingLantern(-5, 3, 0, Material.LANTERN, 4);
        b.hangingLantern(5, 3, 0, Material.LANTERN, 4);

        spawnPad(b, 0, 10);
        spawnPad(b, 0, 11);
        b.set(-1, 0, 11, Material.CRAFTING_TABLE);
        b.set(1, 0, 11, Material.BARREL);
        return b.build(
                "kelp",
                "Kelp aquarium - open surface, flowing stream over hoppers → chests",
                0, 0, 11
        );
    }

    /** Dark mushroom hut — no interior lights; hopper trenches between mycelium columns. */
    public static @NotNull BaseTemplates.BaseBlueprint mushroom() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 3; z++) {
                b.set(x, -2, z, Material.STONE);
                boolean edge = x == -4 || x == 4 || z == -4 || z == 3;
                if (edge) {
                    b.set(x, -1, z, Material.MYCELIUM);
                    b.set(x, 0, z, Material.DARK_OAK_LOG);
                    b.set(x, 1, z, Material.DARK_OAK_LOG);
                    b.set(x, 2, z, Material.DARK_OAK_LOG);
                } else if ((x & 1) == 0) {
                    b.set(x, -1, z, Material.MYCELIUM);
                    b.set(x, 0, z, (x + z) % 2 == 0 ? Material.RED_MUSHROOM : Material.BROWN_MUSHROOM);
                } else {
                    // Odd-X trenches feed south into hopperRow (do not place on z=3 — row wins)
                    b.facing(x, -1, z, Material.HOPPER, BlockFace.SOUTH);
                    b.set(x, 0, z, Material.AIR);
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
        // Last write: loot bay past the door
        hopperRowIntoChest(b, -1, 3, -3, 3, 6);
        // Exterior light only
        b.set(0, 4, -5, Material.DARK_OAK_FENCE);
        b.set(0, 5, -5, Material.LANTERN);
        spawnPad(b, 0, 7);
        spawnPad(b, 0, 8);
        return b.build("mushroom", "Dark mushroom hut - mycelium columns, hopper trenches, no interior light", 0, 0, 8);
    }


    /**
     * Iron golem farm (Java panic design):
     * <ul>
     *   <li>Mid pods: 3 beds + composters each side, clear iron-bar LOS to zombie</li>
     *   <li>Covered zombie cage centered so both pods are within panic range (~8)</li>
     *   <li>Open-sky golem deck ON TOP (y=13) with dry pads + water trenches → drop</li>
     *   <li>Lava blade + hoppers → ground chests</li>
     * </ul>
     * Plugin auto-spawns unemployed villagers (claim farmer) + nametag zombie.
     */
    public static @NotNull BaseTemplates.BaseBlueprint iron() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();

        // ——— Drop shaft (shared by top golem deck) ———
        for (int y = 1; y <= 13; y++) {
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

        // ——— Kill chamber ———
        for (int x = -1; x <= 0; x++) {
            for (int z = -1; z <= 0; z++) {
                b.facing(x, 0, z, Material.HOPPER, BlockFace.SOUTH);
                b.facing(x, 1, z, Material.IRON_TRAPDOOR, BlockFace.SOUTH);
                b.set(x, 2, z, Material.LAVA);
            }
        }
        for (int x = -3; x <= 2; x++) {
            for (int z = -3; z <= 0; z++) {
                boolean shaft = x >= -1 && x <= 0 && z >= -1 && z <= 0;
                if (!shaft) {
                    b.set(x, 0, z, Material.MAGMA_BLOCK);
                    b.set(x, 1, z, Material.MAGMA_BLOCK);
                }
            }
        }
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
        b.facing(0, 0, 1, Material.HOPPER, BlockFace.SOUTH);
        b.facing(1, 0, 1, Material.HOPPER, BlockFace.WEST);
        b.facing(-1, 0, 3, Material.CHEST, BlockFace.SOUTH);
        b.facing(0, 0, 3, Material.CHEST, BlockFace.SOUTH);
        b.facing(1, 0, 3, Material.BARREL, BlockFace.SOUTH);
        b.facing(0, 0, 2, Material.HOPPER, BlockFace.SOUTH);

        for (int x = -2; x <= 2; x++) {
            for (int z = 2; z <= 5; z++) {
                b.set(x, -1, z, Material.STONE_BRICKS);
                if (z == 2 && x >= -1 && x <= 1) {
                    continue;
                }
                if (z == 3 && x >= -1 && x <= 1) {
                    continue;
                }
                b.set(x, 0, z, z == 2 ? Material.MAGMA_BLOCK : Material.STONE_BRICKS);
                b.set(x, 1, z, Material.AIR);
                b.set(x, 2, z, Material.AIR);
                b.set(x, 3, z, Material.STONE_BRICK_SLAB);
            }
        }
        b.set(0, 3, 4, Material.STONE_BRICKS);
        b.hangingLantern(0, 3, 4, Material.LANTERN, 3);

        // ——— Mid-level panic floor (villagers + zombie) ———
        // Pods face a center zombie south of the shaft through IRON BARS only.
        // Solid GLASS blocks villager LOS — use bars / panes on viewing faces.
        for (int x = -7; x <= 7; x++) {
            for (int z = -3; z <= 5; z++) {
                if (x >= -1 && x <= 0 && z >= -1 && z <= 0) {
                    continue; // drop shaft
                }
                b.set(x, 8, z, Material.STONE_BRICKS);
            }
        }

        // Zombie cage at (0,9,2) — south of shaft so LOS does not cross shaft walls
        b.set(0, 9, 2, Material.AIR);
        b.set(0, 10, 2, Material.AIR);
        for (int y = 9; y <= 10; y++) {
            for (int x = -1; x <= 1; x++) {
                b.set(x, y, 1, Material.IRON_BARS);
                b.set(x, y, 3, Material.IRON_BARS);
            }
            b.set(-1, y, 2, Material.IRON_BARS);
            b.set(1, y, 2, Material.IRON_BARS);
        }
        b.set(0, 11, 2, Material.STONE_BRICK_SLAB);

        // Clear open corridor between pods ↔ zombie (eye level air)
        for (int x = -3; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                if (Math.abs(x) <= 1) {
                    continue; // cage
                }
                b.set(x, 9, z, Material.AIR);
                b.set(x, 10, z, Material.AIR);
            }
        }
        // Re-assert cage after corridor clear
        b.set(0, 9, 2, Material.AIR);
        b.set(0, 10, 2, Material.AIR);
        for (int y = 9; y <= 10; y++) {
            for (int x = -1; x <= 1; x++) {
                b.set(x, y, 1, Material.IRON_BARS);
                b.set(x, y, 3, Material.IRON_BARS);
            }
            b.set(-1, y, 2, Material.IRON_BARS);
            b.set(1, y, 2, Material.IRON_BARS);
        }

        // Villager pods at x=±5: bridge=bars toward zombie, side=stand, outer/far=beds
        for (int side : new int[]{-5, 5}) {
            int outer = side < 0 ? side - 1 : side + 1;
            int far = side < 0 ? outer - 1 : outer + 1;
            int bridge = side < 0 ? side + 1 : side - 1;
            BlockFace bedFace = side < 0 ? BlockFace.WEST : BlockFace.EAST;

            for (int dz = 0; dz <= 4; dz++) {
                b.set(side, 8, dz, Material.STONE_BRICKS);
                b.set(outer, 8, dz, Material.STONE_BRICKS);
                b.set(far, 8, dz, Material.STONE_BRICKS);
                b.set(bridge, 8, dz, Material.STONE_BRICKS);
            }
            for (int y = 9; y <= 11; y++) {
                for (int dz = 0; dz <= 4; dz++) {
                    b.set(far, y, dz, Material.GLASS_PANE);
                    if (dz == 0 || dz == 4) {
                        b.set(side, y, dz, Material.GLASS_PANE);
                        b.set(outer, y, dz, Material.GLASS_PANE);
                        b.set(bridge, y, dz, Material.GLASS_PANE);
                    }
                }
            }
            for (int dz = 0; dz <= 4; dz++) {
                b.set(side, 12, dz, Material.STONE_BRICK_SLAB);
                b.set(outer, 12, dz, Material.STONE_BRICK_SLAB);
                b.set(far, 12, dz, Material.STONE_BRICK_SLAB);
                b.set(bridge, 12, dz, Material.STONE_BRICK_SLAB);
            }
            for (int dz = 1; dz <= 3; dz++) {
                b.set(bridge, 9, dz, Material.IRON_BARS);
                b.set(bridge, 10, dz, Material.IRON_BARS);
                b.set(bridge, 11, dz, Material.GLASS_PANE);
            }
            for (int i = 0; i < 3; i++) {
                int z = 1 + i;
                b.bed(outer, 9, z, Material.RED_BED, bedFace);
                b.set(side, 9, z, Material.AIR);
                b.set(side, 10, z, Material.AIR);
                b.set(bridge, 9, z, Material.IRON_BARS);
                b.set(bridge, 10, z, Material.IRON_BARS);
            }
            b.set(outer, 9, 0, Material.COMPOSTER);
            b.set(outer, 9, 4, Material.COMPOSTER);
            b.set(side, 9, 0, Material.COMPOSTER);
        }

        // ——— TOP open-sky golem spawn deck (separate from pods) ———
        int deck = 13;
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 6; z++) {
                b.set(x, deck - 1, z, Material.STONE_BRICKS); // trench support
                b.set(x, deck, z, Material.STONE_BRICKS);     // dry spawn pads
                b.set(x, deck + 1, z, Material.AIR);
                b.set(x, deck + 2, z, Material.AIR);
                b.set(x, deck + 3, z, Material.AIR); // open sky — no roof
            }
        }
        // Cross trenches (water in channels; pads stay dry)
        for (int z = -7; z <= 6; z++) {
            if (z == -1 || z == 0) {
                continue;
            }
            b.set(-1, deck, z, Material.AIR);
            b.set(0, deck, z, Material.AIR);
        }
        for (int x = -7; x <= 6; x++) {
            if (x == -1 || x == 0) {
                continue;
            }
            b.set(x, deck, -1, Material.AIR);
            b.set(x, deck, 0, Material.AIR);
        }
        // Center drop into shaft
        for (int x = -1; x <= 0; x++) {
            for (int z = -1; z <= 0; z++) {
                b.set(x, deck, z, Material.AIR);
                b.set(x, deck - 1, z, Material.AIR);
            }
        }
        // Water sources at trench ends
        b.set(-1, deck, -7, Material.WATER);
        b.set(0, deck, -7, Material.WATER);
        b.set(-1, deck, 6, Material.WATER);
        b.set(0, deck, 6, Material.WATER);
        b.set(-7, deck, -1, Material.WATER);
        b.set(-7, deck, 0, Material.WATER);
        b.set(6, deck, -1, Material.WATER);
        b.set(6, deck, 0, Material.WATER);

        // Signs under hole — water stops; golems fall
        b.facing(-1, deck - 1, -1, Material.OAK_WALL_SIGN, BlockFace.EAST);
        b.facing(-1, deck - 1, 0, Material.OAK_WALL_SIGN, BlockFace.EAST);
        b.facing(0, deck - 1, -1, Material.OAK_WALL_SIGN, BlockFace.WEST);
        b.facing(0, deck - 1, 0, Material.OAK_WALL_SIGN, BlockFace.WEST);
        b.facing(-1, deck - 2, -1, Material.OAK_WALL_SIGN, BlockFace.SOUTH);
        b.facing(0, deck - 2, -1, Material.OAK_WALL_SIGN, BlockFace.SOUTH);
        b.facing(-1, deck - 2, 0, Material.OAK_WALL_SIGN, BlockFace.NORTH);
        b.facing(0, deck - 2, 0, Material.OAK_WALL_SIGN, BlockFace.NORTH);

        spawnPad(b, 0, 6);
        spawnPad(b, 0, 7);
        b.set(-1, 0, 7, Material.CRAFTING_TABLE);
        b.set(1, 0, 7, Material.BARREL);
        return b.build(
                "iron",
                "Iron farm - top golem deck, panic pods + zombie, lava→chests",
                0, 0, 7
        );
    }


    /**
     * Dark-room XP mob farm (classic 22-block drop):
     * one enclosed spawn deck with dry pads + cross water trenches → center 2×2 hole →
     * single drop shaft → magma landing (burn) with hopper ring for loot →
     * AFK house behind iron bars (punch for XP).
     * Removed the second floor / opposite stub channel that looked like an extra zombie tunnel
     * and stopped water from reaching the drop.
     */
    public static @NotNull BaseTemplates.BaseBlueprint xp() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();

        // ——— AFK house south of the kill pit ———
        for (int x = -4; x <= 4; x++) {
            for (int z = 3; z <= 11; z++) {
                b.set(x, -1, z, Material.STONE_BRICKS);
                boolean wall = x == -4 || x == 4 || z == 3 || z == 11;
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
        b.set(0, 1, 11, Material.AIR);
        b.set(0, 2, 11, Material.AIR);
        b.door(0, 1, 11, Material.IRON_DOOR, BlockFace.SOUTH);
        b.facing(-1, 2, 12, Material.STONE_BUTTON, BlockFace.SOUTH);
        b.facing(-1, 2, 10, Material.STONE_BUTTON, BlockFace.NORTH);

        // Kill pit: landing shifted toward AFK window so player can punch for XP.
        // Trapdoors at z=0,1; punch window at z=2; AFK stand at z=3; loot chests at z=5.
        for (int x = -3; x <= 2; x++) {
            for (int z = -3; z <= 2; z++) {
                b.set(x, -1, z, Material.STONE_BRICKS);
                boolean landing = (x == -1 || x == 0) && (z == 0 || z == 1);
                if (landing) {
                    b.facing(x, 0, z, Material.HOPPER, BlockFace.SOUTH);
                    b.facing(x, 1, z, Material.IRON_TRAPDOOR, BlockFace.SOUTH);
                } else if (z < 0) {
                    b.set(x, 0, z, Material.MAGMA_BLOCK);
                    b.set(x, 1, z, Material.AIR);
                } else {
                    BlockFace hopFace = z < 2 ? BlockFace.SOUTH
                            : (x < 0 ? BlockFace.EAST : (x > 0 ? BlockFace.WEST : BlockFace.SOUTH));
                    b.facing(x, 0, z, Material.HOPPER, hopFace);
                    b.set(x, 1, z, Material.AIR);
                }
                b.set(x, 2, z, Material.AIR);
            }
        }

        // Punch window directly in front of landing (z=2) — stand at z=3 and hit
        for (int x = -3; x <= 2; x++) {
            b.set(x, 1, 2, Material.IRON_BARS);
            b.set(x, 2, 2, Material.IRON_BARS);
        }
        b.set(-1, 1, 2, Material.AIR);
        b.set(0, 1, 2, Material.AIR);
        b.set(-1, 2, 2, Material.AIR);
        b.set(0, 2, 2, Material.AIR);
        // Open house wall at z=3 so AFK pad is adjacent to the window
        b.set(-1, 1, 3, Material.AIR);
        b.set(0, 1, 3, Material.AIR);
        b.set(-1, 2, 3, Material.AIR);
        b.set(0, 2, 3, Material.AIR);
        b.set(-1, 0, 3, Material.STONE_BRICKS);
        b.set(0, 0, 3, Material.STONE_BRICKS);
        // Loot chests behind AFK stance (don't block punch)
        b.facing(-1, 1, 5, Material.CHEST, BlockFace.SOUTH);
        b.facing(0, 1, 5, Material.CHEST, BlockFace.SOUTH);
        b.facing(1, 1, 5, Material.BARREL, BlockFace.SOUTH);
        b.facing(0, 0, 2, Material.HOPPER, BlockFace.SOUTH);
        b.facing(0, 0, 3, Material.HOPPER, BlockFace.SOUTH);
        b.facing(0, 0, 4, Material.HOPPER, BlockFace.SOUTH);
        b.facing(0, 0, 5, Material.HOPPER, BlockFace.UP);

        b.set(-3, 1, 7, Material.CRAFTING_TABLE);
        b.set(-3, 1, 8, Material.ANVIL);
        b.set(3, 1, 7, Material.BARREL);
        b.set(3, 1, 8, Material.CHEST);
        b.hangingLantern(0, 3, 7, Material.LANTERN, 3);

        // Drop shaft aligned to landing (z=0,1)
        for (int y = 2; y <= 22; y++) {
            for (int x = -2; x <= 1; x++) {
                for (int z = -1; z <= 2; z++) {
                    boolean wall = x == -2 || x == 1 || z == -1 || z == 2;
                    boolean shaft = (x == -1 || x == 0) && (z == 0 || z == 1);
                    if (wall) {
                        b.set(x, y, z, Material.STONE_BRICKS);
                    } else if (shaft) {
                        b.set(x, y, z, Material.AIR);
                    }
                }
            }
        }
        for (int x = -1; x <= 0; x++) {
            for (int z = 0; z <= 1; z++) {
                b.facing(x, 0, z, Material.HOPPER, BlockFace.SOUTH);
                b.facing(x, 1, z, Material.IRON_TRAPDOOR, BlockFace.SOUTH);
            }
        }

        for (int y = 1; y <= 23; y++) {
            b.facing(-3, y, 0, Material.LADDER, BlockFace.WEST);
        }

        // Dark spawn deck — hole above landing
        int deck = 23;
        for (int x = -8; x <= 7; x++) {
            for (int z = -8; z <= 7; z++) {
                b.set(x, deck - 1, z, Material.COBBLESTONE); // trench bed
                b.set(x, deck, z, Material.COBBLESTONE);     // spawn floor
                b.set(x, deck + 1, z, Material.AIR);
                b.set(x, deck + 2, z, Material.AIR);
                b.set(x, deck + 3, z, Material.COBBLESTONE);  // dark roof
            }
        }
        for (int yy = deck + 1; yy <= deck + 2; yy++) {
            for (int x = -8; x <= 7; x++) {
                b.set(x, yy, -8, Material.COBBLESTONE);
                b.set(x, yy, 7, Material.COBBLESTONE);
            }
            for (int z = -8; z <= 7; z++) {
                b.set(-8, yy, z, Material.COBBLESTONE);
                b.set(7, yy, z, Material.COBBLESTONE);
            }
        }

        // Cross trenches → hole at z=0,1 (aligned with punch landing)
        for (int z = -7; z <= 6; z++) {
            if (z == 0 || z == 1) {
                continue;
            }
            b.set(-1, deck, z, Material.AIR);
            b.set(0, deck, z, Material.AIR);
        }
        for (int x = -7; x <= 6; x++) {
            if (x == -1 || x == 0) {
                continue;
            }
            b.set(x, deck, 0, Material.AIR);
            b.set(x, deck, 1, Material.AIR);
        }

        // Center hole into the shaft above landing
        for (int x = -1; x <= 0; x++) {
            for (int z = 0; z <= 1; z++) {
                b.set(x, deck, z, Material.AIR);
                b.set(x, deck + 1, z, Material.AIR);
                b.set(x, deck + 2, z, Material.AIR);
                b.set(x, deck + 3, z, Material.AIR);
            }
        }

        // Water SOURCES at far trench ends only
        b.set(-1, deck, -7, Material.WATER);
        b.set(0, deck, -7, Material.WATER);
        b.set(-1, deck, 6, Material.WATER);
        b.set(0, deck, 6, Material.WATER);
        b.set(-7, deck, 0, Material.WATER);
        b.set(-7, deck, 1, Material.WATER);
        b.set(6, deck, 0, Material.WATER);
        b.set(6, deck, 1, Material.WATER);

        for (int x = -1; x <= 0; x++) {
            for (int z = 0; z <= 1; z++) {
                b.set(x, deck, z, Material.AIR);
            }
        }

        // Shaft air
        for (int y = 2; y <= deck + 2; y++) {
            for (int x = -1; x <= 0; x++) {
                for (int z = 0; z <= 1; z++) {
                    b.set(x, y, z, Material.AIR);
                }
            }
        }
        for (int x = -1; x <= 0; x++) {
            for (int z = 0; z <= 1; z++) {
                b.facing(x, 0, z, Material.HOPPER, BlockFace.SOUTH);
                b.facing(x, 1, z, Material.IRON_TRAPDOOR, BlockFace.SOUTH);
            }
        }

        // Signs under hole — water stops; mobs fall
        b.facing(-1, deck - 1, 0, Material.OAK_WALL_SIGN, BlockFace.EAST);
        b.facing(-1, deck - 1, 1, Material.OAK_WALL_SIGN, BlockFace.EAST);
        b.facing(0, deck - 1, 0, Material.OAK_WALL_SIGN, BlockFace.WEST);
        b.facing(0, deck - 1, 1, Material.OAK_WALL_SIGN, BlockFace.WEST);
        b.facing(-1, deck - 2, 0, Material.OAK_WALL_SIGN, BlockFace.SOUTH);
        b.facing(0, deck - 2, 0, Material.OAK_WALL_SIGN, BlockFace.SOUTH);
        b.facing(-1, deck - 2, 1, Material.OAK_WALL_SIGN, BlockFace.NORTH);
        b.facing(0, deck - 2, 1, Material.OAK_WALL_SIGN, BlockFace.NORTH);

        spawnPad(b, 0, 12);
        spawnPad(b, 0, 13);
        b.set(-1, 0, 13, Material.CRAFTING_TABLE);
        b.set(1, 0, 13, Material.BARREL);
        return b.build(
                "xp",
                "XP mob farm - stand at window, punch 1HP mobs; loot → chests behind you",
                0, 0, 13
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

    /**
     * Shared wheat/potato layout: hydrated farmland + villager pod (bed + composter over hopper).
     */
    private static @NotNull BaseTemplates.BaseBlueprint cropVillagerFarm(
            @NotNull String id,
            @NotNull Material crop,
            @NotNull String description
    ) {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int size = 4;
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                b.set(x, -2, z, Material.STONE);
                boolean edge = x == -size || x == size || z == -size || z == size;
                if (edge) {
                    b.set(x, -1, z, Material.STONE_BRICKS);
                    b.set(x, 0, z, Material.OAK_FENCE);
                    b.set(x, 1, z, Material.OAK_FENCE);
                } else if (x == 0 || z == 0) {
                    // Water cross hydrates farmland — no hoppers here (collection is via composter)
                    b.set(x, -1, z, Material.STONE);
                    b.set(x, 0, z, Material.WATER);
                } else {
                    b.set(x, -1, z, Material.DIRT);
                    b.set(x, 0, z, Material.FARMLAND);
                    b.set(x, 1, z, crop);
                }
            }
        }
        // Farmer villager pod on +Z (bed + composter -> hopper -> chest)
        for (int x = -2; x <= 2; x++) {
            for (int z = size + 1; z <= size + 4; z++) {
                b.set(x, -1, z, Material.STONE_BRICKS);
                b.set(x, 0, z, Material.SMOOTH_STONE);
                boolean wall = x == -2 || x == 2 || z == size + 1 || z == size + 4;
                for (int y = 1; y <= 3; y++) {
                    if (wall) {
                        b.set(x, y, z, Material.OAK_PLANKS);
                    } else {
                        b.set(x, y, z, Material.AIR);
                    }
                }
                b.set(x, 4, z, Material.OAK_SLAB);
            }
        }
        b.door(0, 1, size + 1, Material.OAK_DOOR, BlockFace.SOUTH);
        b.set(0, 0, size + 2, Material.COMPOSTER);
        b.facing(0, -1, size + 2, Material.HOPPER, BlockFace.SOUTH);
        b.bed(-1, 1, size + 3, Material.RED_BED, BlockFace.EAST);
        b.facing(1, 1, size + 2, Material.CHEST, BlockFace.WEST);
        b.set(-1, 1, size + 2, Material.CRAFTING_TABLE);
        hopperRowIntoChest(b, -1, size + 2, -1, 1, size + 5);
        // Field gate so the farmer can reach every crop
        b.set(0, 0, size, Material.AIR);
        b.set(0, 1, size, Material.AIR);
        b.facing(0, 0, size, Material.OAK_FENCE_GATE, BlockFace.SOUTH);
        postHangingLantern(b, -size, 0, -size, Material.LANTERN);
        postHangingLantern(b, size, 0, -size, Material.LANTERN);
        spawnPad(b, 0, size + 6);
        spawnPad(b, 0, size + 7);
        b.set(-1, 0, size + 7, Material.COMPOSTER);
        b.set(1, 0, size + 7, Material.BARREL);
        return b.build(id, description, 0, 0, size + 7);
    }

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
        // Water + feed inside pen (not on fence line); gate only on courtyard-facing side
        b.set(x1 + 1, 0, z1 + 1, Material.WATER);
        b.set(x2 - 1, 1, z1 + 1, Material.CHEST);
        b.set(x1 + 1, 1, z2 - 1, Material.HAY_BLOCK);
        // Double-tall fence on edges so animals cannot jump out
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                if (edge) {
                    b.set(x, 2, z, fence);
                }
            }
        }
        int gateX = (x1 + x2) / 2;
        int gateZ = z2;
        b.set(gateX, 1, gateZ, Material.AIR);
        b.set(gateX, 2, gateZ, Material.AIR);
        b.facing(gateX, 1, gateZ, gate, BlockFace.SOUTH);
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
     * Hopper collection line → loot bay where hoppers sit ON TOP of chests (facing DOWN).
     * That is the only reliable Minecraft layout: items push down into the chest below.
     * Chests stay at ground y=0 (visible); hoppers are at y=1 on top of them.
     */
    private static void hopperRowIntoChest(
            @NotNull BaseTemplates.Builder b,
            int hopperY,
            int lineZ,
            int xMin,
            int xMax,
            int chestZ
    ) {
        final int chestY = 0;
        final int topY = 1; // hoppers sit on the chests
        final int feedZ = chestZ - 1; // approach from north into the center top hopper

        // 1) Collection row → toward X=0, then toward +Z / down
        for (int x = xMin; x <= xMax; x++) {
            BlockFace face;
            if (x < 0) {
                face = BlockFace.EAST;
            } else if (x > 0) {
                face = BlockFace.WEST;
            } else if (lineZ < feedZ) {
                face = BlockFace.SOUTH;
            } else if (lineZ > feedZ) {
                face = BlockFace.NORTH;
            } else if (hopperY > topY) {
                face = BlockFace.DOWN;
            } else if (hopperY < topY) {
                face = BlockFace.UP;
            } else {
                face = BlockFace.SOUTH;
            }
            b.facing(x, hopperY, lineZ, Material.HOPPER, face);
        }

        // 2) Run along Z at collection height to the feed column
        if (lineZ != feedZ) {
            int step = lineZ < feedZ ? 1 : -1;
            BlockFace along = step > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
            for (int z = lineZ + step; z != feedZ; z += step) {
                b.facing(0, hopperY, z, Material.HOPPER, along);
            }
            if (hopperY > topY) {
                b.facing(0, hopperY, feedZ, Material.HOPPER, BlockFace.DOWN);
            } else if (hopperY < topY) {
                b.facing(0, hopperY, feedZ, Material.HOPPER, BlockFace.UP);
            } else {
                b.facing(0, hopperY, feedZ, Material.HOPPER, BlockFace.SOUTH);
            }
        } else if (hopperY > topY) {
            b.facing(0, hopperY, feedZ, Material.HOPPER, BlockFace.DOWN);
        } else if (hopperY < topY) {
            b.facing(0, hopperY, feedZ, Material.HOPPER, BlockFace.UP);
        }

        // 3) Vertical pipe on feedZ between collection Y and topY
        if (hopperY > topY) {
            for (int y = hopperY - 1; y > topY; y--) {
                b.facing(0, y, feedZ, Material.HOPPER, BlockFace.DOWN);
            }
            b.facing(0, topY, feedZ, Material.HOPPER, BlockFace.SOUTH);
        } else if (hopperY < topY) {
            for (int y = hopperY + 1; y < topY; y++) {
                b.facing(0, y, feedZ, Material.HOPPER, BlockFace.UP);
            }
            b.facing(0, topY, feedZ, Material.HOPPER, BlockFace.SOUTH);
        } else if (lineZ == feedZ) {
            b.facing(0, topY, feedZ, Material.HOPPER, BlockFace.SOUTH);
        }

        // 4) Loot bay: CHESTS at y=0, HOPPERS on top at y=1 facing DOWN into them
        // Support under chests only — never overwrite the feed hopper column
        b.set(-1, chestY - 1, chestZ, Material.SMOOTH_STONE);
        b.set(0, chestY - 1, chestZ, Material.SMOOTH_STONE);
        b.set(1, chestY - 1, chestZ, Material.SMOOTH_STONE);
        if (hopperY >= topY) {
            // Feed hoppers already at/above ground; safe to put stone under feedZ
            b.set(0, chestY - 1, feedZ, Material.SMOOTH_STONE);
        } else {
            // Collection is underground — support under the vertical pipe without wiping y=hopperY hoppers
            b.set(0, hopperY - 1, feedZ, Material.SMOOTH_STONE);
        }

        b.facing(-1, chestY, chestZ, Material.CHEST, BlockFace.SOUTH);
        b.facing(0, chestY, chestZ, Material.CHEST, BlockFace.SOUTH);
        b.facing(1, chestY, chestZ, Material.BARREL, BlockFace.SOUTH);

        // Hoppers sit ON the chests and push DOWN (correct Minecraft connection)
        b.facing(-1, topY, chestZ, Material.HOPPER, BlockFace.DOWN);
        b.facing(0, topY, chestZ, Material.HOPPER, BlockFace.DOWN);
        b.facing(1, topY, chestZ, Material.HOPPER, BlockFace.DOWN);

        // Clear headroom + lantern marker
        b.set(-1, topY + 1, chestZ, Material.AIR);
        b.set(0, topY + 1, chestZ, Material.AIR);
        b.set(1, topY + 1, chestZ, Material.AIR);
        b.set(0, topY + 2, chestZ, Material.OAK_FENCE);
        b.set(0, topY + 3, chestZ, Material.LANTERN);
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
