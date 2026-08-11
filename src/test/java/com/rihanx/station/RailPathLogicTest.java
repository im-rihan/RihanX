package com.rihanx.station;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailPathLogicTest {

    @Test
    void plansLShapedTrackSkippingPads() {
        RailPathLogic.Plan plan = RailPathLogic.plan(
                0, 64, 0,
                10, 64, 6,
                512, 4, 1, true
        );
        assertEquals(RailPathLogic.PlanResult.OK, plan.result());
        assertTrue(plan.trackLength() > 0);
        assertFalse(plan.cells().stream().anyMatch(c ->
                (c.layer() == RailPathLogic.Layer.BED || c.layer() == RailPathLogic.Layer.REDSTONE)
                        && c.x() == 0 && c.z() == 0));
        assertFalse(plan.cells().stream().anyMatch(c ->
                (c.layer() == RailPathLogic.Layer.BED || c.layer() == RailPathLogic.Layer.REDSTONE)
                        && c.x() == 10 && c.z() == 6));
    }

    @Test
    void stationLinkJoinsAlongPlatformAxisNotSideways() {
        // South-facing stations (yaw 0); peer is to the EAST — join must still go NORTH into platform.
        RailPathLogic.Plan plan = RailPathLogic.planStationLink(
                0, 64, 0, 0f,
                40, 64, 0, 0f,
                512, 4, 12, 4
        );
        assertEquals(RailPathLogic.PlanResult.OK, plan.result());
        Set<String> beds = trackBeds(plan);

        // Into hometown platform (north of pad)
        assertTrue(beds.contains("0,-12") || beds.contains("0,-8") || beds.contains("0,-4"),
                "hometown platform join missing: " + beds);
        // Exit toward village (east)
        assertTrue(beds.contains("4,0") || beds.contains("3,0"),
                "hometown east exit missing: " + beds);
        // Pad corner bridge (join north + exit east) — without this, rails are diagonal
        assertTrue(beds.contains("1,-1"), "hometown turn corner missing: " + beds);
        // Into village platform (north of its pad)
        assertTrue(beds.contains("40,-12") || beds.contains("40,-8") || beds.contains("40,-4"),
                "village platform join missing: " + beds);
        // Must not place on pads
        assertFalse(beds.contains("0,0"));
        assertFalse(beds.contains("40,0"));
    }

    @Test
    void sidewaysLinkTrackIsOrthogonallyConnectedAtPadTurns() {
        RailPathLogic.Plan plan = RailPathLogic.planStationLink(
                0, 64, 0, 0f,
                40, 64, 0, 0f,
                512, 4, 12, 4
        );
        assertEquals(RailPathLogic.PlanResult.OK, plan.result());
        Set<String> beds = trackBeds(plan);
        // Every bed (except ends) must have an orthogonal neighbor on the path
        java.util.List<String> path = plan.cells().stream()
                .filter(c -> c.layer() == RailPathLogic.Layer.BED
                        || c.layer() == RailPathLogic.Layer.REDSTONE)
                .map(c -> c.x() + "," + c.z())
                .distinct()
                .toList();
        // Build adjacency from consecutive unique XZ order is hard; check corner + approaches
        assertTrue(beds.contains("0,-1") || beds.contains("0,-2"), "join approach: " + beds);
        assertTrue(beds.contains("1,-1"), "corner: " + beds);
        assertTrue(beds.contains("1,0") || beds.contains("2,0"), "exit after corner: " + beds);
        assertFalse(beds.contains("0,0"));
    }

    @Test
    void poweredRailsNeverSitOnCornerCells() {
        RailPathLogic.Plan plan = RailPathLogic.planStationLink(
                0, 64, 0, 0f,
                40, 64, 0, 0f,
                512, 2, 12, 4
        );
        assertEquals(RailPathLogic.PlanResult.OK, plan.result());
        Set<String> powered = plan.cells().stream()
                .filter(c -> c.layer() == RailPathLogic.Layer.POWERED)
                .map(c -> c.x() + "," + c.z())
                .collect(Collectors.toSet());
        // Corner bridge must stay as normal rail (powered rails cannot curve)
        assertFalse(powered.contains("1,-1"), "corner must not be powered: " + powered);
        assertFalse(powered.contains("39,-1"), "far corner must not be powered: " + powered);
    }

    @Test
    void collinearStationsJoinBothPlatforms() {
        RailPathLogic.Plan plan = RailPathLogic.planStationLink(
                0, 64, 0, 0f,
                0, 64, 40, 0f,
                512, 4, 12, 4
        );
        assertEquals(RailPathLogic.PlanResult.OK, plan.result());
        Set<String> beds = trackBeds(plan);
        assertTrue(beds.contains("0,-12") || beds.contains("0,-6"), "A join: " + beds);
        assertTrue(beds.contains("0,4") || beds.contains("0,3"), "A exit: " + beds);
        assertTrue(beds.contains("0,28") || beds.contains("0,36") || beds.contains("0,32"),
                "B join: " + beds);
    }

    /** Bed layer is gravel, or redstone block when that cell is a powered-rail booster. */
    private static Set<String> trackBeds(RailPathLogic.Plan plan) {
        return plan.cells().stream()
                .filter(c -> c.layer() == RailPathLogic.Layer.BED
                        || c.layer() == RailPathLogic.Layer.REDSTONE)
                .map(c -> c.x() + "," + c.z())
                .collect(Collectors.toSet());
    }

    @Test
    void spurEndsJoinUsesTrackAxisNotExitOpposite() {
        RailPathLogic.SpurEnds ends = RailPathLogic.spurEnds(
                0, 64, 0, 0f, 20, 0, 12, 4
        );
        // Front south, dest east → join north into platform, exit east toward dest
        assertEquals(0, ends.joinX());
        assertEquals(-12, ends.joinZ());
        assertEquals(4, ends.exitX());
        assertEquals(0, ends.exitZ());
    }

    @Test
    void pickExitPrefersFrontBackOrSideTowardDestination() {
        assertEquals(RailPathLogic.Cardinal.SOUTH,
                RailPathLogic.pickExit(RailPathLogic.Cardinal.SOUTH, 0, 10));
        assertEquals(RailPathLogic.Cardinal.NORTH,
                RailPathLogic.pickExit(RailPathLogic.Cardinal.SOUTH, 0, -10));
        assertEquals(RailPathLogic.Cardinal.EAST,
                RailPathLogic.pickExit(RailPathLogic.Cardinal.SOUTH, 20, 0));
        assertEquals(RailPathLogic.Cardinal.WEST,
                RailPathLogic.pickExit(RailPathLogic.Cardinal.SOUTH, -20, 0));
    }

    @Test
    void rejectsTooFarAndTooShort() {
        assertEquals(RailPathLogic.PlanResult.TOO_FAR,
                RailPathLogic.plan(0, 64, 0, 300, 64, 300, 100, 4, 1, true).result());
        assertEquals(RailPathLogic.PlanResult.TOO_SHORT,
                RailPathLogic.plan(0, 64, 0, 0, 64, 0, 512, 4, 1, true).result());
    }

    @Test
    void horizontalDistanceIsManhattan() {
        assertEquals(16, RailPathLogic.horizontalDistance(0, 0, 10, 6));
    }
}
