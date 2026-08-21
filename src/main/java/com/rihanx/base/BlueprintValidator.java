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

        return errors;
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
                || target.material() == Material.TRAPPED_CHEST) {
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
