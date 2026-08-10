package com.rihanx.build;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClearPadLogicTest {

    @Test
    void squarePlacesFloorAndClearsAbove() {
        List<ClearPadLogic.Cell> cells = ClearPadLogic.square(0, 64, 0, 5, 10);
        assertEquals(5 * 5 * (1 + 10), cells.size());
        assertTrue(cells.stream().anyMatch(c -> c.floor() && c.y() == 64 && c.x() == 0 && c.z() == 0));
        assertTrue(cells.stream().anyMatch(c -> !c.floor() && c.y() == 74));
        assertFalse(cells.stream().anyMatch(c -> c.y() == 75));
        // Odd size 5 → half=2 → x from -2..2
        assertTrue(cells.stream().anyMatch(c -> c.x() == -2 && c.z() == 2 && c.floor()));
        assertFalse(cells.stream().anyMatch(c -> c.x() == 3));
    }

    @Test
    void diskIsCircularAndClearsTreesHeight() {
        List<ClearPadLogic.Cell> cells = ClearPadLogic.disk(10, 70, 20, 3, 48);
        assertEquals(ClearPadLogic.estimateBlocks(ClearPadLogic.diskFootprint(3), 48), cells.size());
        // Corner of bounding box outside circle
        assertFalse(cells.stream().anyMatch(c -> c.x() == 10 + 3 && c.z() == 20 + 3));
        assertTrue(cells.stream().anyMatch(c -> !c.floor() && c.y() == 70 + 48));
    }

    @Test
    void zeroClearHeightIsFloorOnly() {
        List<ClearPadLogic.Cell> cells = ClearPadLogic.square(0, 0, 0, 3, 0);
        assertEquals(9, cells.size());
        assertTrue(cells.stream().allMatch(ClearPadLogic.Cell::floor));
    }
}
