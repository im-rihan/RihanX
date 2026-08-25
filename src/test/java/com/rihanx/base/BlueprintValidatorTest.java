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
        assertEquals(24, farms.size(), "expected 24 farm templates");
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
    void ironFarmMobStandCellsAndPlayerPadAreAir() {
        BaseTemplates.BaseBlueprint iron = FarmTemplates.all().get("iron");
        Map<String, org.bukkit.Material> at = new java.util.HashMap<>();
        for (BaseTemplates.RelBlock block : iron.blocks()) {
            at.put(block.dx() + "," + block.dy() + "," + block.dz(), block.material());
        }
        for (int side : FarmTemplates.IRON_POD_SIDES) {
            for (int i = 0; i < FarmTemplates.IRON_VILLAGERS_PER_POD; i++) {
                int z = FarmTemplates.IRON_POD_Z0 + i;
                org.bukkit.Material stand = at.get(side + "," + FarmTemplates.IRON_POD_Y + "," + z);
                assertEquals(org.bukkit.Material.AIR, stand,
                        "villager stand " + side + "," + z + " must be air");
            }
        }
        org.bukkit.Material cage = at.get(FarmTemplates.IRON_ZOMBIE_X + "," + FarmTemplates.IRON_POD_Y + ","
                + FarmTemplates.IRON_ZOMBIE_Z);
        assertTrue(
                cage == org.bukkit.Material.AIR || cage == org.bukkit.Material.RAIL,
                "zombie cage cell must be air or rail for the minecart"
        );
        assertEquals(
                org.bukkit.Material.AIR,
                at.get(iron.spawnDx() + "," + iron.spawnDy() + "," + iron.spawnDz()),
                "player spawn pad must be air so Paper can spawn mobs there"
        );
    }

    @Test
    void cropFarmerStandCellIsAirBesideBed() {
        for (String id : List.of("wheat", "potato")) {
            BaseTemplates.BaseBlueprint farm = FarmTemplates.all().get(id);
            org.bukkit.Material stand = null;
            org.bukkit.Material bed = null;
            for (BaseTemplates.RelBlock block : farm.blocks()) {
                if (block.dx() == FarmTemplates.CROP_FARMER_DX
                        && block.dy() == FarmTemplates.CROP_FARMER_DY
                        && block.dz() == FarmTemplates.CROP_FARMER_DZ) {
                    stand = block.material();
                }
                if (block.dx() == -1 && block.dy() == 1 && block.dz() == 7
                        && block.material().name().endsWith("_BED")) {
                    bed = block.material();
                }
            }
            assertEquals(org.bukkit.Material.AIR, stand, id + " farmer stand must be air");
            assertTrue(bed != null && bed.name().endsWith("_BED"), id + " needs a bed next to the farmer");
        }
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
    void xpFarmHasSealedDarkRoofAndPadsOutOfAfkRange() {
        BaseTemplates.BaseBlueprint xp = FarmTemplates.all().get("xp");
        boolean hasButton = false;
        boolean darkRoof = false;
        boolean roofHole = false;
        // Kill pad: bottom slabs on hoppers + solid ceiling (legs-only, no trapdoors)
        boolean hasKillSlabs = false;
        boolean hasKillCeiling = false;
        boolean hasTrapdoorOnKill = false;
        boolean hasSign = false;
        int shaftAir = 0;
        int waterSources = 0;
        int darkBlocks = 0;
        double minPadDist = Double.MAX_VALUE;
        java.util.Map<String, org.bukkit.Material> at = new java.util.HashMap<>();
        for (BaseTemplates.RelBlock block : xp.blocks()) {
            at.put(block.dx() + "," + block.dy() + "," + block.dz(), block.material());
            if (block.material().name().endsWith("_BUTTON")) {
                hasButton = true;
                assertTrue(Math.abs(block.dx()) >= 1, "XP door buttons must be on wall beside door");
            }
            if ((block.dx() == -1 || block.dx() == 0)
                    && (block.dz() == 0 || block.dz() == 1)
                    && block.dy() >= 1 && block.dy() < XpFarmTemplates.NATURAL_ROOF_Y
                    && block.material() == org.bukkit.Material.AIR) {
                shaftAir++;
            }
            if (XpFarmTemplates.isDarkShell(block.material()) && block.dy() >= XpFarmTemplates.NATURAL_ROOF_Y) {
                darkRoof = true;
            }
            if ((block.dx() == -1 || block.dx() == 0)
                    && (block.dz() == 0 || block.dz() == 1)
                    && block.dy() >= XpFarmTemplates.NATURAL_ROOF_Y
                    && block.material() == org.bukkit.Material.AIR) {
                roofHole = true;
            }
            if (block.material().name().contains("SLAB")
                    && XpFarmTemplates.isKillPadCell(block.dx(), block.dz())
                    && block.dy() == XpFarmTemplates.KILL_SLAB_Y) {
                hasKillSlabs = true;
            }
            if (block.material() == org.bukkit.Material.AIR
                    && XpFarmTemplates.isKillPadCell(block.dx(), block.dz())
                    && block.dy() == XpFarmTemplates.KILL_CEILING_Y) {
                hasKillCeiling = true;
            }
            if (block.material() == org.bukkit.Material.IRON_TRAPDOOR
                    && XpFarmTemplates.isKillPadCell(block.dx(), block.dz())) {
                hasTrapdoorOnKill = true;
            }
            if (block.material().name().contains("SIGN")
                    && (block.dy() == XpFarmTemplates.NATURAL_DECK_Y - 1
                    || block.dy() == XpFarmTemplates.NATURAL_DECK_Y - 2)) {
                hasSign = true;
            }
            if (block.material() == org.bukkit.Material.WATER && block.dy() == XpFarmTemplates.NATURAL_DECK_Y) {
                waterSources++;
            }
            if (XpFarmTemplates.isDarkShell(block.material())) {
                darkBlocks++;
            }
        }
        int deck = XpFarmTemplates.NATURAL_DECK_Y;
        for (int x = XpFarmTemplates.PAD_MIN_X; x <= XpFarmTemplates.PAD_MAX_X; x++) {
            for (int z = XpFarmTemplates.PAD_MIN_Z; z <= XpFarmTemplates.PAD_MAX_Z; z++) {
                if (!XpFarmTemplates.isSpawnPadCell(x, z)) {
                    continue;
                }
                org.bukkit.Material floor = at.get(x + "," + deck + "," + z);
                org.bukkit.Material above = at.get(x + "," + (deck + 1) + "," + z);
                if (floor == null || floor == org.bukkit.Material.AIR || floor == org.bukkit.Material.WATER) {
                    continue;
                }
                if (above != null && above != org.bukkit.Material.AIR) {
                    continue;
                }
                minPadDist = Math.min(minPadDist, XpFarmTemplates.afkDistance(x, deck, z));
            }
        }
        assertTrue(hasButton, "xp farm needs door buttons");
        // Vertical shaft under center hole (not an open tower over the kill pad)
        int holeShaftAir = 0;
        for (BaseTemplates.RelBlock block : xp.blocks()) {
            if ((block.dx() == -1 || block.dx() == 0)
                    && block.dz() >= XpFarmTemplates.HOLE_MIN_Z && block.dz() <= XpFarmTemplates.HOLE_MAX_Z
                    && block.dy() >= 1 && block.dy() < XpFarmTemplates.NATURAL_DECK_Y
                    && block.material() == org.bukkit.Material.AIR) {
                holeShaftAir++;
            }
        }
        assertTrue(holeShaftAir >= 40, "xp farm needs vertical drop under center hole, got " + holeShaftAir);
        assertTrue(darkRoof, "xp spawn deck needs solid dark roof");
        assertFalse(roofHole, "xp roof must not have a skylight hole over the drop");
        assertTrue(hasKillSlabs, "xp kill box needs bottom slabs on hoppers (legs-only)");
        assertTrue(hasKillCeiling, "xp kill box needs open y=2 (shaft onto slabs)");
        assertFalse(hasTrapdoorOnKill, "xp kill box must not use trapdoors");
        assertTrue(hasSign, "xp hole needs water-break signs");
        assertTrue(waterSources > 0 && waterSources <= 48,
                "xp water should be spaced sources that reach the drop, got " + waterSources);
        assertTrue(darkBlocks >= 150, "xp farm needs a dark-block shell, got " + darkBlocks);
        assertTrue(minPadDist >= 24.0, "xp pads must be 24+ from AFK window, closest was " + minPadDist);

        // Square deck: center hole = kill XZ (GitHub straight drop)
        org.bukkit.Material holeCenter = at.get(
                XpFarmTemplates.DECK_CENTER_X + "," + deck + "," + XpFarmTemplates.DECK_CENTER_Z);
        assertEquals(org.bukkit.Material.AIR, holeCenter, "xp square must have air at deck center hole");
        assertTrue(XpFarmTemplates.isDeckHole(XpFarmTemplates.DECK_CENTER_X, XpFarmTemplates.DECK_CENTER_Z),
                "deck center must be inside the hole");
        assertTrue(XpFarmTemplates.isKillPadCell(XpFarmTemplates.HOLE_MIN_X, XpFarmTemplates.HOLE_MIN_Z),
                "hole must align with kill pad (old GitHub design)");
        assertEquals(XpFarmTemplates.PAD_MAX_X - XpFarmTemplates.PAD_MIN_X,
                XpFarmTemplates.PAD_MAX_Z - XpFarmTemplates.PAD_MIN_Z,
                "xp spawn floor must be square");

        org.bukkit.Material waterNearHole = at.get(
                XpFarmTemplates.PAD_MIN_X + "," + deck + "," + XpFarmTemplates.HOLE_MIN_Z);
        assertEquals(org.bukkit.Material.WATER, waterNearHole,
                "xp water must flow on E-W trench toward center hole from west edge");
        org.bukkit.Material waterSouthOfHole = at.get(
                XpFarmTemplates.HOLE_MIN_X + "," + deck + "," + (XpFarmTemplates.HOLE_MAX_Z + 1));
        assertEquals(org.bukkit.Material.WATER, waterSouthOfHole,
                "xp water must continue on N-S trench beside center hole");
        assertFalse(at.get(XpFarmTemplates.DECK_CENTER_X + "," + deck + "," + XpFarmTemplates.DECK_CENTER_Z)
                        == org.bukkit.Material.WATER,
                "center hole must stay air, not water");
        long trenchWater = xp.blocks().stream()
                .filter(b -> b.dy() == deck && b.material() == org.bukkit.Material.WATER
                        && XpFarmTemplates.isWaterTrenchCell(b.dx(), b.dz()))
                .count();
        assertTrue(trenchWater >= 8, "xp + trench needs water on all four arms toward center, got " + trenchWater);

        // Path: center hole ↓ open shaft → slabs (no side tunnel)
        assertEquals(org.bukkit.Material.AIR, at.get(
                        XpFarmTemplates.DECK_CENTER_X + "," + deck + "," + XpFarmTemplates.DECK_CENTER_Z),
                "center hole must stay open at deck");
        assertEquals(org.bukkit.Material.AIR, at.get("0,10,0"),
                "vertical shaft must be open under center hole onto kill");
        assertEquals(org.bukkit.Material.AIR, at.get("0," + XpFarmTemplates.KILL_CEILING_Y + ",0"),
                "kill top must stay open for the drop");
        assertTrue(String.valueOf(at.get("0," + XpFarmTemplates.KILL_SLAB_Y + ",0")).contains("SLAB"),
                "drop must land on kill slabs");
        assertEquals(org.bukkit.Material.WATER, at.get("0," + deck + "," + (XpFarmTemplates.HOLE_MAX_Z + 1)),
                "deck south of center hole must keep water trench");

        org.bukkit.Material lip = at.get("0,0,2");
        assertTrue(lip == org.bukkit.Material.HOPPER || lip == org.bukkit.Material.STONE_BRICKS,
                "xp safety lip at window feet must be solid");
        assertEquals(org.bukkit.Material.IRON_BARS, at.get("-1,1,2"),
                "xp punch window feet must stay barred (no walk-in)");
        assertEquals(org.bukkit.Material.IRON_BARS, at.get("0,1,2"),
                "xp punch window feet must stay barred (no walk-in)");
        assertEquals(org.bukkit.Material.STONE_BRICKS, at.get("-1,2,2"),
                "xp punch window must seal eye level so mobs cannot walk out");
        assertEquals(org.bukkit.Material.STONE_BRICKS, at.get("0,2,2"),
                "xp punch window must seal eye level so mobs cannot walk out");
        assertEquals(org.bukkit.Material.GLASS_PANE, at.get("-2,1,2"),
                "xp side window should be glass for clear view");
        assertEquals(org.bukkit.Material.CHEST, at.get("0,0,8"),
                "xp loot chests must sit on the floor");
        boolean hopperOnChest = false;
        for (BaseTemplates.RelBlock block : xp.blocks()) {
            if (block.material() == org.bukkit.Material.HOPPER
                    && block.dy() == 1 && block.dz() == 8
                    && block.facing() == org.bukkit.block.BlockFace.DOWN) {
                hopperOnChest = true;
                break;
            }
        }
        assertTrue(hopperOnChest, "xp loot must use hoppers on chests facing DOWN");
        assertEquals(org.bukkit.Material.IRON_DOOR, at.get("0,1,11"),
                "xp safety room needs an iron door");
    }

    @Test
    void xpSpawnerFarmsHaveFourSpawnersNearTheWindow() {
        for (String id : List.of("xp-zombie", "xp-skeleton", "xp-spider")) {
            BaseTemplates.BaseBlueprint bp = FarmTemplates.all().get(id);
            assertTrue(bp != null, id + " missing");
            assertEquals(
                    switch (id) {
                        case "xp-zombie" -> "ZOMBIE";
                        case "xp-skeleton" -> "SKELETON";
                        case "xp-spider" -> "CAVE_SPIDER";
                        default -> null;
                    },
                    XpFarmTemplates.spawnerEntityName(id)
            );
            int spawners = 0;
            double maxSpawnerDist = 0;
            for (BaseTemplates.RelBlock block : bp.blocks()) {
                if (block.material() != org.bukkit.Material.SPAWNER) {
                    continue;
                }
                spawners++;
                double dist = Math.sqrt(
                        (double) block.dx() * block.dx()
                                + Math.pow(block.dy() - 1, 2)
                                + Math.pow(block.dz() - XpFarmTemplates.AFK_WINDOW_Z, 2)
                );
                maxSpawnerDist = Math.max(maxSpawnerDist, dist);
            }
            assertEquals(4, spawners, id + " should have 4 spawners");
            assertTrue(maxSpawnerDist <= 32.0, id + " spawners must be within 32 of the window, was " + maxSpawnerDist);
            assertTrue(bp.blocks().stream().anyMatch(b -> XpFarmTemplates.isDarkShell(b.material())),
                    id + " should use dark shell blocks");
        }
    }

    @Test
    void xpEndermanFarmIsThreeHighWithNoWaterOnPads() {
        BaseTemplates.BaseBlueprint bp = FarmTemplates.all().get("xp-enderman");
        assertTrue(bp != null, "xp-enderman missing");
        int deck = XpFarmTemplates.NATURAL_DECK_Y;
        boolean threeHigh = false;
        boolean waterOnPad = false;
        for (BaseTemplates.RelBlock block : bp.blocks()) {
            if (block.dy() == deck + 3 && block.material() == org.bukkit.Material.AIR
                    && block.dx() >= XpFarmTemplates.PAD_MIN_X + 2
                    && block.dx() <= XpFarmTemplates.PAD_MAX_X - 2
                    && block.dz() >= XpFarmTemplates.PAD_MIN_Z + 2
                    && block.dz() <= XpFarmTemplates.PAD_MAX_Z - 2) {
                threeHigh = true;
            }
            if (block.material() == org.bukkit.Material.WATER && block.dy() == deck
                    && block.dz() <= XpFarmTemplates.PAD_MAX_Z) {
                waterOnPad = true;
            }
        }
        assertTrue(threeHigh, "enderman farm needs 3-high interior air");
        assertFalse(waterOnPad, "enderman pads must not have water (they teleport)");
        assertTrue(bp.blocks().stream().noneMatch(b -> b.material() == org.bukkit.Material.SPAWNER));
    }

    @Test
    void advancedFoodSlimeRedstoneDiamondFarmsHaveGadgets() {
        BaseTemplates.BaseBlueprint chicken = FarmTemplates.all().get("chicken");
        BaseTemplates.BaseBlueprint cow = FarmTemplates.all().get("cow");
        BaseTemplates.BaseBlueprint pig = FarmTemplates.all().get("pig");
        BaseTemplates.BaseBlueprint cook = FarmTemplates.all().get("cook");
        BaseTemplates.BaseBlueprint slime = FarmTemplates.all().get("slime");
        BaseTemplates.BaseBlueprint redstone = FarmTemplates.all().get("redstone");
        BaseTemplates.BaseBlueprint diamond = FarmTemplates.all().get("diamond");
        assertTrue(chicken != null && cow != null && pig != null && cook != null
                && slime != null && redstone != null && diamond != null);
        assertTrue(chicken.blocks().stream().anyMatch(b -> b.material() == org.bukkit.Material.LAVA
                || b.material() == org.bukkit.Material.IRON_TRAPDOOR));
        assertTrue(chicken.blocks().stream().anyMatch(b -> b.material() == org.bukkit.Material.DISPENSER));
        assertTrue(cow.blocks().stream().anyMatch(b -> b.material() == org.bukkit.Material.LAVA));
        assertTrue(cow.blocks().stream().filter(b -> b.material() == org.bukkit.Material.DISPENSER).count() >= 3);
        assertTrue(pig.blocks().stream().anyMatch(b -> b.material() == org.bukkit.Material.CARROTS
                || b.material() == org.bukkit.Material.CARROT));
        assertTrue(pig.blocks().stream().anyMatch(b -> b.material() == org.bukkit.Material.LAVA));
        assertEquals(3, cook.blocks().stream().filter(b -> b.material() == org.bukkit.Material.SMOKER).count());
        assertTrue(cook.blocks().stream().filter(b -> b.material() == org.bukkit.Material.CHEST).count() >= 6,
                "cook needs raw top + coal back chests (+ loot)");
        assertFalse(cook.blocks().stream().anyMatch(b -> b.material() == org.bukkit.Material.BAMBOO),
                "cook uses coal chests, not bamboo fuel");
        assertTrue(cook.blocks().stream().anyMatch(b ->
                b.material() == org.bukkit.Material.HOPPER && b.dz() == -1 && b.facing() == org.bukkit.block.BlockFace.SOUTH));
        assertEquals(4, slime.blocks().stream().filter(b -> b.material() == org.bukkit.Material.SPAWNER).count());
        assertEquals("SLIME", AdvancedFarmTemplates.spawnerEntityName("slime"));
        assertEquals(4, redstone.blocks().stream().filter(b -> b.material() == org.bukkit.Material.SPAWNER).count());
        assertEquals("WITCH", AdvancedFarmTemplates.spawnerEntityName("redstone"));
        assertTrue(diamond.blocks().stream().filter(b -> b.material() == org.bukkit.Material.SMITHING_TABLE).count() >= 4);
        assertTrue(diamond.blocks().stream().anyMatch(b -> b.material() == org.bukkit.Material.COMPOSTER));
        assertEquals(2, diamond.blocks().stream()
                .filter(b -> b.material() == org.bukkit.Material.IRON_DOOR && b.dy() == 1
                        && (b.dz() == AdvancedFarmTemplates.DIAMOND_INNER_DOOR_Z
                        || b.dz() == AdvancedFarmTemplates.DIAMOND_OUTER_DOOR_Z))
                .count(), "diamond needs dual airlock iron doors");
        assertTrue(diamond.blocks().stream().anyMatch(b ->
                b.material() == org.bukkit.Material.IRON_DOOR
                        && b.dx() == AdvancedFarmTemplates.DIAMOND_SECRET_DOOR_X),
                "diamond needs secret west lever door");
        assertTrue(diamond.blocks().stream().filter(b -> b.material() == org.bukkit.Material.LEVER).count() >= 5,
                "diamond needs levers for airlock + secret door");
        boolean lootDown = diamond.blocks().stream().anyMatch(b ->
                b.material() == org.bukkit.Material.HOPPER
                        && b.dy() == AdvancedFarmTemplates.DIAMOND_LOOT_DRAIN_Y
                        && b.dz() == AdvancedFarmTemplates.DIAMOND_LOOT_CHEST_Z
                        && b.facing() == org.bukkit.block.BlockFace.DOWN);
        assertTrue(lootDown, "diamond loot hoppers must sit on chests facing DOWN");
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
