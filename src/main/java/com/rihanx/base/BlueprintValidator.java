package com.rihanx.base;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure validation of blueprints (no world paste). Used by unit tests.
 */
public final class BlueprintValidator {

    private BlueprintValidator() {
    }

    public static @NotNull Set<String> validate(@NotNull BaseTemplates.BaseBlueprint blueprint) {
        Set<String> errors = new HashSet<>();
        if (blueprint.blocks().isEmpty()) {
            errors.add("empty-blocks");
        }

        Map<Long, BaseTemplates.RelBlock> cells = new HashMap<>();
        Map<Long, Bed.Part> bedParts = new HashMap<>();
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (BaseTemplates.RelBlock block : blueprint.blocks()) {
            long key = pack(block.dx(), block.dy(), block.dz());
            cells.put(key, block);
            minY = Math.min(minY, block.dy());
            maxY = Math.max(maxY, block.dy());

            if (FarmTemplates.gadgetMaterials().contains(block.material())) {
                // tracked for farms via validateFarm
            }
            if (block.material().name().endsWith("_BED") && block.bedPart() != null) {
                bedParts.put(key, block.bedPart());
            }
        }

        // Water must have solid support beneath (or another water) in final map
        for (BaseTemplates.RelBlock block : cells.values()) {
            if (block.material() != Material.WATER) {
                continue;
            }
            BaseTemplates.RelBlock under = cells.get(pack(block.dx(), block.dy() - 1, block.dz()));
            if (under == null || under.material() == Material.AIR || under.material().name().endsWith("_CARPET")) {
                errors.add("water-no-support@" + block.dx() + "," + block.dy() + "," + block.dz());
            }
        }

        // Bed foot must have matching head in facing direction
        for (BaseTemplates.RelBlock block : cells.values()) {
            if (block.bedPart() != Bed.Part.FOOT || block.facing() == null) {
                continue;
            }
            BlockFace f = block.facing();
            long headKey = pack(block.dx() + f.getModX(), block.dy(), block.dz() + f.getModZ());
            BaseTemplates.RelBlock head = cells.get(headKey);
            if (head == null || head.bedPart() != Bed.Part.HEAD || head.material() != block.material()) {
                errors.add("bed-missing-head@" + block.dx() + "," + block.dz());
            }
        }

        // Spawn head space should not be an occluding cube (name-based — no Paper registry needed in tests)
        BaseTemplates.RelBlock headSpace = cells.get(pack(blueprint.spawnDx(), blueprint.spawnDy() + 1, blueprint.spawnDz()));
        if (headSpace != null && isLikelyFullCube(headSpace.material())) {
            errors.add("spawn-head-blocked");
        }

        if (maxY - minY < 1) {
            errors.add("too-flat-vertically");
        }

        return errors;
    }

    private static boolean isLikelyFullCube(@NotNull Material material) {
        String n = material.name();
        if (n.equals("AIR") || n.equals("WATER") || n.equals("LAVA") || n.equals("FIRE")) {
            return false;
        }
        if (n.contains("DOOR") || n.contains("GATE") || n.contains("SLAB") || n.contains("STAIRS")) {
            return false;
        }
        if (n.contains("FENCE") || n.contains("WALL") || n.contains("PANE") || n.contains("CARPET")) {
            return false;
        }
        if (n.contains("LANTERN") || n.contains("TORCH") || n.contains("SIGN") || n.contains("BUTTON")) {
            return false;
        }
        if (n.contains("PRESSURE") || n.contains("RAIL") || n.contains("SAPLING") || n.contains("FLOWER")) {
            return false;
        }
        if (n.contains("CROP") || n.contains("WHEAT") || n.contains("STEM") || n.contains("WART")) {
            return false;
        }
        if (n.contains("SUGAR_CANE") || n.contains("BAMBOO") || n.contains("CACTUS") || n.contains("KELP")) {
            return false;
        }
        return !n.contains("PATH") && !n.equals("FARMLAND");
    }

    public static @NotNull Set<String> validateFarm(@NotNull BaseTemplates.BaseBlueprint blueprint) {
        Set<String> errors = validate(blueprint);
        boolean hasGadget = false;
        boolean hasHopper = false;
        boolean hasStorage = false;
        for (BaseTemplates.RelBlock block : blueprint.blocks()) {
            if (FarmTemplates.gadgetMaterials().contains(block.material())) {
                hasGadget = true;
            }
            if (block.material() == Material.HOPPER) {
                hasHopper = true;
            }
            if (block.material() == Material.CHEST || block.material() == Material.BARREL) {
                hasStorage = true;
            }
        }
        if (!hasGadget) {
            errors.add("missing-gadgets");
        }
        if (!hasHopper && !"animal".equals(blueprint.id())) {
            // Animal pens are feed/storage only — no auto hopper collection
            errors.add("missing-hopper");
        }
        if (!hasStorage) {
            errors.add("missing-storage");
        }
        errors.addAll(validateHopperOutputsReachStorage(blueprint));
        errors.addAll(validateObserverPistonCircuits(blueprint));
        errors.addAll(validateFarmPlayability(blueprint));
        return errors;
    }

