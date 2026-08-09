package com.rihanx.build;

import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Geometry helpers that do not need a live World.
 */
class BuildShapesTest {

    @Test
    void yawToFaceCardinals() {
        assertEquals(BlockFace.SOUTH, BuildShapes.yawToFace(0f));
        assertEquals(BlockFace.WEST, BuildShapes.yawToFace(90f));
        assertEquals(BlockFace.NORTH, BuildShapes.yawToFace(180f));
        assertEquals(BlockFace.EAST, BuildShapes.yawToFace(270f));
    }

    @Test
    void rotateClockwise() {
        assertEquals(BlockFace.EAST, BuildShapes.rotateClockwise(BlockFace.NORTH));
        assertEquals(BlockFace.SOUTH, BuildShapes.rotateClockwise(BlockFace.EAST));
        assertEquals(BlockFace.WEST, BuildShapes.rotateClockwise(BlockFace.SOUTH));
        assertEquals(BlockFace.NORTH, BuildShapes.rotateClockwise(BlockFace.WEST));
    }

    @Test
    void packIsStableForSameCoords() {
        assertEquals(BuildShapes.pack(10, 64, -20), BuildShapes.pack(10, 64, -20));
    }

    @Test
    void rotateClockwiseCycles() {
        assertEquals(BlockFace.NORTH, BuildShapes.rotateClockwise(
                BuildShapes.rotateClockwise(
                        BuildShapes.rotateClockwise(
                                BuildShapes.rotateClockwise(BlockFace.NORTH)))));
    }
}
