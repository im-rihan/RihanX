package com.rihanx.kingdom;

import com.rihanx.base.BaseTemplates;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Creation-path tests: founding rules, five-tower layout, palette, and undo LIFO.
 */
class KingdomCreationTest {

    @Test
    void foundingRejectsBadNames() {
        assertFalse(KingdomFounding.validName("ab"));
        assertFalse(KingdomFounding.validName("Camelot!"));
        assertFalse(KingdomFounding.validName(""));
        assertFalse(KingdomFounding.validName(null));
        assertTrue(KingdomFounding.validName("Camelot"));
        assertTrue(KingdomFounding.validName("Old_Kingdom-1"));
        assertEquals("camelot", KingdomFounding.normalizeId("Camelot"));
    }

    @Test
    void foundingRejectsRadiusOutsideRange() {
        assertEquals(-1, KingdomFounding.resolveRadius(8, 128, 16, 256));
        assertEquals(-1, KingdomFounding.resolveRadius(512, 128, 16, 256));
        assertEquals(128, KingdomFounding.resolveRadius(null, 128, 16, 256));
        assertEquals(64, KingdomFounding.resolveRadius(64, 128, 16, 256));
    }

    @Test
    void foundingRejectsOverlapAndAllowsTouchingWorlds() {
        KingdomBounds a = new KingdomBounds("world", 0, 0, 32, -64, 320);
        KingdomBounds overlap = new KingdomBounds("world", 40, 0, 16, -64, 320);
        KingdomBounds far = new KingdomBounds("world", 200, 200, 16, -64, 320);
        KingdomBounds otherWorld = new KingdomBounds("nether", 0, 0, 32, -64, 320);
        assertTrue(KingdomFounding.overlapsAny(overlap, List.of(a)));
        assertFalse(KingdomFounding.overlapsAny(far, List.of(a)));
        assertFalse(KingdomFounding.overlapsAny(otherWorld, List.of(a)));
    }

    @Test
    void validateCreateCoversEveryFailure() {
        KingdomBounds candidate = new KingdomBounds("world", 0, 0, 32, -64, 320);
        List<KingdomBounds> none = List.of();

        assertEquals(KingdomFounding.Failure.NAME_INVALID, KingdomFounding.validateCreate(
                "x", 32, 0, 3, false, 128, 16, 256, false, candidate, none).failure());
        assertEquals(KingdomFounding.Failure.EXISTS, KingdomFounding.validateCreate(
                "Camelot", 32, 0, 3, false, 128, 16, 256, true, candidate, none).failure());
        assertEquals(KingdomFounding.Failure.LIMIT, KingdomFounding.validateCreate(
                "Camelot", 32, 3, 3, false, 128, 16, 256, false, candidate, none).failure());
        assertTrue(KingdomFounding.validateCreate(
                "Camelot", 32, 3, 3, true, 128, 16, 256, false, candidate, none).ok());
        assertEquals(KingdomFounding.Failure.RADIUS, KingdomFounding.validateCreate(
                "Camelot", 8, 0, 3, false, 128, 16, 256, false, candidate, none).failure());
        assertEquals(KingdomFounding.Failure.OVERLAP, KingdomFounding.validateCreate(
                "Camelot", 32, 0, 3, false, 128, 16, 256, false, candidate, List.of(candidate)).failure());
        assertTrue(KingdomFounding.validateCreate(
                "Camelot", 32, 0, 3, false, 128, 16, 256, false, candidate, none).ok());
    }