    /**
     * BuildGuides-style playability checks: storage must be visible without digging,
     * beds need floors, cane needs water adjacency, iron deck must not flood spawn pads.
     */
    public static @NotNull Set<String> validateFarmPlayability(@NotNull BaseTemplates.BaseBlueprint blueprint) {
        Set<String> errors = new HashSet<>();
        Map<Long, BaseTemplates.RelBlock> cells = new HashMap<>();
        for (BaseTemplates.RelBlock block : blueprint.blocks()) {
            cells.put(pack(block.dx(), block.dy(), block.dz()), block);
        }

        int buriedStorage = 0;
        int visibleStorage = 0;
        for (BaseTemplates.RelBlock block : cells.values()) {
            if (block.material() != Material.CHEST && block.material() != Material.BARREL) {
                continue;
            }
            if (block.dy() < 0) {
                buriedStorage++;
                errors.add("buried-storage@" + block.dx() + "," + block.dy() + "," + block.dz());
            } else {
                visibleStorage++;
            }
        }
        if (visibleStorage == 0) {
            errors.add("no-visible-storage");
        }

        // Beds need solid under foot and head
        for (BaseTemplates.RelBlock block : cells.values()) {
            if (block.bedPart() == null || !block.material().name().endsWith("_BED")) {
                continue;
            }
            BaseTemplates.RelBlock under = cells.get(pack(block.dx(), block.dy() - 1, block.dz()));
            if (under == null || under.material() == Material.AIR || under.material() == Material.WATER) {
                errors.add("bed-no-floor@" + block.dx() + "," + block.dy() + "," + block.dz());
            }
        }

        String id = blueprint.id();
        if ("cane".equals(id)) {
            for (BaseTemplates.RelBlock block : cells.values()) {
                if (block.material() != Material.SUGAR_CANE) {
                    continue;
                }
                // Soil under cane; water must share that soil Y orthogonally
                BaseTemplates.RelBlock soil = cells.get(pack(block.dx(), block.dy() - 1, block.dz()));
                if (soil == null) {
                    errors.add("cane-no-soil@" + block.dx() + "," + block.dz());
                    continue;
                }
                int sy = soil.dy();
                boolean watered = false;
                for (BlockFace f : List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
                    BaseTemplates.RelBlock n = cells.get(pack(soil.dx() + f.getModX(), sy, soil.dz() + f.getModZ()));
                    if (n != null && n.material() == Material.WATER) {
                        watered = true;
                        break;
                    }
                }
                if (!watered) {
                    errors.add("cane-no-water@" + block.dx() + "," + block.dz());
                }
            }
        }

        if ("iron".equals(id)) {
            // Water on top of solid golem deck (y=14 over stone at y=13) floods spawn space
            for (BaseTemplates.RelBlock block : cells.values()) {
                if (block.material() != Material.WATER || block.dy() != 14) {
                    continue;
                }
                BaseTemplates.RelBlock under = cells.get(pack(block.dx(), 13, block.dz()));
                if (under != null && under.material() != Material.AIR && under.material() != Material.WATER) {
                    errors.add("iron-flood-on-deck@" + block.dx() + "," + block.dz());
                }
            }
            // Dry pads on TOP golem deck (y=13)
            boolean dryPad = false;
            for (int[] p : new int[][]{{3, 3}, {-4, 3}, {3, -4}, {-4, -4}}) {
                BaseTemplates.RelBlock floor = cells.get(pack(p[0], 13, p[1]));
                BaseTemplates.RelBlock above = cells.get(pack(p[0], 14, p[1]));
                if (floor != null && floor.material() != Material.AIR && floor.material() != Material.WATER
                        && (above == null || above.material() == Material.AIR)) {
                    dryPad = true;
                    break;
                }
            }
            if (!dryPad) {
                errors.add("iron-no-dry-spawn-pad");
            }
        }

        if ("mushroom".equals(id)) {
            for (BaseTemplates.RelBlock block : cells.values()) {
                if (block.material() != Material.LANTERN && block.material() != Material.SOUL_LANTERN) {
                    continue;
                }
                // Interior volume roughly |x|<=3, |z|<=2, y<=3 — lanterns there kill growth
                if (Math.abs(block.dx()) <= 3 && block.dz() >= -3 && block.dz() <= 2 && block.dy() <= 3) {
                    errors.add("mushroom-interior-light@" + block.dx() + "," + block.dy() + "," + block.dz());
                }
            }
        }

        if ("bamboo".equals(id)) {
            for (BaseTemplates.RelBlock block : cells.values()) {
                if (block.material() != Material.BAMBOO) {
                    continue;
                }
                for (BlockFace f : List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
                    BaseTemplates.RelBlock n = cells.get(pack(block.dx() + f.getModX(), block.dy(), block.dz() + f.getModZ()));
                    if (n != null && n.material() == Material.WATER) {
                        errors.add("bamboo-water-adjacent@" + block.dx() + "," + block.dz());
                        break;
                    }
                }
            }
        }

        if ("melon".equals(id)) {
            for (BaseTemplates.RelBlock block : cells.values()) {
                if (block.material() != Material.MELON_STEM) {
                    continue;
                }
                BaseTemplates.RelBlock soil = cells.get(pack(block.dx(), block.dy() - 1, block.dz()));
                if (soil == null || soil.material() != Material.FARMLAND) {
                    errors.add("melon-stem-no-farmland@" + block.dx() + "," + block.dz());
                }
                // Water at stem Y floods/breaks stems
                for (BlockFace f : List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
                    BaseTemplates.RelBlock n = cells.get(pack(block.dx() + f.getModX(), block.dy(), block.dz() + f.getModZ()));
                    if (n != null && n.material() == Material.WATER) {
                        errors.add("melon-stem-flood@" + block.dx() + "," + block.dz());
                        break;
                    }
                }
            }
        }

        if ("nether".equals(id)) {
            for (BaseTemplates.RelBlock block : cells.values()) {
                if (block.material() != Material.NETHER_WART) {
                    continue;
                }
                BaseTemplates.RelBlock soil = cells.get(pack(block.dx(), block.dy() - 1, block.dz()));
                if (soil == null || soil.material() != Material.SOUL_SAND) {
                    errors.add("nether-wart-no-soul-sand@" + block.dx() + "," + block.dz());
                }
            }
        }

        errors.addAll(validatePreLiveRules(blueprint, cells));
        return errors;
    }

