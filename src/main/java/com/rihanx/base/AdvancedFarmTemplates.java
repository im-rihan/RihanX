package com.rihanx.base;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Advanced production farms — restored to the original RihanX design:
 * chicken / cow / pig lava cookers, 3-smoker battery (raw top / coal back),
 * slime + witch spawners, master toolsmith diamond hall.
 * <p>
 * Layout sizes are derived from the constants below (not magic numbers mid-method).
 * Loot uses calculated hopper-on-chest plumbing ({@link FarmTemplates#LOOT_CHEST_Y}).
 */
public final class AdvancedFarmTemplates {

    // ── Shared loot math (matches FarmTemplates) ───────────────────────────
    /** Loot chests sit on the floor; drain hoppers are always chestY+1 facing DOWN. */
    public static final int LOOT_CHEST_Y = FarmTemplates.LOOT_CHEST_Y;
    public static final int LOOT_DRAIN_Y = FarmTemplates.LOOT_DRAIN_Y;

    // ── Chicken coop (calculated) ──────────────────────────────────────────
    /** Coop interior X: exclusive walls at ± half → width = 2*HALF_X + 1 cells of pads. */
    public static final int CHICKEN_HALF_X = 2;          // pads x=-2..1, walls x=-3 and x=2
    public static final int CHICKEN_WALL_MIN_X = -CHICKEN_HALF_X - 1; // -3
    public static final int CHICKEN_WALL_MAX_X = CHICKEN_HALF_X;      // 2
    public static final int CHICKEN_MIN_Z = -5;
    public static final int CHICKEN_MAX_Z = 0;           // south wall / cooker interface
    public static final int CHICKEN_Y = 1;
    public static final int CHICKEN_LOOT_CHEST_Z = 6;
    /** Hens stand on trapdoors (interior, not canal center). */
    public static final int[][] CHICKEN_STANDS = {
            {-2, -4}, {1, -4}, {-2, -3}, {1, -3}, {-2, -2}, {1, -2}
    };
    public static final int CHICKEN_EGG_DISPENSER_X = 3;
    public static final int CHICKEN_EGG_DISPENSER_Y = 1;
    public static final int CHICKEN_EGG_DISPENSER_Z = -2;
    /** 2×2 lava cooker origin (x0,z0). */
    public static final int CHICKEN_LAVA_X0 = -1;
    public static final int CHICKEN_LAVA_Z0 = 1;

    // ── Cow / pig pen (calculated) ─────────────────────────────────────────
    public static final int LIVESTOCK_Y = 1;
    public static final int LIVESTOCK_WALL_MIN_X = -3;
    public static final int LIVESTOCK_WALL_MAX_X = 2;
    public static final int LIVESTOCK_MIN_Z = -6;
    public static final int LIVESTOCK_MAX_Z = -1;
    public static final int LIVESTOCK_LOOT_CHEST_Z = 6;
    public static final int LIVESTOCK_LAVA_X0 = -1;
    public static final int LIVESTOCK_LAVA_Z0 = 1;
    public static final int[][] COW_STANDS = {{-2, -4}, {0, -4}, {1, -4}, {-1, -3}};
    public static final int[][] PIG_STANDS = {{-2, -4}, {0, -4}, {1, -4}, {-1, -3}};
    public static final int[][] COW_DISPENSERS = {{-2, 1, -6}, {0, 1, -6}, {2, 1, -6}};
    public static final int[][] PIG_DISPENSERS = {{-2, 1, -6}, {0, 1, -6}, {2, 1, -6}};
    public static final int COW_FEED_X = 3;
    public static final int COW_FEED_Y = 1;
    public static final int COW_FEED_Z = -6;
    public static final int PIG_FEED_X = 3;
    public static final int PIG_FEED_Y = 1;
    public static final int PIG_FEED_Z = -6;

    // ── Cook smoker battery (original: raw top, coal back) ──────────────────
    /** Smoker row X = -SMOKER_HALF .. +SMOKER_HALF → 3 smokers. */
    public static final int COOK_SMOKER_HALF = 1;
    public static final int COOK_SMOKER_Y = 1;
    public static final int COOK_RAW_HOPPER_Y = COOK_SMOKER_Y + 1; // 2
    public static final int COOK_RAW_CHEST_Y = COOK_SMOKER_Y + 2;  // 3 — top chests
    public static final int COOK_FUEL_HOPPER_Z = -1;               // into smoker fuel face
    public static final int COOK_FUEL_CHEST_Y = COOK_SMOKER_Y + 1; // 2 — on fuel hoppers
    public static final int COOK_FUEL_CHEST_Z = COOK_FUEL_HOPPER_Z;
    public static final int COOK_RAW_CHEST_X = -COOK_SMOKER_HALF;  // leftmost top chest
    public static final int COOK_RAW_CHEST_Z = 0;
    public static final int COOK_LOOT_CHEST_Z = 5;
    public static final int COOK_PAD_MIN_X = -2;
    public static final int COOK_PAD_MAX_X = 2;
    public static final int COOK_PAD_MIN_Z = -3;
    public static final int COOK_PAD_MAX_Z = 2;

    // ── Slime / witch spawner room (calculated) ────────────────────────────
    public static final int SPAWNER_DECK_Y = 5;
    public static final int SPAWNER_MIN_X = -5;
    public static final int SPAWNER_MAX_X = 4;
    public static final int SPAWNER_MIN_Z = -6;
    public static final int SPAWNER_MAX_Z = 2;
    public static final int SPAWNER_LOOT_CHEST_Z = 6;
    /** Kill pad = XpFarmTemplates legs-only box (same math as XP farms). */
    public static final int SPAWNER_KILL_MIN_X = XpFarmTemplates.KILL_MIN_X;
    public static final int SPAWNER_KILL_MAX_X = XpFarmTemplates.KILL_MAX_X;
    public static final int SPAWNER_KILL_MIN_Z = XpFarmTemplates.KILL_MIN_Z;
    public static final int SPAWNER_KILL_MAX_Z = XpFarmTemplates.KILL_MAX_Z;

    // ── Diamond hall (calculated) ──────────────────────────────────────────
    public static final int DIAMOND_Y = 1;
    public static final int[] DIAMOND_SMITH_X = {-5, -2, 1, 4}; // spacing = 3
    public static final int DIAMOND_SMITH_STAND_Z = 2;
    public static final int DIAMOND_BED_Z = -1; // foot; head lands at z=0 (south) — job stays at z=1
    public static final int DIAMOND_JOB_Z = 1;
    public static final int[][] DIAMOND_FARMERS = {{-7, 2}, {6, 2}};
    public static final int DIAMOND_EMERALD_X = 0;
    public static final int DIAMOND_EMERALD_Y = 1;
    public static final int DIAMOND_EMERALD_Z = 6;
    public static final int DIAMOND_LOOT_CHEST_Y = LOOT_CHEST_Y;
    public static final int DIAMOND_LOOT_CHEST_Z = 5;
    public static final int DIAMOND_LOOT_DRAIN_Y = DIAMOND_LOOT_CHEST_Y + 1;
    public static final int DIAMOND_COLLECT_Y = -1;
    public static final int DIAMOND_COLLECT_Z = 2;
    public static final int DIAMOND_HALL_MIN_X = -8;
    public static final int DIAMOND_HALL_MAX_X = 7;
    public static final int DIAMOND_HALL_MIN_Z = -2;
    public static final int DIAMOND_HALL_MAX_Z = 7;
    public static final int DIAMOND_DOOR_Z = 7;
    public static final int DIAMOND_INNER_DOOR_Z = DIAMOND_DOOR_Z;
    public static final int DIAMOND_VESTIBULE_Z = 8;
    public static final int DIAMOND_OUTER_DOOR_Z = 9;
    public static final int DIAMOND_SECRET_DOOR_X = DIAMOND_HALL_MIN_X;
    public static final int DIAMOND_SECRET_DOOR_Z = 4;

    private static final Material SHELL = Material.POLISHED_BLACKSTONE_BRICKS;
    private static final Material FLOOR = Material.BLACKSTONE;

    private AdvancedFarmTemplates() {
    }

    public static @Nullable String spawnerEntityName(@NotNull String farmId) {
        return switch (farmId) {
            case "slime" -> "SLIME";
            case "redstone" -> "WITCH";
            default -> null;
        };
    }

    /**
     * Hens on trapdoors → eggs into hoppers; water/push into lava cooker → cooked loot.
     */
    public static @NotNull BaseTemplates.BaseBlueprint chicken() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        for (int x = CHICKEN_WALL_MIN_X; x <= CHICKEN_WALL_MAX_X; x++) {
            for (int z = CHICKEN_MIN_Z; z <= CHICKEN_MAX_Z; z++) {
                b.set(x, -1, z, Material.STONE_BRICKS);
                boolean wall = x == CHICKEN_WALL_MIN_X || x == CHICKEN_WALL_MAX_X
                        || z == CHICKEN_MIN_Z || z == CHICKEN_MAX_Z;
                if (wall) {
                    b.set(x, 0, z, Material.STONE_BRICKS);
                    b.set(x, 1, z, Material.STONE_BRICKS);
                    b.set(x, 2, z, Material.STONE_BRICKS);
                } else {
                    // Merge math: sides → center column (x=0) → SOUTH into lava cooker hoppers
                    BlockFace hop = x < 0 ? BlockFace.EAST : (x > 0 ? BlockFace.WEST : BlockFace.SOUTH);
                    b.facing(x, 0, z, Material.HOPPER, hop);
                    b.facing(x, 1, z, Material.IRON_TRAPDOOR, BlockFace.SOUTH);
                    b.set(x, 2, z, Material.STONE_BRICK_SLAB);
                }
            }
        }
        // Water canal sources on north wall (y=0, dz≤-1) — does not break hopper graph
        b.set(-1, 0, CHICKEN_MIN_Z, Material.WATER);
        b.set(0, 0, CHICKEN_MIN_Z, Material.WATER);
        b.set(-1, 1, CHICKEN_MIN_Z, Material.AIR);
        b.set(0, 1, CHICKEN_MIN_Z, Material.AIR);
        // South wall punch: center columns feed lava cooker (CHICKEN_LAVA_*)
        b.facing(-1, 0, CHICKEN_MAX_Z, Material.HOPPER, BlockFace.EAST);
        b.facing(0, 0, CHICKEN_MAX_Z, Material.HOPPER, BlockFace.SOUTH);
        b.set(-1, 1, CHICKEN_MAX_Z, Material.AIR);
        b.set(0, 1, CHICKEN_MAX_Z, Material.AIR);

        // Egg overflow → dispenser (dawn pulse)
        b.facing(CHICKEN_EGG_DISPENSER_X, 0, CHICKEN_EGG_DISPENSER_Z, Material.HOPPER, BlockFace.UP);
        b.facing(CHICKEN_EGG_DISPENSER_X, CHICKEN_EGG_DISPENSER_Y, CHICKEN_EGG_DISPENSER_Z,
                Material.DISPENSER, BlockFace.WEST);
        b.set(CHICKEN_EGG_DISPENSER_X + 1, CHICKEN_EGG_DISPENSER_Y, CHICKEN_EGG_DISPENSER_Z,
                Material.DAYLIGHT_DETECTOR);

        lavaCookHole(b, CHICKEN_LAVA_X0, CHICKEN_LAVA_Z0);
        // Loot LAST so booth/floor never overwrite chests
        FarmTemplates.connectKillPadLoot(
                b, 0, LOOT_CHEST_Y, CHICKEN_LOOT_CHEST_Z,
                CHICKEN_LAVA_X0, CHICKEN_LAVA_X0 + 1,
                CHICKEN_LAVA_Z0, CHICKEN_LAVA_Z0 + 1
        );
        FarmTemplates.spawnPad(b, 0, 8);
        FarmTemplates.spawnPad(b, 0, 9);
        return b.build(
                "chicken",
                "Auto chicken cooker - hens on trapdoors, eggs→hoppers, lava→loot chests",
                0, 0, 9
        );
    }

    public static @NotNull BaseTemplates.BaseBlueprint cow() {
        return livestock(
                "cow",
                Material.WHEAT,
                Material.HAY_BLOCK,
                "Auto cow cooker - wheat dispensers breed at dawn, water→lava→cooked beef"
        );
    }

    public static @NotNull BaseTemplates.BaseBlueprint pig() {
        return livestock(
                "pig",
                Material.CARROTS,
                Material.PUMPKIN,
                "Auto pig cooker - carrot dispensers breed at dawn, water→lava→cooked pork"
        );
    }

    private static @NotNull BaseTemplates.BaseBlueprint livestock(
            @NotNull String id,
            @NotNull Material feed,
            @NotNull Material decor,
            @NotNull String description
    ) {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        for (int x = LIVESTOCK_WALL_MIN_X; x <= LIVESTOCK_WALL_MAX_X; x++) {
            for (int z = LIVESTOCK_MIN_Z; z <= LIVESTOCK_MAX_Z; z++) {
                b.set(x, -1, z, Material.DIRT);
                b.set(x, 0, z, Material.GRASS_BLOCK);
                boolean wall = x == LIVESTOCK_WALL_MIN_X || x == LIVESTOCK_WALL_MAX_X
                        || z == LIVESTOCK_MIN_Z || z == LIVESTOCK_MAX_Z;
                if (wall) {
                    b.set(x, 1, z, Material.OAK_FENCE);
                    b.set(x, 2, z, Material.OAK_FENCE);
                } else {
                    b.set(x, 1, z, Material.AIR);
                    b.set(x, 2, z, Material.AIR);
                }
            }
        }
        // Water flush toward south cooker opening
        b.set(-1, 0, LIVESTOCK_MIN_Z, Material.WATER);
        b.set(0, 0, LIVESTOCK_MIN_Z, Material.WATER);
        b.set(-1, 0, LIVESTOCK_MAX_Z, Material.AIR);
        b.set(0, 0, LIVESTOCK_MAX_Z, Material.AIR);
        b.set(-1, 1, LIVESTOCK_MAX_Z, Material.AIR);
        b.set(0, 1, LIVESTOCK_MAX_Z, Material.AIR);
        b.set(-1, 2, LIVESTOCK_MAX_Z, Material.AIR);
        b.set(0, 2, LIVESTOCK_MAX_Z, Material.AIR);

        for (int[] d : new int[][]{{-2, LIVESTOCK_MIN_Z}, {0, LIVESTOCK_MIN_Z}, {2, LIVESTOCK_MIN_Z}}) {
            b.set(d[0], 0, d[1], Material.STONE_BRICKS);
            b.facing(d[0], 1, d[1], Material.DISPENSER, BlockFace.SOUTH);
            b.facing(d[0], 2, d[1], Material.CHEST, BlockFace.SOUTH); // feed chest on dispenser
            b.facing(d[0], 3, d[1], Material.HOPPER, BlockFace.DOWN);
        }
        b.set(0, 3, -4, Material.DAYLIGHT_DETECTOR);
        b.facing(0, 2, -4, Material.REPEATER, BlockFace.NORTH);
        b.set(3, 1, -4, decor);
        b.set(3, 1, -3, feed);

        // Tiny refill plot (3×3 hydrated)
        for (int x = 3; x <= 5; x++) {
            for (int z = -2; z <= 0; z++) {
                b.set(x, -1, z, Material.DIRT);
                if (x == 4 && z == -1) {
                    b.set(x, 0, z, Material.WATER);
                } else {
                    b.set(x, 0, z, Material.FARMLAND);
                    b.set(x, 1, z, id.equals("pig") ? Material.CARROTS : Material.WHEAT);
                }
            }
        }

        lavaCookHole(b, LIVESTOCK_LAVA_X0, LIVESTOCK_LAVA_Z0);
        FarmTemplates.connectKillPadLoot(
                b, 0, LOOT_CHEST_Y, LIVESTOCK_LOOT_CHEST_Z,
                LIVESTOCK_LAVA_X0, LIVESTOCK_LAVA_X0 + 1,
                LIVESTOCK_LAVA_Z0, LIVESTOCK_LAVA_Z0 + 1
        );
        FarmTemplates.spawnPad(b, 0, 8);
        FarmTemplates.spawnPad(b, 0, 9);
        return b.build(id, description, 0, 0, 9);
    }

    /**
     * Original cook design: 3 smokers, raw in top chests, coal in back chests, cooked→loot.
     * <pre>
     *   y=3  [raw chest] [raw chest] [raw chest]
     *   y=2  [hopper↓]   [hopper↓]   [hopper↓]     [coal chest on fuel hopper]
     *   y=1  [smoker]    [smoker]    [smoker]  ←── [hopper facing south]
     *   y=0  [hopper] merge → south loot bay (chestY=0, drainY=1)
     * </pre>
     */
    public static @NotNull BaseTemplates.BaseBlueprint cook() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        for (int x = COOK_PAD_MIN_X; x <= COOK_PAD_MAX_X; x++) {
            for (int z = COOK_PAD_MIN_Z; z <= COOK_PAD_MAX_Z; z++) {
                b.set(x, -1, z, Material.STONE_BRICKS);
                b.set(x, 0, z, Material.SMOOTH_STONE);
            }
        }
        for (int x = -COOK_SMOKER_HALF; x <= COOK_SMOKER_HALF; x++) {
            b.facing(x, COOK_SMOKER_Y, 0, Material.SMOKER, BlockFace.SOUTH);
            // Raw: top chest → hopper DOWN into ingredient slot
            b.facing(x, COOK_RAW_CHEST_Y, 0, Material.CHEST, BlockFace.SOUTH);
            b.facing(x, COOK_RAW_HOPPER_Y, 0, Material.HOPPER, BlockFace.DOWN);
            // Coal: back chest on hopper → SOUTH into fuel slot
            b.facing(x, COOK_FUEL_CHEST_Y, COOK_FUEL_CHEST_Z, Material.CHEST, BlockFace.NORTH);
            b.facing(x, COOK_SMOKER_Y, COOK_FUEL_HOPPER_Z, Material.HOPPER, BlockFace.SOUTH);
            // Cooked out under smoker
            b.facing(x, 0, 0, Material.HOPPER, x < 0 ? BlockFace.EAST
                    : (x > 0 ? BlockFace.WEST : BlockFace.SOUTH));
            b.facing(x, -1, 0, Material.HOPPER, BlockFace.SOUTH);
        }
        FarmTemplates.hopperRowIntoChest(
                b, -1, 0, -COOK_SMOKER_HALF, COOK_SMOKER_HALF, COOK_LOOT_CHEST_Z, LOOT_CHEST_Y
        );
        // Re-assert kitchen after loot pipe
        for (int x = -COOK_SMOKER_HALF; x <= COOK_SMOKER_HALF; x++) {
            b.facing(x, COOK_SMOKER_Y, 0, Material.SMOKER, BlockFace.SOUTH);
            b.facing(x, COOK_RAW_CHEST_Y, 0, Material.CHEST, BlockFace.SOUTH);
            b.facing(x, COOK_RAW_HOPPER_Y, 0, Material.HOPPER, BlockFace.DOWN);
            b.facing(x, COOK_FUEL_CHEST_Y, COOK_FUEL_CHEST_Z, Material.CHEST, BlockFace.NORTH);
            b.facing(x, COOK_SMOKER_Y, COOK_FUEL_HOPPER_Z, Material.HOPPER, BlockFace.SOUTH);
            b.facing(x, 0, 0, Material.HOPPER, BlockFace.DOWN);
            b.facing(x, -1, 0, Material.HOPPER, BlockFace.SOUTH);
        }
        FarmTemplates.hopperRowIntoChest(
                b, -1, 0, -COOK_SMOKER_HALF, COOK_SMOKER_HALF, COOK_LOOT_CHEST_Z, LOOT_CHEST_Y
        );

        b.set(0, 4, 0, Material.OAK_PLANKS);
        b.hangingLantern(0, 3, 2, Material.LANTERN, 3);
        FarmTemplates.spawnPad(b, 0, 7);
        FarmTemplates.spawnPad(b, 0, 8);
        return b.build(
                "cook",
                "3-smoker battery - raw in top chests, coal in back chests, cooked→loot",
                0, 0, 8
        );
    }

    public static @NotNull BaseTemplates.BaseBlueprint slime() {
        return spawnerBox(
                "slime",
                "Slime farm - 4 slime spawners; legs-only kill pad → slimeballs"
        );
    }

    public static @NotNull BaseTemplates.BaseBlueprint redstone() {
        return spawnerBox(
                "redstone",
                "Witch farm - 4 witch spawners; legs-only kill pad → redstone/glowstone/sugar/sticks"
        );
    }

    private static @NotNull BaseTemplates.BaseBlueprint spawnerBox(
            @NotNull String id,
            @NotNull String description
    ) {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int deck = SPAWNER_DECK_Y;
        for (int x = SPAWNER_MIN_X; x <= SPAWNER_MAX_X; x++) {
            for (int z = SPAWNER_MIN_Z; z <= SPAWNER_MAX_Z; z++) {
                b.set(x, deck - 1, z, Material.DEEPSLATE);
                b.set(x, deck, z, FLOOR);
                b.set(x, deck + 1, z, Material.AIR);
                b.set(x, deck + 2, z, Material.AIR);
                b.set(x, deck + 3, z, Material.AIR);
                b.set(x, deck + 4, z, SHELL);
                b.set(x, deck + 5, z, SHELL);
            }
        }
        for (int y = deck + 1; y <= deck + 3; y++) {
            for (int x = SPAWNER_MIN_X; x <= SPAWNER_MAX_X; x++) {
                b.set(x, y, SPAWNER_MIN_Z, SHELL);
                b.set(x, y, SPAWNER_MAX_Z, SHELL);
            }
            for (int z = SPAWNER_MIN_Z; z <= SPAWNER_MAX_Z; z++) {
                b.set(SPAWNER_MIN_X, y, z, SHELL);
                b.set(SPAWNER_MAX_X, y, z, SHELL);
            }
        }
        // Water stream on deck → drop into kill columns
        for (int z = SPAWNER_MIN_Z + 1; z <= -1; z++) {
            b.set(-1, deck, z, Material.AIR);
            b.set(0, deck, z, Material.AIR);
        }
        b.set(-1, deck, SPAWNER_MIN_Z + 1, Material.WATER);
        b.set(0, deck, SPAWNER_MIN_Z + 1, Material.WATER);
        for (int x = SPAWNER_KILL_MIN_X; x <= SPAWNER_KILL_MAX_X; x++) {
            for (int z = SPAWNER_KILL_MIN_Z; z <= SPAWNER_KILL_MAX_Z; z++) {
                b.set(x, deck, z, Material.AIR);
                b.set(x, deck + 1, z, Material.AIR);
                b.set(x, deck + 2, z, Material.AIR);
            }
        }
        // Drop shaft: air from kill slabs up through deck (open-top legs pad)
        for (int y = XpFarmTemplates.KILL_CEILING_Y; y <= deck + 2; y++) {
            for (int x = -2; x <= 1; x++) {
                for (int z = -1; z <= 2; z++) {
                    boolean rim = x == -2 || x == 1 || z == -1 || z == 2;
                    boolean shaft = (x == -1 || x == 0) && (z == 0 || z == 1);
                    if (rim) {
                        b.set(x, y, z, SHELL);
                    } else if (shaft) {
                        b.set(x, y, z, Material.AIR);
                    }
                }
            }
        }
        // Same calculated kill pad as XP (hopper → slab → sealed ceiling)
        XpFarmTemplates.legsOnlyKillBox(b);

        // AFK booth south of kill
        for (int x = -4; x <= 3; x++) {
            for (int z = 3; z <= 8; z++) {
                b.set(x, -1, z, Material.STONE_BRICKS);
                boolean wall = x == -4 || x == 3 || z == 3 || z == 8;
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
        for (int x = -1; x <= 0; x++) {
            b.set(x, 1, 3, Material.IRON_BARS);
            b.set(x, 2, 3, Material.STONE_BRICKS);
        }
        // Re-assert kill pad after booth walls (z=3 touches kill rim)
        XpFarmTemplates.legsOnlyKillBox(b);

        // 4 spawners on deck corners (within 16 of window)
        b.set(-3, deck + 1, -4, Material.SPAWNER);
        b.set(2, deck + 1, -4, Material.SPAWNER);
        b.set(-3, deck + 1, -1, Material.SPAWNER);
        b.set(2, deck + 1, -1, Material.SPAWNER);

        // Loot LAST — pad = XP kill math (KILL_MIN/MAX)
        FarmTemplates.connectKillPadLoot(
                b, XpFarmTemplates.KILL_HOPPER_Y, LOOT_CHEST_Y, SPAWNER_LOOT_CHEST_Z,
                XpFarmTemplates.KILL_MIN_X, XpFarmTemplates.KILL_MAX_X,
                XpFarmTemplates.KILL_MIN_Z, XpFarmTemplates.KILL_MAX_Z
        );
        XpFarmTemplates.legsOnlyKillBox(b);
        for (int x = -1; x <= 0; x++) {
            b.set(x, 1, 3, Material.IRON_BARS);
            b.set(x, 2, 3, Material.STONE_BRICKS);
        }
        FarmTemplates.spawnPad(b, 0, 9);
        FarmTemplates.spawnPad(b, 0, 10);
        return b.build(id, description, 0, 0, 10);
    }

    /**
     * Master toolsmith trading hall + two farmer emerald engines.
     * Vanilla has no AFK diamond-ore generator — this is emeralds in → diamond gear out.
     */
    public static @NotNull BaseTemplates.BaseBlueprint diamond() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        for (int x = DIAMOND_HALL_MIN_X; x <= DIAMOND_HALL_MAX_X; x++) {
            for (int z = DIAMOND_HALL_MIN_Z; z <= DIAMOND_HALL_MAX_Z; z++) {
                b.set(x, -1, z, Material.DEEPSLATE);
                b.set(x, 0, z, Material.POLISHED_DEEPSLATE);
                boolean wall = x == DIAMOND_HALL_MIN_X || x == DIAMOND_HALL_MAX_X
                        || z == DIAMOND_HALL_MIN_Z || z == DIAMOND_HALL_MAX_Z;
                if (wall) {
                    for (int y = 1; y <= 3; y++) {
                        b.set(x, y, z, Material.DEEPSLATE_BRICKS);
                    }
                    b.set(x, 4, z, Material.DEEPSLATE_TILE_SLAB);
                } else {
                    b.set(x, 1, z, Material.AIR);
                    b.set(x, 2, z, Material.AIR);
                    b.set(x, 3, z, Material.AIR);
                    b.set(x, 4, z, Material.DEEPSLATE_TILES);
                }
            }
        }
        for (int x : DIAMOND_SMITH_X) {
            b.bed(x, DIAMOND_Y, DIAMOND_BED_Z, Material.RED_BED, BlockFace.SOUTH);
            b.set(x, DIAMOND_Y, DIAMOND_JOB_Z, Material.SMITHING_TABLE);
            b.set(x, DIAMOND_Y, DIAMOND_SMITH_STAND_Z, Material.AIR);
            b.set(x, 2, DIAMOND_SMITH_STAND_Z, Material.AIR);
            b.set(x, DIAMOND_Y, 3, Material.IRON_BARS);
            b.set(x, 2, 3, Material.IRON_BARS);
        }
        for (int[] farmer : DIAMOND_FARMERS) {
            int fx = farmer[0];
            int fz = farmer[1];
            b.bed(fx, DIAMOND_Y, fz - 2, Material.LIME_BED, BlockFace.SOUTH);
            b.set(fx, 0, fz, Material.COMPOSTER);
            b.facing(fx, DIAMOND_COLLECT_Y, fz, Material.HOPPER,
                    fx < 0 ? BlockFace.EAST : BlockFace.WEST);
            int cropX = fx < 0 ? fx - 1 : fx + 1;
            for (int dz = 0; dz <= 2; dz++) {
                b.set(cropX, -1, fz - 1 + dz, Material.DIRT);
                b.set(cropX, 0, fz - 1 + dz, Material.FARMLAND);
                b.set(cropX, 1, fz - 1 + dz, Material.WHEAT);
            }
            b.set(cropX, 0, fz, Material.WATER);
            b.set(cropX, 1, fz, Material.AIR);
        }

        // Farmer collect → calculated loot bay (LAST plumbing)
        FarmTemplates.hopperRowIntoChest(
                b, DIAMOND_COLLECT_Y, DIAMOND_COLLECT_Z,
                DIAMOND_FARMERS[0][0], DIAMOND_FARMERS[1][0],
                DIAMOND_LOOT_CHEST_Z, DIAMOND_LOOT_CHEST_Y
        );

        // Emerald / wheat stock OFF the hopper feed row
        b.facing(DIAMOND_EMERALD_X, DIAMOND_EMERALD_Y, DIAMOND_EMERALD_Z, Material.CHEST, BlockFace.SOUTH);
        b.facing(DIAMOND_EMERALD_X + 1, DIAMOND_EMERALD_Y, DIAMOND_EMERALD_Z, Material.CHEST, BlockFace.SOUTH);
        b.facing(DIAMOND_EMERALD_X - 1, DIAMOND_EMERALD_Y, DIAMOND_EMERALD_Z, Material.BARREL, BlockFace.SOUTH);

        b.set(0, 4, 5, Material.DEEPSLATE_BRICKS);
        b.hangingLantern(0, 3, 5, Material.LANTERN, 4);

        // Dual airlock + secret west door
        b.set(0, 1, DIAMOND_INNER_DOOR_Z, Material.AIR);
        b.set(0, 2, DIAMOND_INNER_DOOR_Z, Material.AIR);
        b.door(0, 1, DIAMOND_INNER_DOOR_Z, Material.IRON_DOOR, BlockFace.SOUTH);
        b.set(0, 1, DIAMOND_OUTER_DOOR_Z, Material.AIR);
        b.set(0, 2, DIAMOND_OUTER_DOOR_Z, Material.AIR);
        b.door(0, 1, DIAMOND_OUTER_DOOR_Z, Material.IRON_DOOR, BlockFace.SOUTH);
        b.facing(-1, 2, DIAMOND_INNER_DOOR_Z + 1, Material.LEVER, BlockFace.SOUTH);
        b.facing(-1, 2, DIAMOND_OUTER_DOOR_Z + 1, Material.LEVER, BlockFace.SOUTH);
        b.facing(1, 2, DIAMOND_VESTIBULE_Z, Material.LEVER, BlockFace.EAST);
        b.set(DIAMOND_SECRET_DOOR_X, 1, DIAMOND_SECRET_DOOR_Z, Material.AIR);
        b.set(DIAMOND_SECRET_DOOR_X, 2, DIAMOND_SECRET_DOOR_Z, Material.AIR);
        b.door(DIAMOND_SECRET_DOOR_X, 1, DIAMOND_SECRET_DOOR_Z, Material.IRON_DOOR, BlockFace.EAST);
        b.facing(DIAMOND_SECRET_DOOR_X + 1, 2, DIAMOND_SECRET_DOOR_Z, Material.LEVER, BlockFace.EAST);
        b.facing(DIAMOND_SECRET_DOOR_X - 1, 2, DIAMOND_SECRET_DOOR_Z, Material.LEVER, BlockFace.WEST);

        // Re-assert farmer compost hoppers + emerald stock after loot pipe
        for (int[] farmer : DIAMOND_FARMERS) {
            BlockFace intoPipe = farmer[0] < 0 ? BlockFace.EAST : BlockFace.WEST;
            b.facing(farmer[0], DIAMOND_COLLECT_Y, farmer[1], Material.HOPPER, intoPipe);
            b.set(farmer[0], 0, farmer[1], Material.COMPOSTER);
        }
        b.facing(DIAMOND_EMERALD_X, DIAMOND_EMERALD_Y, DIAMOND_EMERALD_Z, Material.CHEST, BlockFace.SOUTH);
        b.facing(DIAMOND_EMERALD_X + 1, DIAMOND_EMERALD_Y, DIAMOND_EMERALD_Z, Material.CHEST, BlockFace.SOUTH);
        b.facing(DIAMOND_EMERALD_X - 1, DIAMOND_EMERALD_Y, DIAMOND_EMERALD_Z, Material.BARREL, BlockFace.SOUTH);

        // Beds LAST — foot at BED_Z, head at BED_Z+1 (south); job stays at JOB_Z
        for (int x : DIAMOND_SMITH_X) {
            b.bed(x, DIAMOND_Y, DIAMOND_BED_Z, Material.RED_BED, BlockFace.SOUTH);
            b.set(x, DIAMOND_Y, DIAMOND_JOB_Z, Material.SMITHING_TABLE);
        }
        for (int[] farmer : DIAMOND_FARMERS) {
            b.bed(farmer[0], DIAMOND_Y, farmer[1] - 2, Material.LIME_BED, BlockFace.SOUTH);
        }

        FarmTemplates.spawnPad(b, 0, 10);
        FarmTemplates.spawnPad(b, 0, 11);
        return b.build(
                "diamond",
                "Diamond hall - 4 master toolsmiths + 2 farmers; emeralds in → diamond gear out",
                0, 0, 11
        );
    }

    /** 2×2 lava cooker over open trapdoors / hoppers. Lava is walled so it cannot spread. */
    private static void lavaCookHole(@NotNull BaseTemplates.Builder b, int x0, int z0) {
        for (int x = x0; x <= x0 + 1; x++) {
            for (int z = z0; z <= z0 + 1; z++) {
                b.set(x, -1, z, Material.STONE_BRICKS);
                b.facing(x, 0, z, Material.HOPPER, BlockFace.SOUTH);
                b.facing(x, 1, z, Material.IRON_TRAPDOOR, BlockFace.SOUTH);
                b.set(x, 2, z, Material.LAVA);
            }
        }
        for (int x = x0 - 1; x <= x0 + 2; x++) {
            for (int z = z0 - 1; z <= z0 + 2; z++) {
                boolean rim = x < x0 || x > x0 + 1 || z < z0 || z > z0 + 1;
                if (rim) {
                    b.set(x, 2, z, Material.STONE_BRICKS);
                    b.set(x, 3, z, Material.STONE_BRICK_SLAB);
                }
            }
        }
        b.facing(x0, 2, z0 - 1, Material.OAK_WALL_SIGN, BlockFace.NORTH);
        b.facing(x0 + 1, 2, z0 - 1, Material.OAK_WALL_SIGN, BlockFace.NORTH);
    }
}
