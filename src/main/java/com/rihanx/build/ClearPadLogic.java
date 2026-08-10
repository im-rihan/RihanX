package com.rihanx.build;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry for cleared build pads: solid floor + air column above so trees/buildings are removed.
 */
public final class ClearPadLogic {

    /**
     * @param floor true = place floor material; false = clear to air
     */
    public record Cell(int x, int y, int z, boolean floor) {
    }

    private ClearPadLogic() {
    }

    /**
     * Square pad centered on {@code (cx, floorY, cz)}.
     * {@code size} is the side length in blocks (same convention as {@link BuildShapes#platform}).
     */
    public static @NotNull List<Cell> square(
            int cx,
            int floorY,
            int cz,
            int size,
            int clearHeight
    ) {
        int side = Math.max(1, size);
        int clear = Math.max(0, clearHeight);
        int half = side / 2;
        int min = -half;
        int max = side % 2 == 0 ? half - 1 : half;
        List<Cell> cells = new ArrayList<>(side * side * (1 + clear));
        for (int dx = min; dx <= max; dx++) {
            for (int dz = min; dz <= max; dz++) {
                int x = cx + dx;
                int z = cz + dz;
                cells.add(new Cell(x, floorY, z, true));
                for (int h = 1; h <= clear; h++) {
                    cells.add(new Cell(x, floorY + h, z, false));
                }
            }
        }
        return cells;
    }

    /**
     * Disk flatten centered on {@code (cx, floorY, cz)} with circular radius.
     */
    public static @NotNull List<Cell> disk(
            int cx,
            int floorY,
            int cz,
            int radius,
            int clearHeight
    ) {
        int r = Math.max(0, radius);
        int clear = Math.max(0, clearHeight);
        int r2 = r * r;
        List<Cell> cells = new ArrayList<>((2 * r + 1) * (2 * r + 1) * (1 + clear));
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r2) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;
                cells.add(new Cell(x, floorY, z, true));
                for (int h = 1; h <= clear; h++) {
                    cells.add(new Cell(x, floorY + h, z, false));
                }
            }
        }
        return cells;
    }

    /** Approximate block count for planning / limits. */
    public static int estimateBlocks(int footprint, int clearHeight) {
        return Math.max(0, footprint) * (1 + Math.max(0, clearHeight));
    }

    public static int squareFootprint(int size) {
        int side = Math.max(1, size);
        return side * side;
    }

    public static int diskFootprint(int radius) {
        int r = Math.max(0, radius);
        int r2 = r * r;
        int count = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= r2) {
                    count++;
                }
            }
        }
        return count;
    }
}