    /**
     * Extra go-live checks for recently fixed farm layouts (XP kill pad, cookers, slime seal, bamboo).
     */
    public static @NotNull Set<String> validatePreLiveRules(
            @NotNull BaseTemplates.BaseBlueprint blueprint,
            @NotNull Map<Long, BaseTemplates.RelBlock> cells
    ) {
        return switch (blueprint.id()) {
            case "xp", "xp-zombie", "xp-skeleton", "xp-spider", "xp-enderman" -> {
                Set<String> xpErrors = new HashSet<>(validateXpKillChamber(cells));
                xpErrors.addAll(validateXpSquareDeck(blueprint.id(), cells));
                xpErrors.addAll(validateXpDropChute(blueprint.id(), cells));
                yield xpErrors;
            }
            case "chicken", "cow", "pig" -> validateLivestockCooker(cells, blueprint.id());
            case "slime", "redstone" -> validateSpawnerKillBox(cells);
            case "bamboo" -> validateBambooDualHoppers(cells);
            case "cook" -> validateCookBattery(cells);
            case "diamond" -> validateDiamondHall(cells);
            default -> Set.of();
        };
    }

    /** Standalone pre-live gate — structure + hoppers + playability + farm-specific rules. */
    public static @NotNull Set<String> validatePreLive(@NotNull BaseTemplates.BaseBlueprint blueprint) {
        return validateFarm(blueprint);
    }

    private static @NotNull Set<String> validateXpKillChamber(@NotNull Map<Long, BaseTemplates.RelBlock> cells) {
        Set<String> errors = new HashSet<>();
        // Punch window: bars at feet (legs-only), solid at eye level so mobs cannot escape
        for (int x = -1; x <= 0; x++) {
            BaseTemplates.RelBlock feet = cells.get(pack(x, 1, 2));
            BaseTemplates.RelBlock eyes = cells.get(pack(x, 2, 2));
            if (feet == null || feet.material() != Material.IRON_BARS) {
                errors.add("xp-window-feet-bars@" + x);
            }
            if (eyes == null || eyes.material() != Material.STONE_BRICKS) {
                errors.add("xp-window-eye-sealed@" + x);
            }
        }
        // Side view glass
        BaseTemplates.RelBlock sideGlass = cells.get(pack(-2, 1, 2));
        if (sideGlass == null || sideGlass.material() != Material.GLASS_PANE) {
            errors.add("xp-side-glass-missing");
        }
        // Kill pad: hoppers + bottom slabs (legs-only), solid ceiling, no trapdoors or magma
        for (int x = XpFarmTemplates.KILL_MIN_X; x <= XpFarmTemplates.KILL_MAX_X; x++) {
            for (int z = XpFarmTemplates.KILL_MIN_Z; z <= XpFarmTemplates.KILL_MAX_Z; z++) {
                BaseTemplates.RelBlock hopper = cells.get(pack(x, XpFarmTemplates.KILL_HOPPER_Y, z));
                if (hopper == null || hopper.material() != Material.HOPPER) {
                    errors.add("xp-kill-hopper@" + x + "," + z);
                }
                BaseTemplates.RelBlock slab = cells.get(pack(x, XpFarmTemplates.KILL_SLAB_Y, z));
                if (slab == null || !slab.material().name().contains("SLAB")) {
                    errors.add("xp-kill-slab@" + x + "," + z);
                }
                BaseTemplates.RelBlock ceiling = cells.get(pack(x, XpFarmTemplates.KILL_CEILING_Y, z));
                // Open top for vertical drop from center hole (GitHub); sides/window seal escape
                if (ceiling == null || ceiling.material() != Material.AIR) {
                    errors.add("xp-kill-ceiling@" + x + "," + z);
                }
                BaseTemplates.RelBlock floor = cells.get(pack(x, XpFarmTemplates.KILL_HOPPER_Y, z));
                if (floor != null && floor.material() == Material.MAGMA_BLOCK) {
                    errors.add("xp-magma-on-kill-pad@" + x + "," + z);
                }
                BaseTemplates.RelBlock trap = cells.get(pack(x, XpFarmTemplates.KILL_SLAB_Y, z));
                if (trap != null && trap.material() == Material.IRON_TRAPDOOR) {
                    errors.add("xp-kill-trapdoor@" + x + "," + z);
                }
            }
        }
        // Floor chests + hoppers DOWN
        BaseTemplates.RelBlock chest = cells.get(pack(0, 0, 8));
        if (chest == null || chest.material() != Material.CHEST) {
            errors.add("xp-floor-chest-missing");
        }
        boolean hopperDown = cells.values().stream()
                .anyMatch(b -> b.material() == Material.HOPPER && b.dy() == 1 && b.dz() == 8
                        && b.facing() == BlockFace.DOWN);
        if (!hopperDown) {
            errors.add("xp-hopper-on-chest-missing");
        }
        for (int x = -1; x <= 0; x++) {
            BaseTemplates.RelBlock drain = cells.get(pack(x, 1, 8));
            if (drain == null || drain.material() != Material.HOPPER || drain.facing() != BlockFace.DOWN) {
                errors.add("xp-chest-hopper-down@" + x);
            }
        }
        // Safety door
        BaseTemplates.RelBlock door = cells.get(pack(0, 1, 11));
        if (door == null || door.material() != Material.IRON_DOOR) {
            errors.add("xp-safety-door-missing");
        }
        return errors;
    }

