package com.rihanx.base;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Slab;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * XP farm blueprints — restored to the GitHub layout with calculated constants.
 * <p>
 * <b>Old design (origin/main):</b> 2×2 hole is the same XZ as the kill landing;
 * mobs fall straight down the shaft onto the pad (no long side tunnel).
 * Water cross on the dark deck flushes into that center hole.
 * <p>
 * <b>Math:</b>
 * <ul>
 *   <li>Fall = {@code NATURAL_DECK_Y - KILL_SLAB_Y} = 22 → ~1 HP left for punch XP</li>
 *   <li>Spawn pads require 3D distance ≥ 24 from {@link #AFK_WINDOW_Z} (Java hostile rule)</li>
 *   <li>Deck is a square of half {@link #DECK_HALF} centered on the hole (= kill)</li>
 *   <li>Kill box: hopper y=0 → bottom slab y=1 → solid ceiling y=2 (legs-only)</li>
 * </ul>
 */
public final class XpFarmTemplates {

    /** Natural dark-room spawn floor Y. Fall height to kill slab = 22. */
    public static final int NATURAL_DECK_Y = 23;
    /** First solid roof layer over the natural pads (no skylight hole). */
    public static final int NATURAL_ROOF_Y = 26;
    /** AFK punch stance (local) — used with the 24-block spawn rule. */
    public static final int AFK_WINDOW_Z = 3;

    /**
     * Kill pad = drop landing (GitHub: hole XZ == kill XZ).
     * Player punches through bars at z=2; AFK stand at {@link #AFK_WINDOW_Z}.
     */
    public static final int KILL_MIN_X = -1;
    public static final int KILL_MAX_X = 0;
    public static final int KILL_MIN_Z = 0;
    public static final int KILL_MAX_Z = 1;
    public static final int KILL_HOPPER_Y = 0;
    public static final int KILL_SLAB_Y = 1;
    public static final int KILL_CEILING_Y = 2;

    /**
     * 2×2 center hole — identical to kill pad (straight drop, old design).
     * Derived: holeMin = killMin, holeMax = killMax.
     */
    public static final int HOLE_MIN_X = KILL_MIN_X;
    public static final int HOLE_MAX_X = KILL_MAX_X;
    public static final int HOLE_MIN_Z = KILL_MIN_Z;
    public static final int HOLE_MAX_Z = KILL_MAX_Z;

    /**
     * Spawn square half-size → width = 2*{@link #DECK_HALF}+1 = 15 (GitHub used ~16; we use odd so
     * the 2×2 hole sits on the true center cell pair).
     */
    public static final int DECK_HALF = 7;
    /** Center of the spawn square = center of the hole/kill. */
    public static final int DECK_CENTER_X = (HOLE_MIN_X + HOLE_MAX_X) / 2; // 0
    public static final int DECK_CENTER_Z = (HOLE_MIN_Z + HOLE_MAX_Z) / 2; // 0

    /** Outer spawn square bounds (inclusive). */
    public static final int PAD_MIN_X = DECK_CENTER_X - DECK_HALF;
    public static final int PAD_MAX_X = DECK_CENTER_X + DECK_HALF;
    public static final int PAD_MIN_Z = DECK_CENTER_Z - DECK_HALF;
    public static final int PAD_MAX_Z = DECK_CENTER_Z + DECK_HALF;

    /** Fall distance in blocks (deck floor → kill slab). */
    public static final int FALL_HEIGHT = NATURAL_DECK_Y - KILL_SLAB_Y;

    /** Java water flows 8 blocks — place sources every this many blocks along trenches. */
    public static final int WATER_SOURCE_SPACING = 7;

    /** Minimum 3D spawn distance from AFK (hostile despawn/spawn rule). */
    public static final double MIN_SPAWN_RANGE = 24.0;

    /** Compact spawner-room floor (within 16 of the AFK window). */
    public static final int SPAWNER_DECK_Y = 5;
    /** 9×9 spawner square; hole = kill (same straight-drop math as natural). */
    public static final int SPAWNER_SQUARE_HALF = 4;
    public static final int SPAWNER_CENTER_X = DECK_CENTER_X;
    public static final int SPAWNER_CENTER_Z = DECK_CENTER_Z;
    public static final int SPAWNER_PAD_MIN_X = SPAWNER_CENTER_X - SPAWNER_SQUARE_HALF;
    public static final int SPAWNER_PAD_MAX_X = SPAWNER_CENTER_X + SPAWNER_SQUARE_HALF;
    public static final int SPAWNER_PAD_MIN_Z = SPAWNER_CENTER_Z - SPAWNER_SQUARE_HALF;
    public static final int SPAWNER_PAD_MAX_Z = SPAWNER_CENTER_Z + SPAWNER_SQUARE_HALF;
    public static final int SPAWNER_HOLE_MIN_X = HOLE_MIN_X;
    public static final int SPAWNER_HOLE_MAX_X = HOLE_MAX_X;
    public static final int SPAWNER_HOLE_MIN_Z = HOLE_MIN_Z;
    public static final int SPAWNER_HOLE_MAX_Z = HOLE_MAX_Z;

    private static final Material SHELL = Material.POLISHED_BLACKSTONE_BRICKS;
    private static final Material FLOOR = Material.BLACKSTONE;
    private static final Material ROOF = Material.POLISHED_BLACKSTONE;
    private static final Material FILL = Material.DEEPSLATE;

    private XpFarmTemplates() {
    }

    /**
     * Monster-spawner entity name for {@code /farm xp-zombie} (etc.), or {@code null}.
     * Stored as a string so unit tests never touch the live Paper entity registry.
     */
    public static @Nullable String spawnerEntityName(@NotNull String farmId) {
        return switch (farmId) {
            case "xp-zombie" -> "ZOMBIE";
            case "xp-skeleton" -> "SKELETON";
            case "xp-spider" -> "CAVE_SPIDER";
            default -> null;
        };
    }

    /** True for the 2×2 kill-box / hole floor. */
    public static boolean isKillPadCell(int x, int z) {
        return x >= KILL_MIN_X && x <= KILL_MAX_X && z >= KILL_MIN_Z && z <= KILL_MAX_Z;
    }

    /**
     * True when a dry floor cell is far enough from AFK for hostile spawns.
     * South of the hole sits near the window — those cells use non-spawn slabs.
     */
    public static boolean isInSpawnRange(int x, int deckY, int z) {
        return afkDistance(x, deckY, z) >= MIN_SPAWN_RANGE;
    }

    public static boolean isDarkShell(@NotNull Material material) {
        String name = material.name();
        return name.contains("BLACKSTONE") || name.contains("DEEPSLATE") || material == Material.OBSIDIAN;
    }

    /** True for the 2×2 drop hole centered on {@link #DECK_CENTER_X}/{@link #DECK_CENTER_Z}. */
    public static boolean isDeckHole(int x, int z) {
        return x >= HOLE_MIN_X && x <= HOLE_MAX_X && z >= HOLE_MIN_Z && z <= HOLE_MAX_Z;
    }

    /** True for cells inside the outer spawn square (inclusive). */
    public static boolean isInsideSpawnSquare(int x, int z) {
        return x >= PAD_MIN_X && x <= PAD_MAX_X && z >= PAD_MIN_Z && z <= PAD_MAX_Z;
    }

    /**
     * Water cross (+) on the square floor — every cell in the N-S or E-W arms except the center hole.
     * Water sources on the outer edges flow inward toward the 2×2 center drop.
     */
    public static boolean isWaterTrenchCell(int x, int z) {
        if (!isInsideSpawnSquare(x, z) || isDeckHole(x, z)) {
            return false;
        }
        boolean inNorthSouthArm = x >= HOLE_MIN_X && x <= HOLE_MAX_X;
        boolean inEastWestArm = z >= HOLE_MIN_Z && z <= HOLE_MAX_Z;
        return inNorthSouthArm || inEastWestArm;
    }

    /** Dry spawn ring cell inside the square (not hole, not water trench, ≥24 from AFK). */
    public static boolean isSpawnPadCell(int x, int z) {
        if (!isInsideSpawnSquare(x, z)) {
            return false;
        }
        if (isDeckHole(x, z) || isWaterTrenchCell(x, z)) {
            return false;
        }
        return isInSpawnRange(x, NATURAL_DECK_Y, z);
    }

    /** 3D distance from AFK feet at {@link #AFK_WINDOW_Z} to a deck cell. */
    public static double afkDistance(int x, int deckY, int z) {
        return Math.sqrt(
                (double) x * x + Math.pow(deckY - 1, 2) + Math.pow(z - AFK_WINDOW_Z, 2)
        );
    }

    /**
     * Sealed blackstone dark room. Pads sit north of the drop so 3D distance from the
     * punch window is at least 24 blocks; roof is double-layer with no skylight hole.
     */
    public static @NotNull BaseTemplates.BaseBlueprint naturalHostile() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        killChamberAndHouse(b);
        darkSpawnChamber(b, 2);
        buildSquareDeck(b, NATURAL_DECK_Y, DECK_CENTER_X, DECK_CENTER_Z, DECK_HALF,
                HOLE_MIN_X, HOLE_MAX_X, HOLE_MIN_Z, HOLE_MAX_Z, true);
        dropShaftOntoKill(b, NATURAL_DECK_Y);
        holeSigns(b, NATURAL_DECK_Y, HOLE_MIN_X, HOLE_MAX_X, HOLE_MIN_Z, HOLE_MAX_Z);
        finishEntrance(b);
        return b.build(
                "xp",
                "XP dark room - center hole over kill (GitHub), 22-block fall, 24+ pads, punch XP",
                0, 0, 13
        );
    }

    /**
     * 3-high sealed pads for endermen. No water (they teleport when wet).
     */
    public static @NotNull BaseTemplates.BaseBlueprint enderman() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        killChamberAndHouse(b);
        darkSpawnChamber(b, 3);
        buildSquareDeck(b, NATURAL_DECK_Y, DECK_CENTER_X, DECK_CENTER_Z, DECK_HALF,
                HOLE_MIN_X, HOLE_MAX_X, HOLE_MIN_Z, HOLE_MAX_Z, false);
        // Walk-off: open the south rim of the square so endermen fall into the hole/shaft
        for (int x = PAD_MIN_X + 1; x <= PAD_MAX_X - 1; x++) {
            if (x >= HOLE_MIN_X && x <= HOLE_MAX_X) {
                continue;
            }
            b.set(x, NATURAL_DECK_Y, PAD_MAX_Z, Material.AIR);
            b.set(x, NATURAL_DECK_Y + 1, PAD_MAX_Z, Material.AIR);
            b.set(x, NATURAL_DECK_Y + 2, PAD_MAX_Z, Material.AIR);
            b.set(x, NATURAL_DECK_Y + 3, PAD_MAX_Z, Material.AIR);
        }
        dropShaftOntoKill(b, NATURAL_DECK_Y);
        holeSigns(b, NATURAL_DECK_Y, HOLE_MIN_X, HOLE_MAX_X, HOLE_MIN_Z, HOLE_MAX_Z);
        finishEntrance(b);
        return b.build(
                "xp-enderman",
                "Enderman XP - 3-high sealed pads, walk-off into center hole over kill",
                0, 0, 13
        );
    }

    public static @NotNull BaseTemplates.BaseBlueprint zombieSpawner() {
        return spawnerFarm(
                "xp-zombie",
                "Zombie spawner XP - 4 spawners; stay at the window (16 block range)"
        );
    }

    public static @NotNull BaseTemplates.BaseBlueprint skeletonSpawner() {
        return spawnerFarm(
                "xp-skeleton",
                "Skeleton spawner XP - 4 spawners; stay at the window (16 block range)"
        );
    }

    public static @NotNull BaseTemplates.BaseBlueprint spiderSpawner() {
        return spawnerFarm(
                "xp-spider",
                "Cave-spider spawner XP - 4 spawners; stay at the window (milk for poison)"
        );
    }

    private static @NotNull BaseTemplates.BaseBlueprint spawnerFarm(
            @NotNull String id,
            @NotNull String description
    ) {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        killChamberAndHouse(b);
        spawnerRoom(b);
        dropShaftOntoKill(b, SPAWNER_DECK_Y);
        finishEntrance(b);
        return b.build(id, description, 0, 0, 13);
    }

    /**
     * Ground AFK booth + legs-only kill box (GitHub punch window south of landing).
     * Kill chamber sealed except south punch bars (feet only).
     */
    private static void killChamberAndHouse(@NotNull BaseTemplates.Builder b) {
        // Magma catch north of the drop (old GitHub layout)
        for (int x = -3; x <= 2; x++) {
            for (int z = -3; z <= -1; z++) {
                b.set(x, -1, z, Material.STONE_BRICKS);
                b.set(x, 0, z, Material.MAGMA_BLOCK);
                b.set(x, 1, z, Material.AIR);
                b.set(x, 2, z, Material.AIR);
                b.set(x, 3, z, Material.STONE_BRICKS);
            }
        }

        legsOnlyKillBox(b);
        sealKillChamberShell(b);

        // Enclosed safety booth (AFK + XP + loot) — south of kill only
        for (int x = -4; x <= 3; x++) {
            for (int z = 2; z <= 11; z++) {
                b.set(x, -1, z, Material.STONE_BRICKS);
                boolean wall = x == -4 || x == 3 || z == 2 || z == 11;
                if (wall) {
                    for (int y = 0; y <= 4; y++) {
                        b.set(x, y, z, Material.STONE_BRICKS);
                    }
                } else {
                    b.set(x, 0, z, Material.STONE_BRICKS);
                    b.set(x, 1, z, Material.AIR);
                    b.set(x, 2, z, Material.AIR);
                    b.set(x, 3, z, Material.AIR);
                    b.set(x, 4, z, Material.STONE_BRICKS);
                }
            }
        }

        // Booth north wall — side glass for view; center = feet-only punch
        for (int x = -4; x <= 3; x++) {
            b.set(x, 0, 2, Material.STONE_BRICKS);
            if (x >= KILL_MIN_X && x <= KILL_MAX_X) {
                b.set(x, 1, 2, Material.IRON_BARS);
                b.set(x, 2, 2, Material.STONE_BRICKS);
            } else {
                b.set(x, 1, 2, Material.GLASS_PANE);
                b.set(x, 2, 2, Material.GLASS_PANE);
            }
            b.set(x, 3, 2, Material.STONE_BRICKS);
        }

        b.set(-1, 1, 3, Material.AIR);
        b.set(0, 1, 3, Material.AIR);
        b.set(-1, 2, 3, Material.STONE_BRICKS);
        b.set(0, 2, 3, Material.STONE_BRICKS);

        b.set(0, 1, 11, Material.AIR);
        b.set(0, 2, 11, Material.AIR);
        b.door(0, 1, 11, Material.IRON_DOOR, BlockFace.SOUTH);
        b.facing(-1, 2, 12, Material.STONE_BUTTON, BlockFace.SOUTH);
        b.facing(-1, 2, 10, Material.STONE_BUTTON, BlockFace.NORTH);

        b.set(-3, 1, 9, Material.CRAFTING_TABLE);
        b.set(-3, 1, 10, Material.ANVIL);
        b.set(2, 1, 9, Material.BARREL);
        b.hangingLantern(0, 3, 9, Material.LANTERN, 4);

        sealPunchWindow(b);
    }

    /**
     * Kill chamber walls: solid west/east; ceiling solid (mobs drop in from ABOVE through slabs? 
     * No — shaft opens above ceiling then... wait GitHub had trapdoors open, shaft air down to y=2.
     * Legs-only: ceiling at y=2 must have a hole for fall OR mobs fall onto slabs through open ceiling.
     *
     * GitHub: shaft air from y=2 to deck onto trapdoors at y=1. So y=2 was AIR over kill.
     * Legs-only redesign used solid ceiling with side tunnel entry — that was the NEW design.
     *
     * Restoring old: drop onto slabs from above → ceiling over kill must be OPEN (air) for the fall,
     * with solid walls so they can't walk out. Punch at feet through bars.
     *
     * Actually legs-only with solid ceiling + side entry was to stop walk-out. Direct drop needs
     * opening in ceiling: air at y=2 over kill for shaft, OR slabs only and air above.
     * Pattern: hopper y=0, slab y=1, AIR y=2+ for shaft (no solid ceiling over pad).
     * Walls at x=-2,1 and z seal with bars south. Mobs can't walk out through bars at feet.
     */
    private static void sealKillChamberShell(@NotNull BaseTemplates.Builder b) {
        for (int z = KILL_MIN_Z; z <= KILL_MAX_Z; z++) {
            for (int y = KILL_SLAB_Y; y <= KILL_CEILING_Y; y++) {
                b.set(-2, y, z, Material.STONE_BRICKS);
                b.set(1, y, z, Material.STONE_BRICKS);
            }
        }
        // North face solid (no tunnel mouth — drop is vertical)
        for (int x = -2; x <= 1; x++) {
            for (int y = KILL_SLAB_Y; y <= KILL_CEILING_Y; y++) {
                if (x >= KILL_MIN_X && x <= KILL_MAX_X && y == KILL_CEILING_Y) {
                    continue; // shaft opens here
                }
                b.set(x, y, -1, Material.STONE_BRICKS);
            }
        }
    }

    /** Feet-only punch gap: bars at y=1, solid at y=2 — mobs cannot walk through. */
    static void sealPunchWindow(@NotNull BaseTemplates.Builder b) {
        b.set(-1, 0, 2, Material.STONE_BRICKS);
        b.set(0, 0, 2, Material.STONE_BRICKS);
        b.set(1, 0, 2, Material.STONE_BRICKS);
        b.set(-1, 1, 2, Material.IRON_BARS);
        b.set(0, 1, 2, Material.IRON_BARS);
        b.set(-1, 2, 2, Material.STONE_BRICKS);
        b.set(0, 2, 2, Material.STONE_BRICKS);
        b.set(-1, 2, 3, Material.STONE_BRICKS);
        b.set(0, 2, 3, Material.STONE_BRICKS);
    }

    /**
     * 2×2 legs-only landing under the center hole (GitHub geometry).
     * hopper → bottom slab; y=2+ is AIR so the shaft can drop onto the slabs.
     * Side walls + eye-sealed punch window keep mobs from walking out.
     */
    public static void legsOnlyKillBox(@NotNull BaseTemplates.Builder b) {
        for (int z = KILL_MIN_Z; z <= KILL_MAX_Z; z++) {
            for (int x = KILL_MIN_X; x <= KILL_MAX_X; x++) {
                b.set(x, -1, z, Material.STONE_BRICKS);
                BlockFace hopFace = x < 0 ? BlockFace.EAST : BlockFace.SOUTH;
                b.facing(x, KILL_HOPPER_Y, z, Material.HOPPER, hopFace);
                b.slab(x, KILL_SLAB_Y, z, Material.STONE_BRICK_SLAB, Slab.Type.BOTTOM);
                // Open top — direct drop from center hole (old design). Not a solid lid.
                b.set(x, KILL_CEILING_Y, z, Material.AIR);
                b.set(x, KILL_CEILING_Y + 1, z, Material.AIR);
            }
        }
    }

    /**
     * GitHub path: vertical shaft under the center hole (= kill XZ) only.
     * Air from above the slabs up to the deck. No sideways tunnel.
     */
    private static void dropShaftOntoKill(@NotNull BaseTemplates.Builder b, int deck) {
        int wallMinX = HOLE_MIN_X - 1;
        int wallMaxX = HOLE_MAX_X + 1;
        int wallMinZ = HOLE_MIN_Z - 1;
        int wallMaxZ = HOLE_MAX_Z + 1;

        for (int y = KILL_CEILING_Y; y < deck; y++) {
            for (int x = wallMinX; x <= wallMaxX; x++) {
                for (int z = wallMinZ; z <= wallMaxZ; z++) {
                    boolean wall = x == wallMinX || x == wallMaxX || z == wallMinZ || z == wallMaxZ;
                    boolean shaft = x >= HOLE_MIN_X && x <= HOLE_MAX_X
                            && z >= HOLE_MIN_Z && z <= HOLE_MAX_Z;
                    if (wall) {
                        b.set(x, y, z, Material.STONE_BRICKS);
                    } else if (shaft) {
                        b.set(x, y, z, Material.AIR);
                    }
                }
            }
        }
        // Hole open at deck floor + interior only — roof above stays solid (no skylight)
        for (int x = HOLE_MIN_X; x <= HOLE_MAX_X; x++) {
            for (int z = HOLE_MIN_Z; z <= HOLE_MAX_Z; z++) {
                b.set(x, deck, z, Material.AIR);
                int clearTo = Math.min(deck + 2, (deck == NATURAL_DECK_Y ? NATURAL_ROOF_Y : deck + 4) - 1);
                for (int y = deck + 1; y <= clearTo; y++) {
                    b.set(x, y, z, Material.AIR);
                }
            }
        }
        if (deck == NATURAL_DECK_Y) {
            for (int x = HOLE_MIN_X; x <= HOLE_MAX_X; x++) {
                for (int z = HOLE_MIN_Z; z <= HOLE_MAX_Z; z++) {
                    b.set(x, NATURAL_ROOF_Y, z, ROOF);
                    b.set(x, NATURAL_ROOF_Y + 1, z, ROOF);
                }
            }
        }
        // Re-assert kill pad + window after shaft carve
        legsOnlyKillBox(b);
        sealKillChamberShell(b);
        sealPunchWindow(b);
        // Ladder access (old GitHub)
        for (int y = 1; y <= deck; y++) {
            b.facing(-3, y, 0, Material.LADDER, BlockFace.WEST);
        }
    }

    /**
     * Sealed dark shell around the spawn square (walls + double roof, no skylight).
     */
    private static void darkSpawnChamber(@NotNull BaseTemplates.Builder b, int interiorHeight) {
        int deck = NATURAL_DECK_Y;
        int roof = deck + interiorHeight + 1;
        int minX = PAD_MIN_X - 2;
        int maxX = PAD_MAX_X + 2;
        int minZ = PAD_MIN_Z - 2;
        int maxZ = PAD_MAX_Z + 2;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                b.set(x, deck - 1, z, FILL);
                if (!isInsideSpawnSquare(x, z)) {
                    b.set(x, deck, z, FLOOR);
                }
                for (int y = 1; y <= interiorHeight; y++) {
                    b.set(x, deck + y, z, Material.AIR);
                }
                b.set(x, roof, z, ROOF);
                b.set(x, roof + 1, z, ROOF);
                b.slab(x, roof + 2, z, Material.BLACKSTONE_SLAB, Slab.Type.BOTTOM);
            }
        }

        for (int y = deck + 1; y <= deck + interiorHeight; y++) {
            for (int x = minX; x <= maxX; x++) {
                b.set(x, y, minZ, SHELL);
                b.set(x, y, minZ + 1, SHELL);
                b.set(x, y, maxZ, SHELL);
                b.set(x, y, maxZ - 1, SHELL);
            }
            for (int z = minZ; z <= maxZ; z++) {
                b.set(minX, y, z, SHELL);
                b.set(minX + 1, y, z, SHELL);
                b.set(maxX, y, z, SHELL);
                b.set(maxX - 1, y, z, SHELL);
            }
        }
        // Keep roof solid over the hole (no skylight)
        for (int x = HOLE_MIN_X - 1; x <= HOLE_MAX_X + 1; x++) {
            for (int z = HOLE_MIN_Z - 1; z <= HOLE_MAX_Z + 1; z++) {
                b.set(x, roof, z, ROOF);
                b.set(x, roof + 1, z, ROOF);
            }
        }
    }

    /**
     * 15×15 (or any odd) spawn square from calculated center:
     * dry pads in corners, + shaped water trenches, 2×2 air hole at exact center.
     */
    private static void buildSquareDeck(
            @NotNull BaseTemplates.Builder b,
            int deck,
            int centerX,
            int centerZ,
            int half,
            int holeMinX,
            int holeMaxX,
            int holeMinZ,
            int holeMaxZ,
            boolean withWater
    ) {
        int padMinX = centerX - half;
        int padMaxX = centerX + half;
        int padMinZ = centerZ - half;
        int padMaxZ = centerZ + half;

        for (int x = padMinX; x <= padMaxX; x++) {
            for (int z = padMinZ; z <= padMaxZ; z++) {
                boolean hole = x >= holeMinX && x <= holeMaxX && z >= holeMinZ && z <= holeMaxZ;
                boolean trench = !hole && ((x >= holeMinX && x <= holeMaxX)
                        || (z >= holeMinZ && z <= holeMaxZ));
                if (hole) {
                    b.set(x, deck, z, Material.AIR);
                    // Interior clearance only — never punch NATURAL_ROOF_Y+ (no skylight)
                    int clearTo = Math.min(deck + 2, NATURAL_ROOF_Y - 1);
                    for (int y = deck + 1; y <= clearTo; y++) {
                        b.set(x, y, z, Material.AIR);
                    }
                } else if (trench) {
                    b.set(x, deck, z, Material.AIR);
                } else if (deck == NATURAL_DECK_Y && !isInSpawnRange(x, deck, z)) {
                    // Too close to AFK — bottom slabs (no hostile spawn) keep the floor filled
                    b.slab(x, deck, z, Material.BLACKSTONE_SLAB, Slab.Type.BOTTOM);
                } else {
                    b.set(x, deck, z, FLOOR);
                }
            }
        }

        if (withWater) {
            placeCenterFlowWater(b, deck, padMinX, padMaxX, padMinZ, padMaxZ,
                    holeMinX, holeMaxX, holeMinZ, holeMaxZ);
        }
    }

    /**
     * Water sources on the outer edges of the + trenches (every 7 blocks + rim).
     * Flow pushes mobs toward the center 2×2 hole — hole cells stay AIR (never water).
     */
    private static void placeCenterFlowWater(
            @NotNull BaseTemplates.Builder b,
            int deck,
            int padMinX,
            int padMaxX,
            int padMinZ,
            int padMaxZ,
            int holeMinX,
            int holeMaxX,
            int holeMinZ,
            int holeMaxZ
    ) {
        // N-S arms: sources from north & south edges flowing toward center z
        for (int x = holeMinX; x <= holeMaxX; x++) {
            for (int z = padMinZ; z <= padMaxZ; z++) {
                if (z >= holeMinZ && z <= holeMaxZ) {
                    continue; // center hole row — stay dry
                }
                boolean northRim = z == padMinZ;
                boolean southRim = z == padMaxZ;
                boolean spaced = (z - padMinZ) % WATER_SOURCE_SPACING == 0
                        || (padMaxZ - z) % WATER_SOURCE_SPACING == 0;
                if (northRim || southRim || spaced) {
                    b.set(x, deck, z, Material.WATER);
                }
            }
        }
        // E-W arms: sources from west & east edges flowing toward center x
        for (int z = holeMinZ; z <= holeMaxZ; z++) {
            for (int x = padMinX; x <= padMaxX; x++) {
                if (x >= holeMinX && x <= holeMaxX) {
                    continue; // already handled by N-S arm (or hole)
                }
                boolean westRim = x == padMinX;
                boolean eastRim = x == padMaxX;
                boolean spaced = (x - padMinX) % WATER_SOURCE_SPACING == 0
                        || (padMaxX - x) % WATER_SOURCE_SPACING == 0;
                if (westRim || eastRim || spaced) {
                    b.set(x, deck, z, Material.WATER);
                }
            }
        }
        // Four outer corners of the square — guarantees water reaches every dry pad corner
        for (int[] corner : new int[][]{
                {padMinX, padMinZ}, {padMaxX, padMinZ}, {padMinX, padMaxZ}, {padMaxX, padMaxZ}
        }) {
            int cx = corner[0];
            int cz = corner[1];
            boolean trench = (cx >= holeMinX && cx <= holeMaxX) || (cz >= holeMinZ && cz <= holeMaxZ);
            boolean hole = cx >= holeMinX && cx <= holeMaxX && cz >= holeMinZ && cz <= holeMaxZ;
            if (trench && !hole) {
                b.set(cx, deck, cz, Material.WATER);
            }
        }
        // Ring of sources touching the center hole — guaranteed flow into the drop
        for (int x = holeMinX; x <= holeMaxX; x++) {
            if (holeMinZ - 1 >= padMinZ) {
                b.set(x, deck, holeMinZ - 1, Material.WATER);
            }
            if (holeMaxZ + 1 <= padMaxZ) {
                b.set(x, deck, holeMaxZ + 1, Material.WATER);
            }
        }
        for (int z = holeMinZ; z <= holeMaxZ; z++) {
            if (holeMinX - 1 >= padMinX) {
                b.set(holeMinX - 1, deck, z, Material.WATER);
            }
            if (holeMaxX + 1 <= padMaxX) {
                b.set(holeMaxX + 1, deck, z, Material.WATER);
            }
        }
    }

    private static void holeSigns(
            @NotNull BaseTemplates.Builder b,
            int deck,
            int holeMinX,
            int holeMaxX,
            int holeMinZ,
            int holeMaxZ
    ) {
        for (int x = holeMinX; x <= holeMaxX; x++) {
            for (int z = holeMinZ; z <= holeMaxZ; z++) {
                b.facing(x, deck - 1, z, Material.OAK_WALL_SIGN, x == holeMinX ? BlockFace.EAST : BlockFace.WEST);
                b.facing(x, deck - 2, z, Material.OAK_WALL_SIGN, z == holeMinZ ? BlockFace.SOUTH : BlockFace.NORTH);
            }
        }
    }

    private static void spawnerRoom(@NotNull BaseTemplates.Builder b) {
        int deck = SPAWNER_DECK_Y;
        int minX = SPAWNER_PAD_MIN_X - 1;
        int maxX = SPAWNER_PAD_MAX_X + 1;
        int minZ = SPAWNER_PAD_MIN_Z - 1;
        int maxZ = SPAWNER_PAD_MAX_Z + 1;

        // Room shell (9×9 spawn square + 1-block wall ring)
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                b.set(x, deck - 1, z, FILL);
                boolean wall = x == minX || x == maxX || z == minZ || z == maxZ;
                if (wall) {
                    b.set(x, deck, z, FLOOR);
                    b.set(x, deck + 1, z, SHELL);
                    b.set(x, deck + 2, z, SHELL);
                    b.set(x, deck + 3, z, ROOF);
                    b.set(x, deck + 4, z, ROOF);
                } else {
                    b.set(x, deck + 1, z, Material.AIR);
                    b.set(x, deck + 2, z, Material.AIR);
                }
            }
        }

        // 9×9 square deck: dry corners + center water cross + 2×2 hole (calculated from center)
        buildSquareDeck(b, deck, SPAWNER_CENTER_X, SPAWNER_CENTER_Z, SPAWNER_SQUARE_HALF,
                SPAWNER_HOLE_MIN_X, SPAWNER_HOLE_MAX_X, SPAWNER_HOLE_MIN_Z, SPAWNER_HOLE_MAX_Z, true);

        // Four spawners on dry corner pads (within 16 of window; hole = kill center)
        b.set(-3, deck + 1, -3, Material.SPAWNER);
        b.set(2, deck + 1, -3, Material.SPAWNER);
        b.set(-3, deck + 1, 3, Material.SPAWNER);
        b.set(2, deck + 1, 3, Material.SPAWNER);

        holeSigns(b, deck, SPAWNER_HOLE_MIN_X, SPAWNER_HOLE_MAX_X,
                SPAWNER_HOLE_MIN_Z, SPAWNER_HOLE_MAX_Z);
    }

    private static void finishEntrance(@NotNull BaseTemplates.Builder b) {
        legsOnlyKillBox(b);
        sealPunchWindow(b);
        FarmTemplates.connectKillPadLoot(
                b,
                KILL_HOPPER_Y,
                0,
                8,
                KILL_MIN_X,
                KILL_MAX_X,
                KILL_MIN_Z,
                KILL_MAX_Z
        );
        spawnPad(b, 0, 12);
        spawnPad(b, 0, 13);
        b.set(-1, 0, 13, Material.CRAFTING_TABLE);
        b.set(1, 0, 13, Material.BARREL);
    }

    private static void spawnPad(@NotNull BaseTemplates.Builder b, int x, int z) {
        b.set(x, -1, z, Material.DIRT_PATH);
        b.set(x, 0, z, Material.AIR);
        b.set(x, 1, z, Material.AIR);
    }
}
