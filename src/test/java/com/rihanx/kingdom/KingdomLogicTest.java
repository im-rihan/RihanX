package com.rihanx.kingdom;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KingdomBoundsTest {

    @Test
    void containsInclusiveSquareFootprint() {
        KingdomBounds bounds = new KingdomBounds("world", 0, 0, 10, -64, 320);
        assertTrue(bounds.contains(0, 64, 0));
        assertTrue(bounds.contains(10, 64, 10));
        assertTrue(bounds.contains(-10, -64, -10));
        assertFalse(bounds.contains(11, 64, 0));
        assertFalse(bounds.contains(0, 321, 0));
        assertFalse(bounds.contains(0, -65, 0));
    }

    @Test
    void fluidWouldCrossOnAnyFace() {
        KingdomBounds bounds = new KingdomBounds("world", 0, 0, 8, 0, 100);
        assertTrue(bounds.fluidWouldCross(8, 50, 0, 9, 50, 0));
        assertTrue(bounds.fluidWouldCross(0, 50, 0, 0, 101, 0));
        assertFalse(bounds.fluidWouldCross(1, 10, 1, 2, 10, 2));
    }

    @Test
    void expandGrowsRadiusWithoutMovingCenter() {
        KingdomBounds bounds = new KingdomBounds("world", 100, -20, 32, -64, 320);
        KingdomBounds grown = bounds.expanded(16);
        assertEquals(48, grown.radius());
        assertEquals(100, grown.centerX());
        assertEquals(-20, grown.centerZ());
        assertEquals(52, grown.minX());
    }

    @Test
    void overlapsDetectsSharedFootprint() {
        KingdomBounds a = new KingdomBounds("world", 0, 0, 20, 0, 100);
        KingdomBounds b = new KingdomBounds("world", 30, 0, 20, 0, 100);
        KingdomBounds c = new KingdomBounds("other", 0, 0, 20, 0, 100);
        assertTrue(a.overlaps(b));
        assertFalse(a.overlaps(c));
    }

    @Test
    void gateAnchorSitsOnNamedFace() {
        KingdomBounds bounds = new KingdomBounds("world", 0, 0, 16, -64, 320);
        int[] north = bounds.gateAnchor(70, GateSide.NORTH);
        assertEquals(0, north[0]);
        assertEquals(-16, north[2]);
        int[] above = bounds.gateAnchor(70, GateSide.ABOVE);
        assertEquals(320, above[1]);
    }

    @Test
    void towerAnchorsSitInsideCorners() {
        KingdomBounds bounds = new KingdomBounds("world", 0, 0, 32, -64, 320);
        int[] nw = bounds.towerAnchor(TowerType.NORTH_WEST, 3);
        assertEquals(-29, nw[0]);
        assertEquals(-29, nw[1]);
        int[] central = bounds.towerAnchor(TowerType.CENTRAL, 3);
        assertEquals(0, central[0]);
        assertEquals(0, central[1]);
    }
}

class KingdomRoleTest {

    @Test
    void ranksAndAliases() {
        assertTrue(KingdomRole.SOVEREIGN.canManage());
        assertTrue(KingdomRole.WARDEN.canInvite());
        assertTrue(KingdomRole.CITIZEN.canBuild());
        assertFalse(KingdomRole.GUEST.canBuild());
        assertFalse(KingdomRole.GUEST.receivesAlerts());
        assertSame(KingdomRole.SOVEREIGN, KingdomRole.fromKey("king"));
        assertSame(KingdomRole.WARDEN, KingdomRole.fromKey("guard"));
        assertNull(KingdomRole.fromKey("peasant"));
        assertTrue(KingdomRole.WARDEN.atLeast(KingdomRole.CITIZEN));
    }
}

class WardLayerTest {