    /** Center hole (= kill XZ) → vertical shaft onto legs-only slabs (GitHub path). */
    private static @NotNull Set<String> validateXpDropChute(
            @NotNull String farmId,
            @NotNull Map<Long, BaseTemplates.RelBlock> cells
    ) {
        Set<String> errors = new HashSet<>();
        int holeMinX;
        int holeMaxX;
        int holeMinZ;
        int holeMaxZ;
        int deck;
        if (farmId.startsWith("xp-") && !"xp-enderman".equals(farmId)) {
            holeMinX = XpFarmTemplates.SPAWNER_HOLE_MIN_X;
            holeMaxX = XpFarmTemplates.SPAWNER_HOLE_MAX_X;
            holeMinZ = XpFarmTemplates.SPAWNER_HOLE_MIN_Z;
            holeMaxZ = XpFarmTemplates.SPAWNER_HOLE_MAX_Z;
            deck = XpFarmTemplates.SPAWNER_DECK_Y;
        } else {
            holeMinX = XpFarmTemplates.HOLE_MIN_X;
            holeMaxX = XpFarmTemplates.HOLE_MAX_X;
            holeMinZ = XpFarmTemplates.HOLE_MIN_Z;
            holeMaxZ = XpFarmTemplates.HOLE_MAX_Z;
            deck = XpFarmTemplates.NATURAL_DECK_Y;
        }
        int centerX = (holeMinX + holeMaxX) / 2;
        int centerZ = (holeMinZ + holeMaxZ) / 2;
        int shaftY = Math.max(XpFarmTemplates.KILL_CEILING_Y, deck / 2);

        // Hole must equal kill pad (straight drop)
        if (holeMinX != XpFarmTemplates.KILL_MIN_X || holeMaxX != XpFarmTemplates.KILL_MAX_X
                || holeMinZ != XpFarmTemplates.KILL_MIN_Z || holeMaxZ != XpFarmTemplates.KILL_MAX_Z) {
            errors.add("xp-hole-not-aligned-with-kill");
        }

        for (int[] probe : new int[][]{
                {centerX, deck, centerZ},
                {centerX, shaftY, centerZ},
                {centerX, XpFarmTemplates.KILL_CEILING_Y, centerZ},
                {centerX, XpFarmTemplates.KILL_SLAB_Y, centerZ}
        }) {
            BaseTemplates.RelBlock block = cells.get(pack(probe[0], probe[1], probe[2]));
            if (probe[1] == XpFarmTemplates.KILL_SLAB_Y) {
                if (block == null || !block.material().name().contains("SLAB")) {
                    errors.add("xp-drop-path-blocked@" + probe[0] + "," + probe[1] + "," + probe[2]);
                }
            } else if (block == null || block.material() != Material.AIR) {
                errors.add("xp-drop-path-blocked@" + probe[0] + "," + probe[1] + "," + probe[2]);
            }
        }
        return errors;
    }

    private static @NotNull Set<String> validateXpSquareDeck(
            @NotNull String farmId,
            @NotNull Map<Long, BaseTemplates.RelBlock> cells
    ) {
        Set<String> errors = new HashSet<>();
        if ("xp-enderman".equals(farmId)) {
            BaseTemplates.RelBlock center = cells.get(pack(
                    XpFarmTemplates.DECK_CENTER_X, XpFarmTemplates.NATURAL_DECK_Y,
                    XpFarmTemplates.DECK_CENTER_Z));
            if (center == null || center.material() != Material.AIR) {
                errors.add("xp-enderman-center-hole-missing");
            }
            return errors;
        }
        if ("xp".equals(farmId)) {
            errors.addAll(validateSquareDeckWater(cells, XpFarmTemplates.NATURAL_DECK_Y,
                    XpFarmTemplates.PAD_MIN_X, XpFarmTemplates.PAD_MIN_Z,
                    XpFarmTemplates.HOLE_MIN_X, XpFarmTemplates.HOLE_MAX_X,
                    XpFarmTemplates.HOLE_MIN_Z, XpFarmTemplates.HOLE_MAX_Z,
                    XpFarmTemplates.DECK_CENTER_X, XpFarmTemplates.DECK_CENTER_Z));
            return errors;
        }
        // Spawner variants: 9×9 square at compact deck
        errors.addAll(validateSquareDeckWater(cells, XpFarmTemplates.SPAWNER_DECK_Y,
                XpFarmTemplates.SPAWNER_PAD_MIN_X, XpFarmTemplates.SPAWNER_PAD_MIN_Z,
                XpFarmTemplates.SPAWNER_HOLE_MIN_X, XpFarmTemplates.SPAWNER_HOLE_MAX_X,
                XpFarmTemplates.SPAWNER_HOLE_MIN_Z, XpFarmTemplates.SPAWNER_HOLE_MAX_Z,
                XpFarmTemplates.SPAWNER_CENTER_X, XpFarmTemplates.SPAWNER_CENTER_Z));
        BaseTemplates.RelBlock spCenter = cells.get(pack(
                XpFarmTemplates.SPAWNER_CENTER_X, XpFarmTemplates.SPAWNER_DECK_Y,
                XpFarmTemplates.SPAWNER_CENTER_Z));
        if (spCenter == null || spCenter.material() != Material.AIR) {
            errors.add("xp-spawner-center-not-air");
        }
        return errors;
    }

