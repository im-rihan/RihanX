package com.rihanx.build;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Stairs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure geometry helpers for aim-and-build tools. Plans overwrite by packed key.
 */
public final class BuildShapes {

    private BuildShapes() {
    }

    public record PlannedBlock(int x, int y, int z, @NotNull BlockData data) {
    }

    public static void plan(
            @NotNull Map<Long, PlannedBlock> planned,
            @NotNull World world,
            int x,
            int y,
            int z,
            @NotNull BlockData data
    ) {
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
            return;
        }
        planned.put(pack(x, y, z), new PlannedBlock(x, y, z, data));
    }

    public static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    public static @NotNull BlockFace yawToFace(float yaw) {
        float rot = (yaw % 360 + 360) % 360;
        if (rot >= 45 && rot < 135) {
            return BlockFace.WEST;
        }
        if (rot >= 135 && rot < 225) {
            return BlockFace.NORTH;
        }
        if (rot >= 225 && rot < 315) {
            return BlockFace.EAST;
        }
        return BlockFace.SOUTH;
    }

    public static @NotNull BlockFace rotateClockwise(@NotNull BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> face;
        };
    }

    public static @Nullable Material resolveRailing(@NotNull Material deck) {
        String name = deck.name();
        if (name.endsWith("_PLANKS")) {
            Material fence = Material.matchMaterial(name.substring(0, name.length() - "_PLANKS".length()) + "_FENCE");
            if (fence != null && fence.isBlock()) {
                return fence;
            }
        }
        if (name.contains("BRICK") || name.contains("STONE") || name.contains("DEEPSLATE")
                || name.contains("COBBLE") || name.contains("ANDESITE") || name.contains("DIORITE")
                || name.contains("GRANITE") || name.contains("BLACKSTONE") || name.contains("TUFF")) {
            Material wall = Material.matchMaterial(name + "_WALL");
            if (wall != null && wall.isBlock()) {
                return wall;
            }
            return Material.COBBLESTONE_WALL;
        }
        return Material.OAK_FENCE;
    }

    public static @NotNull Map<Long, PlannedBlock> platform(
            @NotNull World world,
            int cx,
            int cy,
            int cz,
            int size,
            @NotNull BlockData data
    ) {
        Map<Long, PlannedBlock> planned = new LinkedHashMap<>();
        int half = size / 2;
        int min = -half;
        int max = size % 2 == 0 ? half - 1 : half;
        for (int dx = min; dx <= max; dx++) {
            for (int dz = min; dz <= max; dz++) {
                plan(planned, world, cx + dx, cy, cz + dz, data);
            }
        }
        return planned;
    }

    public static @NotNull Map<Long, PlannedBlock> wall(
            @NotNull World world,
            int startX,
            int startY,
            int startZ,
            @NotNull BlockFace facing,
            int length,
            int height,
            @NotNull BlockData data
    ) {
        Map<Long, PlannedBlock> planned = new LinkedHashMap<>();
        BlockFace right = rotateClockwise(facing);
        // Wall sits one block in front, centered on facing axis across length
        int half = length / 2;
        int min = -half;
        int max = length % 2 == 0 ? half - 1 : half;
        int baseX = startX + facing.getModX();
        int baseZ = startZ + facing.getModZ();
        for (int i = min; i <= max; i++) {
            int x = baseX + right.getModX() * i;
            int z = baseZ + right.getModZ() * i;
            for (int y = 0; y < height; y++) {
                plan(planned, world, x, startY + y, z, data);
            }
        }
        return planned;
    }

    public static @NotNull Map<Long, PlannedBlock> pillar(
            @NotNull World world,
            int x,
            int startY,
            int z,
            int height,
            @NotNull BlockData data
    ) {
        Map<Long, PlannedBlock> planned = new LinkedHashMap<>();
        if (height == 0) {
            return planned;
        }
        int step = height > 0 ? 1 : -1;
        int abs = Math.abs(height);
        // height > 0: up from feet floor; height < 0: down from feet floor
        for (int i = 0; i < abs; i++) {
            plan(planned, world, x, startY + i * step, z, data);
        }
        return planned;
    }

    public static @NotNull Map<Long, PlannedBlock> cylinder(
            @NotNull World world,
            int cx,
            int baseY,
            int cz,
            int radius,
            int height,
            boolean hollow,
            @NotNull BlockData data
    ) {
        Map<Long, PlannedBlock> planned = new LinkedHashMap<>();
        int r2 = radius * radius;
        int inner = Math.max(0, radius - 1);
        int inner2 = inner * inner;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int dist2 = dx * dx + dz * dz;
                if (dist2 > r2) {
                    continue;
                }
                if (hollow && radius > 0 && dist2 < inner2) {
                    continue;
                }
                for (int y = 0; y < height; y++) {
                    plan(planned, world, cx + dx, baseY + y, cz + dz, data);
                }
            }
        }
        return planned;
    }

    public static @NotNull Map<Long, PlannedBlock> sphere(
            @NotNull World world,
            int cx,
            int cy,
            int cz,
            int radius,
            boolean hollow,
            @NotNull BlockData data
    ) {
        Map<Long, PlannedBlock> planned = new LinkedHashMap<>();
        int r2 = radius * radius;
        int inner = Math.max(0, radius - 1);
        int inner2 = inner * inner;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int dist2 = dx * dx + dy * dy + dz * dz;
                    if (dist2 > r2) {
                        continue;
                    }
                    if (hollow && radius > 0 && dist2 < inner2) {
                        continue;
                    }
                    plan(planned, world, cx + dx, cy + dy, cz + dz, data);
                }
            }
        }
        return planned;
    }

    public static @NotNull Map<Long, PlannedBlock> tunnel(
            @NotNull World world,
            int startX,
            int floorY,
            int startZ,
            @NotNull BlockFace facing,
            int length,
            int width,
            int height,
            @Nullable BlockData lining
    ) {
        Map<Long, PlannedBlock> planned = new LinkedHashMap<>();
        BlockFace right = rotateClockwise(facing);
        int half = width / 2;
        int minW = -half;
        int maxW = width % 2 == 0 ? half - 1 : half;
        BlockData air = Material.AIR.createBlockData();
        for (int i = 1; i <= length; i++) {
            int cx = startX + facing.getModX() * i;
            int cz = startZ + facing.getModZ() * i;
            for (int w = minW; w <= maxW; w++) {
                int x = cx + right.getModX() * w;
                int z = cz + right.getModZ() * w;
                for (int y = 0; y < height; y++) {
                    plan(planned, world, x, floorY + y, z, air);
                }
            }
            if (lining != null) {
                // Floor + ceiling + side walls
                for (int w = minW; w <= maxW; w++) {
                    int x = cx + right.getModX() * w;
                    int z = cz + right.getModZ() * w;
                    plan(planned, world, x, floorY - 1, z, lining);
                    plan(planned, world, x, floorY + height, z, lining);
                }
                for (int y = 0; y < height; y++) {
                    int lx = cx + right.getModX() * (minW - 1);
                    int lz = cz + right.getModZ() * (minW - 1);
                    int rx = cx + right.getModX() * (maxW + 1);
                    int rz = cz + right.getModZ() * (maxW + 1);
                    plan(planned, world, lx, floorY + y, lz, lining);
                    plan(planned, world, rx, floorY + y, rz, lining);
                }
            }
        }
        return planned;
    }

    public static @NotNull Map<Long, PlannedBlock> bridge(
            @NotNull World world,
            int startX,
            int deckY,
            int startZ,
            @NotNull BlockFace facing,
            int length,
            int width,
            @NotNull BlockData deck,
            boolean railings,
            @Nullable Material railingMaterial
    ) {
        Map<Long, PlannedBlock> planned = new LinkedHashMap<>();
        BlockFace right = rotateClockwise(facing);
        int half = width / 2;
        int minW = -half;
        int maxW = width % 2 == 0 ? half - 1 : half;
        BlockData air = Material.AIR.createBlockData();
        for (int i = 1; i <= length; i++) {
            int cx = startX + facing.getModX() * i;
            int cz = startZ + facing.getModZ() * i;
            for (int w = minW; w <= maxW; w++) {
                int x = cx + right.getModX() * w;
                int z = cz + right.getModZ() * w;
                plan(planned, world, x, deckY, z, deck);
                plan(planned, world, x, deckY + 1, z, air);
                plan(planned, world, x, deckY + 2, z, air);
            }
            if (railings && railingMaterial != null) {
                int lx = cx + right.getModX() * minW;
                int lz = cz + right.getModZ() * minW;
                int rx = cx + right.getModX() * maxW;
                int rz = cz + right.getModZ() * maxW;
                plan(planned, world, lx, deckY + 1, lz, railingMaterial.createBlockData());
                if (minW != maxW) {
                    plan(planned, world, rx, deckY + 1, rz, railingMaterial.createBlockData());
                }
            }
        }
        return planned;
    }

    /**
     * Flatten: for each column in radius, set the topmost solid (or target Y) so ground is flat at targetY.
     * Fills air below targetY with fill material; clears solids above targetY down to targetY+1 air/headroom.
     */
    public static @NotNull Map<Long, PlannedBlock> flatten(
            @NotNull World world,
            int cx,
            int targetY,
            int cz,
            int radius,
            @NotNull BlockData fill
    ) {
        Map<Long, PlannedBlock> planned = new LinkedHashMap<>();
        BlockData air = Material.AIR.createBlockData();
        int r2 = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > r2) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;
                plan(planned, world, x, targetY, z, fill);
                // Clear a few blocks above for walkability
                for (int y = 1; y <= 3; y++) {
                    plan(planned, world, x, targetY + y, z, air);
                }
            }
        }
        return planned;
    }

    /**
     * Stepped pyramid centered on (cx, baseY, cz). {@code size} is height in layers.
     * Bottom footprint is {@code (2*size-1)^2}; top is 1×1.
     */
    public static @NotNull Map<Long, PlannedBlock> pyramid(
            @NotNull World world,
            int cx,
            int baseY,
            int cz,
            int size,
            boolean hollow,
            @NotNull BlockData data
    ) {
        Map<Long, PlannedBlock> planned = new LinkedHashMap<>();
        if (size < 1) {
            return planned;
        }
        for (int y = 0; y < size; y++) {
            int half = size - y - 1;
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    boolean edge = Math.abs(dx) == half || Math.abs(dz) == half || half == 0;
                    if (hollow && !edge) {
                        continue;
                    }
                    plan(planned, world, cx + dx, baseY + y, cz + dz, data);
                }
            }
        }
        return planned;
    }

    /**
     * Staircase going forward and up from the player's feet floor.
     * Each step is {@code width} blocks wide; rise 1 per step forward.
     */
    public static @NotNull Map<Long, PlannedBlock> stairs(
            @NotNull World world,
            int startX,
            int floorY,
            int startZ,
            @NotNull BlockFace facing,
            int length,
            int width,
            @NotNull BlockData data
    ) {
        Map<Long, PlannedBlock> planned = new LinkedHashMap<>();
        BlockFace right = rotateClockwise(facing);
        int half = width / 2;
        int minW = -half;
        int maxW = width % 2 == 0 ? half - 1 : half;
        BlockData air = Material.AIR.createBlockData();
        BlockData tread = orientStairs(data, facing);
        for (int i = 1; i <= length; i++) {
            int cx = startX + facing.getModX() * i;
            int cz = startZ + facing.getModZ() * i;
            int stepY = floorY + (i - 1);
            for (int w = minW; w <= maxW; w++) {
                int x = cx + right.getModX() * w;
                int z = cz + right.getModZ() * w;
                plan(planned, world, x, stepY, z, tread);
                // Headroom above the tread
                plan(planned, world, x, stepY + 1, z, air);
                plan(planned, world, x, stepY + 2, z, air);
            }
        }
        return planned;
    }

    private static @NotNull BlockData orientStairs(@NotNull BlockData data, @NotNull BlockFace facing) {
        BlockData clone = data.clone();
        if (clone instanceof Stairs stairs) {
            stairs.setFacing(facing);
            stairs.setHalf(Bisected.Half.BOTTOM);
            stairs.setShape(Stairs.Shape.STRAIGHT);
            return stairs;
        }
        if (clone instanceof Directional directional && directional.getFaces().contains(facing)) {
            directional.setFacing(facing);
            return directional;
        }
        return clone;
    }

    public static @NotNull Map<Long, PlannedBlock> drain(
            @NotNull World world,
            int cx,
            int cy,
            int cz,
            int radius
    ) {
        Map<Long, PlannedBlock> planned = new LinkedHashMap<>();
        BlockData air = Material.AIR.createBlockData();
        int r2 = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2) {
                        continue;
                    }
                    int x = cx + dx;
                    int y = cy + dy;
                    int z = cz + dz;
                    if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
                        continue;
                    }
                    Material type = world.getBlockAt(x, y, z).getType();
                    if (type == Material.WATER || type == Material.LAVA || type == Material.BUBBLE_COLUMN) {
                        plan(planned, world, x, y, z, air);
                    }
                }
            }
        }
        return planned;
    }
}
