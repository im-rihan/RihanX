package com.rihanx.farm;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dry-run validators for calculated {@link FarmPlan}s (no world mutation).
 */
public final class FarmPlanValidator {

    private FarmPlanValidator() {
    }

    public static @NotNull List<String> validatePlan(@NotNull FarmPlan plan) {
        List<String> errors = new ArrayList<>();
        if (plan.placements().isEmpty()) {
            errors.add("empty-plan");
            return errors;
        }
        if (plan.width() < 1 || plan.length() < 1 || plan.height() < 1) {
            errors.add("invalid-dimensions");
        }

        Set<Long> seen = new HashSet<>();
        for (BlockPlacement p : plan.placements()) {
            if (!seen.add(p.packKey())) {
                errors.add("duplicate-coord@" + p.dx() + "," + p.dy() + "," + p.dz());
            }
        }

        Map<Long, BlockPlacement> index = plan.indexByCoord();
        for (BlockPlacement p : plan.placements()) {
            if (p.material() != Material.HOPPER) {
                continue;
            }
            BlockFace face = p.facing() == null ? BlockFace.DOWN : p.facing();
            if (!hopperChainReachesStorage(index, p.dx(), p.dy(), p.dz(), face, 0)) {
                errors.add("hopper-dead-end@" + p.dx() + "," + p.dy() + "," + p.dz() + "->" + face.name());
            }
        }

        for (BlockPlacement p : plan.placements()) {
            if (p.material().name().contains("OBSERVER") && p.facing() == null) {
                errors.add("observer-missing-facing@" + p.dx() + "," + p.dy() + "," + p.dz());
            }
            if (p.material().name().contains("PISTON") && p.facing() == null) {
                errors.add("piston-missing-facing@" + p.dx() + "," + p.dy() + "," + p.dz());
            }
            if (p.material() == Material.DISPENSER && p.facing() == null) {
                errors.add("dispenser-missing-facing@" + p.dx() + "," + p.dy() + "," + p.dz());
            }
        }
        return errors;
    }

    public static @NotNull List<String> validateWorldHeight(
            @NotNull FarmBuildContext context,
            @NotNull FarmPlan plan
    ) {
        List<String> errors = new ArrayList<>();
        int minWorld = context.world().getMinHeight();
        int maxWorld = context.world().getMaxHeight() - 1;
        int planMin = context.worldY(plan.minY());
        int planMax = context.worldY(plan.maxY());
        if (planMin < minWorld) {
            errors.add("below-world-min@" + planMin + "<" + minWorld);
        }
        if (planMax > maxWorld) {
            errors.add("above-world-max@" + planMax + ">" + maxWorld);
        }
        return errors;
    }

    private static boolean hopperChainReachesStorage(
            @NotNull Map<Long, BlockPlacement> cells,
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
        long key = ((long) (tx + 512) & 0x3FF)
                | (((long) (ty + 64) & 0xFF) << 10)
                | (((long) (tz + 512) & 0x3FF) << 18);
        BlockPlacement target = cells.get(key);
        if (target == null) {
            return false;
        }
        Material m = target.material();
        if (m == Material.CHEST || m == Material.BARREL || m == Material.TRAPPED_CHEST
                || m == Material.SMOKER || m == Material.FURNACE || m == Material.BLAST_FURNACE
                || m == Material.DISPENSER || m == Material.DROPPER) {
            return true;
        }
        if (m == Material.HOPPER) {
            BlockFace next = target.facing() == null ? BlockFace.DOWN : target.facing();
            return hopperChainReachesStorage(cells, tx, ty, tz, next, depth + 1);
        }
        return false;
    }
}