    private static @NotNull Set<String> validateSquareDeckWater(
            @NotNull Map<Long, BaseTemplates.RelBlock> cells,
            int deck,
            int padMinX,
            int padMinZ,
            int holeMinX,
            int holeMaxX,
            int holeMinZ,
            int holeMaxZ,
            int centerX,
            int centerZ
    ) {
        Set<String> errors = new HashSet<>();
        BaseTemplates.RelBlock center = cells.get(pack(centerX, deck, centerZ));
        if (center != null && center.material() == Material.WATER) {
            errors.add("xp-center-hole-has-water");
        }
        if (center != null && center.material() != Material.AIR) {
            errors.add("xp-center-hole-not-air");
        }
        boolean westArm = false;
        boolean northArm = false;
        for (BaseTemplates.RelBlock block : cells.values()) {
            if (block.dy() != deck || block.material() != Material.WATER) {
                continue;
            }
            if (block.dx() == padMinX && block.dz() >= holeMinZ && block.dz() <= holeMaxZ) {
                westArm = true;
            }
            if (block.dz() == padMinZ && block.dx() >= holeMinX && block.dx() <= holeMaxX) {
                northArm = true;
            }
        }
        if (!westArm) {
            errors.add("xp-water-west-arm-missing");
        }
        if (!northArm) {
            errors.add("xp-water-north-arm-missing");
        }
        return errors;
    }

    private static @NotNull Set<String> validateLivestockCooker(
            @NotNull Map<Long, BaseTemplates.RelBlock> cells,
            @NotNull String id
    ) {
        Set<String> errors = new HashSet<>();
        boolean hasLavaCook = false;
        boolean hasWaterCanal = false;
        for (BaseTemplates.RelBlock block : cells.values()) {
            if (block.material() == Material.LAVA && block.dz() >= 0 && block.dz() <= 1) {
                hasLavaCook = true;
            }
            if (block.material() == Material.WATER && block.dy() == 0 && block.dz() <= -1) {
                hasWaterCanal = true;
            }
        }
        if (!hasLavaCook) {
            errors.add(id + "-no-lava-cooker");
        }
        if (!hasWaterCanal) {
            errors.add(id + "-no-water-canal");
        }
        return errors;
    }

    private static @NotNull Set<String> validateSpawnerKillBox(@NotNull Map<Long, BaseTemplates.RelBlock> cells) {
        Set<String> errors = new HashSet<>();
        // Same legs-only kill pad as XP farms
        for (int x = XpFarmTemplates.KILL_MIN_X; x <= XpFarmTemplates.KILL_MAX_X; x++) {
            for (int z = XpFarmTemplates.KILL_MIN_Z; z <= XpFarmTemplates.KILL_MAX_Z; z++) {
                BaseTemplates.RelBlock slab = cells.get(pack(x, XpFarmTemplates.KILL_SLAB_Y, z));
                if (slab == null || !slab.material().name().contains("SLAB")) {
                    errors.add("slime-kill-slab@" + x + "," + z);
                }
                BaseTemplates.RelBlock ceiling = cells.get(pack(x, XpFarmTemplates.KILL_CEILING_Y, z));
                if (ceiling == null || ceiling.material() != Material.AIR) {
                    errors.add("slime-kill-ceiling@" + x + "," + z);
                }
            }
        }
        // View wall z=3 — bars at feet, sealed at eye level for punch columns
        for (int x = -1; x <= 0; x++) {
            BaseTemplates.RelBlock feet = cells.get(pack(x, 1, 3));
            BaseTemplates.RelBlock eyes = cells.get(pack(x, 2, 3));
            if (feet == null || feet.material() != Material.IRON_BARS) {
                errors.add("slime-window-feet-bars@" + x);
            }
            if (eyes == null || eyes.material() != Material.STONE_BRICKS) {
                errors.add("slime-window-eye-sealed@" + x);
            }
        }
        return errors;
    }

    private static @NotNull Set<String> validateBambooDualHoppers(@NotNull Map<Long, BaseTemplates.RelBlock> cells) {
        Set<String> errors = new HashSet<>();
        for (BaseTemplates.RelBlock block : cells.values()) {
            if (block.material() != Material.BAMBOO || block.dx() == 0) {
                continue;
            }
            int x = block.dx();
            int y = block.dy();
            boolean northHopper = cells.containsKey(pack(x, y - 1, -1))
                    && cells.get(pack(x, y - 1, -1)).material() == Material.HOPPER;
            boolean southHopper = cells.containsKey(pack(x, y - 1, 1))
                    && cells.get(pack(x, y - 1, 1)).material() == Material.HOPPER;
            if (!northHopper) {
                errors.add("bamboo-missing-north-hopper@" + x);
            }
            if (!southHopper) {
                errors.add("bamboo-missing-south-hopper@" + x);
            }
        }
        return errors;
    }

