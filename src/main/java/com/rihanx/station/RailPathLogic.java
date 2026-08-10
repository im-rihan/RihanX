package com.rihanx.station;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure geometry for an axis-aligned (manhattan) railway between two stops.
 * Rails cannot run diagonally, so the path goes along X then Z (or Z then X).
 * <p>
 * Station links also build short join/exit spurs using each stop's facing so both
 * station platforms connect to the long path (not only the destination end).
 */
public final class RailPathLogic {

    public enum Layer {
        SUPPORT,
        GLOW,
        BED,
        RAIL,
        POWERED,
        REDSTONE,
        CLEAR
    }

    public record Cell(int x, int y, int z, @NotNull Layer layer) {
    }

    public enum PlanResult {
        OK,
        TOO_SHORT,
        TOO_FAR,
        VERTICAL_ONLY
    }

    public record Plan(@NotNull PlanResult result, @NotNull List<Cell> cells, int trackLength) {
        public static @NotNull Plan fail(@NotNull PlanResult result) {
            return new Plan(result, List.of(), 0);
        }
    }

    /** Cardinal facing from player/portal yaw (same rules as build tools). */
    public enum Cardinal {
        SOUTH(0, 1),
        WEST(-1, 0),
        NORTH(0, -1),
        EAST(1, 0);

        final int dx;
        final int dz;

        Cardinal(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }

        @NotNull Cardinal opposite() {
            return switch (this) {
                case SOUTH -> NORTH;
                case NORTH -> SOUTH;
                case EAST -> WEST;
                case WEST -> EAST;
            };
        }
    }

    private RailPathLogic() {
    }

    public static @NotNull Cardinal yawToCardinal(float yaw) {
        float rot = (yaw % 360 + 360) % 360;
        if (rot >= 45 && rot < 135) {
            return Cardinal.WEST;
        }
        if (rot >= 135 && rot < 225) {
            return Cardinal.NORTH;
        }
        if (rot >= 225 && rot < 315) {
            return Cardinal.EAST;
        }
        return Cardinal.SOUTH;
    }

    /**
     * Pick exit direction from a station: prefer the structure front if it points
     * toward the destination; otherwise leave out the back.
     */
    public static @NotNull Cardinal pickExit(@NotNull Cardinal front, int dxToDest, int dzToDest) {
        int dot = front.dx * dxToDest + front.dz * dzToDest;
        return dot >= 0 ? front : front.opposite();
    }

    /**
     * Plan a railway from stop A to stop B (pad cells), with station join/exit spurs.
     *
     * @param joinLen  blocks from each pad back into the station (meet platform rails)
     * @param exitLen  blocks from each pad out toward the other stop before the long path
     */
    public static @NotNull Plan planStationLink(
            int x1, int y1, int z1, float yaw1,
            int x2, int y2, int z2, float yaw2,
            int maxDistance,
            int poweredEvery,
            int joinLen,
            int exitLen
    ) {
        int dx = x2 - x1;
        int dz = z2 - z1;
        int horiz = Math.abs(dx) + Math.abs(dz);
        if (horiz == 0) {
            return Plan.fail(y1 == y2 ? PlanResult.TOO_SHORT : PlanResult.VERTICAL_ONLY);
        }
        if (horiz > Math.max(1, maxDistance)) {
            return Plan.fail(PlanResult.TOO_FAR);
        }

        int join = Math.max(0, joinLen);
        int exit = Math.max(1, exitLen);
        Cardinal front1 = yawToCardinal(yaw1);
        Cardinal front2 = yawToCardinal(yaw2);
        Cardinal exit1 = pickExit(front1, dx, dz);
        Cardinal exit2 = pickExit(front2, -dx, -dz);
        Cardinal join1 = exit1.opposite();
        Cardinal join2 = exit2.opposite();

        int ex1x = x1 + exit1.dx * exit;
        int ex1z = z1 + exit1.dz * exit;
        int ex2x = x2 + exit2.dx * exit;
        int ex2z = z2 + exit2.dz * exit;

        boolean preferXFirst = Math.abs(ex2x - ex1x) >= Math.abs(ex2z - ex1z);
        List<int[]> middle = centerline(ex1x, y1, ex1z, ex2x, y2, ex2z, preferXFirst);
        if (middle.size() < 2) {
            return Plan.fail(PlanResult.TOO_SHORT);
        }

        // Ordered centerline: join1 → pad1 (skipped) → exit1 → middle → exit2 → pad2 (skipped) → join2
        List<int[]> center = new ArrayList<>();
        for (int i = join; i >= 1; i--) {
            center.add(new int[]{x1 + join1.dx * i, y1, z1 + join1.dz * i});
        }
        for (int i = 1; i <= exit; i++) {
            int y = interpolateY(y1, y1, i, exit); // flat at A until middle
            center.add(new int[]{x1 + exit1.dx * i, y, z1 + exit1.dz * i});
        }
        // middle includes both exits — skip duplicate exits
        for (int i = 1; i < middle.size() - 1; i++) {
            center.add(middle.get(i));
        }
        for (int i = exit; i >= 1; i--) {
            center.add(new int[]{x2 + exit2.dx * i, y2, z2 + exit2.dz * i});
        }
        for (int i = 1; i <= join; i++) {
            center.add(new int[]{x2 + join2.dx * i, y2, z2 + join2.dz * i});
        }

        // Deduplicate while preserving order
        Map<Long, int[]> unique = new LinkedHashMap<>();
        for (int[] p : center) {
            unique.putIfAbsent(pack(p[0], p[1], p[2]), p);
        }
        List<int[]> track = new ArrayList<>(unique.values());
        if (track.isEmpty()) {
            return Plan.fail(PlanResult.TOO_SHORT);
        }

        return cellsAlong(track, poweredEvery);
    }

