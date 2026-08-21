package com.rihanx.base;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Slab;
import org.jetbrains.annotations.NotNull;

/**
 * Luxury / large real-world style bases (plus GrabCraft-inspired advanced estates).
 * Local space: front door faces +Z (SOUTH before rotation).
 * <p>
 * Advanced IDs ({@code estate}, {@code chateau}, {@code skyvilla}, {@code palace}) take cues from
 * GrabCraft amenity layouts (multi-wing mansions, modernist villas, grand halls) but are original
 * procedural builds — not copies of GrabCraft blueprints.
 */
public final class LuxuryBaseTemplates {

    private LuxuryBaseTemplates() {
    }

    /** Single-storey luxury bungalow — 5 bedrooms, pool, kitchen, lounge, baths. */
    public static @NotNull BaseTemplates.BaseBlueprint bungalow() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int minX = -10;
        int maxX = 10;
        int minZ = -9;
        int maxZ = 6;
        int wallH = 3;

        // Platform + luxury floor
        fillRect(b, minX - 1, maxX + 1, minZ - 1, maxZ + 1, -1, Material.SMOOTH_STONE);
        fillRect(b, minX, maxX, minZ, maxZ, 0, Material.SMOOTH_QUARTZ);
        clearVolume(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, 1, wallH);

        // Exterior shell — wood frame + plaster look (depth, not a flat box)
        shell(b, minX, maxX, minZ, maxZ, 1, wallH, Material.CALCITE, Material.STRIPPED_OAK_LOG);
        // Foundation skirt under walls
        for (int x = minX; x <= maxX; x++) {
            b.set(x, 0, minZ, Material.STONE_BRICKS);
            b.set(x, 0, maxZ, Material.STONE_BRICKS);
        }
        for (int z = minZ; z <= maxZ; z++) {
            b.set(minX, 0, z, Material.STONE_BRICKS);
            b.set(maxX, 0, z, Material.STONE_BRICKS);
        }
        // Framed windows (sill + pane + lintel)
        for (int z = -7; z <= 4; z += 3) {
            framedWindow(b, minX, 2, z, BlockFace.WEST);
            framedWindow(b, maxX, 2, z, BlockFace.EAST);
        }
        for (int x = -8; x <= 8; x += 3) {
            if (x == -1 || x == 0) {
                continue;
            }
            framedWindow(b, x, 2, minZ, BlockFace.NORTH);
        }

        // Continuous pitched roof (full ceiling — no sky holes over rooms)
        gabledRoof(
                b, minX, maxX, minZ, maxZ, wallH + 1,
                Material.DARK_OAK_STAIRS, Material.DARK_OAK_PLANKS,
                Material.DARK_OAK_SLAB, Material.CALCITE
        );
        // Brick chimney for silhouette
        b.set(8, 1, -7, Material.BRICKS);
        b.set(8, 2, -7, Material.BRICKS);
        b.set(8, 3, -7, Material.BRICKS);
        b.set(8, 4, -7, Material.BRICKS);
        b.set(8, 5, -7, Material.BRICKS);
        b.set(8, 6, -7, Material.BRICKS);
        b.set(8, 7, -7, Material.CAMPFIRE);

        // Front entrance
        b.doubleDoor(-1, 1, maxZ, Material.OAK_DOOR, BlockFace.SOUTH);
        // Glass facade
        for (int x = -8; x <= 8; x++) {
            if (x == -1 || x == 0) {
                continue;
            }
            if (x % 2 == 0) {
                b.set(x, 2, maxZ, Material.GLASS);
            }
        }

        // Corridor spine (open center) - keep floor solid, carpet sits as floor accent via quartz pattern
        for (int z = minZ + 1; z <= maxZ - 1; z++) {
            b.set(-1, 1, z, Material.AIR);
            b.set(0, 1, z, Material.AIR);
            b.set(1, 1, z, Material.AIR);
            b.set(-1, 2, z, Material.AIR);
            b.set(0, 2, z, Material.AIR);
            b.set(1, 2, z, Material.AIR);
            b.set(-1, 3, z, Material.AIR);
            b.set(0, 3, z, Material.AIR);
            b.set(1, 3, z, Material.AIR);
            b.set(0, 0, z, Material.RED_TERRACOTTA); // runner (solid - never carpet-only floor)
        }

        // Internal walls for 5 bedrooms (wider rooms so beds fit)
        wallLineX(b, -4, minZ + 1, 3, 1, wallH, Material.WHITE_CONCRETE);
        wallLineX(b, 4, minZ + 1, 3, 1, wallH, Material.WHITE_CONCRETE);
        wallLineZ(b, minX + 1, -4, -5, 1, wallH, Material.WHITE_CONCRETE);
        wallLineZ(b, 4, maxX - 1, -5, 1, wallH, Material.WHITE_CONCRETE);
        wallLineZ(b, minX + 1, -4, -1, 1, wallH, Material.WHITE_CONCRETE);
        wallLineZ(b, 4, maxX - 1, -1, 1, wallH, Material.WHITE_CONCRETE);
        wallLineZ(b, minX + 1, -4, 2, 1, wallH, Material.WHITE_CONCRETE);

        // Doorways into bedrooms
        doorway(b, -4, 1, -7, BlockFace.WEST, Material.BIRCH_DOOR);
        doorway(b, -4, 1, -3, BlockFace.WEST, Material.BIRCH_DOOR);
        doorway(b, -4, 1, 0, BlockFace.WEST, Material.BIRCH_DOOR);
        doorway(b, 4, 1, -7, BlockFace.EAST, Material.BIRCH_DOOR);
        doorway(b, 4, 1, -3, BlockFace.EAST, Material.BIRCH_DOOR);

        // 5 bedrooms — warm wood floors, lanterns hang from real ceiling (y=4)
        furnishBedroom(b, -9, -5, -8, -6, 1, wallH + 1,
                Material.OAK_PLANKS, Material.STRIPPED_OAK_LOG, Material.RED_BED, BlockFace.EAST);
        furnishBedroom(b, -9, -5, -4, -2, 1, wallH + 1,
                Material.OAK_PLANKS, Material.STRIPPED_OAK_LOG, Material.BLUE_BED, BlockFace.EAST);
        furnishBedroom(b, -9, -5, 0, 2, 1, wallH + 1,
                Material.OAK_PLANKS, Material.STRIPPED_OAK_LOG, Material.YELLOW_BED, BlockFace.EAST);
        furnishBedroom(b, 5, 9, -8, -6, 1, wallH + 1,
                Material.OAK_PLANKS, Material.STRIPPED_OAK_LOG, Material.GREEN_BED, BlockFace.WEST);
        furnishBedroom(b, 5, 9, -4, -2, 1, wallH + 1,
                Material.OAK_PLANKS, Material.STRIPPED_OAK_LOG, Material.PURPLE_BED, BlockFace.WEST);

        // Side windows already framed above
        // (removed duplicate flat glass)

        // Living / lounge (front center)
        b.stairs(-2, 1, 4, Material.OAK_STAIRS, BlockFace.SOUTH);
        b.stairs(-1, 1, 4, Material.OAK_STAIRS, BlockFace.SOUTH);
        b.stairs(1, 1, 4, Material.OAK_STAIRS, BlockFace.SOUTH);
        b.stairs(2, 1, 4, Material.OAK_STAIRS, BlockFace.SOUTH);
        b.set(0, 1, 3, Material.OAK_TRAPDOOR);
        b.set(-1, 1, 3, Material.OAK_TRAPDOOR);
        b.set(1, 1, 3, Material.OAK_TRAPDOOR);
        // Ceiling lantern only (y=3) — never at head height in the walk path
        safeCeilingLantern(b, 0, 4, 4);
        b.set(-2, 1, 5, Material.BOOKSHELF);
        b.set(2, 1, 5, Material.BOOKSHELF);

        // Kitchen (front-right open)
        b.set(7, 1, 4, Material.CRAFTING_TABLE);
        b.facing(8, 1, 4, Material.SMOKER, BlockFace.WEST);
        b.facing(9, 1, 4, Material.FURNACE, BlockFace.WEST);
        b.facing(9, 1, 3, Material.BLAST_FURNACE, BlockFace.WEST);
        b.set(8, 1, 3, Material.BARREL);
        b.set(7, 1, 3, Material.BARREL);
        b.set(6, 1, 4, Material.SMITHING_TABLE);
        b.set(5, 1, 4, Material.GRINDSTONE);
        b.set(5, 1, 5, Material.ANVIL);

        // Bath 1 + bath 2 (cauldron / brewing as spa feel)
        b.set(-5, 1, 4, Material.CAULDRON);
        b.set(-6, 1, 4, Material.BREWING_STAND);
        b.set(-7, 1, 4, Material.FLOWER_POT);
        b.set(4, 1, 1, Material.CAULDRON);
        b.set(4, 1, 0, Material.BREWING_STAND);

        // Enchant nook
        b.set(-2, 1, -8, Material.BOOKSHELF);
        b.set(-1, 1, -8, Material.BOOKSHELF);
        b.set(1, 1, -8, Material.BOOKSHELF);
        b.set(2, 1, -8, Material.BOOKSHELF);
        b.set(0, 1, -8, Material.ENCHANTING_TABLE);
        b.facing(0, 1, -7, Material.ENDER_CHEST, BlockFace.SOUTH);

        // Ceiling lighting only — skip corridor spine and doorway columns so rooms stay walkable
        for (int x = -8; x <= 8; x += 4) {
            if (x >= -1 && x <= 1) {
                continue; // corridor
            }
            for (int z = -7; z <= 5; z += 4) {
                if (z == -7 || z == -3 || z == 0) {
                    continue; // doorway rows
                }
                safeCeilingLantern(b, x, z, 4);
            }
        }

        // Deep covered porch — posts to roof beam, continuous awning
        for (int x = -4; x <= 3; x++) {
            b.set(x, 0, maxZ + 1, Material.OAK_PLANKS);
            b.set(x, 0, maxZ + 2, Material.OAK_PLANKS);
            b.set(x, 0, maxZ + 3, Material.OAK_SLAB);
            b.stairs(x, 0, maxZ + 4, Material.OAK_STAIRS, BlockFace.SOUTH);
            b.set(x, 4, maxZ + 1, Material.DARK_OAK_PLANKS);
            b.set(x, 4, maxZ + 2, Material.DARK_OAK_PLANKS);
            b.stairs(x, 4, maxZ + 3, Material.DARK_OAK_STAIRS, BlockFace.SOUTH);
        }
        for (int y = 1; y <= 3; y++) {
            b.set(-4, y, maxZ + 2, Material.STRIPPED_OAK_LOG);
            b.set(3, y, maxZ + 2, Material.STRIPPED_OAK_LOG);
            b.set(-4, y, maxZ + 1, Material.STRIPPED_OAK_LOG);
            b.set(3, y, maxZ + 1, Material.STRIPPED_OAK_LOG);
        }
        safeCeilingLantern(b, -4, maxZ + 2, 4);
        safeCeilingLantern(b, 3, maxZ + 2, 4);
        b.set(0, 1, maxZ + 2, Material.AIR);
        b.set(0, 2, maxZ + 2, Material.AIR);
        b.set(-1, 1, maxZ + 2, Material.AIR);
        b.set(-1, 2, maxZ + 2, Material.AIR);

        // Side patio → swimming pool (attached, walkable, stairs into water)
        poolWalkway(b, 4, 10, maxZ + 1, maxZ + 2, 0);
        // Side door from kitchen/lounge out to the pool patio
        b.set(maxX, 1, 4, Material.AIR);
        b.set(maxX, 2, 4, Material.AIR);
        b.door(maxX, 1, 4, Material.OAK_DOOR, BlockFace.EAST);
        buildPool(b, 5, 10, maxZ + 3, maxZ + 8, 0, BlockFace.NORTH);