    private static @NotNull Set<String> validateCookBattery(@NotNull Map<Long, BaseTemplates.RelBlock> cells) {
        Set<String> errors = new HashSet<>();
        int smokers = 0;
        int fuelHoppers = 0;
        int rawChests = 0;
        int fuelChests = 0;
        int half = AdvancedFarmTemplates.COOK_SMOKER_HALF;
        for (int x = -half; x <= half; x++) {
            BaseTemplates.RelBlock smoker = cells.get(pack(x, AdvancedFarmTemplates.COOK_SMOKER_Y, 0));
            if (smoker != null && smoker.material() == Material.SMOKER) {
                smokers++;
            }
            BaseTemplates.RelBlock raw = cells.get(pack(x, AdvancedFarmTemplates.COOK_RAW_CHEST_Y, 0));
            if (raw != null && raw.material() == Material.CHEST) {
                rawChests++;
            }
            BaseTemplates.RelBlock fuel = cells.get(pack(
                    x, AdvancedFarmTemplates.COOK_FUEL_CHEST_Y, AdvancedFarmTemplates.COOK_FUEL_CHEST_Z));
            if (fuel != null && fuel.material() == Material.CHEST) {
                fuelChests++;
            }
            BaseTemplates.RelBlock fuelHop = cells.get(pack(
                    x, AdvancedFarmTemplates.COOK_SMOKER_Y, AdvancedFarmTemplates.COOK_FUEL_HOPPER_Z));
            if (fuelHop != null && fuelHop.material() == Material.HOPPER
                    && fuelHop.facing() == BlockFace.SOUTH) {
                fuelHoppers++;
            }
        }
        if (smokers < 3) {
            errors.add("cook-smokers-missing");
        }
        if (rawChests < 3) {
            errors.add("cook-raw-top-chests-missing");
        }
        if (fuelChests < 3) {
            errors.add("cook-coal-back-chests-missing");
        }
        if (fuelHoppers < 3) {
            errors.add("cook-fuel-hopper-line-missing");
        }
        int feedHoppers = 0;
        int feedZ = AdvancedFarmTemplates.COOK_LOOT_CHEST_Z - 1;
        for (int col = FarmTemplates.LOOT_BAY_MIN_X; col <= FarmTemplates.LOOT_BAY_MAX_X; col++) {
            BaseTemplates.RelBlock feed = cells.get(pack(col, FarmTemplates.LOOT_DRAIN_Y, feedZ));
            if (feed != null && feed.material() == Material.HOPPER && feed.facing() == BlockFace.SOUTH) {
                feedHoppers++;
            }
        }
        if (feedHoppers < 3) {
            errors.add("cook-loot-feed-hoppers-missing");
        }
        return errors;
    }

    private static @NotNull Set<String> validateDiamondHall(@NotNull Map<Long, BaseTemplates.RelBlock> cells) {
        Set<String> errors = new HashSet<>();
        // Dual airlock iron doors
        BaseTemplates.RelBlock inner = cells.get(pack(0, 1, AdvancedFarmTemplates.DIAMOND_INNER_DOOR_Z));
        BaseTemplates.RelBlock outer = cells.get(pack(0, 1, AdvancedFarmTemplates.DIAMOND_OUTER_DOOR_Z));
        if (inner == null || inner.material() != Material.IRON_DOOR) {
            errors.add("diamond-inner-door-missing");
        }
        if (outer == null || outer.material() != Material.IRON_DOOR) {
            errors.add("diamond-outer-door-missing");
        }
        // Secret west lever door
        BaseTemplates.RelBlock secret = cells.get(pack(
                AdvancedFarmTemplates.DIAMOND_SECRET_DOOR_X,
                1,
                AdvancedFarmTemplates.DIAMOND_SECRET_DOOR_Z));
        if (secret == null || secret.material() != Material.IRON_DOOR) {
            errors.add("diamond-secret-door-missing");
        }
        long levers = cells.values().stream().filter(b -> b.material() == Material.LEVER).count();
        if (levers < 5) {
            errors.add("diamond-levers-missing");
        }
        // Loot bay: chests at chestY, hoppers ON TOP facing DOWN
        int cy = AdvancedFarmTemplates.DIAMOND_LOOT_CHEST_Y;
        int cz = AdvancedFarmTemplates.DIAMOND_LOOT_CHEST_Z;
        int dy = AdvancedFarmTemplates.DIAMOND_LOOT_DRAIN_Y;
        BaseTemplates.RelBlock chest = cells.get(pack(0, cy, cz));
        if (chest == null || chest.material() != Material.CHEST) {
            errors.add("diamond-loot-chest-missing");
        }
        BaseTemplates.RelBlock drain = cells.get(pack(0, dy, cz));
        if (drain == null || drain.material() != Material.HOPPER || drain.facing() != BlockFace.DOWN) {
            errors.add("diamond-loot-hopper-not-down");
        }
        if (dy != cy + 1) {
            errors.add("diamond-loot-height-math-wrong");
        }
        // Emerald stock must not overwrite drain hoppers
        BaseTemplates.RelBlock emerald = cells.get(pack(
                AdvancedFarmTemplates.DIAMOND_EMERALD_X,
                AdvancedFarmTemplates.DIAMOND_EMERALD_Y,
                AdvancedFarmTemplates.DIAMOND_EMERALD_Z));
        if (emerald == null || (emerald.material() != Material.CHEST && emerald.material() != Material.BARREL)) {
            errors.add("diamond-emerald-chest-missing");
        }
        if (AdvancedFarmTemplates.DIAMOND_EMERALD_Z == cz
                && AdvancedFarmTemplates.DIAMOND_EMERALD_Y == dy) {
            errors.add("diamond-emerald-overwrites-loot-hopper");
        }
        if (AdvancedFarmTemplates.DIAMOND_EMERALD_Z == cz - 1
                && AdvancedFarmTemplates.DIAMOND_EMERALD_Y == dy) {
            errors.add("diamond-emerald-overwrites-loot-feed");
        }
        // Feed hoppers must remain SOUTH into drain row
        for (int col = FarmTemplates.LOOT_BAY_MIN_X; col <= FarmTemplates.LOOT_BAY_MAX_X; col++) {
            BaseTemplates.RelBlock feed = cells.get(pack(col, dy, cz - 1));
            if (feed == null || feed.material() != Material.HOPPER || feed.facing() != BlockFace.SOUTH) {
                errors.add("diamond-loot-feed-missing@" + col);
            }
        }
        // Smith trading bars present
        for (int x : AdvancedFarmTemplates.DIAMOND_SMITH_X) {
            BaseTemplates.RelBlock bars = cells.get(pack(x, 1, 3));
            if (bars == null || bars.material() != Material.IRON_BARS) {
                errors.add("diamond-smith-bars-missing@" + x);
            }
        }
        return errors;
    }