    /**
     * Plan a railway from stop A to stop B (legacy: pad-to-pad with margin, no facing).
     */
    public static @NotNull Plan plan(
            int x1, int y1, int z1,
            int x2, int y2, int z2,
            int maxDistance,
            int poweredEvery,
            int margin,
            boolean preferXFirst
    ) {
        int dx = Math.abs(x2 - x1);
        int dz = Math.abs(z2 - z1);
        int horiz = dx + dz;
        if (horiz == 0) {
            return Plan.fail(y1 == y2 ? PlanResult.TOO_SHORT : PlanResult.VERTICAL_ONLY);
        }
        if (horiz > Math.max(1, maxDistance)) {
            return Plan.fail(PlanResult.TOO_FAR);
        }

        int every = Math.max(2, poweredEvery);
        int skip = Math.max(0, margin);
        List<int[]> center = centerline(x1, y1, z1, x2, y2, z2, preferXFirst);
        if (center.size() <= skip * 2) {
            return Plan.fail(PlanResult.TOO_SHORT);
        }

        List<int[]> track = center.subList(skip, center.size() - skip);
        return cellsAlong(track, every);
    }

    private static @NotNull Plan cellsAlong(@NotNull List<int[]> track, int poweredEvery) {
        int every = Math.max(2, poweredEvery);
        List<Cell> cells = new ArrayList<>(track.size() * 5);
        for (int i = 0; i < track.size(); i++) {
            int[] p = track.get(i);
            int x = p[0];
            int y = p[1];
            int z = p[2];
            boolean powered = (i % every) == 0;
            Layer under = powered
                    ? Layer.REDSTONE
                    : (i % 8 == 0 ? Layer.GLOW : Layer.SUPPORT);
            cells.add(new Cell(x, y - 1, z, under));
            cells.add(new Cell(x, y, z, Layer.BED));
            cells.add(new Cell(x, y + 1, z, powered ? Layer.POWERED : Layer.RAIL));
            cells.add(new Cell(x, y + 2, z, Layer.CLEAR));
        }
        return new Plan(PlanResult.OK, List.copyOf(cells), track.size());
    }

    public static int horizontalDistance(int x1, int z1, int x2, int z2) {
        return Math.abs(x2 - x1) + Math.abs(z2 - z1);
    }

    static @NotNull List<int[]> centerline(
            int x1, int y1, int z1,
            int x2, int y2, int z2,
            boolean preferXFirst
    ) {
        int dx = Math.abs(x2 - x1);
        int dz = Math.abs(z2 - z1);
        int steps = dx + dz;
        List<int[]> points = new ArrayList<>(steps + 1);
        points.add(new int[]{x1, y1, z1});

        int x = x1;
        int y = y1;
        int z = z1;
        int stepIndex = 0;
        int sx = Integer.signum(x2 - x1);
        int sz = Integer.signum(z2 - z1);

        if (preferXFirst) {
            while (x != x2) {
                x += sx;
                stepIndex++;
                y = interpolateY(y1, y2, stepIndex, steps);
                points.add(new int[]{x, y, z});
            }
            while (z != z2) {
                z += sz;
                stepIndex++;
                y = interpolateY(y1, y2, stepIndex, steps);
                points.add(new int[]{x, y, z});
            }
        } else {
            while (z != z2) {
                z += sz;
                stepIndex++;
                y = interpolateY(y1, y2, stepIndex, steps);
                points.add(new int[]{x, y, z});
            }
            while (x != x2) {
                x += sx;
                stepIndex++;
                y = interpolateY(y1, y2, stepIndex, steps);
                points.add(new int[]{x, y, z});
            }
        }
        return points;
    }

    static int interpolateY(int y1, int y2, int stepIndex, int steps) {
        if (steps <= 0) {
            return y2;
        }
        return y1 + (int) Math.round((y2 - y1) * (stepIndex / (double) steps));
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }
}