        // Garden beds + flower boxes for a richer exterior
        for (int x = minX; x <= -2; x++) {
            b.set(x, 0, maxZ + 4, Material.GRASS_BLOCK);
            b.set(x, 1, maxZ + 4, x % 2 == 0 ? Material.OAK_LEAVES : Material.POPPY);
        }
        for (int z = -6; z <= 2; z += 4) {
            b.set(minX - 1, 0, z, Material.GRASS_BLOCK);
            b.set(minX - 1, 1, z, Material.AZALEA);
            b.set(maxX + 1, 0, z, Material.GRASS_BLOCK);
            b.set(maxX + 1, 1, z, Material.FLOWERING_AZALEA);
        }
        // Window flower boxes (front sill planters)
        for (int x = -6; x <= 6; x += 3) {
            if (x >= -1 && x <= 0) {
                continue;
            }
            b.set(x, 1, minZ - 1, Material.OAK_FENCE);
            b.set(x, 2, minZ - 1, Material.POTTED_POPPY);
        }

        return finish(b, "bungalow", "Luxury 5-bedroom bungalow - pitched roof, porch, pool patio, kitchen", 0, maxZ + 2);
    }

    /** Two-storey villa — lift (bubble elevator), pool, loft, multi-suite. */
    public static @NotNull BaseTemplates.BaseBlueprint villa() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int minX = -11;
        int maxX = 11;
        int minZ = -10;
        int maxZ = 7;
        int groundTop = 3;
        int loftFloor = 4;
        int loftTop = 7;

        fillRect(b, minX - 1, maxX + 1, minZ - 1, maxZ + 1, -1, Material.DEEPSLATE_TILES);
        fillRect(b, minX, maxX, minZ, maxZ, 0, Material.DARK_OAK_PLANKS);
        clearVolume(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, 1, loftTop);

        shell(b, minX, maxX, minZ, maxZ, 1, loftTop, Material.QUARTZ_BLOCK, Material.DEEPSLATE_BRICKS);

        // Upper floor deck
        fillRect(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, loftFloor, Material.SMOOTH_QUARTZ);
        // Keep lift + stair voids
        clearVolume(b, -1, 1, -1, 1, loftFloor, loftFloor);
        clearVolume(b, 8, 10, 2, 5, loftFloor, loftFloor);

        // Continuous pitched roof over the villa (home silhouette, not a flat slab lid)
        gabledRoof(
                b, minX, maxX, minZ, maxZ, loftTop + 1,
                Material.DARK_OAK_STAIRS, Material.DARK_OAK_PLANKS,
                Material.DARK_OAK_SLAB, Material.QUARTZ_BLOCK
        );

        // Grand entrance
        b.doubleDoor(-1, 1, maxZ, Material.DARK_OAK_DOOR, BlockFace.SOUTH);
        b.set(-2, 2, maxZ, Material.GLASS);
        b.set(1, 2, maxZ, Material.GLASS);

        // Glass curtain walls
        for (int z = -8; z <= 5; z += 2) {
            b.set(minX, 2, z, Material.GLASS);
            b.set(maxX, 2, z, Material.GLASS);
            b.set(minX, 5, z, Material.GLASS);
            b.set(maxX, 5, z, Material.GLASS);
        }
        for (int x = -9; x <= 9; x += 3) {
            b.set(x, 2, minZ, Material.GLASS);
            b.set(x, 5, minZ, Material.GLASS);
        }

        // Bubble lift — landings at ground walk (y=1) and loft walk (loftFloor+1)
        buildLift(b, 0, 0, 0, loftTop, 1, loftFloor + 1);
        markLiftLobby(b, 0, 0, 1);
        markLiftLobby(b, 0, 0, loftFloor + 1);

        // Formal stairs (backup to lift) — clearly visible on +X side
        b.stairs(9, 1, 5, Material.QUARTZ_STAIRS, BlockFace.NORTH);
        b.stairs(9, 2, 4, Material.QUARTZ_STAIRS, BlockFace.NORTH);
        b.stairs(9, 3, 3, Material.QUARTZ_STAIRS, BlockFace.NORTH);
        b.stairs(9, 4, 2, Material.QUARTZ_STAIRS, BlockFace.NORTH);
        b.set(9, 1, 4, Material.QUARTZ_BLOCK);
        b.set(9, 2, 3, Material.QUARTZ_BLOCK);
        b.set(9, 3, 2, Material.QUARTZ_BLOCK);
        // Stair handrail cue
        b.set(10, 1, 5, Material.QUARTZ_PILLAR);
        b.set(10, 2, 4, Material.QUARTZ_PILLAR);
        b.set(10, 3, 3, Material.QUARTZ_PILLAR);
        b.set(10, 4, 2, Material.QUARTZ_PILLAR);

        // Ground bedrooms 1-3
        wallLineX(b, -4, minZ + 1, 1, 1, groundTop, Material.WHITE_CONCRETE);
        wallLineX(b, 4, minZ + 1, 1, 1, groundTop, Material.WHITE_CONCRETE);
        wallLineZ(b, minX + 1, -4, -5, 1, groundTop, Material.WHITE_CONCRETE);
        wallLineZ(b, minX + 1, -4, -1, 1, groundTop, Material.WHITE_CONCRETE);
        wallLineZ(b, 4, maxX - 1, -5, 1, groundTop, Material.WHITE_CONCRETE);

        doorway(b, -4, 1, -7, BlockFace.WEST, Material.BIRCH_DOOR);
        doorway(b, -4, 1, -3, BlockFace.WEST, Material.BIRCH_DOOR);
        doorway(b, 4, 1, -7, BlockFace.EAST, Material.BIRCH_DOOR);

        furnishBedroom(b, -10, -5, -9, -6, 1, groundTop + 1,
                Material.DARK_OAK_PLANKS, Material.SPRUCE_PLANKS, Material.RED_BED, BlockFace.EAST);
        furnishBedroom(b, -10, -5, -4, -2, 1, groundTop + 1,
                Material.DARK_OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BLUE_BED, BlockFace.EAST);
        furnishBedroom(b, 5, 10, -9, -6, 1, groundTop + 1,
                Material.DARK_OAK_PLANKS, Material.SPRUCE_PLANKS, Material.GREEN_BED, BlockFace.WEST);

        // Loft bedrooms 4-5
        wallLineX(b, -3, minZ + 1, -1, loftFloor + 1, loftTop, Material.WHITE_CONCRETE);
        wallLineX(b, 3, minZ + 1, -1, loftFloor + 1, loftTop, Material.WHITE_CONCRETE);
        doorway(b, -3, loftFloor + 1, -6, BlockFace.WEST, Material.BIRCH_DOOR);
        doorway(b, 3, loftFloor + 1, -6, BlockFace.EAST, Material.BIRCH_DOOR);
        furnishBedroom(b, -10, -4, -9, -5, loftFloor + 1, loftTop + 1,
                Material.SMOOTH_QUARTZ, Material.QUARTZ_BLOCK, Material.CYAN_BED, BlockFace.EAST);
        furnishBedroom(b, 4, 10, -9, -5, loftFloor + 1, loftTop + 1,
                Material.SMOOTH_QUARTZ, Material.QUARTZ_BLOCK, Material.PURPLE_BED, BlockFace.WEST);

        // Ground kitchen + lounge
        b.set(8, 1, 5, Material.CRAFTING_TABLE);
        b.facing(10, 1, 5, Material.FURNACE, BlockFace.WEST);
        b.facing(10, 1, 4, Material.SMOKER, BlockFace.WEST);
        b.facing(10, 1, 3, Material.BLAST_FURNACE, BlockFace.WEST);
        b.set(8, 1, 6, Material.BARREL); // was (9,1,5) — blocked stair foot
        b.set(8, 1, 4, Material.SMITHING_TABLE);
        b.set(8, 1, 3, Material.ANVIL);
        b.set(-9, 1, 5, Material.BOOKSHELF);
        b.set(-8, 1, 5, Material.BOOKSHELF);
        b.set(-7, 1, 5, Material.ENCHANTING_TABLE);
        b.set(-6, 1, 5, Material.BREWING_STAND);
        b.set(-5, 1, 5, Material.CAULDRON);
        b.facing(-9, 1, 4, Material.ENDER_CHEST, BlockFace.SOUTH);
        b.facing(-8, 1, 4, Material.CHEST, BlockFace.SOUTH);
        b.facing(-7, 1, 4, Material.CHEST, BlockFace.SOUTH);

        b.stairs(-2, 1, 4, Material.DARK_OAK_STAIRS, BlockFace.SOUTH);
        b.stairs(-1, 1, 4, Material.DARK_OAK_STAIRS, BlockFace.SOUTH);
        b.stairs(1, 1, 4, Material.DARK_OAK_STAIRS, BlockFace.SOUTH);
        b.stairs(2, 1, 4, Material.DARK_OAK_STAIRS, BlockFace.SOUTH);
        b.set(0, 1, 3, Material.DARK_OAK_TRAPDOOR);

        // Balcony
        for (int x = -4; x <= 4; x++) {
            b.set(x, loftFloor, maxZ + 1, Material.SMOOTH_QUARTZ);
            b.set(x, loftFloor + 1, maxZ + 1, Material.OAK_FENCE);
        }
        b.set(-4, loftFloor + 3, maxZ + 1, Material.SMOOTH_QUARTZ);
        b.set(4, loftFloor + 3, maxZ + 1, Material.SMOOTH_QUARTZ);
        safeCeilingLantern(b, -4, maxZ + 1, loftFloor + 3);
        safeCeilingLantern(b, 4, maxZ + 1, loftFloor + 3);

        // Lighting — end rods on ceiling only (no sea lanterns in the walk plane)
        for (int x = -9; x <= 9; x += 3) {
            for (int z = -8; z <= 5; z += 3) {
                if (x >= -1 && x <= 2 && z >= -1 && z <= 2) {
                    continue; // keep lift atrium clear
                }
                b.set(x, loftTop, z, Material.END_ROD);
                b.set(x, groundTop, z, Material.END_ROD);
            }
        }

        // Covered portico / driveway in front of doors (player spawns here)
        for (int x = -2; x <= 1; x++) {
            for (int z = maxZ + 1; z <= maxZ + 3; z++) {
                b.set(x, -1, z, Material.POLISHED_BLACKSTONE);
                b.set(x, 0, z, Material.SMOOTH_QUARTZ);
                b.set(x, 1, z, Material.AIR);
                b.set(x, 2, z, Material.AIR);
            }
            b.set(x, loftFloor, maxZ + 1, Material.DARK_OAK_PLANKS);
            b.set(x, loftFloor, maxZ + 2, Material.DARK_OAK_PLANKS);
            b.stairs(x, loftFloor, maxZ + 3, Material.DARK_OAK_STAIRS, BlockFace.SOUTH);
        }
        for (int y = 1; y <= loftFloor - 1; y++) {
            b.set(-2, y, maxZ + 2, Material.QUARTZ_PILLAR);
            b.set(1, y, maxZ + 2, Material.QUARTZ_PILLAR);
        }

        // Pool patio attached to house (+X), walk-on deck, stairs into water
        poolWalkway(b, 3, 10, maxZ + 1, maxZ + 2, 0);
        b.set(maxX, 1, 4, Material.AIR);
        b.set(maxX, 2, 4, Material.AIR);
        b.door(maxX, 1, 4, Material.BIRCH_DOOR, BlockFace.EAST);
        buildPool(b, 4, 10, maxZ + 3, maxZ + 8, 0, BlockFace.NORTH);

        return finish(b, "villa", "2-storey luxury villa - bubble lift, 5 bedrooms, pool patio & balcony", 0, maxZ + 2);
    }

    /** Mega mansion — 3 levels, dual lifts, indoor + outdoor pools, 6 suites. */
    public static @NotNull BaseTemplates.BaseBlueprint mansion() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int minX = -14;
        int maxX = 14;
        int minZ = -12;
        int maxZ = 8;
        int f1 = 4;
        int f2 = 8;
        int top = 11;

        fillRect(b, minX - 2, maxX + 2, minZ - 2, maxZ + 2, -1, Material.POLISHED_BLACKSTONE);
        fillRect(b, minX, maxX, minZ, maxZ, 0, Material.DARK_OAK_PLANKS);
        clearVolume(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, 1, top);
        shell(b, minX, maxX, minZ, maxZ, 1, top, Material.QUARTZ_BLOCK, Material.GOLD_BLOCK);

        fillRect(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, f1, Material.SMOOTH_QUARTZ);
        fillRect(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, f2, Material.SMOOTH_QUARTZ);
        clearVolume(b, -1, 1, -1, 1, f1, f1);
        clearVolume(b, -1, 1, -1, 1, f2, f2);
        clearVolume(b, 11, 13, 3, 6, f1, f1);
        clearVolume(b, 11, 13, 3, 6, f2, f2);

        gabledRoof(
                b, minX, maxX, minZ, maxZ, top + 1,
                Material.DARK_OAK_STAIRS, Material.DARK_OAK_PLANKS,
                Material.DARK_OAK_SLAB, Material.QUARTZ_BLOCK
        );
        // Gold accents at corners for mansion bling (not a flat gold lid)
        b.set(minX, top + 1, minZ, Material.GOLD_BLOCK);
        b.set(maxX, top + 1, minZ, Material.GOLD_BLOCK);
        b.set(minX, top + 1, maxZ, Material.GOLD_BLOCK);
        b.set(maxX, top + 1, maxZ, Material.GOLD_BLOCK);

        // Entrance
        b.doubleDoor(-1, 1, maxZ, Material.DARK_OAK_DOOR, BlockFace.SOUTH);
        b.set(0, 2, maxZ, Material.GLASS);

        // Dual lifts with landings on every floor walk height
        buildLift(b, 0, 0, 0, top, 1, f1 + 1, f2 + 1);
        buildLift(b, -12, 0, 0, top, 1, f1 + 1, f2 + 1);
        markLiftLobby(b, 0, 0, 1);
        markLiftLobby(b, 0, 0, f1 + 1);
        markLiftLobby(b, 0, 0, f2 + 1);
        markLiftLobby(b, -12, 0, 1);
        markLiftLobby(b, -12, 0, f1 + 1);

        // Grand stairs
        for (int i = 0; i < 4; i++) {
            b.stairs(12, 1 + i, 6 - i, Material.QUARTZ_STAIRS, BlockFace.NORTH);
            b.set(12, 1 + i, 5 - i, Material.QUARTZ_BLOCK);
        }
        for (int i = 0; i < 4; i++) {
            b.stairs(12, f1 + 1 + i, 6 - i, Material.QUARTZ_STAIRS, BlockFace.NORTH);
            b.set(12, f1 + 1 + i, 5 - i, Material.QUARTZ_BLOCK);
        }

        // 6 bedrooms across floors — matching floors, hang from real ceiling
        furnishBedroom(b, -13, -6, -11, -8, 1, f1,
                Material.DARK_OAK_PLANKS, Material.SPRUCE_PLANKS, Material.RED_BED, BlockFace.EAST);
        furnishBedroom(b, -13, -6, -6, -3, 1, f1,
                Material.DARK_OAK_PLANKS, Material.SPRUCE_PLANKS, Material.ORANGE_BED, BlockFace.EAST);
        furnishBedroom(b, 6, 13, -11, -8, 1, f1,
                Material.DARK_OAK_PLANKS, Material.SPRUCE_PLANKS, Material.YELLOW_BED, BlockFace.WEST);
        furnishBedroom(b, -13, -6, -11, -8, f1 + 1, f2,
                Material.SMOOTH_QUARTZ, Material.QUARTZ_BLOCK, Material.LIME_BED, BlockFace.EAST);
        furnishBedroom(b, 6, 13, -11, -8, f1 + 1, f2,
                Material.SMOOTH_QUARTZ, Material.QUARTZ_BLOCK, Material.LIGHT_BLUE_BED, BlockFace.WEST);
        furnishBedroom(b, -13, -6, -11, -8, f2 + 1, top + 1,
                Material.SMOOTH_QUARTZ, Material.QUARTZ_BLOCK, Material.MAGENTA_BED, BlockFace.EAST);

        // Windows
        for (int y : new int[]{2, 6, 10}) {
            for (int z = -10; z <= 6; z += 2) {
                b.set(minX, y, z, Material.GLASS);
                b.set(maxX, y, z, Material.GLASS);
            }
        }

        // Ballroom lounge
        for (int x = -3; x <= 3; x++) {
            b.stairs(x, 1, 5, Material.DARK_OAK_STAIRS, BlockFace.SOUTH);
        }
        b.set(0, 1, 4, Material.JUKEBOX);
        b.set(-4, 1, 6, Material.BOOKSHELF);
        b.set(4, 1, 6, Material.ENCHANTING_TABLE);
        b.set(-10, 1, 6, Material.CRAFTING_TABLE);
        b.facing(-13, 1, 6, Material.FURNACE, BlockFace.SOUTH);
        b.facing(-12, 1, 6, Material.SMOKER, BlockFace.SOUTH);
        b.facing(-11, 1, 6, Material.BLAST_FURNACE, BlockFace.SOUTH);

        // Ceiling lights only — never sea lanterns in the walk volume
        for (int x = -12; x <= 12; x += 4) {
            for (int z = -10; z <= 6; z += 4) {
                if (x >= -1 && x <= 2 && z >= -1 && z <= 2) {
                    continue;
                }
                b.set(x, f1 - 1, z, Material.END_ROD);
                b.set(x, f2 - 1, z, Material.END_ROD);
                b.set(x, top, z, Material.END_ROD);
            }
        }

        // Outdoor pool with patio from the east wing
        poolWalkway(b, 5, 12, maxZ + 1, maxZ + 2, 0);
        buildPool(b, 5, 12, maxZ + 3, maxZ + 9, 0, BlockFace.NORTH);
        for (int x = -2; x <= 1; x++) {
            for (int z = maxZ + 1; z <= maxZ + 3; z++) {
                b.set(x, -1, z, Material.POLISHED_BLACKSTONE);
                b.set(x, 0, z, Material.SMOOTH_QUARTZ);
                b.set(x, 1, z, Material.AIR);
                b.set(x, 2, z, Material.AIR);
            }
        }
        // Indoor plunge with open west access from lounge
        buildPool(b, 6, 10, -1, 2, 0, BlockFace.WEST);

        return finish(b, "mansion", "3-level gold mansion - dual lifts, 6 suites, indoor & outdoor pools", 0, maxZ + 2);
    }

    /** Modern cube home — concrete, glass, rooftop pool, compact lift. */
    public static @NotNull BaseTemplates.BaseBlueprint modern() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int minX = -8;
        int maxX = 8;
        int minZ = -8;
        int maxZ = 6;
        int loft = 4;
        int top = 7;

        fillRect(b, minX - 1, maxX + 1, minZ - 1, maxZ + 1, -1, Material.GRAY_CONCRETE);
        fillRect(b, minX, maxX, minZ, maxZ, 0, Material.LIGHT_GRAY_CONCRETE);
        clearVolume(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, 1, top);
        shell(b, minX, maxX, minZ, maxZ, 1, top, Material.WHITE_CONCRETE, Material.BLACK_CONCRETE);
        fillRect(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, loft, Material.WHITE_CONCRETE);
        clearVolume(b, -1, 1, -1, 1, loft, loft);

        // Glass walls
        for (int y = 1; y <= 3; y++) {
            for (int x = minX + 1; x <= maxX - 1; x++) {
                b.set(x, y, maxZ, Material.GLASS);
                b.set(x, y, minZ, Material.GLASS);
            }
            for (int z = minZ + 1; z <= maxZ - 1; z++) {
                b.set(minX, y, z, Material.GLASS);
                b.set(maxX, y, z, Material.GLASS);
            }
        }
        b.door(0, 1, maxZ, Material.OAK_DOOR, BlockFace.SOUTH);

        buildLift(b, 0, 0, 0, top, 1, loft + 1);
        markLiftLobby(b, 0, 0, 1);
        markLiftLobby(b, 0, 0, loft + 1);
        furnishBedroom(b, -7, -3, -7, -4, 1, loft,
                Material.LIGHT_GRAY_CONCRETE, Material.WHITE_CONCRETE, Material.WHITE_BED, BlockFace.EAST);
        furnishBedroom(b, 3, 7, -7, -4, 1, loft,
                Material.LIGHT_GRAY_CONCRETE, Material.WHITE_CONCRETE, Material.LIGHT_GRAY_BED, BlockFace.WEST);
        furnishBedroom(b, -7, -3, -7, -4, loft + 1, top + 1,
                Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.CYAN_BED, BlockFace.EAST);
        furnishBedroom(b, 3, 7, -7, -4, loft + 1, top + 1,
                Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.BLUE_BED, BlockFace.WEST);
        furnishBedroom(b, -2, 2, -7, -5, loft + 1, top + 1,
                Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.GRAY_BED, BlockFace.SOUTH);

        b.set(6, 1, 4, Material.CRAFTING_TABLE);
        b.facing(7, 1, 4, Material.SMOKER, BlockFace.WEST);
        b.facing(7, 1, 3, Material.FURNACE, BlockFace.WEST);
        b.set(-6, 1, 4, Material.ENCHANTING_TABLE);
        b.set(-7, 1, 4, Material.BOOKSHELF);
        b.set(-5, 1, 4, Material.ENDER_CHEST);

        for (int x = -6; x <= 6; x += 3) {
            b.set(x, loft - 1, 0, Material.END_ROD);
            b.set(x, top, 0, Material.END_ROD);
        }

        // Front stand pad
        for (int x = -1; x <= 1; x++) {
            b.set(x, 0, maxZ + 1, Material.LIGHT_GRAY_CONCRETE);
            b.set(x, 0, maxZ + 2, Material.LIGHT_GRAY_CONCRETE);
            b.set(x, 1, maxZ + 2, Material.AIR);
            b.set(x, 2, maxZ + 2, Material.AIR);
        }

        // Rooftop pool
        fillRect(b, minX, maxX, minZ, maxZ, top + 1, Material.WHITE_CONCRETE);
        // Rooftop pool with open south access from the roof deck
        buildPool(b, -5, 5, -5, 3, top + 1, BlockFace.SOUTH);

        return finish(b, "modern", "Modern glass-concrete home - lift, 5 beds, rooftop pool", 0, maxZ + 2);
    }

    /** Resort pavilion — open luxury pool club with cabanas (bedroom pods). */
    public static @NotNull BaseTemplates.BaseBlueprint resort() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();

        // Deck platform
        fillRect(b, -12, 12, -8, 14, -1, Material.SANDSTONE);
        fillRect(b, -12, 12, -8, 14, 0, Material.SMOOTH_SANDSTONE);

        // Huge resort pool center
        // Huge resort pool center — open access from clubhouse (north)
        buildPool(b, -6, 6, -2, 10, 0, BlockFace.NORTH);

        // Main clubhouse at back (-Z)
        int minX = -8;
        int maxX = 8;
        int minZ = -8;
        int maxZ = -3;
        fillRect(b, minX, maxX, minZ, maxZ, 0, Material.CUT_SANDSTONE);
        clearVolume(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, 1, 3);
        shell(b, minX, maxX, minZ, maxZ, 1, 3, Material.SANDSTONE, Material.SMOOTH_SANDSTONE);
        gabledRoof(
                b, minX, maxX, minZ, maxZ, 4,
                Material.JUNGLE_STAIRS, Material.JUNGLE_PLANKS,
                Material.JUNGLE_SLAB, Material.SANDSTONE
        );
        b.door(0, 1, maxZ, Material.BIRCH_DOOR, BlockFace.SOUTH);

        // 5 cabana bedrooms around pool
        cabana(b, -12, -9, 2, 5, Material.RED_BED);
        cabana(b, -12, -9, 7, 10, Material.BLUE_BED);
        cabana(b, 9, 12, 2, 5, Material.YELLOW_BED);
        cabana(b, 9, 12, 7, 10, Material.GREEN_BED);
        cabana(b, -2, 2, 11, 14, Material.PURPLE_BED);

        // Bar / kitchen
        b.set(-3, 1, -6, Material.CRAFTING_TABLE);
        b.facing(-4, 1, -6, Material.SMOKER, BlockFace.SOUTH);
        b.set(3, 1, -6, Material.BARREL);
        b.set(4, 1, -6, Material.BARREL);
        b.set(0, 1, -6, Material.JUKEBOX);
        b.set(0, 4, -5, Material.CUT_SANDSTONE);
        safeCeilingLantern(b, 0, -5, 4);

        // Palm-like posts + sea lanterns
        for (int x = -10; x <= 10; x += 5) {
            b.set(x, 1, 12, Material.JUNGLE_LOG);
            b.set(x, 2, 12, Material.JUNGLE_LEAVES);
            b.set(x, 1, -2, Material.SEA_LANTERN);
        }

        // Lounge chairs
        for (int x = -5; x <= 5; x += 2) {
            b.stairs(x, 1, 0, Material.BIRCH_STAIRS, BlockFace.SOUTH);
        }

        return finish(b, "resort", "Resort club - mega pool, 5 cabana bedrooms, bar lounge", 0, -1);
    }

    /**
     * GrabCraft-inspired huge modern estate (Huge Modern Mansion vibes):
     * wide sandstone/quartz wings, library, dual lifts, court pool, 8 suites.
     */
    public static @NotNull BaseTemplates.BaseBlueprint estate() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int minX = -18;
        int maxX = 18;
        int minZ = -14;
        int maxZ = 10;
        int f1 = 4;
        int f2 = 8;
        int top = 11;

        fillRect(b, minX - 2, maxX + 2, minZ - 2, maxZ + 3, -1, Material.SANDSTONE);
        fillRect(b, minX, maxX, minZ, maxZ, 0, Material.SMOOTH_SANDSTONE);
        clearVolume(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, 1, top);
        shell(b, minX, maxX, minZ, maxZ, 1, top, Material.SANDSTONE, Material.QUARTZ_BLOCK);

        fillRect(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, f1, Material.SMOOTH_QUARTZ);
        fillRect(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, f2, Material.SMOOTH_QUARTZ);
        clearVolume(b, -1, 1, -1, 1, f1, f1);
        clearVolume(b, -1, 1, -1, 1, f2, f2);
        clearVolume(b, 14, 16, 4, 7, f1, f1);
        clearVolume(b, 14, 16, 4, 7, f2, f2);

        // Flat modern roof deck (GrabCraft huge-mansion silhouette) + parapet
        fillRect(b, minX, maxX, minZ, maxZ, top + 1, Material.SMOOTH_SANDSTONE);
        ring(b, minX, maxX, minZ, maxZ, top + 2, Material.SANDSTONE_WALL);

        b.doubleDoor(-1, 1, maxZ, Material.BIRCH_DOOR, BlockFace.SOUTH);
        for (int x = -6; x <= 6; x++) {
            if (x == -1 || x == 0) {
                continue;
            }
            b.set(x, 2, maxZ, Material.GLASS);
            b.set(x, 3, maxZ, Material.GLASS);
        }

        buildLift(b, 0, 0, 0, top, 1, f1 + 1, f2 + 1);
        buildLift(b, -15, 0, 0, top, 1, f1 + 1, f2 + 1);
        markLiftLobby(b, 0, 0, 1);
        markLiftLobby(b, 0, 0, f1 + 1);
        markLiftLobby(b, 0, 0, f2 + 1);
        markLiftLobby(b, -15, 0, 1);
        markLiftLobby(b, -15, 0, f1 + 1);

        // Grand stair
        for (int i = 0; i < 4; i++) {
            b.stairs(15, 1 + i, 7 - i, Material.QUARTZ_STAIRS, BlockFace.NORTH);
            b.set(15, 1 + i, 6 - i, Material.QUARTZ_BLOCK);
        }
        for (int i = 0; i < 4; i++) {
            b.stairs(15, f1 + 1 + i, 7 - i, Material.QUARTZ_STAIRS, BlockFace.NORTH);
            b.set(15, f1 + 1 + i, 6 - i, Material.QUARTZ_BLOCK);
        }

        // 8 suites across floors
        furnishBedroom(b, -17, -8, -13, -10, 1, f1,
                Material.DARK_OAK_PLANKS, Material.SPRUCE_PLANKS, Material.RED_BED, BlockFace.EAST);
        furnishBedroom(b, -17, -8, -8, -5, 1, f1,
                Material.DARK_OAK_PLANKS, Material.SPRUCE_PLANKS, Material.ORANGE_BED, BlockFace.EAST);
        furnishBedroom(b, 8, 17, -13, -10, 1, f1,
                Material.DARK_OAK_PLANKS, Material.SPRUCE_PLANKS, Material.YELLOW_BED, BlockFace.WEST);
        furnishBedroom(b, 8, 17, -8, -5, 1, f1,
                Material.DARK_OAK_PLANKS, Material.SPRUCE_PLANKS, Material.LIME_BED, BlockFace.WEST);
        furnishBedroom(b, -17, -8, -13, -10, f1 + 1, f2,
                Material.SMOOTH_QUARTZ, Material.QUARTZ_BLOCK, Material.LIGHT_BLUE_BED, BlockFace.EAST);
        furnishBedroom(b, 8, 17, -13, -10, f1 + 1, f2,
                Material.SMOOTH_QUARTZ, Material.QUARTZ_BLOCK, Material.CYAN_BED, BlockFace.WEST);
        furnishBedroom(b, -17, -8, -13, -10, f2 + 1, top + 1,
                Material.SMOOTH_QUARTZ, Material.QUARTZ_BLOCK, Material.PURPLE_BED, BlockFace.EAST);
        furnishBedroom(b, 8, 17, -13, -10, f2 + 1, top + 1,
                Material.SMOOTH_QUARTZ, Material.QUARTZ_BLOCK, Material.MAGENTA_BED, BlockFace.WEST);

        // Library wing (GrabCraft estate amenity)
        for (int x = -16; x <= -10; x++) {
            for (int z = 4; z <= 8; z++) {
                b.set(x, 1, z, Material.BOOKSHELF);
            }
        }
        b.set(-13, 1, 6, Material.ENCHANTING_TABLE);
        b.set(-12, 1, 6, Material.LECTERN);
        clearVolume(b, -15, -11, 5, 7, 1, 2);

        // Kitchen / utility
        b.set(10, 1, 7, Material.CRAFTING_TABLE);
        b.facing(11, 1, 7, Material.SMOKER, BlockFace.WEST);
        b.facing(12, 1, 7, Material.FURNACE, BlockFace.WEST);
        b.facing(13, 1, 7, Material.BLAST_FURNACE, BlockFace.WEST);
        b.set(14, 1, 8, Material.BARREL);
        b.set(0, 1, 7, Material.JUKEBOX);

        for (int y : new int[]{2, 6, 10}) {
            for (int z = -12; z <= 8; z += 2) {
                b.set(minX, y, z, Material.GLASS);
                b.set(maxX, y, z, Material.GLASS);
            }
        }
        for (int x = -16; x <= 16; x += 4) {
            for (int z = -12; z <= 8; z += 4) {
                if (Math.abs(x) <= 2 && Math.abs(z) <= 2) {
                    continue;
                }
                b.set(x, f1 - 1, z, Material.END_ROD);
                b.set(x, f2 - 1, z, Material.END_ROD);
                b.set(x, top, z, Material.END_ROD);
            }
        }

        // Court pool + patio (front +Z)
        poolWalkway(b, -8, 8, maxZ + 1, maxZ + 2, 0);
        buildPool(b, -8, 8, maxZ + 3, maxZ + 12, 0, BlockFace.NORTH);
        // Rooftop plunge
        buildPool(b, -4, 4, -4, 2, top + 1, BlockFace.SOUTH);

        return finish(
                b,
                "estate",
                "GrabCraft-style mega estate - 3 floors, dual lifts, library, 8 suites, court + roof pools",
                0, maxZ + 2
        );
    }

    /**
     * GrabCraft-inspired tall fantasy chateau / wooden mansion:
     * stone brick shell, corner turrets, great hall, gabled roof, side pool.
     */
    public static @NotNull BaseTemplates.BaseBlueprint chateau() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int minX = -12;
        int maxX = 12;
        int minZ = -11;
        int maxZ = 7;
        int f1 = 4;
        int top = 7;

        fillRect(b, minX - 1, maxX + 1, minZ - 1, maxZ + 2, -1, Material.COBBLESTONE);
        fillRect(b, minX, maxX, minZ, maxZ, 0, Material.DARK_OAK_PLANKS);
        clearVolume(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, 1, top);
        shell(b, minX, maxX, minZ, maxZ, 1, top, Material.STONE_BRICKS, Material.DARK_OAK_LOG);

        fillRect(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, f1, Material.SPRUCE_PLANKS);
        clearVolume(b, -1, 1, -1, 1, f1, f1);

        gabledRoof(
                b, minX, maxX, minZ, maxZ, top + 1,
                Material.DARK_OAK_STAIRS, Material.DARK_OAK_PLANKS,
                Material.DARK_OAK_SLAB, Material.STONE_BRICKS
        );

        // Corner turrets (fantasy mansion silhouette)
        for (int[] c : new int[][]{
                {minX, minZ}, {maxX, minZ}, {minX, maxZ}, {maxX, maxZ}
        }) {
            int tx = c[0];
            int tz = c[1];
            for (int y = 1; y <= top + 3; y++) {
                b.set(tx, y, tz, Material.STONE_BRICKS);
            }
            b.set(tx, top + 4, tz, Material.DARK_OAK_FENCE);
            b.set(tx, top + 5, tz, Material.LANTERN);
            for (int dy = 2; dy <= 6; dy += 2) {
                if (tx == minX) {
                    b.set(tx + 1, dy, tz, Material.GLASS_PANE);
                } else {
                    b.set(tx - 1, dy, tz, Material.GLASS_PANE);
                }
            }
        }

        b.doubleDoor(-1, 1, maxZ, Material.DARK_OAK_DOOR, BlockFace.SOUTH);
        b.set(0, 2, maxZ, Material.IRON_BARS);

        buildLift(b, 0, 0, 0, top, 1, f1 + 1);
        markLiftLobby(b, 0, 0, 1);
        markLiftLobby(b, 0, 0, f1 + 1);

        for (int i = 0; i < 4; i++) {
            b.stairs(9, 1 + i, 5 - i, Material.STONE_BRICK_STAIRS, BlockFace.NORTH);
            b.set(9, 1 + i, 4 - i, Material.STONE_BRICKS);
            // Keep stair column clear of furniture
            b.set(9, 1 + i, 5 - i + 1, Material.AIR);
        }

        furnishBedroom(b, -11, -5, -10, -7, 1, f1,
                Material.DARK_OAK_PLANKS, Material.SPRUCE_PLANKS, Material.RED_BED, BlockFace.EAST);
        furnishBedroom(b, -11, -5, -5, -2, 1, f1,
                Material.DARK_OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BLUE_BED, BlockFace.EAST);
        furnishBedroom(b, 5, 11, -10, -7, 1, f1,
                Material.DARK_OAK_PLANKS, Material.SPRUCE_PLANKS, Material.GREEN_BED, BlockFace.WEST);
        furnishBedroom(b, -11, -5, -10, -7, f1 + 1, top + 1,
                Material.SPRUCE_PLANKS, Material.DARK_OAK_PLANKS, Material.YELLOW_BED, BlockFace.EAST);
        furnishBedroom(b, 5, 11, -10, -7, f1 + 1, top + 1,
                Material.SPRUCE_PLANKS, Material.DARK_OAK_PLANKS, Material.PURPLE_BED, BlockFace.WEST);
        furnishBedroom(b, -3, 3, -10, -8, f1 + 1, top + 1,
                Material.SPRUCE_PLANKS, Material.DARK_OAK_PLANKS, Material.WHITE_BED, BlockFace.SOUTH);

        // Great hall
        for (int x = -4; x <= 4; x++) {
            b.stairs(x, 1, 4, Material.DARK_OAK_STAIRS, BlockFace.SOUTH);
        }
        b.set(0, 1, 3, Material.JUKEBOX);
        b.set(-8, 1, 5, Material.CRAFTING_TABLE);
        b.facing(-9, 1, 5, Material.FURNACE, BlockFace.SOUTH);
        b.set(8, 1, 5, Material.ENCHANTING_TABLE);
        b.set(10, 1, 5, Material.BOOKSHELF); // was (9,1,5) — blocked stair
        b.set(10, 1, 6, Material.BOOKSHELF);
        b.set(0, 1, -9, Material.ANVIL);

        for (int y = 2; y <= 6; y += 2) {
            for (int z = -9; z <= 5; z += 3) {
                framedWindow(b, minX, y, z, BlockFace.WEST);
                framedWindow(b, maxX, y, z, BlockFace.EAST);
            }
        }
        for (int x = -9; x <= 9; x += 3) {
            safeCeilingLantern(b, x, 0, f1);
            safeCeilingLantern(b, x, 0, top + 1);
        }

        poolWalkway(b, 4, 11, maxZ + 1, maxZ + 2, 0);
        buildPool(b, 4, 11, maxZ + 3, maxZ + 9, 0, BlockFace.NORTH);

        return finish(
                b,
                "chateau",
                "GrabCraft-style fantasy chateau - turrets, great hall, lift, 6 suites, pool",
                0, maxZ + 2
        );
    }

    /**
     * GrabCraft-inspired modernist villa (Modernist / Exotic Modern Villa vibes):
     * white concrete + glass, split levels, balcony, large garden pool.
     */
    public static @NotNull BaseTemplates.BaseBlueprint skyvilla() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int minX = -14;
        int maxX = 14;
        int minZ = -12;
        int maxZ = 8;
        int loft = 4;
        int top = 7;

        fillRect(b, minX - 1, maxX + 1, minZ - 1, maxZ + 2, -1, Material.GRAY_CONCRETE);
        fillRect(b, minX, maxX, minZ, maxZ, 0, Material.WHITE_CONCRETE);
        clearVolume(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, 1, top);
        shell(b, minX, maxX, minZ, maxZ, 1, top, Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE);

        fillRect(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, loft, Material.WHITE_CONCRETE);
        clearVolume(b, -2, 2, -2, 2, loft, loft);

        // Full glass façades
        for (int y = 1; y <= 3; y++) {
            for (int x = minX + 2; x <= maxX - 2; x++) {
                b.set(x, y, maxZ, Material.GLASS);
                b.set(x, y, minZ, Material.GLASS);
            }
            for (int z = minZ + 2; z <= maxZ - 2; z++) {
                b.set(minX, y, z, Material.GLASS);
                b.set(maxX, y, z, Material.GLASS);
            }
        }
        // Upper glass band
        for (int y = loft + 1; y <= loft + 2; y++) {
            for (int x = minX + 2; x <= maxX - 2; x++) {
                b.set(x, y, maxZ, Material.GLASS);
            }
        }

        b.doubleDoor(-1, 1, maxZ, Material.OAK_DOOR, BlockFace.SOUTH);

        buildLift(b, 0, 0, 0, top, 1, loft + 1);
        markLiftLobby(b, 0, 0, 1);
        markLiftLobby(b, 0, 0, loft + 1);

        // Floating balcony slab over front
        fillRect(b, -6, 6, maxZ + 1, maxZ + 3, loft, Material.WHITE_CONCRETE);
        for (int x = -6; x <= 6; x++) {
            b.set(x, loft + 1, maxZ + 3, Material.GLASS_PANE);
        }
        for (int i = 0; i < 4; i++) {
            // Ascend toward -Z (into the house) as Y rises — facing NORTH
            b.stairs(5, 1 + i, maxZ - i, Material.QUARTZ_STAIRS, BlockFace.NORTH);
            b.set(5, 1 + i, maxZ - i - 1, Material.SMOOTH_QUARTZ);
        }

        furnishBedroom(b, -13, -6, -11, -7, 1, loft,
                Material.LIGHT_GRAY_CONCRETE, Material.WHITE_CONCRETE, Material.WHITE_BED, BlockFace.EAST);
        furnishBedroom(b, 6, 13, -11, -7, 1, loft,
                Material.LIGHT_GRAY_CONCRETE, Material.WHITE_CONCRETE, Material.LIGHT_GRAY_BED, BlockFace.WEST);
        furnishBedroom(b, -13, -6, -5, -2, 1, loft,
                Material.LIGHT_GRAY_CONCRETE, Material.WHITE_CONCRETE, Material.CYAN_BED, BlockFace.EAST);
        furnishBedroom(b, -13, -6, -11, -7, loft + 1, top + 1,
                Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.BLUE_BED, BlockFace.EAST);
        furnishBedroom(b, 6, 13, -11, -7, loft + 1, top + 1,
                Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.PURPLE_BED, BlockFace.WEST);
        furnishBedroom(b, -3, 3, -11, -8, loft + 1, top + 1,
                Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.GRAY_BED, BlockFace.SOUTH);

        b.set(10, 1, 5, Material.CRAFTING_TABLE);
        b.facing(11, 1, 5, Material.SMOKER, BlockFace.WEST);
        b.facing(12, 1, 5, Material.FURNACE, BlockFace.WEST);
        b.set(-10, 1, 5, Material.ENCHANTING_TABLE);
        b.set(-11, 1, 5, Material.BOOKSHELF);
        b.set(-9, 1, 5, Material.ENDER_CHEST);
        b.set(0, 1, 5, Material.JUKEBOX);

        for (int x = -12; x <= 12; x += 4) {
            b.set(x, loft - 1, 0, Material.END_ROD);
            b.set(x, top, 0, Material.END_ROD);
        }

        // Flat roof + roof deck pool
        fillRect(b, minX, maxX, minZ, maxZ, top + 1, Material.WHITE_CONCRETE);
        buildPool(b, -5, 5, -6, 2, top + 1, BlockFace.SOUTH);

        // Large garden pool (Exotic Modern Villa outdoor focus)
        poolWalkway(b, -10, 10, maxZ + 1, maxZ + 2, 0);
        buildPool(b, -10, 10, maxZ + 4, maxZ + 14, 0, BlockFace.NORTH);

        // Garden planters
        for (int x = -12; x <= 12; x += 4) {
            b.set(x, 0, maxZ + 3, Material.PODZOL);
            b.set(x, 1, maxZ + 3, Material.AZALEA);
        }

        return finish(
                b,
                "skyvilla",
                "GrabCraft-style modernist villa - glass façades, balcony, 6 beds, garden + roof pools",
                0, maxZ + 2
        );
    }

    /**
     * GrabCraft-inspired spruce longhouse (Nordic): timber frame, peaked roof, hearth hall, loft beds.
     * Original layout — not a scraped GrabCraft schematic.
     */
    public static @NotNull BaseTemplates.BaseBlueprint nordic() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int minX = -7;
        int maxX = 7;
        int minZ = -6;
        int maxZ = 5;
        int wallH = 4;

        fillRect(b, minX - 1, maxX + 1, minZ - 1, maxZ + 1, -1, Material.COBBLESTONE);
        fillRect(b, minX, maxX, minZ, maxZ, 0, Material.SPRUCE_PLANKS);
        clearVolume(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, 1, wallH);
        shell(b, minX, maxX, minZ, maxZ, 1, wallH, Material.SPRUCE_PLANKS, Material.SPRUCE_LOG);

        // Timber posts every 3 blocks
        for (int x = minX; x <= maxX; x += 3) {
            for (int y = 1; y <= wallH; y++) {
                b.set(x, y, minZ, Material.SPRUCE_LOG);
                b.set(x, y, maxZ, Material.SPRUCE_LOG);
            }
        }
        for (int z = minZ; z <= maxZ; z += 3) {
            for (int y = 1; y <= wallH; y++) {
                b.set(minX, y, z, Material.SPRUCE_LOG);
                b.set(maxX, y, z, Material.SPRUCE_LOG);
            }
        }

        gabledRoof(
                b, minX, maxX, minZ, maxZ, wallH + 1,
                Material.SPRUCE_STAIRS, Material.SPRUCE_PLANKS,
                Material.SPRUCE_SLAB, Material.SPRUCE_PLANKS
        );

        b.doubleDoor(-1, 1, maxZ, Material.SPRUCE_DOOR, BlockFace.SOUTH);
        for (int x = -5; x <= 5; x += 2) {
            if (x == -1 || x == 0) {
                continue;
            }
            framedWindow(b, x, 2, minZ, BlockFace.NORTH);
            framedWindow(b, x, 2, maxZ, BlockFace.SOUTH);
        }
        framedWindow(b, minX, 2, 0, BlockFace.WEST);
        framedWindow(b, maxX, 2, 0, BlockFace.EAST);

        // Great hearth
        b.set(0, 1, -2, Material.CAMPFIRE);
        b.set(-1, 1, -2, Material.COBBLESTONE);
        b.set(1, 1, -2, Material.COBBLESTONE);
        b.set(0, 1, -3, Material.COBBLESTONE);
        b.set(0, 1, -1, Material.COBBLESTONE);
        b.set(0, 2, -2, Material.IRON_BARS);
        b.set(0, wallH + 1, -2, Material.COBBLESTONE);
        b.set(0, wallH + 2, -2, Material.COBBLESTONE);
        b.set(0, wallH + 3, -2, Material.CAMPFIRE);

        // Benches + table
        for (int x = -4; x <= 4; x++) {
            b.stairs(x, 1, 1, Material.SPRUCE_STAIRS, BlockFace.SOUTH);
            b.stairs(x, 1, 3, Material.SPRUCE_STAIRS, BlockFace.NORTH);
        }
        fillRect(b, -3, 3, 2, 2, 1, Material.SPRUCE_PLANKS);

        // Side bedrooms
        furnishBedroom(b, -6, -5, -5, -3, 1, wallH + 1,
                Material.SPRUCE_PLANKS, Material.SPRUCE_LOG, Material.RED_BED, BlockFace.EAST);
        furnishBedroom(b, 5, 6, -5, -3, 1, wallH + 1,
                Material.SPRUCE_PLANKS, Material.SPRUCE_LOG, Material.BLUE_BED, BlockFace.WEST);
        furnishBedroom(b, -6, -5, 0, 2, 1, wallH + 1,
                Material.SPRUCE_PLANKS, Material.SPRUCE_LOG, Material.YELLOW_BED, BlockFace.EAST);
        furnishBedroom(b, 5, 6, 0, 2, 1, wallH + 1,
                Material.SPRUCE_PLANKS, Material.SPRUCE_LOG, Material.GREEN_BED, BlockFace.WEST);

        // Storage loft markers
        b.set(-5, 1, 4, Material.BARREL);
        b.set(5, 1, 4, Material.BARREL);
        b.set(0, 1, 4, Material.CRAFTING_TABLE);
        b.set(-2, 1, 4, Material.FURNACE);
        b.set(2, 1, 4, Material.CHEST);
        safeCeilingLantern(b, -3, 0, wallH);
        safeCeilingLantern(b, 3, 0, wallH);
        safeCeilingLantern(b, 0, 3, wallH);

        // Front path
        b.set(0, 0, maxZ + 1, Material.COBBLESTONE_SLAB);
        b.set(0, 0, maxZ + 2, Material.DIRT_PATH);
        b.set(-1, 0, maxZ + 2, Material.DIRT_PATH);
        b.set(1, 0, maxZ + 2, Material.DIRT_PATH);

        return b.build("nordic", "Nordic spruce longhouse - hearth hall, timber posts, loft beds", 0, 0, maxZ + 2);
    }

    /**
     * GrabCraft-inspired medieval stone manor: keep walls, battlements, courtyard, great hall.
     * Original layout — not a scraped GrabCraft schematic.
     */
    public static @NotNull BaseTemplates.BaseBlueprint medieval() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int minX = -10;
        int maxX = 10;
        int minZ = -9;
        int maxZ = 7;
        int wallH = 5;

        fillRect(b, minX - 2, maxX + 2, minZ - 2, maxZ + 2, -1, Material.STONE_BRICKS);
        fillRect(b, minX, maxX, minZ, maxZ, 0, Material.STONE_BRICKS);
        clearVolume(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, 1, wallH);
        shell(b, minX, maxX, minZ, maxZ, 1, wallH, Material.STONE_BRICKS, Material.COBBLESTONE);

        // Corner towers
        for (int[] c : new int[][]{{minX, minZ}, {maxX, minZ}, {minX, maxZ}, {maxX, maxZ}}) {
            for (int y = 1; y <= wallH + 2; y++) {
                b.set(c[0], y, c[1], Material.STONE_BRICKS);
                b.set(c[0] + (c[0] < 0 ? 1 : -1), y, c[1], Material.STONE_BRICKS);
                b.set(c[0], y, c[1] + (c[1] < 0 ? 1 : -1), Material.STONE_BRICKS);
            }
            b.set(c[0], wallH + 3, c[1], Material.STONE_BRICK_WALL);
            b.set(c[0], wallH + 4, c[1], Material.LANTERN);
        }

        // Battlement crenels along walls
        for (int x = minX + 2; x <= maxX - 2; x += 2) {
            b.set(x, wallH + 1, minZ, Material.STONE_BRICK_WALL);
            b.set(x, wallH + 1, maxZ, Material.STONE_BRICK_WALL);
        }
        for (int z = minZ + 2; z <= maxZ - 2; z += 2) {
            b.set(minX, wallH + 1, z, Material.STONE_BRICK_WALL);
            b.set(maxX, wallH + 1, z, Material.STONE_BRICK_WALL);
        }

        gabledRoof(
                b, minX + 2, maxX - 2, minZ + 2, maxZ - 2, wallH + 1,
                Material.DARK_OAK_STAIRS, Material.DARK_OAK_PLANKS,
                Material.DARK_OAK_SLAB, Material.STONE_BRICKS
        );

        b.doubleDoor(-1, 1, maxZ, Material.IRON_DOOR, BlockFace.SOUTH);
        b.facing(-2, 2, maxZ + 1, Material.STONE_BUTTON, BlockFace.SOUTH);
        b.facing(-2, 2, maxZ - 1, Material.STONE_BUTTON, BlockFace.NORTH);

        // Arrow-slit windows
        for (int z = -6; z <= 4; z += 3) {
            b.set(minX, 2, z, Material.IRON_BARS);
            b.set(minX, 3, z, Material.IRON_BARS);
            b.set(maxX, 2, z, Material.IRON_BARS);
            b.set(maxX, 3, z, Material.IRON_BARS);
        }

        // Open courtyard fountain
        fillRect(b, -2, 2, -2, 2, 0, Material.POLISHED_ANDESITE);
        b.set(0, 1, 0, Material.WATER);
        b.set(-1, 1, 0, Material.STONE_BRICK_WALL);
        b.set(1, 1, 0, Material.STONE_BRICK_WALL);
        b.set(0, 1, -1, Material.STONE_BRICK_WALL);
        b.set(0, 1, 1, Material.STONE_BRICK_WALL);
        b.set(0, 2, 0, Material.LANTERN);

        // Great hall furniture
        for (int x = -6; x <= -3; x++) {
            b.stairs(x, 1, 4, Material.DARK_OAK_STAIRS, BlockFace.SOUTH);
        }
        for (int x = 3; x <= 6; x++) {
            b.stairs(x, 1, 4, Material.DARK_OAK_STAIRS, BlockFace.SOUTH);
        }
        b.set(0, 1, 5, Material.LECTERN);
        b.set(-1, 1, 5, Material.BOOKSHELF);
        b.set(1, 1, 5, Material.BOOKSHELF);

        furnishBedroom(b, -9, -7, -8, -5, 1, wallH + 1,
                Material.STONE_BRICKS, Material.COBBLESTONE, Material.RED_BED, BlockFace.EAST);
        furnishBedroom(b, 7, 9, -8, -5, 1, wallH + 1,
                Material.STONE_BRICKS, Material.COBBLESTONE, Material.BLUE_BED, BlockFace.WEST);
        furnishBedroom(b, -9, -7, -2, 0, 1, wallH + 1,
                Material.STONE_BRICKS, Material.COBBLESTONE, Material.YELLOW_BED, BlockFace.EAST);
        furnishBedroom(b, 7, 9, -2, 0, 1, wallH + 1,
                Material.STONE_BRICKS, Material.COBBLESTONE, Material.GREEN_BED, BlockFace.WEST);

        b.set(-8, 1, 5, Material.CRAFTING_TABLE);
        b.set(-7, 1, 5, Material.FURNACE);
        b.set(7, 1, 5, Material.BLAST_FURNACE);
        b.set(8, 1, 5, Material.SMOKER);
        b.set(0, 1, -7, Material.CHEST);
        b.set(-1, 1, -7, Material.BARREL);
        b.set(1, 1, -7, Material.BARREL);
        safeCeilingLantern(b, -5, 2, wallH);
        safeCeilingLantern(b, 5, 2, wallH);
        safeCeilingLantern(b, 0, -5, wallH);

        b.set(0, 0, maxZ + 1, Material.STONE_BRICK_SLAB);
        b.set(0, 0, maxZ + 2, Material.STONE_BRICKS);
        b.set(-1, 0, maxZ + 2, Material.STONE_BRICKS);
        b.set(1, 0, maxZ + 2, Material.STONE_BRICKS);

        return b.build("medieval", "Medieval stone manor - towers, battlements, courtyard fountain", 0, 0, maxZ + 2);
    }

    /**
     * GrabCraft-inspired grand palace: courtyard, fountain, gold accents, ballroom, dual wings.
     */
    public static @NotNull BaseTemplates.BaseBlueprint palace() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int minX = -16;
        int maxX = 16;
        int minZ = -13;
        int maxZ = 9;
        int f1 = 4;
        int top = 8;

        fillRect(b, minX - 2, maxX + 2, minZ - 2, maxZ + 2, -1, Material.POLISHED_DIORITE);
        fillRect(b, minX, maxX, minZ, maxZ, 0, Material.SMOOTH_QUARTZ);
        clearVolume(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, 1, top);
        shell(b, minX, maxX, minZ, maxZ, 1, top, Material.QUARTZ_BLOCK, Material.GOLD_BLOCK);

        fillRect(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, f1, Material.SMOOTH_QUARTZ);
        clearVolume(b, -2, 2, -2, 2, f1, f1);

        gabledRoof(
                b, minX, maxX, minZ, maxZ, top + 1,
                Material.QUARTZ_STAIRS, Material.QUARTZ_BLOCK,
                Material.QUARTZ_SLAB, Material.QUARTZ_BLOCK
        );
        // Gold crown corners
        for (int[] c : new int[][]{
                {minX, minZ}, {maxX, minZ}, {minX, maxZ}, {maxX, maxZ}
        }) {
            b.set(c[0], top + 1, c[1], Material.GOLD_BLOCK);
            b.set(c[0], top + 2, c[1], Material.GOLD_BLOCK);
            b.set(c[0], top + 3, c[1], Material.LANTERN);
        }

        b.doubleDoor(-1, 1, maxZ, Material.DARK_OAK_DOOR, BlockFace.SOUTH);
        b.set(-2, 2, maxZ, Material.GOLD_BLOCK);
        b.set(1, 2, maxZ, Material.GOLD_BLOCK);

        // Open courtyard hole through floor 1 (palace atrium)
        clearVolume(b, -4, 4, -4, 4, 1, f1 - 1);
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                boolean edge = x == -4 || x == 4 || z == -4 || z == 4;
                if (edge) {
                    b.set(x, 0, z, Material.QUARTZ_PILLAR);
                } else {
                    b.set(x, 0, z, Material.WATER);
                    b.set(x, -1, z, Material.PRISMARINE);
                }
            }
        }
        b.set(0, -1, 0, Material.SEA_LANTERN);
        b.set(0, 1, 0, Material.AIR);
        // Fountain jet marker
        b.set(0, 0, 0, Material.WATER);
        for (int y = 1; y <= 3; y++) {
            b.set(0, y, 0, Material.IRON_BARS);
        }
        b.set(0, 4, 0, Material.SEA_LANTERN);

        buildLift(b, -14, 0, 0, top, 1, f1 + 1);
        buildLift(b, 14, 0, 0, top, 1, f1 + 1);
        markLiftLobby(b, -14, 0, 1);
        markLiftLobby(b, -14, 0, f1 + 1);
        markLiftLobby(b, 14, 0, 1);
        markLiftLobby(b, 14, 0, f1 + 1);

        for (int i = 0; i < 4; i++) {
            b.stairs(-8, 1 + i, 6 - i, Material.QUARTZ_STAIRS, BlockFace.NORTH);
            b.stairs(8, 1 + i, 6 - i, Material.QUARTZ_STAIRS, BlockFace.NORTH);
            b.set(-8, 1 + i, 5 - i, Material.SMOOTH_QUARTZ);
            b.set(8, 1 + i, 5 - i, Material.SMOOTH_QUARTZ);
        }
        // Second flight to upper floor (f1 → top)
        for (int i = 0; i < 4; i++) {
            b.stairs(-8, f1 + 1 + i, 6 - i, Material.QUARTZ_STAIRS, BlockFace.NORTH);
            b.stairs(8, f1 + 1 + i, 6 - i, Material.QUARTZ_STAIRS, BlockFace.NORTH);
            b.set(-8, f1 + 1 + i, 5 - i, Material.SMOOTH_QUARTZ);
            b.set(8, f1 + 1 + i, 5 - i, Material.SMOOTH_QUARTZ);
        }

        furnishBedroom(b, -15, -8, -12, -9, 1, f1,
                Material.DARK_OAK_PLANKS, Material.RED_WOOL, Material.RED_BED, BlockFace.EAST);
        furnishBedroom(b, -15, -8, -7, -4, 1, f1,
                Material.DARK_OAK_PLANKS, Material.BLUE_WOOL, Material.BLUE_BED, BlockFace.EAST);
        furnishBedroom(b, 8, 15, -12, -9, 1, f1,
                Material.DARK_OAK_PLANKS, Material.YELLOW_WOOL, Material.YELLOW_BED, BlockFace.WEST);
        furnishBedroom(b, 8, 15, -7, -4, 1, f1,
                Material.DARK_OAK_PLANKS, Material.LIME_WOOL, Material.LIME_BED, BlockFace.WEST);
        furnishBedroom(b, -15, -8, -12, -9, f1 + 1, top + 1,
                Material.SMOOTH_QUARTZ, Material.PURPLE_WOOL, Material.PURPLE_BED, BlockFace.EAST);
        furnishBedroom(b, 8, 15, -12, -9, f1 + 1, top + 1,
                Material.SMOOTH_QUARTZ, Material.MAGENTA_WOOL, Material.MAGENTA_BED, BlockFace.WEST);
        furnishBedroom(b, -6, -3, -12, -9, f1 + 1, top + 1,
                Material.SMOOTH_QUARTZ, Material.WHITE_WOOL, Material.WHITE_BED, BlockFace.SOUTH);
        furnishBedroom(b, 3, 6, -12, -9, f1 + 1, top + 1,
                Material.SMOOTH_QUARTZ, Material.LIGHT_BLUE_WOOL, Material.LIGHT_BLUE_BED, BlockFace.SOUTH);

        // Ballroom / throne lounge (south of courtyard)
        for (int x = -5; x <= 5; x++) {
            b.stairs(x, 1, 7, Material.QUARTZ_STAIRS, BlockFace.SOUTH);
        }
        b.set(0, 1, 6, Material.JUKEBOX);
        b.set(-3, 1, 6, Material.GOLD_BLOCK);
        b.set(3, 1, 6, Material.GOLD_BLOCK);
        b.set(-12, 1, 7, Material.CRAFTING_TABLE);
        b.facing(-13, 1, 7, Material.SMOKER, BlockFace.SOUTH);
        b.set(12, 1, 7, Material.ENCHANTING_TABLE);
        b.set(13, 1, 7, Material.BOOKSHELF);
        b.set(14, 1, 7, Material.BOOKSHELF);

        for (int y = 2; y <= 6; y += 2) {
            for (int z = -11; z <= 7; z += 2) {
                b.set(minX, y, z, Material.GLASS);
                b.set(maxX, y, z, Material.GLASS);
            }
        }
        for (int x = -14; x <= 14; x += 4) {
            for (int z = -11; z <= 7; z += 4) {
                if (Math.abs(x) <= 4 && Math.abs(z) <= 4) {
                    continue;
                }
                b.set(x, f1 - 1, z, Material.END_ROD);
                b.set(x, top, z, Material.END_ROD);
            }
        }

        poolWalkway(b, 6, 14, maxZ + 1, maxZ + 2, 0);
        buildPool(b, 6, 14, maxZ + 3, maxZ + 10, 0, BlockFace.NORTH);

        return finish(
                b,
                "palace",
                "GrabCraft-style grand palace - courtyard fountain, gold trim, dual lifts, 8 suites, pool",
                0, maxZ + 2
        );
    }

    // ——— helpers ———

    private static void cabana(
            @NotNull BaseTemplates.Builder b,
            int x1, int x2, int z1, int z2,
            @NotNull Material bed
    ) {
        fillRect(b, x1, x2, z1, z2, 0, Material.CUT_SANDSTONE);
        shell(b, x1, x2, z1, z2, 1, 2, Material.BIRCH_PLANKS, Material.JUNGLE_LOG);
        gabledRoof(
                b, x1, x2, z1, z2, 3,
                Material.JUNGLE_STAIRS, Material.JUNGLE_PLANKS,
                Material.JUNGLE_SLAB, Material.BIRCH_PLANKS
        );
        int doorZ = z2;
        int doorX = (x1 + x2) / 2;
        b.set(doorX, 1, doorZ, Material.AIR);
        b.set(doorX, 2, doorZ, Material.AIR);
        b.door(doorX, 1, doorZ, Material.BIRCH_DOOR, BlockFace.SOUTH);
        furnishBedroom(
                b, x1 + 1, x2 - 1, z1 + 1, z2 - 1, 1, 3,
                Material.BIRCH_PLANKS, Material.STRIPPED_BIRCH_LOG, bed, BlockFace.SOUTH
        );
    }

    /**
     * Dual bubble-column lifts (Bedrock + Java):
     * sealed glass tubes with water sources; soul sand = up, magma = down.
     * Entries use closed iron trapdoors (hold water, no leak) — not open glass gaps.
     */
    private static void buildLift(
            @NotNull BaseTemplates.Builder b,
            int x,
            int z,
            int yBottom,
            int yTop,
            int... walkFloors
    ) {
        // Fully sealed glass shells (up at x, down at x+2)
        for (int y = yBottom; y <= yTop; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    b.set(x + dx, y, z + dz, Material.GLASS);
                    b.set(x + 2 + dx, y, z + dz, Material.GLASS);
                }
            }
        }
        b.set(x, yBottom, z, Material.SOUL_SAND);
        b.set(x + 2, yBottom, z, Material.MAGMA_BLOCK);
        for (int y = yBottom + 1; y <= yTop; y++) {
            b.set(x, y, z, Material.WATER);
            b.set(x + 2, y, z, Material.WATER);
            b.set(x + 1, y, z, Material.GLASS);
        }
        int[] floors = walkFloors.length > 0
                ? walkFloors
                : new int[]{yBottom + 1, Math.max(yBottom + 1, yTop - 1)};
        for (int walkY : floors) {
            if (walkY < yBottom + 1 || walkY > yTop - 1) {
                continue;
            }
            trapdoorLiftDoor(b, x, z, walkY);
            trapdoorLiftDoor(b, x + 2, z, walkY);
            for (int dx = 0; dx <= 2; dx++) {
                b.set(x + dx, walkY, z + 2, Material.AIR);
                b.set(x + dx, walkY + 1, z + 2, Material.AIR);
                b.set(x + dx, walkY, z + 3, Material.AIR);
                b.set(x + dx, walkY + 1, z + 3, Material.AIR);
            }
            // Re-seal side/back glass after lobby clears (prevents water spill)
            b.set(x - 1, walkY, z, Material.GLASS);
            b.set(x - 1, walkY + 1, z, Material.GLASS);
            b.set(x + 3, walkY, z, Material.GLASS);
            b.set(x + 3, walkY + 1, z, Material.GLASS);
            b.set(x, walkY, z - 1, Material.GLASS);
            b.set(x, walkY + 1, z - 1, Material.GLASS);
            b.set(x + 2, walkY, z - 1, Material.GLASS);
            b.set(x + 2, walkY + 1, z - 1, Material.GLASS);
        }
        b.set(x, yBottom - 1, z, Material.SEA_LANTERN);
        b.set(x + 2, yBottom - 1, z, Material.SEA_LANTERN);
        b.set(x, yTop + 1, z, Material.SMOOTH_QUARTZ);
        b.set(x + 2, yTop + 1, z, Material.SMOOTH_QUARTZ);
        b.set(x, yTop + 1, z + 1, Material.END_ROD);
        b.set(x + 2, yTop + 1, z + 1, Material.END_ROD);
    }

    /**
     * 2-high wall-sign door on the south face — holds water, no collision.
     * (Trapdoors are forced open by paste for farm lava blades, so signs are safer here.)
     */
    private static void trapdoorLiftDoor(
            @NotNull BaseTemplates.Builder b,
            int shaftX,
            int shaftZ,
            int walkY
    ) {
        b.set(shaftX + 1, walkY, shaftZ + 1, Material.GLASS);
        b.set(shaftX + 1, walkY + 1, shaftZ + 1, Material.GLASS);
        b.set(shaftX - 1, walkY, shaftZ + 1, Material.GLASS);
        b.set(shaftX - 1, walkY + 1, shaftZ + 1, Material.GLASS);
        b.facing(shaftX, walkY, shaftZ + 1, Material.OAK_WALL_SIGN, BlockFace.WEST);
        b.facing(shaftX, walkY + 1, shaftZ + 1, Material.OAK_WALL_SIGN, BlockFace.WEST);
    }

    /** @deprecated kept for any leftover callers — use {@link #trapdoorLiftDoor} */
    private static void signLiftDoor(
            @NotNull BaseTemplates.Builder b,
            int shaftX,
            int shaftZ,
            int walkY
    ) {
        trapdoorLiftDoor(b, shaftX, shaftZ, walkY);
    }

    /** Sea-lantern approach + gold/end-rod posts so the lift is obvious from hallways. */
    private static void markLiftLobby(
            @NotNull BaseTemplates.Builder b,
            int liftX,
            int liftZ,
            int walkY
    ) {
        int floorY = walkY - 1;
        for (int dx = -1; dx <= 3; dx++) {
            // Keep z+1 as sign doors — lobby markers start at z+2
            b.set(liftX + dx, floorY, liftZ + 2, Material.SEA_LANTERN);
            b.set(liftX + dx, floorY, liftZ + 3, Material.POLISHED_BLACKSTONE);
        }
        b.set(liftX - 1, walkY, liftZ + 2, Material.GOLD_BLOCK);
        b.set(liftX + 3, walkY, liftZ + 2, Material.GOLD_BLOCK);
        b.set(liftX - 1, walkY + 1, liftZ + 2, Material.END_ROD);
        b.set(liftX + 3, walkY + 1, liftZ + 2, Material.END_ROD);
        b.set(liftX, walkY, liftZ + 3, Material.AIR);
        b.set(liftX + 1, walkY, liftZ + 3, Material.AIR);
        b.set(liftX + 2, walkY, liftZ + 3, Material.AIR);
        b.set(liftX, walkY + 1, liftZ + 3, Material.AIR);
        b.set(liftX + 1, walkY + 1, liftZ + 3, Material.AIR);
        b.set(liftX + 2, walkY + 1, liftZ + 3, Material.AIR);
    }

    /**
     * Always-clear spawn pad: solid under feet, air at feet/head (spawnDy = 0), open 3×3.
     * Last write wins — call immediately before {@code b.build(...)}.
     */
    static void protectSpawnPad(
            @NotNull BaseTemplates.Builder b,
            int spawnX,
            int spawnZ
    ) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = spawnX + dx;
                int z = spawnZ + dz;
                b.set(x, -1, z, Material.SMOOTH_STONE);
                b.set(x, 0, z, Material.AIR);
                b.set(x, 1, z, Material.AIR);
            }
        }
        b.set(spawnX, -1, spawnZ, Material.DIRT_PATH);
        b.set(spawnX, 0, spawnZ, Material.AIR);
        b.set(spawnX, 1, spawnZ, Material.AIR);
    }

    private static @NotNull BaseTemplates.BaseBlueprint finish(
            @NotNull BaseTemplates.Builder b,
            @NotNull String id,
            @NotNull String description,
            int spawnX,
            int spawnZ
    ) {
        protectSpawnPad(b, spawnX, spawnZ);
        return b.build(id, description, spawnX, 0, spawnZ);
    }

    /** Lantern flush under ceiling — never below y=3 (head clearance on y=0 floors). */
    private static void safeCeilingLantern(
            @NotNull BaseTemplates.Builder b,
            int x,
            int z,
            int ceilingY
    ) {
        if (ceilingY < 4) {
            b.set(x, 2, z, Material.TORCH);
            return;
        }
        b.hangingLantern(x, ceilingY - 1, z, Material.LANTERN, ceilingY);
    }

    /**
     * Swimming pool with deck, open access from {@code access} side, stairs into water, and ladder.
     * No slab wall on the access edge so you can walk onto the deck from the house patio.
     */
    private static void buildPool(
            @NotNull BaseTemplates.Builder b,
            int x1, int x2, int z1, int z2,
            int deckY,
            @NotNull BlockFace access
    ) {
        int deckMinX = x1 - 1;
        int deckMaxX = x2 + 1;
        int deckMinZ = z1 - 1;
        int deckMaxZ = z2 + 1;

        // Full deck platform (walkable) under and around the basin
        for (int x = deckMinX; x <= deckMaxX; x++) {
            for (int z = deckMinZ; z <= deckMaxZ; z++) {
                b.set(x, deckY - 1, z, Material.PRISMARINE_BRICKS);
                boolean inWater = x >= x1 && x <= x2 && z >= z1 && z <= z2;
                if (inWater) {
                    b.set(x, deckY - 1, z, Material.PRISMARINE);
                    if ((x + z) % 3 == 0) {
                        b.set(x, deckY - 1, z, Material.SEA_LANTERN);
                    }
                    b.set(x, deckY, z, Material.WATER);
                } else {
                    b.set(x, deckY, z, Material.PRISMARINE_BRICKS);
                    b.set(x, deckY + 1, z, Material.AIR);
                    b.set(x, deckY + 2, z, Material.AIR);
                }
            }
        }

        // Low fence on non-access edges only (keeps look without blocking entry)
        for (int x = deckMinX; x <= deckMaxX; x++) {
            for (int z = deckMinZ; z <= deckMaxZ; z++) {
                boolean edge = x == deckMinX || x == deckMaxX || z == deckMinZ || z == deckMaxZ;
                if (!edge) {
                    continue;
                }
                boolean accessEdge =
                        (access == BlockFace.WEST && x == deckMinX)
                                || (access == BlockFace.EAST && x == deckMaxX)
                                || (access == BlockFace.NORTH && z == deckMinZ)
                                || (access == BlockFace.SOUTH && z == deckMaxZ);
                if (accessEdge) {
                    continue; // open for walking onto the deck
                }
                b.set(x, deckY + 1, z, Material.PRISMARINE_WALL);
            }
        }

        // Gate opening 3 blocks wide on access edge
        int midX = (x1 + x2) / 2;
        int midZ = (z1 + z2) / 2;
        if (access == BlockFace.WEST) {
            for (int z = midZ - 1; z <= midZ + 1; z++) {
                b.set(deckMinX, deckY + 1, z, Material.AIR);
            }
        } else if (access == BlockFace.EAST) {
            for (int z = midZ - 1; z <= midZ + 1; z++) {
                b.set(deckMaxX, deckY + 1, z, Material.AIR);
            }
        } else if (access == BlockFace.NORTH) {
            for (int x = midX - 1; x <= midX + 1; x++) {
                b.set(x, deckY + 1, deckMinZ, Material.AIR);
            }
        } else {
            for (int x = midX - 1; x <= midX + 1; x++) {
                b.set(x, deckY + 1, deckMaxZ, Material.AIR);
            }
        }

        // Stairs into the water from the access side
        if (access == BlockFace.WEST) {
            b.stairs(x1, deckY, midZ, Material.PRISMARINE_STAIRS, BlockFace.EAST);
            b.set(x1, deckY + 1, midZ, Material.AIR);
        } else if (access == BlockFace.EAST) {
            b.stairs(x2, deckY, midZ, Material.PRISMARINE_STAIRS, BlockFace.WEST);
            b.set(x2, deckY + 1, midZ, Material.AIR);
        } else if (access == BlockFace.NORTH) {
            b.stairs(midX, deckY, z1, Material.PRISMARINE_STAIRS, BlockFace.SOUTH);
            b.set(midX, deckY + 1, z1, Material.AIR);
        } else {
            b.stairs(midX, deckY, z2, Material.PRISMARINE_STAIRS, BlockFace.NORTH);
            b.set(midX, deckY + 1, z2, Material.AIR);
        }

        // Ladder on far side to climb out
        if (access == BlockFace.WEST) {
            b.facing(x2, deckY, midZ, Material.LADDER, BlockFace.WEST);
        } else if (access == BlockFace.EAST) {
            b.facing(x1, deckY, midZ, Material.LADDER, BlockFace.EAST);
        } else if (access == BlockFace.NORTH) {
            b.facing(midX, deckY, z2, Material.LADDER, BlockFace.NORTH);
        } else {
            b.facing(midX, deckY, z1, Material.LADDER, BlockFace.SOUTH);
        }
    }

    /** Patio walkway connecting house wall to pool deck (solid path, no gaps). */
    private static void poolWalkway(
            @NotNull BaseTemplates.Builder b,
            int x1, int x2, int z1, int z2,
            int y
    ) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                b.set(x, y - 1, z, Material.SMOOTH_STONE);
                b.set(x, y, z, Material.SMOOTH_QUARTZ);
                b.set(x, y + 1, z, Material.AIR);
                b.set(x, y + 2, z, Material.AIR);
            }
        }
    }

    private static void furnishBedroom(
            @NotNull BaseTemplates.Builder b,
            int x1, int x2, int z1, int z2,
            @NotNull Material bed,
            @NotNull BlockFace facing
    ) {
        furnishBedroom(
                b, x1, x2, z1, z2, 1, 4,
                Material.OAK_PLANKS, Material.STRIPPED_OAK_LOG, bed, facing
        );
    }

    private static void furnishBedroom(
            @NotNull BaseTemplates.Builder b,
            int x1, int x2, int z1, int z2,
            int y,
            @NotNull Material bed,
            @NotNull BlockFace facing
    ) {
        furnishBedroom(
                b, x1, x2, z1, z2, y, y + 3,
                Material.SMOOTH_QUARTZ, Material.QUARTZ_BLOCK, bed, facing
        );
    }

    /**
     * Bedroom furniture + matching floor accents. Lantern hangs from {@code ceilingY}
     * without overwriting the roof (roof must already be solid there).
     */
    private static void furnishBedroom(
            @NotNull BaseTemplates.Builder b,
            int x1, int x2, int z1, int z2,
            int y,
            int ceilingY,
            @NotNull Material floorA,
            @NotNull Material floorB,
            @NotNull Material bed,
            @NotNull BlockFace facing
    ) {
        int bx = (x1 + x2) / 2;
        int bz = (z1 + z2) / 2;
        int footX = bx;
        int footZ = bz;
        if (facing.getModX() != 0) {
            footX = Math.min(x2 - 1, Math.max(x1, bx - (facing.getModX() > 0 ? 1 : 0)));
            footZ = Math.min(z2, Math.max(z1, bz));
        } else {
            footZ = Math.min(z2 - 1, Math.max(z1, bz - (facing.getModZ() > 0 ? 1 : 0)));
            footX = Math.min(x2, Math.max(x1, bx));
        }
        int headX = footX + facing.getModX();
        int headZ = footZ + facing.getModZ();
        if (headX < x1 || headX > x2 || headZ < z1 || headZ > z2) {
            footX = x1;
            footZ = z1;
            if (facing == BlockFace.EAST) {
                footX = x1;
            } else if (facing == BlockFace.WEST) {
                footX = x2 - 1;
            } else if (facing == BlockFace.SOUTH) {
                footZ = z1;
            } else {
                footZ = z2 - 1;
            }
        }
        // Matching room floor (never punches roof; never leaves carpet-only)
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                b.set(x, y - 1, z, ((x + z) & 1) == 0 ? floorA : floorB);
            }
        }
        b.bed(footX, y, footZ, bed, facing);
        if (facing == BlockFace.EAST) {
            b.facing(x2, y, z1, Material.CHEST, BlockFace.WEST);
            b.set(x2, y, z2, Material.BARREL);
        } else if (facing == BlockFace.WEST) {
            b.facing(x1, y, z1, Material.CHEST, BlockFace.EAST);
            b.set(x1, y, z2, Material.BARREL);
        } else {
            b.facing(x1, y, z1, Material.CHEST, facing.getOppositeFace());
            b.set(x2, y, z1, Material.BARREL);
        }
        // Soft rug beside bed — never on the doorway approach tile
        int rugX = Math.min(x2, Math.max(x1, footX - facing.getModZ()));
        int rugZ = Math.min(z2, Math.max(z1, footZ - facing.getModX()));
        boolean rugOnBed = (rugX == footX && rugZ == footZ)
                || (rugX == footX + facing.getModX() && rugZ == footZ + facing.getModZ());
        if (!rugOnBed) {
            b.set(rugX, y, rugZ, Material.WHITE_CARPET);
        }
        // Lantern in the back corner of the room (away from door), flush under ceiling
        int doorX = facing == BlockFace.EAST ? x2 : facing == BlockFace.WEST ? x1 : (x1 + x2) / 2;
        int doorZ = facing == BlockFace.SOUTH ? z2 : facing == BlockFace.NORTH ? z1 : (z1 + z2) / 2;
        int lx = facing.getModX() != 0 ? (facing.getModX() > 0 ? x1 : x2) : (x1 + x2) / 2;
        int lz = facing.getModZ() != 0 ? (facing.getModZ() > 0 ? z1 : z2) : (z1 + z2) / 2;
        // Prefer corner opposite the door
        if (facing == BlockFace.EAST || facing == BlockFace.WEST) {
            lx = facing == BlockFace.EAST ? x1 : x2;
            lz = z1;
            if (lz == doorZ) {
                lz = z2;
            }
        } else {
            lz = facing == BlockFace.SOUTH ? z1 : z2;
            lx = x1;
            if (lx == doorX) {
                lx = x2;
            }
        }
        safeCeilingLantern(b, lx, lz, ceilingY);
    }

    /**
     * Continuous gabled roof: solid ceiling over the whole footprint + stepped pitch + eaves.
     * Ridge runs along X; slopes toward ±Z. Fixes sky-holes over rooms.
     */
    static void gabledRoof(
            @NotNull BaseTemplates.Builder b,
            int minX, int maxX, int minZ, int maxZ,
            int baseY,
            @NotNull Material stairs,
            @NotNull Material planks,
            @NotNull Material slab,
            @NotNull Material gableFill
    ) {
        int oMinX = minX - 1;
        int oMaxX = maxX + 1;
        int oMinZ = minZ - 1;
        int oMaxZ = maxZ + 1;

        // Full ceiling — rooms are never open to the sky
        fillRect(b, oMinX, oMaxX, oMinZ, oMaxZ, baseY, planks);

        int depth = oMaxZ - oMinZ;
        int rises = Math.min(4, Math.max(2, depth / 4));

        for (int tier = 1; tier <= rises; tier++) {
            int inset = tier;
            int y = baseY + tier;
            int z0 = oMinZ + inset;
            int z1 = oMaxZ - inset;
            if (z0 > z1) {
                break;
            }
            for (int x = oMinX; x <= oMaxX; x++) {
                b.stairs(x, y, z0, stairs, BlockFace.SOUTH);
                b.stairs(x, y, z1, stairs, BlockFace.NORTH);
                for (int z = z0 + 1; z <= z1 - 1; z++) {
                    b.set(x, y, z, planks);
                }
            }
            if (tier == rises || z0 + 1 >= z1) {
                for (int x = oMinX; x <= oMaxX; x++) {
                    for (int z = z0; z <= z1; z++) {
                        b.slab(x, y + 1, z, slab, Slab.Type.BOTTOM);
                    }
                }
            }
        }

        // Triangular gable ends on ±X
        for (int tier = 0; tier <= rises; tier++) {
            int inset = tier;
            int y = baseY + tier;
            int z0 = oMinZ + inset;
            int z1 = oMaxZ - inset;
            if (z0 > z1) {
                break;
            }
            for (int z = z0; z <= z1; z++) {
                b.set(minX, y, z, gableFill);
                b.set(maxX, y, z, gableFill);
            }
        }
    }

    private static void framedWindow(
            @NotNull BaseTemplates.Builder b,
            int x, int y, int z,
            @NotNull BlockFace outward
    ) {
        b.set(x, y, z, Material.GLASS_PANE);
        b.set(x, y - 1, z, Material.OAK_TRAPDOOR);
        // Lintel / trim
        if (outward == BlockFace.WEST || outward == BlockFace.EAST) {
            b.set(x, y + 1, z, Material.STRIPPED_OAK_LOG);
        } else {
            b.set(x, y + 1, z, Material.STRIPPED_OAK_LOG);
        }
    }

    private static void doorway(
            @NotNull BaseTemplates.Builder b,
            int x, int y, int z,
            @NotNull BlockFace facing,
            @NotNull Material door
    ) {
        b.set(x, y, z, Material.AIR);
        b.set(x, y + 1, z, Material.AIR);
        b.door(x, y, z, door, facing);
        // Clear 2-high approach on both sides so lanterns/furniture never seal the door
        int ox = facing.getModX();
        int oz = facing.getModZ();
        b.set(x + ox, y, z + oz, Material.AIR);
        b.set(x + ox, y + 1, z + oz, Material.AIR);
        b.set(x - ox, y, z - oz, Material.AIR);
        b.set(x - ox, y + 1, z - oz, Material.AIR);
    }

    private static void shell(
            @NotNull BaseTemplates.Builder b,
            int minX, int maxX, int minZ, int maxZ,
            int y1, int y2,
            @NotNull Material wall,
            @NotNull Material pillar
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                if (!edge) {
                    continue;
                }
                boolean corner = (x == minX || x == maxX) && (z == minZ || z == maxZ);
                Material mat = corner ? pillar : wall;
                for (int y = y1; y <= y2; y++) {
                    b.set(x, y, z, mat);
                }
            }
        }
    }

    private static void fillRect(
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

    private static void clearVolume(
            @NotNull BaseTemplates.Builder b,
            int minX, int maxX, int minZ, int maxZ,
            int y1, int y2
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = y1; y <= y2; y++) {
                    b.set(x, y, z, Material.AIR);
                }
            }
        }
    }

    private static void ring(
            @NotNull BaseTemplates.Builder b,
            int minX, int maxX, int minZ, int maxZ,
            int y,
            @NotNull Material material
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (x == minX || x == maxX || z == minZ || z == maxZ) {
                    if (material.name().endsWith("_STAIRS")) {
                        BlockFace face = BlockFace.SOUTH;
                        if (z == maxZ) {
                            face = BlockFace.NORTH;
                        } else if (z == minZ) {
                            face = BlockFace.SOUTH;
                        } else if (x == maxX) {
                            face = BlockFace.WEST;
                        } else if (x == minX) {
                            face = BlockFace.EAST;
                        }
                        b.stairs(x, y, z, material, face);
                    } else if (material.name().endsWith("_SLAB")) {
                        b.slab(x, y, z, material, Slab.Type.BOTTOM);
                    } else {
                        b.set(x, y, z, material);
                    }
                }
            }
        }
    }

    private static void wallLineX(
            @NotNull BaseTemplates.Builder b,
            int x, int z1, int z2, int y1, int y2,
            @NotNull Material material
    ) {
        for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
            for (int y = y1; y <= y2; y++) {
                b.set(x, y, z, material);
            }
        }
    }

    private static void wallLineZ(
            @NotNull BaseTemplates.Builder b,
            int x1, int x2, int z, int y1, int y2,
            @NotNull Material material
    ) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = y1; y <= y2; y++) {
                b.set(x, y, z, material);
            }
        }
    }
}