    /** 3D distance from AFK window (0,1,AFK_Z) to closest dry spawn pad on natural XP deck. */
    public static double closestXpPadDistance(@NotNull BaseTemplates.BaseBlueprint xp) {
        Map<Long, BaseTemplates.RelBlock> cells = new HashMap<>();
        for (BaseTemplates.RelBlock block : xp.blocks()) {
            cells.put(pack(block.dx(), block.dy(), block.dz()), block);
        }
        int deck = XpFarmTemplates.NATURAL_DECK_Y;
        double min = Double.MAX_VALUE;
        for (int x = XpFarmTemplates.PAD_MIN_X; x <= XpFarmTemplates.PAD_MAX_X; x++) {
            for (int z = XpFarmTemplates.PAD_MIN_Z; z <= XpFarmTemplates.PAD_MAX_Z; z++) {
                if (!XpFarmTemplates.isSpawnPadCell(x, z)) {
                    continue;
                }
                BaseTemplates.RelBlock floor = cells.get(pack(x, deck, z));
                BaseTemplates.RelBlock above = cells.get(pack(x, deck + 1, z));
                if (floor == null || floor.material() == Material.AIR || floor.material() == Material.WATER) {
                    continue;
                }
                if (above != null && above.material() != Material.AIR) {
                    continue;
                }
                min = Math.min(min, XpFarmTemplates.afkDistance(x, deck, z));
            }
        }
        return min;
    }

    /**
     * Every observer used for auto-harvest must have redstone dust on its output face
     * (same Y, one block behind) so the pulse can reach the piston — no torch/button required.
     */
    public static @NotNull Set<String> validateObserverPistonCircuits(
            @NotNull BaseTemplates.BaseBlueprint blueprint
    ) {
        Set<String> errors = new HashSet<>();
        Map<Long, BaseTemplates.RelBlock> cells = new HashMap<>();
        for (BaseTemplates.RelBlock block : blueprint.blocks()) {
            cells.put(pack(block.dx(), block.dy(), block.dz()), block);
        }
        boolean anyObserver = false;
        for (BaseTemplates.RelBlock block : cells.values()) {
            if (block.material() != Material.OBSERVER) {
                continue;
            }
            anyObserver = true;
            BlockFace face = block.facing() == null ? BlockFace.NORTH : block.facing();
            BlockFace out = face.getOppositeFace();
            int ox = block.dx() + out.getModX();
            int oy = block.dy() + out.getModY();
            int oz = block.dz() + out.getModZ();
            BaseTemplates.RelBlock dust = cells.get(pack(ox, oy, oz));
            if (dust == null || dust.material() != Material.REDSTONE_WIRE) {
                errors.add("observer-no-dust-output@" + block.dx() + "," + block.dy() + "," + block.dz()
                        + "->" + out.name());
            }
            // Prefer a piston facing the same way under/near the observer (classic on-top layout)
            BaseTemplates.RelBlock under = cells.get(pack(block.dx(), block.dy() - 1, block.dz()));
            if (under == null || under.material() != Material.PISTON
                    || under.facing() != face) {
                errors.add("observer-piston-stack@" + block.dx() + "," + block.dz());
            }
        }
        if (anyObserver) {
            boolean hasPiston = cells.values().stream().anyMatch(b -> b.material() == Material.PISTON);
            if (!hasPiston) {
                errors.add("observer-without-piston");
            }
        }
        return errors;
    }

    /**
     * Every hopper must either point into another hopper, or point into / down onto a chest or barrel.
     */
    public static @NotNull Set<String> validateHopperOutputsReachStorage(
            @NotNull BaseTemplates.BaseBlueprint blueprint
    ) {
        Set<String> errors = new HashSet<>();
        Map<Long, BaseTemplates.RelBlock> cells = new HashMap<>();
        for (BaseTemplates.RelBlock block : blueprint.blocks()) {
            cells.put(pack(block.dx(), block.dy(), block.dz()), block);
        }
        for (BaseTemplates.RelBlock block : cells.values()) {
            if (block.material() != Material.HOPPER) {
                continue;
            }
            BlockFace face = block.facing() == null ? BlockFace.DOWN : block.facing();
            if (!hopperOutputConnected(cells, block.dx(), block.dy(), block.dz(), face, 0)) {
                errors.add("hopper-dead-end@" + block.dx() + "," + block.dy() + "," + block.dz()
                        + "->" + face.name());
            }
        }
        return errors;
    }

    private static boolean hopperOutputConnected(
            @NotNull Map<Long, BaseTemplates.RelBlock> cells,
            int x,
            int y,
            int z,
            @NotNull BlockFace face,
            int depth
    ) {
        if (depth > 48) {
            return false;
        }
        int tx = x + face.getModX();
        int ty = y + face.getModY();
        int tz = z + face.getModZ();
        BaseTemplates.RelBlock target = cells.get(pack(tx, ty, tz));
        if (target == null) {
            return false;
        }
        if (target.material() == Material.CHEST || target.material() == Material.BARREL
                || target.material() == Material.TRAPPED_CHEST
                || target.material() == Material.SMOKER || target.material() == Material.FURNACE
                || target.material() == Material.BLAST_FURNACE || target.material() == Material.DISPENSER
                || target.material() == Material.DROPPER) {
            return true;
        }
        if (target.material() == Material.HOPPER) {
            BlockFace next = target.facing() == null ? BlockFace.DOWN : target.facing();
            return hopperOutputConnected(cells, tx, ty, tz, next, depth + 1);
        }
        return false;
    }