    @Test
    void fromKeyResolvesAliases() {
        assertEquals(WardLayer.OUTER, WardLayer.fromKey("boundary"));
        assertEquals(WardLayer.ELEMENTAL, WardLayer.fromKey("water"));
        assertEquals(WardLayer.SKY, WardLayer.fromKey("ceiling"));
        assertEquals(WardLayer.DEEP, WardLayer.fromKey("foundation"));
        assertEquals(WardLayer.INNER, WardLayer.fromKey("sanctum"));
        assertEquals(WardLayer.BEACON, WardLayer.fromKey("spire"));
        assertNull(WardLayer.fromKey("nope"));
        assertEquals(WardLayer.values().length, WardLayer.keys().length);
    }
}

class BeaconColorTest {

    @Test
    void parsesHexAndRainbow() {
        BeaconColor copper = BeaconColor.parse("#B87333");
        assertEquals(0xB87333, copper.rgb());
        assertFalse(copper.rainbow());
        assertEquals("#B87333", copper.hex());
        assertTrue(BeaconColor.parse("rainbow").rainbow());
        assertTrue(BeaconColor.isValidHex("#50C"));
        assertFalse(BeaconColor.isValidHex("not-a-color"));
    }

    @Test
    void rainbowCyclesHue() {
        BeaconColor rainbow = new BeaconColor(0xB87333, true);
        int a = rainbow.rgbAtTick(0, 20);
        int b = rainbow.rgbAtTick(10, 20);
        assertNotEquals(a, b);
        assertEquals(0xB87333, new BeaconColor(0xB87333, false).rgbAtTick(99, 20));
    }
}

class DurationParserTest {

    @Test
    void parsesCompactUnits() {
        assertEquals(10_000L, DurationParser.millis("10s", 0));
        assertEquals(120_000L, DurationParser.millis("2m", 0));
        assertEquals(500L, DurationParser.millis("500ms", 0));
        assertEquals(3_600_000L, DurationParser.millis("1h", 0));
        assertEquals(5_000L, DurationParser.millis(null, 5_000L));
        assertEquals(-1L, DurationParser.millis("nope", 0));
        assertEquals("10s", DurationParser.format(10_000L));
    }
}

class KingdomGateLogicTest {

    @Test
    void openCloseAndPassageWindow() {
        KingdomGate gate = new KingdomGate("NorthGate", GateSide.NORTH, 0, 64, -32);
        long now = 1_000_000L;
        assertFalse(gate.isOpen(now));
        gate.openFor(10_000L, now);
        assertTrue(gate.isOpen(now + 5_000L));
        assertTrue(gate.allowsPassage(0, 64, -32, now + 1_000L));
        assertFalse(gate.allowsPassage(8, 64, -32, now + 1_000L));
        gate.close();
        assertFalse(gate.isOpen(now + 1_000L));
    }
}

class KingdomMembershipTest {

    @Test
    void sovereignInviteKickAndRoles() {
        UUID owner = UUID.randomUUID();
        UUID citizen = UUID.randomUUID();
        UUID guest = UUID.randomUUID();
        Kingdom kingdom = new Kingdom(
                "Camelot",
                "Camelot",
                owner,
                new KingdomBounds("world", 0, 0, 32, -64, 320)
        );
        assertEquals(KingdomRole.SOVEREIGN, kingdom.roleOf(owner));
        kingdom.setRole(citizen, KingdomRole.CITIZEN);
        kingdom.setRole(guest, KingdomRole.GUEST);
        assertTrue(kingdom.canBuild(citizen));
        assertFalse(kingdom.canBuild(guest));
        assertFalse(kingdom.kick(owner));
        assertTrue(kingdom.kick(guest));
        assertFalse(kingdom.isMember(guest));
        kingdom.sealAll();
        kingdom.unseal(WardLayer.ELEMENTAL);
        assertFalse(kingdom.isSealed(WardLayer.ELEMENTAL));
        assertTrue(kingdom.isSealed(WardLayer.OUTER));
    }

    @Test
    void innerRulesDefaultToSealedCitadel() {
        KingdomRules rules = new KingdomRules();
        assertFalse(rules.pvp());
        assertFalse(rules.mobSpawn());
        assertFalse(rules.fireSpread());
        assertFalse(rules.explosions());
        assertTrue(rules.toggle("pvp"));
        assertTrue(rules.pvp());
    }
}
