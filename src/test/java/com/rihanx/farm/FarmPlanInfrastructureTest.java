package com.rihanx.farm;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarmPlanInfrastructureTest {

    @Test
    void registryContainsAllRequiredFarms() {
        Set<String> required = Set.of(
                "wheat", "potato", "cane", "bamboo", "kelp", "nether", "animal", "cactus",
                "iron", "xp", "xp-zombie", "xp-skeleton", "xp-spider", "xp-enderman",
                "chicken", "cow", "pig", "cook", "slime", "redstone", "diamond"
        );
        assertTrue(FarmRegistry.get().ids().containsAll(required),
                "missing farms: " + required.stream().filter(id -> FarmRegistry.get().get(id) == null).toList());
    }

    @Test
    void everyFarmPlanHasNoDuplicateCoordsAndValidDimensions() {
        for (String id : FarmRegistry.get().ids()) {
            FarmPlan plan = FarmRegistry.get().plan(id);
            assertTrue(plan.width() > 0, id + " width");
            assertTrue(plan.length() > 0, id + " length");
            assertTrue(plan.height() > 0, id + " height");
            Set<Long> keys = new HashSet<>();
            for (BlockPlacement p : plan.placements()) {
                assertTrue(keys.add(p.packKey()), id + " duplicate @" + p.dx() + "," + p.dy() + "," + p.dz());
            }
            MaterialCalculator.Report report = MaterialCalculator.calculate(plan);
            assertEquals(plan.placements().size(), report.totalBlocks(), id + " material total mismatch");
        }
    }

    @Test
    void wheatPlanPassesDryValidation() {
        FarmPlan plan = FarmRegistry.get().plan("wheat");
        List<String> errors = FarmRegistry.get().get("wheat").validate(null, plan);
        assertTrue(errors.isEmpty(), "wheat errors: " + errors);
    }

    @Test
    void coordinateRotationMatchesCardinals() {
        int[] s = CoordinateTransformer.rotate(3, 5, BlockFace.SOUTH);
        assertEquals(3, s[0]);
        assertEquals(5, s[1]);
        int[] n = CoordinateTransformer.rotate(3, 5, BlockFace.NORTH);
        assertEquals(-3, n[0]);
        assertEquals(-5, n[1]);
        assertEquals(BlockFace.EAST, CoordinateTransformer.mapFacing(BlockFace.NORTH, BlockFace.WEST));
    }

    @Test
    void printCalculatedDimensionsForDocs() {
        StringBuilder sb = new StringBuilder();
        for (String id : FarmRegistry.get().ids()) {
            FarmPlan p = FarmRegistry.get().plan(id);
            MaterialCalculator.Report r = MaterialCalculator.calculate(p);
            sb.append(id).append('|').append(r.width()).append('x').append(r.length())
                    .append('x').append(r.height()).append('|').append(r.totalBlocks()).append('\n');
        }
        System.out.println("=== FARM DIMENSIONS ===\n" + sb);
        assertTrue(sb.length() > 50);
    }
}