    @Test
    void createPlansExactlyFiveTowersInsideTheFootprint() {
        Kingdom kingdom = camelot(128, 64);
        KingdomStructurePlanner.registerDefaultTowers(kingdom, 3, 64);
        assertEquals(5, kingdom.towers().size());
        assertNotNull(kingdom.tower(TowerType.CENTRAL));
        assertNotNull(kingdom.tower(TowerType.NORTH_EAST));
        assertNotNull(kingdom.tower(TowerType.NORTH_WEST));
        assertNotNull(kingdom.tower(TowerType.SOUTH_EAST));
        assertNotNull(kingdom.tower(TowerType.SOUTH_WEST));

        KingdomBounds bounds = kingdom.bounds();
        for (KingdomTower tower : kingdom.towers().values()) {
            assertTrue(bounds.containsHorizontal(tower.x(), tower.z()), tower.type() + " outside footprint");
            assertTrue(tower.height() >= 128, tower.type() + " too short");
        }
        assertEquals(kingdom.towerHeight() + 16, kingdom.tower(TowerType.CENTRAL).height());
        assertEquals("Camelot-Central", kingdom.tower(TowerType.CENTRAL).name(kingdom.displayName()));
        assertEquals("Camelot-NorthEast", kingdom.tower(TowerType.NORTH_EAST).name(kingdom.displayName()));
    }

    @Test
    void towerPlanUsesOldKingdomPaletteAndCrownsTheSpire() {
        Kingdom kingdom = camelot(48, 70);
        KingdomStructurePlanner.registerDefaultTowers(kingdom, 3, 70);
        List<KingdomStructurePlanner.Planned> plan = KingdomStructurePlanner.planKingdom(kingdom, 3, 32, true, 320);
        assertFalse(plan.isEmpty());
        assertTrue(plan.size() > 500, "expected a full citadel, got " + plan.size());

        Set<String> materials = new HashSet<>();
        boolean sea = false;
        boolean emerald = false;
        boolean lantern = false;
        boolean soul = false;
        boolean copper = false;
        boolean barrel = false;
        for (KingdomStructurePlanner.Planned block : plan) {
            materials.add(block.material());
            assertTrue(
                    KingdomStructurePlanner.isAllowedMaterial(block.material()),
                    "off-palette " + block.material()
            );
            assertTrue(kingdom.bounds().containsHorizontal(block.x(), block.z()), "block outside kingdom");
            if (block.material().equals("SEA_LANTERN")) {
                sea = true;
            }
            if (block.material().equals("EMERALD_BLOCK")) {
                emerald = true;
            }
            if (block.material().equals("LANTERN") || block.material().equals("IRON_BARS")) {
                lantern = true;
            }
            if (block.material().equals("SOUL_LANTERN")) {
                soul = true;
            }
            if (block.material().equals("WAXED_COPPER_BLOCK")) {
                copper = true;
            }
            if (block.material().equals("BARREL")) {
                barrel = true;
            }
        }
        assertTrue(sea, "missing sea-lantern crown");
        assertTrue(emerald, "missing emerald beacon cap");
        assertTrue(lantern, "missing deck railings");
        assertTrue(soul, "missing soul-lantern corners");
        assertTrue(copper, "missing copper accents");
        assertTrue(barrel, "missing treasury barrel");
        assertTrue(materials.contains("STONE_BRICKS") || materials.contains("MOSSY_STONE_BRICKS"));
        assertTrue(materials.contains("LADDER"));
    }

    @Test
    void eachTowerHasDecksEveryThirtyTwoBlocks() {
        Kingdom kingdom = camelot(32, 80);
        KingdomStructurePlanner.registerDefaultTowers(kingdom, 3, 80);
        KingdomTower central = kingdom.tower(TowerType.CENTRAL);
        assertNotNull(central);
        List<KingdomStructurePlanner.Planned> plan = KingdomStructurePlanner.planTower(central, 32, true, 320);
        Set<Integer> lanternYs = new HashSet<>();
        for (KingdomStructurePlanner.Planned block : plan) {
            if (block.material().equals("LANTERN") || block.material().equals("SOUL_LANTERN")
                    || block.material().equals("IRON_BARS")) {
                lanternYs.add(block.y());
            }
        }
        assertFalse(lanternYs.isEmpty());
        int top = Math.min(318, central.topY());
        assertTrue(plan.stream().anyMatch(b -> b.y() == top && b.material().equals("SEA_LANTERN")));
        assertTrue(plan.stream().anyMatch(b -> b.y() == top + 1 && b.material().equals("EMERALD_BLOCK")));
    }

