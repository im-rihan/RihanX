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
                c.layer() == RailPathLogic.Layer.BED && c.x() == 0 && c.z() == 0));
        assertFalse(plan.cells().stream().anyMatch(c ->
                c.layer() == RailPathLogic.Layer.BED && c.x() == 10 && c.z() == 6));
        assertTrue(plan.cells().stream().anyMatch(c -> c.layer() == RailPathLogic.Layer.POWERED));
        assertTrue(plan.cells().stream().anyMatch(c -> c.layer() == RailPathLogic.Layer.RAIL));
    }

    @Test
    void stationLinkJoinsBothEndsNotOnlyDestination() {
        // Both stations faced south (yaw 0) when pasted; village is south of hometown.
        // Hometown at (0,64,0), village at (0,64,40).
        RailPathLogic.Plan plan = RailPathLogic.planStationLink(
                0, 64, 0, 0f,
                0, 64, 40, 0f,
                512, 4, 8, 3
        );
        assertEquals(RailPathLogic.PlanResult.OK, plan.result());
        Set<String> beds = plan.cells().stream()
                .filter(c -> c.layer() == RailPathLogic.Layer.BED)
                .map(c -> c.x() + "," + c.z())
                .collect(Collectors.toSet());

        // Join back into hometown (north of pad, opposite of south front)
        assertTrue(beds.contains("0,-3") || beds.contains("0,-8"),
                "hometown join spur missing: " + beds);
        // Exit south from hometown toward village
        assertTrue(beds.contains("0,3"), "hometown exit spur missing: " + beds);
        // Join back into village (north of village pad)
        assertTrue(beds.contains("0,37") || beds.contains("0,32"),
                "village join spur missing: " + beds);
        // Exit north from village toward hometown (pickExit may use opposite of front)
        assertTrue(beds.contains("0,37") || beds.contains("0,43") || beds.contains("0,40"),
                "village approach missing: " + beds);
        // Pads themselves not overwritten
        assertFalse(beds.contains("0,0"));
        assertFalse(beds.contains("0,40"));
    }

    @Test
    void pickExitPrefersFrontTowardDestination() {
        assertEquals(RailPathLogic.Cardinal.SOUTH,
                RailPathLogic.pickExit(RailPathLogic.Cardinal.SOUTH, 0, 10));
        assertEquals(RailPathLogic.Cardinal.NORTH,
                RailPathLogic.pickExit(RailPathLogic.Cardinal.SOUTH, 0, -10));
    }

    @Test
    void rejectsTooFarAndTooShort() {
        assertEquals(RailPathLogic.PlanResult.TOO_FAR,
                RailPathLogic.plan(0, 64, 0, 300, 64, 300, 100, 4, 1, true).result());
        assertEquals(RailPathLogic.PlanResult.TOO_SHORT,
                RailPathLogic.plan(0, 64, 0, 0, 64, 0, 512, 4, 1, true).result());
        assertEquals(RailPathLogic.PlanResult.VERTICAL_ONLY,
                RailPathLogic.plan(0, 64, 0, 0, 70, 0, 512, 4, 1, true).result());
        assertEquals(RailPathLogic.PlanResult.TOO_SHORT,
                RailPathLogic.plan(0, 64, 0, 1, 64, 0, 512, 4, 1, true).result());
    }

    @Test
    void slopesHeightAlongPath() {
        RailPathLogic.Plan plan = RailPathLogic.plan(
                0, 60, 0,
                20, 70, 0,
                512, 4, 1, true
        );
        assertEquals(RailPathLogic.PlanResult.OK, plan.result());
        int minY = plan.cells().stream()
                .filter(c -> c.layer() == RailPathLogic.Layer.BED)
                .mapToInt(RailPathLogic.Cell::y)
                .min().orElse(-1);
        int maxY = plan.cells().stream()
                .filter(c -> c.layer() == RailPathLogic.Layer.BED)
                .mapToInt(RailPathLogic.Cell::y)
                .max().orElse(-1);
        assertTrue(minY >= 60);
        assertTrue(maxY <= 70);
        assertTrue(maxY > minY);
    }

    @Test
    void horizontalDistanceIsManhattan() {
        assertEquals(16, RailPathLogic.horizontalDistance(0, 0, 10, 6));
    }
}
