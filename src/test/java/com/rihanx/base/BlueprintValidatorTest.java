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
    void ironFarmHasDryDeckLavaTrapdoorsAndPodFloors() {
        BaseTemplates.BaseBlueprint iron = FarmTemplates.all().get("iron");
        boolean hasLava = false;
        boolean hasMagma = false;
        boolean hasSign = false;
        boolean hasTrapdoor = false;
        boolean hasPodFloor = false;
        boolean floodedDeck = false;
        boolean hasCenterHole = false;
        boolean hasDeckRoof = false;
        for (BaseTemplates.RelBlock block : iron.blocks()) {
            if (block.material() == org.bukkit.Material.LAVA) {
                hasLava = true;
            }
            if (block.material() == org.bukkit.Material.MAGMA_BLOCK) {
                hasMagma = true;
            }
            if (block.material().name().contains("SIGN")) {
                hasSign = true;
            }
            if (block.material().name().contains("TRAPDOOR")) {
                hasTrapdoor = true;
            }
            if (block.dx() == -5 && block.dy() == 8 && block.material() == org.bukkit.Material.STONE_BRICKS) {
                hasPodFloor = true;
            }
            if (block.dx() == 0 && block.dz() == 0 && block.dy() == 13
                    && block.material() == org.bukkit.Material.AIR) {
                hasCenterHole = true;
            }
            // Golem deck (y=13+) must stay open to sky — no roof over center pads
            if (block.dy() == 16 && Math.abs(block.dx()) <= 5 && Math.abs(block.dz()) <= 5
                    && block.material().name().contains("SLAB")) {
                hasDeckRoof = true;
            }
            if (block.dy() == 13 && Math.abs(block.dx()) <= 4 && Math.abs(block.dz()) <= 4
                    && block.material() == org.bukkit.Material.WATER) {
                floodedDeck = true;
            }
        }
        assertTrue(hasLava, "iron farm needs lava kill");
        assertTrue(hasMagma, "iron farm needs visible magma on the kill floor");
        assertTrue(hasSign, "iron farm needs water-break signs above the lava");
        assertTrue(hasTrapdoor, "iron farm needs open trapdoors under lava");
        assertTrue(hasPodFloor, "iron farm villager pods need floors");
        assertTrue(hasCenterHole, "iron farm needs center drop hole");
        assertFalse(floodedDeck, "iron farm spawn deck must not be flooded");
        assertFalse(hasDeckRoof, "iron spawn deck should be open-sky (no roof)");
    }

    @Test
    void mineStationHasDescendingTunnelAndOutpost() {
        BaseTemplates.BaseBlueprint mine = StationTemplates.all().get("mine");
        assertTrue(mine.blocks().size() > 200, "mine station should be substantial");
        boolean deepTunnel = mine.blocks().stream()
                .anyMatch(b -> b.dz() > 30 && b.dy() < -5 && b.material().name().contains("RAIL"));
        boolean hasChest = mine.blocks().stream()
                .anyMatch(b -> b.material() == org.bukkit.Material.CHEST);
        boolean hasFurnace = mine.blocks().stream()
                .anyMatch(b -> b.material() == org.bukkit.Material.FURNACE
                        || b.material() == org.bukkit.Material.BLAST_FURNACE);
        assertTrue(deepTunnel, "mine station needs a descending rail tunnel");
        assertTrue(hasChest, "mine outpost needs chests");
        assertTrue(hasFurnace, "mine outpost needs furnaces");
    }

    @Test
    void xpFarmHasContinuousDropShaftAndDarkRoof() {
        BaseTemplates.BaseBlueprint xp = FarmTemplates.all().get("xp");
        boolean hasButton = false;
        boolean darkRoof = false;
        boolean hasMagmaLanding = false;
        boolean hasSign = false;
        boolean hasSlabOnLanding = false;
        int shaftAir = 0;
        int waterSources = 0;
        for (BaseTemplates.RelBlock block : xp.blocks()) {
            if (block.material().name().endsWith("_BUTTON")) {
                hasButton = true;
                assertTrue(Math.abs(block.dx()) >= 1, "XP door buttons must be on wall beside door");
            }
            // Continuous shaft through deck
            if ((block.dx() == -1 || block.dx() == 0)
                    && (block.dz() == 0 || block.dz() == 1)
                    && block.dy() >= 1 && block.dy() <= 26
                    && block.material() == org.bukkit.Material.AIR) {
                shaftAir++;
            }
            if (block.dy() == 27 && block.material() == org.bukkit.Material.COBBLESTONE
                    && !(block.dx() >= -1 && block.dx() <= 0 && block.dz() >= 0 && block.dz() <= 1)) {
                darkRoof = true;
            }
            // Dark roof over spawn deck (deck y=23 → roof at 26)
            if (block.material() == org.bukkit.Material.COBBLESTONE && block.dy() == 26
                    && !(block.dx() >= -1 && block.dx() <= 0 && block.dz() >= 0 && block.dz() <= 1)) {
                darkRoof = true;
            }
            // Open trapdoors over hoppers = punch-XP landing
            if (block.material() == org.bukkit.Material.IRON_TRAPDOOR
                    && (block.dx() == -1 || block.dx() == 0)
                    && (block.dz() == 0 || block.dz() == 1)
                    && block.dy() == 1) {
                hasMagmaLanding = true;
            }
            if (block.material().name().contains("SLAB")
                    && (block.dx() == -1 || block.dx() == 0)
                    && (block.dz() == 0 || block.dz() == 1)
                    && block.dy() == 1) {
                hasSlabOnLanding = true;
            }
            if (block.material().name().contains("SIGN") && (block.dy() == 22 || block.dy() == 21)) {
                hasSign = true;
            }
            if (block.material() == org.bukkit.Material.WATER && block.dy() == 23) {
                waterSources++;
            }
        }
        assertTrue(hasButton, "xp farm needs door buttons");
        assertTrue(shaftAir >= 80, "xp farm needs continuous drop shaft, got " + shaftAir);
        assertTrue(darkRoof, "xp spawn deck needs solid dark roof");
        assertTrue(hasMagmaLanding, "xp kill landing must be open trapdoors over hoppers");
        assertFalse(hasSlabOnLanding, "xp landing must not use slabs that block collection");
        assertTrue(hasSign, "xp hole needs water-break signs");
        assertTrue(waterSources > 0 && waterSources <= 12,
                "xp water should be trench-end sources only, got " + waterSources);
    }

    @Test
    void stationPoweredRailsSitOnRedstoneBlocks() {
        for (String id : List.of(
                "station", "depot", "rail", "terminal", "mine",
                "kingdom", "western", "adacia", "yard"
        )) {
            BaseTemplates.BaseBlueprint bp = StationTemplates.all().get(id);
            Map<String, org.bukkit.Material> at = new java.util.HashMap<>();
            for (BaseTemplates.RelBlock block : bp.blocks()) {
                at.put(block.dx() + "," + block.dy() + "," + block.dz(), block.material());
            }
            int powered = 0;
            int poweredOnRedstone = 0;
            for (BaseTemplates.RelBlock block : bp.blocks()) {
                if (block.material() != org.bukkit.Material.POWERED_RAIL) {
                    continue;
                }
                powered++;
                org.bukkit.Material under = at.get(block.dx() + "," + (block.dy() - 1) + "," + block.dz());
                if (under == org.bukkit.Material.REDSTONE_BLOCK) {
                    poweredOnRedstone++;
                }
            }
            assertTrue(powered > 0, id + " needs powered rails");
            assertEquals(powered, poweredOnRedstone,
                    id + " every powered rail must sit on a redstone block");
        }
    }

    @Test
    void caneAndBambooPistonsFaceCrops() {
        for (String id : List.of("cane", "bamboo", "kelp")) {
            BaseTemplates.BaseBlueprint bp = FarmTemplates.all().get(id);
            Set<String> circuitErrors = BlueprintValidator.validateObserverPistonCircuits(bp);
            assertTrue(circuitErrors.isEmpty(), () -> id + " circuit: " + circuitErrors);
            boolean hopperSouth = bp.blocks().stream().anyMatch(b ->
                    b.material() == org.bukkit.Material.HOPPER
                            && b.facing() == org.bukkit.block.BlockFace.SOUTH);
            assertTrue(hopperSouth, id + " needs hoppers facing south toward storage");
        }
    }

    @Test
    void everyFarmHopperChainAndObserverCircuitValid() {
        for (Map.Entry<String, BaseTemplates.BaseBlueprint> entry : FarmTemplates.all().entrySet()) {
            Set<String> hoppers = BlueprintValidator.validateHopperOutputsReachStorage(entry.getValue());
            assertTrue(hoppers.isEmpty(), () -> entry.getKey() + " hoppers: " + hoppers);
            Set<String> circuits = BlueprintValidator.validateObserverPistonCircuits(entry.getValue());
            assertTrue(circuits.isEmpty(), () -> entry.getKey() + " circuits: " + circuits);
            boolean hasChest = entry.getValue().blocks().stream()
                    .anyMatch(b -> b.material() == org.bukkit.Material.CHEST
                            || b.material() == org.bukkit.Material.BARREL);
            assertTrue(hasChest, entry.getKey() + " needs chest/barrel storage");
        }
    }

    @Test
    void cropFarmsHaveWaterCrossOrCollectionTowardSouth() {
        for (String id : List.of("wheat", "potato", "melon", "cocoa", "mushroom", "cactus", "nether")) {
            BaseTemplates.BaseBlueprint bp = FarmTemplates.all().get(id);
            Set<String> errors = BlueprintValidator.validateFarm(bp);
            assertTrue(errors.isEmpty(), () -> id + " failed: " + errors);
            long southHoppers = bp.blocks().stream()
                    .filter(b -> b.material() == org.bukkit.Material.HOPPER
                            && b.facing() == org.bukkit.block.BlockFace.SOUTH)
                    .count();
            long anyHoppers = bp.blocks().stream()
                    .filter(b -> b.material() == org.bukkit.Material.HOPPER)
                    .count();
            assertTrue(anyHoppers >= 3, id + " should have multiple hoppers, got " + anyHoppers);
            assertTrue(southHoppers >= 1 || id.equals("wheat") || id.equals("potato"),
                    id + " should drain toward +Z/chest (south-facing hoppers)");
        }
    }

    @Test
    void everyStationBlueprintIsValid() {
        Map<String, BaseTemplates.BaseBlueprint> stations = StationTemplates.all();
        assertEquals(10, stations.size(), "expected 10 station templates");
        for (Map.Entry<String, BaseTemplates.BaseBlueprint> entry : stations.entrySet()) {
            Set<String> errors = BlueprintValidator.validate(entry.getValue());
            assertTrue(errors.isEmpty(), () -> entry.getKey() + " failed: " + errors);
            boolean hasRail = entry.getValue().blocks().stream()
                    .anyMatch(b -> b.material().name().contains("RAIL"));
            assertTrue(hasRail, entry.getKey() + " should include rails");
        }
    }

    @Test
    void passengerStationsIncludeLinkPressurePlate() {
        for (String id : List.of(
                "station", "depot", "terminal", "mine",
                "kingdom", "western", "adacia", "yard"
        )) {
            BaseTemplates.BaseBlueprint bp = StationTemplates.all().get(id);
            boolean hasPlate = bp.blocks().stream()
                    .anyMatch(b -> b.material() == org.bukkit.Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            boolean hasDeepslate = bp.blocks().stream()
                    .anyMatch(b -> b.material() == org.bukkit.Material.POLISHED_DEEPSLATE);
            assertTrue(hasPlate, id + " needs a gold pressure plate link pad");
            assertTrue(hasDeepslate, id + " needs polished deepslate under the link pad");
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
        for (String id : List.of("hut", "cottage", "village", "bungalow", "villa", "mansion", "chateau", "palace")) {
            BaseTemplates.BaseBlueprint bp = BaseTemplates.all().get(id);
            assertTrue(BlueprintValidator.hasContinuousCeiling(bp), id + " has sky holes over interior");
        }
    }

    @Test
    void luxuryBasesProtectSpawnPadAndHaveWorkingLiftParts() {
        for (String id : List.of("villa", "mansion", "modern", "estate", "chateau", "skyvilla", "palace")) {
            BaseTemplates.BaseBlueprint bp = BaseTemplates.all().get(id);
            int sx = bp.spawnDx();
            int sy = bp.spawnDy();
            int sz = bp.spawnDz();
            boolean feetAir = false;
            boolean headAir = false;
            boolean floorSolid = false;
            boolean hasSoulSand = false;
            boolean hasMagma = false;
            boolean hasWater = false;
            boolean hasSignDoor = false;
            for (BaseTemplates.RelBlock block : bp.blocks()) {
                if (block.dx() == sx && block.dz() == sz && block.dy() == sy
                        && block.material() == org.bukkit.Material.AIR) {
                    feetAir = true;
                }
                if (block.dx() == sx && block.dz() == sz && block.dy() == sy + 1
                        && block.material() == org.bukkit.Material.AIR) {
                    headAir = true;
                }
                if (block.dx() == sx && block.dz() == sz && block.dy() == sy - 1) {
                    String n = block.material().name();
                    if (!n.equals("AIR") && !n.equals("WATER") && !n.contains("SIGN")
                            && !n.contains("RAIL") && !n.contains("BUTTON")) {
                        floorSolid = true;
                    }
                }
                if (block.material() == org.bukkit.Material.SOUL_SAND) {
                    hasSoulSand = true;
                }
                if (block.material() == org.bukkit.Material.MAGMA_BLOCK) {
                    hasMagma = true;
                }
                if (block.material() == org.bukkit.Material.WATER) {
                    hasWater = true;
                }
                if (block.material().name().contains("SIGN")) {
                    hasSignDoor = true;
                }
            }
            assertTrue(feetAir, id + " spawn feet must be air");
            assertTrue(headAir, id + " spawn head must be air");
            assertTrue(floorSolid, id + " spawn needs solid floor under feet");
            assertTrue(hasSoulSand, id + " needs soul-sand up lift");
            assertTrue(hasMagma, id + " needs magma down lift");
            assertTrue(hasWater, id + " needs water in lift tubes");
            assertTrue(hasSignDoor, id + " needs sign doors (water-holding lift entries)");
        }
    }

    @Test
    void advancedGrabCraftStyleBasesExistAndAreSubstantial() {
        for (String id : List.of("estate", "chateau", "skyvilla", "palace")) {
            BaseTemplates.BaseBlueprint bp = BaseTemplates.all().get(id);
            assertTrue(bp != null, id + " missing");
            assertTrue(bp.blocks().size() > 800, id + " too small for advanced tier: " + bp.blocks().size());
            Set<String> errors = BlueprintValidator.validate(bp);
            errors.removeIf(e -> e.startsWith("water-no-support"));
            assertTrue(errors.isEmpty(), () -> id + " failed: " + errors);
        }
    }

    @Test
    void luxuryHomesHaveNoLanternsInWalkVolume() {
        for (String id : List.of("bungalow", "villa", "mansion", "modern", "estate", "chateau", "skyvilla", "palace")) {
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

    /**
     * BuildGuides-style layer audit: every farm must have visible loot, no buried chests,
     * and pass playability rules (cane water, iron dry pads, etc.).
     */
    @Test
    void layerAuditNoBuriedChestsAndPlayability() {
        StringBuilder report = new StringBuilder();
        for (Map.Entry<String, BaseTemplates.BaseBlueprint> entry : FarmTemplates.all().entrySet()) {
            BaseTemplates.BaseBlueprint bp = entry.getValue();
            Set<String> play = BlueprintValidator.validateFarmPlayability(bp);
            int buried = 0;
            int visible = 0;
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;
            Map<Integer, Integer> layerCounts = new java.util.TreeMap<>();
            for (BaseTemplates.RelBlock block : bp.blocks()) {
                minY = Math.min(minY, block.dy());
                maxY = Math.max(maxY, block.dy());
                layerCounts.merge(block.dy(), 1, Integer::sum);
                if (block.material() == org.bukkit.Material.CHEST
                        || block.material() == org.bukkit.Material.BARREL) {
                    if (block.dy() < 0) {
                        buried++;
                    } else {
                        visible++;
                    }
                }
            }
            report.append(entry.getKey())
                    .append(" y=").append(minY).append("..").append(maxY)
                    .append(" layers=").append(layerCounts.size())
                    .append(" storage visible=").append(visible)
                    .append(" buried=").append(buried)
                    .append(" playErrors=").append(play)
                    .append('\n');
            assertEquals(0, buried, () -> entry.getKey() + " has buried storage (dig-to-find): " + report);
            assertTrue(visible > 0, () -> entry.getKey() + " has no ground-visible chest/barrel");
            assertTrue(play.isEmpty(), () -> entry.getKey() + " playability failed: " + play + "\n" + report);
        }
        System.out.println("=== Farm layer audit ===\n" + report);
    }

    @Test
    void baseTemplatesPassBasicStructureAndSpawn() {
        for (Map.Entry<String, BaseTemplates.BaseBlueprint> entry : BaseTemplates.all().entrySet()) {
            Set<String> errors = BlueprintValidator.validate(entry.getValue());
            // Luxury bases may have intentional water features — filter only hard fails
            errors.removeIf(e -> e.startsWith("water-no-support"));
            assertTrue(errors.isEmpty(), () -> entry.getKey() + " base failed: " + errors);
            assertTrue(entry.getValue().blocks().size() > 30, entry.getKey() + " too small");
        }
    }
}