    @Test
    void gatePlanPlacesPairedLanternsAndBanner() {
        KingdomGate gate = new KingdomGate("North", GateSide.NORTH, 0, 64, -32);
        List<KingdomStructurePlanner.Planned> plan = KingdomStructurePlanner.planGateLanterns(gate, "ORANGE_BANNER");
        assertEquals(6, plan.size());
        assertTrue(plan.stream().anyMatch(b -> b.material().equals("LANTERN")));
        assertTrue(plan.stream().anyMatch(b -> b.material().equals("WAXED_COPPER_BULB")));
        assertTrue(plan.stream().anyMatch(b -> b.material().equals("ORANGE_BANNER")));
    }

    @Test
    void everyVanillaBannerColorIsAllowedOnGates() {
        assertEquals(16, KingdomStructurePlanner.BANNERS.size());
        KingdomGate gate = new KingdomGate("West", GateSide.WEST, -32, 70, 0);
        for (String banner : KingdomStructurePlanner.BANNERS) {
            assertTrue(KingdomStructurePlanner.isAllowedMaterial(banner), banner);
            List<KingdomStructurePlanner.Planned> plan = KingdomStructurePlanner.planGateLanterns(gate, banner);
            assertTrue(plan.stream().anyMatch(b -> b.material().equals(banner)), banner);
        }
        assertTrue(KingdomStructurePlanner.isAllowedMaterial("LIME_WALL_BANNER"));
        assertTrue(KingdomStructurePlanner.isAllowedMaterial("BLACK_BANNER"));
    }

    @Test
    void undoStackIsLifoAndDropsOldestPastMax() {
        KingdomUndoStack stack = new KingdomUndoStack(2);
        UUID player = UUID.randomUUID();
        UUID world = UUID.randomUUID();
        stack.push(player, session("towers", "camelot", world, 1));
        stack.push(player, session("drain", "camelot", world, 2));
        stack.push(player, session("gate", "camelot", world, 3));
        assertEquals(2, stack.depth(player));
        KingdomUndoStack.Session top = stack.pop(player);
        assertNotNull(top);
        assertEquals("gate", top.tool());
        KingdomUndoStack.Session next = stack.pop(player);
        assertNotNull(next);
        assertEquals("drain", next.tool());
        assertNull(stack.pop(player));
        assertTrue(stack.isEmpty(player));
    }

    @Test
    void undoSnapshotsRememberOriginalMaterials() {
        List<KingdomUndoStack.Snapshot> snapshots = List.of(
                new KingdomUndoStack.Snapshot(0, 64, 0, "GRASS_BLOCK"),
                new KingdomUndoStack.Snapshot(0, 65, 0, "AIR")
        );
        KingdomUndoStack.Session session = new KingdomUndoStack.Session(
                "towers",
                "camelot",
                UUID.randomUUID(),
                snapshots
        );
        assertEquals(2, session.size());
        assertEquals("GRASS_BLOCK", session.snapshots().get(0).beforeMaterial());
        assertEquals("AIR", session.snapshots().get(1).beforeMaterial());
    }

    @Test
    void sealedByDefaultAfterFounding() {
        Kingdom kingdom = camelot(16, 64);
        for (WardLayer layer : WardLayer.values()) {
            assertTrue(kingdom.isSealed(layer), layer + " should start sealed");
        }
        assertEquals(KingdomRole.SOVEREIGN, kingdom.roleOf(kingdom.sovereign()));
        assertFalse(kingdom.rules().pvp());
        assertFalse(kingdom.rules().mobSpawn());
        assertFalse(kingdom.rules().explosions());
    }

