package com.rihanx.station;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure geometry for an axis-aligned (manhattan) railway between two stops.
 * <p>
 * Station links always join along each station's platform track axis (from yaw),
 * then exit toward the other stop, then L-link the exits. That keeps both
 * built station platforms connected even when stations sit on a flat plain.
 * <p>
 * Pad cells (gold plates) are never overwritten; when join and exit meet at a
 * pad, rails detour around the pad so carts never face a diagonal gap at a turn.
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

    public enum Cardinal {
        SOUTH(0, 1),
        WEST(-1, 0),
        NORTH(0, -1),
        EAST(1, 0);

        public final int dx;
        public final int dz;

        Cardinal(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }

        public @NotNull Cardinal opposite() {
            return switch (this) {
                case SOUTH -> NORTH;
                case NORTH -> SOUTH;
                case EAST -> WEST;
                case WEST -> EAST;
            };
        }

        /** Perpendicular for pad detours (prefer EAST/SOUTH). */
        public @NotNull Cardinal side() {
            return switch (this) {
                case NORTH, SOUTH -> EAST;
                case EAST, WEST -> SOUTH;
            };
        }
    }

    /** One station's join (into platform) and exit (toward peer) tips. */
    public record SpurEnds(
            int joinX, int joinY, int joinZ,
            int exitX, int exitY, int exitZ
    ) {
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
     * Leave toward the peer: use the platform front when it faces the destination,
     * the back when the peer is behind, or the sideways cardinal when the peer is
     * perpendicular to the track (common for stations on the same plain).
     */
    public static @NotNull Cardinal pickExit(@NotNull Cardinal front, int dxToDest, int dzToDest) {
        int along = front.dx * dxToDest + front.dz * dzToDest;
        if (along > 0) {
            return front;
        }
        if (along < 0) {
            return front.opposite();
        }
        // Perpendicular to track axis — exit sideways toward the peer.
        if (Math.abs(dxToDest) >= Math.abs(dzToDest)) {
            return dxToDest >= 0 ? Cardinal.EAST : Cardinal.WEST;
        }
        return dzToDest >= 0 ? Cardinal.SOUTH : Cardinal.NORTH;
    }

    /**
     * Compute join tip (into platform along track axis) and exit tip (toward peer).
     * Join always uses opposite(front) so it lands on the built station rails.
     */
    public static @NotNull SpurEnds spurEnds(
            int padX, int padY, int padZ,
            float yaw,
            int dxToDest, int dzToDest,
            int joinLen, int exitLen
    ) {
        Cardinal front = yawToCardinal(yaw);
        Cardinal joinDir = front.opposite();
        Cardinal exitDir = pickExit(front, dxToDest, dzToDest);
        int join = Math.max(1, joinLen);
        int exit = Math.max(1, exitLen);
        return new SpurEnds(
                padX + joinDir.dx * join,
                padY,
                padZ + joinDir.dz * join,
                padX + exitDir.dx * exit,
                padY,
                padZ + exitDir.dz * exit
        );
    }

    /**
     * Plan rails that join both station platforms, then connect exit-to-exit.
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

        SpurEnds a = spurEnds(x1, y1, z1, yaw1, dx, dz, joinLen, exitLen);
        SpurEnds b = spurEnds(x2, y2, z2, yaw2, -dx, -dz, joinLen, exitLen);
        return planPlatformToPlatform(a, b, x1, z1, x2, z2, poweredEvery);
    }

    /**
     * Same as {@link #planStationLink} but with explicit bed Y / tips (e.g. snapped to world rails).
     */
    public static @NotNull Plan planPlatformToPlatform(
            @NotNull SpurEnds a,
            @NotNull SpurEnds b,
            int pad1X, int pad1Z,
            int pad2X, int pad2Z,
            int poweredEvery
    ) {
        boolean preferXFirst = Math.abs(b.exitX() - a.exitX()) >= Math.abs(b.exitZ() - a.exitZ());
        List<int[]> middle = centerline(
                a.exitX(), a.exitY(), a.exitZ(),
                b.exitX(), b.exitY(), b.exitZ(),
                preferXFirst
        );
        if (middle.size() < 2) {
            return Plan.fail(PlanResult.TOO_SHORT);
        }

        List<int[]> center = new ArrayList<>();
        // A: deep join → approach pad → bridge around pad → exit tip
        appendLine(center, a.joinX(), a.joinY(), a.joinZ(), pad1X, a.joinY(), pad1Z, false);
        injectPadBridge(center, pad1X, a.joinY(), pad1Z, a);
        appendLine(center, pad1X, a.exitY(), pad1Z, a.exitX(), a.exitY(), a.exitZ(), true);
        // Middle between exits (skip duplicate tips)
        for (int i = 1; i < middle.size() - 1; i++) {
            center.add(middle.get(i));
        }
        // B: exit → toward pad → bridge → deep join
        appendLine(center, b.exitX(), b.exitY(), b.exitZ(), pad2X, b.exitY(), pad2Z, true);
        injectPadBridge(center, pad2X, b.joinY(), pad2Z, b);
        appendLine(center, pad2X, b.joinY(), pad2Z, b.joinX(), b.joinY(), b.joinZ(), false);

        Map<Long, int[]> unique = new LinkedHashMap<>();
        for (int[] p : center) {
            // Never place track on the gold-plate pad cells themselves
            if ((p[0] == pad1X && p[2] == pad1Z) || (p[0] == pad2X && p[2] == pad2Z)) {
                continue;
            }
            unique.putIfAbsent(pack(p[0], p[1], p[2]), p);
        }
        List<int[]> track = new ArrayList<>(unique.values());
        if (track.isEmpty()) {
            return Plan.fail(PlanResult.TOO_SHORT);
        }
        return cellsAlong(track, poweredEvery);
    }

    /**
     * Orthogonally connect join-approach and exit-approach around a stripped pad.
     * Without this, sideways exits leave a diagonal gap that vanilla rails cannot join.
     */
    static void injectPadBridge(
            @NotNull List<int[]> out,
            int padX, int padY, int padZ,
            @NotNull SpurEnds spur
    ) {
        Cardinal joinFromPad = toward(padX, padZ, spur.joinX(), spur.joinZ());
        Cardinal exitFromPad = toward(padX, padZ, spur.exitX(), spur.exitZ());
        if (joinFromPad == null || exitFromPad == null) {
            return;
        }
        if (joinFromPad == exitFromPad) {
            return;
        }
        if (joinFromPad == exitFromPad.opposite()) {
            // Collinear through pad: detour one block to the side (3-cell U-turn).
            Cardinal side = joinFromPad.side();
            out.add(new int[]{
                    padX + joinFromPad.dx + side.dx, padY, padZ + joinFromPad.dz + side.dz
            });
            out.add(new int[]{padX + side.dx, padY, padZ + side.dz});
            out.add(new int[]{
                    padX + exitFromPad.dx + side.dx, padY, padZ + exitFromPad.dz + side.dz
            });
            return;
        }
        // Perpendicular (typical plain link): one corner cell outside the pad.
        out.add(new int[]{
                padX + joinFromPad.dx + exitFromPad.dx,
                padY,
                padZ + joinFromPad.dz + exitFromPad.dz
        });
    }

    /** Cardinal from (fromX,fromZ) toward (toX,toZ) on a single axis, or null if diagonal/same. */
    static @Nullable Cardinal toward(int fromX, int fromZ, int toX, int toZ) {
        int dx = Integer.signum(toX - fromX);
        int dz = Integer.signum(toZ - fromZ);
        if (dx == 0 && dz == 0) {
            return null;
        }
        if (dx != 0 && dz != 0) {
            // Prefer the larger axis; if equal, prefer X.
            if (Math.abs(toX - fromX) >= Math.abs(toZ - fromZ)) {
                dz = 0;
            } else {
                dx = 0;
            }
        }
        if (dx > 0) {
            return Cardinal.EAST;
        }
        if (dx < 0) {
            return Cardinal.WEST;
        }
        if (dz > 0) {
            return Cardinal.SOUTH;
        }
        return Cardinal.NORTH;
    }

    /** Inclusive axis-aligned line; skips the start cell when {@code skipStart}. */
    static void appendLine(
            @NotNull List<int[]> out,
            int x1, int y1, int z1,
            int x2, int y2, int z2,
            boolean skipStart
    ) {
        List<int[]> line = centerline(x1, y1, z1, x2, y2, z2, Math.abs(x2 - x1) >= Math.abs(z2 - z1));
        int from = skipStart ? 1 : 0;
        for (int i = from; i < line.size(); i++) {
            out.add(line.get(i));
        }
    }

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
            // Powered rails cannot curve — never boost a corner / direction-change cell.
            boolean powered = (i % every) == 0 && !isCorner(track, i);
            Layer under = i % 8 == 0 ? Layer.GLOW : Layer.SUPPORT;
            cells.add(new Cell(x, y - 1, z, under));
            cells.add(new Cell(x, y, z, powered ? Layer.REDSTONE : Layer.BED));
            cells.add(new Cell(x, y + 1, z, powered ? Layer.POWERED : Layer.RAIL));
            cells.add(new Cell(x, y + 2, z, Layer.CLEAR));
        }
        return new Plan(PlanResult.OK, List.copyOf(cells), track.size());
    }

    /** True when the path turns at this index (prev→cur→next changes horizontal direction). */
    static boolean isCorner(@NotNull List<int[]> track, int i) {
        if (i <= 0 || i >= track.size() - 1) {
            return false;
        }
        int[] prev = track.get(i - 1);
        int[] cur = track.get(i);
        int[] next = track.get(i + 1);
        int dx1 = Integer.signum(cur[0] - prev[0]);
        int dz1 = Integer.signum(cur[2] - prev[2]);
        int dx2 = Integer.signum(next[0] - cur[0]);
        int dz2 = Integer.signum(next[2] - cur[2]);
        if ((dx1 == 0 && dz1 == 0) || (dx2 == 0 && dz2 == 0)) {
            return false;
        }
        return dx1 != dx2 || dz1 != dz2;
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
        List<int[]> points = new ArrayList<>(Math.max(1, steps + 1));
        points.add(new int[]{x1, y1, z1});
        if (steps == 0) {
            return points;
        }

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