    /**
     * True when enclosed ground-floor interior columns have a roof-like block above.
     * Outdoor pads (porch tip, pool deck) are ignored — they are not enclosed on both axes.
     */
    public static boolean hasContinuousCeiling(@NotNull BaseTemplates.BaseBlueprint blueprint) {
        Map<Long, BaseTemplates.RelBlock> cells = new HashMap<>();
        for (BaseTemplates.RelBlock block : blueprint.blocks()) {
            cells.put(pack(block.dx(), block.dy(), block.dz()), block);
        }
        int checked = 0;
        for (BaseTemplates.RelBlock floor : cells.values()) {
            if (floor.dy() != 0) {
                continue;
            }
            String floorName = floor.material().name();
            if (floor.material() == Material.AIR || floor.material() == Material.WATER
                    || floorName.contains("PATH") || floorName.contains("SLAB")
                    || floor.material() == Material.GRASS_BLOCK) {
                continue;
            }
            BaseTemplates.RelBlock at1 = cells.get(pack(floor.dx(), 1, floor.dz()));
            if (at1 == null) {
                continue;
            }
            if (isLikelyFullCube(at1.material()) && !at1.material().name().endsWith("_BED")) {
                continue; // wall / pillar column
            }
            if (!isOpenInterior(at1.material())) {
                continue;
            }
            if (!isEnclosedInterior(cells, floor.dx(), floor.dz())) {
                continue;
            }
            // Skip outdoor pool / patio decks (no house roof expected)
            if (isOutdoorAmenity(cells, floor.dx(), floor.dz())) {
                continue;
            }
            checked++;
            if (!columnHasRoof(cells, floor.dx(), floor.dz())) {
                return false;
            }
        }
        return checked > 0;
    }

    private static boolean isOpenInterior(@NotNull Material material) {
        if (material == Material.AIR) {
            return true;
        }
        String n = material.name();
        return n.contains("CARPET") || n.endsWith("_BED") || n.contains("STAIRS")
                || n.contains("CHEST") || n.contains("BARREL") || n.contains("TABLE")
                || n.contains("FURNACE") || n.contains("SMOKER") || n.contains("BLAST")
                || n.contains("BOOK") || n.contains("ANVIL") || n.contains("CAULDRON")
                || n.contains("BREWING") || n.contains("POT") || n.contains("TRAPDOOR")
                || n.contains("LANTERN") || n.contains("DOOR") || n.contains("GRIND")
                || n.contains("SMITHING") || n.contains("ENCHANTING") || n.contains("FLOWER");
    }

    /** Enclosed if wall-like blocks exist toward both X and Z within a few blocks. */
    private static boolean isEnclosedInterior(
            @NotNull Map<Long, BaseTemplates.RelBlock> cells,
            int x,
            int z
    ) {
        boolean wallX = false;
        boolean wallZ = false;
        for (int d = 1; d <= 5; d++) {
            if (isWallLike(cells.get(pack(x + d, 2, z))) || isWallLike(cells.get(pack(x - d, 2, z)))) {
                wallX = true;
            }
            if (isWallLike(cells.get(pack(x, 2, z + d))) || isWallLike(cells.get(pack(x, 2, z - d)))) {
                wallZ = true;
            }
        }
        return wallX && wallZ;
    }

    /** Outdoor pool/patio — near water or prismarine deck, not under house roof. */
    private static boolean isOutdoorAmenity(
            @NotNull Map<Long, BaseTemplates.RelBlock> cells,
            int x,
            int z
    ) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int y = -1; y <= 2; y++) {
                    BaseTemplates.RelBlock n = cells.get(pack(x + dx, y, z + dz));
                    if (n == null) {
                        continue;
                    }
                    String name = n.material().name();
                    if (name.contains("PRISMARINE") || name.equals("WATER")
                            || name.contains("SEA_LANTERN")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isWallLike(BaseTemplates.RelBlock block) {
        if (block == null) {
            return false;
        }
        String n = block.material().name();
        return isLikelyFullCube(block.material())
                && (n.contains("CONCRETE") || n.contains("PLANKS") || n.contains("CALCITE")
                || n.contains("QUARTZ") || n.contains("LOG") || n.contains("BRICK")
                || n.contains("STONE") || n.contains("SANDSTONE") || n.contains("TERRACOTTA")
                || n.contains("BLACKSTONE") || n.contains("GOLD"));
    }

    private static boolean columnHasRoof(
            @NotNull Map<Long, BaseTemplates.RelBlock> cells,
            int x,
            int z
    ) {
        for (int y = 2; y <= 16; y++) {
            BaseTemplates.RelBlock above = cells.get(pack(x, y, z));
            if (above == null) {
                continue;
            }
            String n = above.material().name();
            if (n.equals("AIR") || n.contains("LANTERN") || n.contains("CHAIN")
                    || n.contains("TORCH") || n.contains("ROD") || n.contains("CARPET")
                    || n.contains("DOOR") || n.endsWith("_BED") || n.contains("FENCE")
                    || n.contains("PANE") || n.contains("GLASS")) {
                continue;
            }
            if (n.contains("SLAB") || n.contains("STAIRS") || n.contains("PLANKS")
                    || n.contains("CONCRETE") || n.contains("STONE") || n.contains("BRICK")
                    || n.contains("QUARTZ") || n.contains("CALCITE") || n.contains("LOG")
                    || n.contains("WOOD") || n.contains("TERRACOTTA") || n.contains("GOLD")
                    || n.contains("DEEPSLATE") || n.contains("SANDSTONE") || n.contains("BLACKSTONE")
                    || isLikelyFullCube(above.material())) {
                return true;
            }
        }
        return false;
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x + 512) & 0x3FF)
                | (((long) (y + 64) & 0xFF) << 10)
                | (((long) (z + 512) & 0x3FF) << 18);
    }
}