    @Test
    void citadelKeepUsesOldKingdomPalette() {
        BaseTemplates.BaseBlueprint citadel = BaseTemplates.all().get("citadel");
        assertNotNull(citadel);
        assertEquals("citadel", citadel.id());
        assertTrue(citadel.blocks().size() > 400, "keep too small: " + citadel.blocks().size());

        boolean stone = false;
        boolean copper = false;
        boolean soul = false;
        boolean door = false;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BaseTemplates.RelBlock block : citadel.blocks()) {
            minX = Math.min(minX, block.dx());
            maxX = Math.max(maxX, block.dx());
            minZ = Math.min(minZ, block.dz());
            maxZ = Math.max(maxZ, block.dz());
            if (block.material() == Material.STONE_BRICKS || block.material() == Material.MOSSY_STONE_BRICKS) {
                stone = true;
            }
            if (block.material() == Material.WAXED_COPPER_BLOCK) {
                copper = true;
            }
            if (block.material() == Material.SOUL_LANTERN) {
                soul = true;
            }
            if (block.material() == Material.IRON_DOOR) {
                door = true;
            }
            assertFalse(block.material() == Material.LANTERN, "citadel should use soul lanterns, not regular lanterns");
            assertFalse(block.material() == Material.COBBLESTONE, "citadel should retint cobble to copper");
        }
        assertTrue(stone, "missing stone bricks");
        assertTrue(copper, "missing copper accents");
        assertTrue(soul, "missing soul lanterns");
        assertTrue(door, "missing iron door");
        int half = Math.max(Math.max(-minX, maxX), Math.max(-minZ, maxZ));
        assertTrue(half <= KingdomStructurePlanner.CITADEL_KEEP_CLEARANCE,
                "keep half-extent " + half + " exceeds clearance");
    }

    @Test
    void cornerTowersStayOutsideTheCitadelKeep() {
        Kingdom kingdom = camelot(128, 64);
        int inset = Math.min(3, Math.max(0, 128 - KingdomStructurePlanner.CITADEL_KEEP_CLEARANCE));
        KingdomStructurePlanner.registerDefaultTowers(kingdom, inset, 64, false);
        assertEquals(4, kingdom.towers().size());
        assertNull(kingdom.tower(TowerType.CENTRAL));

        int keepExtent = 0;
        for (BaseTemplates.RelBlock block : BaseTemplates.all().get("citadel").blocks()) {
            keepExtent = Math.max(keepExtent, Math.abs(block.dx()));
            keepExtent = Math.max(keepExtent, Math.abs(block.dz()));
        }
        final int keepHalf = keepExtent;
        int towerHalf = 2;
        for (KingdomTower tower : kingdom.towers().values()) {
            int nearest = Math.min(Math.abs(tower.x()) - towerHalf, Math.abs(tower.z()) - towerHalf);
            assertTrue(
                    Math.abs(tower.x()) - towerHalf > keepHalf || Math.abs(tower.z()) - towerHalf > keepHalf,
                    tower.type() + " at " + tower.x() + "," + tower.z() + " overlaps keep half " + keepHalf
            );
            assertTrue(nearest >= 0, tower.type() + " collapsed onto origin");
        }

        List<KingdomStructurePlanner.Planned> plan = KingdomStructurePlanner.planKingdom(kingdom, inset, 32, true, 320);
        assertTrue(plan.size() > 400, "corner towers too small: " + plan.size());
        assertTrue(plan.stream().anyMatch(b -> b.material().equals("SEA_LANTERN")));
        assertTrue(plan.stream().noneMatch(b ->
                Math.abs(b.x()) <= keepHalf && Math.abs(b.z()) <= keepHalf
        ), "tower masonry must not punch through the keep footprint");
    }

    private static @org.jetbrains.annotations.NotNull Kingdom camelot(int radius, int groundY) {
        UUID owner = UUID.randomUUID();
        Kingdom kingdom = new Kingdom(
                "camelot",
                "Camelot",
                owner,
                new KingdomBounds("world", 0, 0, radius, -64, 320)
        );
        kingdom.setTowerHeight(128);
        return kingdom;
    }

    private static @org.jetbrains.annotations.NotNull KingdomUndoStack.Session session(
            @org.jetbrains.annotations.NotNull String tool,
            @org.jetbrains.annotations.NotNull String id,
            @org.jetbrains.annotations.NotNull UUID world,
            int marker
    ) {
        return new KingdomUndoStack.Session(
                tool,
                id,
                world,
                List.of(new KingdomUndoStack.Snapshot(marker, 64, 0, "AIR"))
        );
    }
}
