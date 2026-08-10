package com.rihanx.station;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StationLinkLogicTest {

    @Test
    void normalizeTrimsAndLowercases() {
        assertEquals("hometown", StationLinkLogic.normalize("  HomeTown "));
        assertEquals("", StationLinkLogic.normalize("   "));
    }

    @Test
    void canRegisterRejectsEmptyAndDuplicates() {
        Set<String> existing = Set.of("north");
        assertEquals(StationLinkLogic.RegisterResult.EMPTY_NAME,
                StationLinkLogic.canRegister(existing, "  "));
        assertEquals(StationLinkLogic.RegisterResult.EXISTS,
                StationLinkLogic.canRegister(existing, "NORTH"));
        assertEquals(StationLinkLogic.RegisterResult.OK,
                StationLinkLogic.canRegister(existing, "south"));
    }

    @Test
    void canLinkRequiresTwoDistinctExistingStops() {
        Set<String> existing = Set.of("north", "south");
        assertEquals(StationLinkLogic.LinkResult.OK,
                StationLinkLogic.canLink(existing, "north", "south"));
        assertEquals(StationLinkLogic.LinkResult.SELF,
                StationLinkLogic.canLink(existing, "north", "North"));
        assertEquals(StationLinkLogic.LinkResult.MISSING_LEFT,
                StationLinkLogic.canLink(existing, "east", "south"));
        assertEquals(StationLinkLogic.LinkResult.MISSING_RIGHT,
                StationLinkLogic.canLink(existing, "north", "west"));
        assertEquals(StationLinkLogic.LinkResult.EMPTY_NAME,
                StationLinkLogic.canLink(existing, "", "south"));
    }

    @Test
    void missingNameReportsCorrectSide() {
        assertEquals("east", StationLinkLogic.missingName(
                StationLinkLogic.LinkResult.MISSING_LEFT, "East", "south"));
        assertEquals("west", StationLinkLogic.missingName(
                StationLinkLogic.LinkResult.MISSING_RIGHT, "north", "West"));
        assertNull(StationLinkLogic.missingName(StationLinkLogic.LinkResult.OK, "a", "b"));
    }

    @Test
    void portalIdMatchesNormalizedStopName() {
        assertEquals("depot-1", StationLinkLogic.portalIdForStop("Depot-1"));
    }
}
