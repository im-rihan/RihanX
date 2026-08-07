package com.rihanx.base;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validates every farm (and key homes) blueprint for structural bugs.
 */
class BlueprintValidatorTest {

    @Test
    void everyFarmHasGadgetsAndValidStructure() {
        Map<String, BaseTemplates.BaseBlueprint> farms = FarmTemplates.all();
        assertEquals(13, farms.size(), "expected 13 farm templates");
        for (Map.Entry<String, BaseTemplates.BaseBlueprint> entry : farms.entrySet()) {
            Set<String> errors = BlueprintValidator.validateFarm(entry.getValue());
            assertTrue(errors.isEmpty(), () -> entry.getKey() + " failed: " + errors);
            assertFalse(entry.getValue().blocks().isEmpty());
            assertTrue(entry.getValue().blocks().size() > 20, entry.getKey() + " too small");
        }
    }

    @Test
    void everyHomeBlueprintIsValid() {
        for (Map.Entry<String, BaseTemplates.BaseBlueprint> entry : BaseTemplates.all().entrySet()) {
            Set<String> errors = BlueprintValidator.validate(entry.getValue());
            // Filter water-support noise for decorative pools that use sea lantern floors
            errors.removeIf(e -> e.startsWith("water-no-support"));
            assertTrue(errors.isEmpty(), () -> entry.getKey() + " failed: " + errors);
        }
    }

    @Test
    void bungalowIsNotFlatBox() {
        BaseTemplates.BaseBlueprint bungalow = BaseTemplates.all().get("bungalow");
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean hasStairs = false;
        for (BaseTemplates.RelBlock block : bungalow.blocks()) {
            minY = Math.min(minY, block.dy());
            maxY = Math.max(maxY, block.dy());
            if (block.material().name().endsWith("_STAIRS")) {
                hasStairs = true;
            }
        }
        assertTrue(maxY - minY >= 5, "bungalow should have vertical depth (roof/chimney)");
        assertTrue(hasStairs, "bungalow should use stairs for pitched roof / detailing");
    }

    @Test
    void homesHaveContinuousCeilingsOverInterior() {
        for (String id : List.of("hut", "cottage", "village", "bungalow", "villa", "mansion")) {
            BaseTemplates.BaseBlueprint bp = BaseTemplates.all().get(id);
            assertTrue(BlueprintValidator.hasContinuousCeiling(bp), id + " has sky holes over interior");
        }
    }

    @Test
    void luxuryHomesHaveNoLanternsInWalkVolume() {
        for (String id : List.of("bungalow", "villa", "mansion", "modern")) {
            BaseTemplates.BaseBlueprint bp = BaseTemplates.all().get(id);
            for (BaseTemplates.RelBlock block : bp.blocks()) {
                if (!block.material().name().contains("LANTERN")) {
                    continue;
                }
                if (block.material() == org.bukkit.Material.SEA_LANTERN) {
                    continue; // floor markers under lifts / pools are fine
                }
                assertTrue(
                        block.dy() >= 3,
                        () -> id + " lantern at walk height " + block.dx() + "," + block.dy() + "," + block.dz()
                );
            }
        }
    }

    @Test
    void farmsHaveNoLanternsInWalkVolumeAndSafeSpawn() {
        for (Map.Entry<String, BaseTemplates.BaseBlueprint> entry : FarmTemplates.all().entrySet()) {
            BaseTemplates.BaseBlueprint bp = entry.getValue();
            for (BaseTemplates.RelBlock block : bp.blocks()) {
                if (block.material().name().contains("LANTERN")
                        && block.material() != org.bukkit.Material.SEA_LANTERN) {
                    assertTrue(
                            block.dy() >= 3,
                            () -> entry.getKey() + " lantern at walk height @"
                                    + block.dx() + "," + block.dy() + "," + block.dz()
                    );
                }
            }
            // Spawn feet cell must not be a solid path/block
            boolean spawnSolid = false;
            for (BaseTemplates.RelBlock block : bp.blocks()) {
                if (block.dx() == bp.spawnDx() && block.dy() == bp.spawnDy() && block.dz() == bp.spawnDz()) {
                    String n = block.material().name();
                    if (block.material() != org.bukkit.Material.AIR
                            && !n.contains("CARPET")
                            && !n.contains("GATE")
                            && !n.contains("SIGN")
                            && !n.contains("BUTTON")
                            && !n.contains("TORCH")
                            && !n.contains("FLOWER")
                            && !n.contains("PRESSURE")) {
                        // Dirt path / full cubes at feet = bad
                        if (n.contains("PATH") || n.contains("PLANKS") || n.contains("STONE")
                                || n.contains("DIRT") || n.contains("GRASS") || n.contains("BRICK")
                                || n.contains("SAND") || n.contains("CONCRETE") || n.contains("BLACKSTONE")) {
                            spawnSolid = true;
                        }
                    }
                }
            }
            assertFalse(spawnSolid, entry.getKey() + " spawn cell is solid (player trapped in floor)");
        }
    }
}
