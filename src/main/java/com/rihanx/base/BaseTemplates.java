package com.rihanx.base;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Slab;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Built-in pasteable base / house templates.
 * Local coords: origin = floor center, front door faces {@code +Z} (SOUTH before rotation).
 */
public final class BaseTemplates {

    private BaseTemplates() {
    }

    public static @NotNull Map<String, BaseBlueprint> all() {
        Map<String, BaseBlueprint> map = new LinkedHashMap<>();
        map.put("hut", hut());
        map.put("cottage", cottage());
        map.put("village", village());
        map.put("bungalow", LuxuryBaseTemplates.bungalow());
        map.put("villa", LuxuryBaseTemplates.villa());
        map.put("mansion", LuxuryBaseTemplates.mansion());
        map.put("modern", LuxuryBaseTemplates.modern());
        map.put("resort", LuxuryBaseTemplates.resort());
        // GrabCraft-inspired advanced estates
        map.put("estate", LuxuryBaseTemplates.estate());
        map.put("chateau", LuxuryBaseTemplates.chateau());
        map.put("skyvilla", LuxuryBaseTemplates.skyvilla());
        map.put("palace", LuxuryBaseTemplates.palace());
        return map;
    }

    /** Beginner - compact oak starter cabin with full walls, door, windows, gabled roof. */
    public static @NotNull BaseBlueprint hut() {
        Builder b = new Builder();
        int min = -3;
        int max = 3;
        int floor = 0;
        int wallTop = 2;
        int roofBase = 3;

        // Foundation + floor
        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                boolean edge = x == min || x == max || z == min || z == max;
                b.set(x, -1, z, edge ? Material.COBBLESTONE : Material.DIRT);
                b.set(x, floor, z, Material.OAK_PLANKS);
            }
        }

        // Clear interior air (y1-y2)
        for (int x = min + 1; x <= max - 1; x++) {
            for (int z = min + 1; z <= max - 1; z++) {
                b.set(x, 1, z, Material.AIR);
                b.set(x, 2, z, Material.AIR);
            }
        }

        // Walls + log corners
        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                boolean wall = x == min || x == max || z == min || z == max;
                if (!wall) {
                    continue;
                }
                boolean corner = (x == min || x == max) && (z == min || z == max);
                Material wallMat = corner ? Material.OAK_LOG : Material.OAK_PLANKS;
                for (int y = 1; y <= wallTop; y++) {
                    b.set(x, y, z, wallMat);
                }
            }
        }

        // Door opening + oak door (front = +Z)
        b.door(0, 1, max, Material.OAK_DOOR, BlockFace.SOUTH);

        // Windows
        b.set(min, 2, 0, Material.GLASS_PANE);
        b.set(max, 2, 0, Material.GLASS_PANE);
        b.set(-2, 2, min, Material.GLASS_PANE);
        b.set(2, 2, min, Material.GLASS_PANE);

        // Continuous gabled roof (no sky holes)
        LuxuryBaseTemplates.gabledRoof(
                b, min, max, min, max, roofBase,
                Material.OAK_STAIRS, Material.OAK_PLANKS,
                Material.OAK_SLAB, Material.OAK_PLANKS
        );

        // Porch step
        b.set(-1, 0, max + 1, Material.OAK_SLAB);
        b.set(0, 0, max + 1, Material.OAK_SLAB);
        b.set(1, 0, max + 1, Material.OAK_SLAB);

        // Furniture — wall lights only (roof too low for hanging lanterns in walk space)
        b.bed(-2, 1, -2, Material.WHITE_BED, BlockFace.SOUTH);
        b.facing(2, 1, -2, Material.CHEST, BlockFace.WEST);
        b.set(2, 1, -1, Material.BARREL);
        b.set(-2, 1, 1, Material.CRAFTING_TABLE);
        b.facing(2, 1, 1, Material.FURNACE, BlockFace.WEST);
        b.set(-2, 2, -2, Material.TORCH);
        b.set(2, 2, 2, Material.TORCH);

        return b.build("hut", "Beginner oak starter cabin (door, windows, gabled roof)", 0, 0, max + 2);
    }

    /** Intermediate - spruce cottage with chimney, porch, and kitchen. */
    public static @NotNull BaseBlueprint cottage() {
        Builder b = new Builder();
        int min = -4;
        int max = 4;
        int front = 4;

        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                boolean edge = x == min || x == max || z == min || z == max;
                b.set(x, -1, z, edge ? Material.COBBLESTONE : Material.COARSE_DIRT);
                b.set(x, 0, z, Material.SPRUCE_PLANKS);
            }
        }
        for (int x = min + 1; x <= max - 1; x++) {
            for (int z = min + 1; z <= max - 1; z++) {
                b.set(x, 1, z, Material.AIR);
                b.set(x, 2, z, Material.AIR);
                b.set(x, 3, z, Material.AIR);
            }
        }

        // Walls
        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                boolean wall = x == min || x == max || z == min || z == max;
                if (!wall) {
                    continue;
                }
                boolean corner = (x == min || x == max) && (z == min || z == max);
                Material mat = corner ? Material.SPRUCE_LOG : Material.SPRUCE_PLANKS;
                for (int y = 1; y <= 3; y++) {
                    b.set(x, y, z, mat);
                }
            }
        }

        b.doubleDoor(-1, 1, front, Material.SPRUCE_DOOR, BlockFace.SOUTH);

        // Windows
        b.set(min, 2, -1, Material.GLASS_PANE);
        b.set(min, 2, 1, Material.GLASS_PANE);
        b.set(max, 2, -1, Material.GLASS_PANE);
        b.set(max, 2, 1, Material.GLASS_PANE);
        b.set(-2, 2, min, Material.GLASS_PANE);
        b.set(2, 2, min, Material.GLASS_PANE);

        // Continuous gabled roof
        LuxuryBaseTemplates.gabledRoof(
                b, min, max, min, front, 4,
                Material.SPRUCE_STAIRS, Material.SPRUCE_PLANKS,
                Material.SPRUCE_SLAB, Material.SPRUCE_PLANKS
        );

        // Chimney
        b.set(3, 1, -3, Material.BRICKS);
        b.set(3, 2, -3, Material.BRICKS);
        b.set(3, 3, -3, Material.BRICKS);
        b.set(3, 4, -3, Material.BRICKS);
        b.set(3, 5, -3, Material.BRICKS);
        b.set(3, 6, -3, Material.BRICKS);
        b.set(3, 7, -3, Material.CAMPFIRE);

        // Covered porch
        for (int x = -2; x <= 1; x++) {
            b.set(x, 0, front + 1, Material.SPRUCE_PLANKS);
            b.set(x, 0, front + 2, Material.SPRUCE_SLAB);
            b.set(x, 4, front + 1, Material.SPRUCE_PLANKS);
            b.stairs(x, 4, front + 2, Material.SPRUCE_STAIRS, BlockFace.SOUTH);
        }
        for (int y = 1; y <= 3; y++) {
            b.set(-2, y, front + 1, Material.SPRUCE_FENCE);
            b.set(1, y, front + 1, Material.SPRUCE_FENCE);
        }
        // Porch lanterns under beam (y=3), not in the doorway
        b.hangingLantern(-2, 3, front + 1, Material.LANTERN, 4);
        b.hangingLantern(1, 3, front + 1, Material.LANTERN, 4);

        // Flower boxes
        b.set(min - 1, 0, 0, Material.GRASS_BLOCK);
        b.set(min - 1, 1, 0, Material.POPPY);
        b.set(max + 1, 0, 0, Material.GRASS_BLOCK);
        b.set(max + 1, 1, 0, Material.DANDELION);

        // Interior rooms
        b.bed(-3, 1, -3, Material.RED_BED, BlockFace.EAST);
        b.facing(3, 1, -2, Material.CHEST, BlockFace.WEST);
        b.facing(3, 1, -1, Material.CHEST, BlockFace.WEST);
        b.set(-3, 1, 2, Material.BOOKSHELF);
        b.set(-3, 1, 3, Material.BOOKSHELF);
        b.set(-2, 1, 3, Material.ENCHANTING_TABLE);
        b.set(2, 1, 2, Material.CRAFTING_TABLE);
        b.facing(3, 1, 2, Material.FURNACE, BlockFace.WEST);
        b.facing(3, 1, 3, Material.SMOKER, BlockFace.WEST);
        b.set(1, 1, 3, Material.BARREL);
        b.stairs(0, 1, -2, Material.SPRUCE_STAIRS, BlockFace.SOUTH); // chair
        b.stairs(-1, 1, -2, Material.SPRUCE_STAIRS, BlockFace.SOUTH);
        b.set(0, 1, -3, Material.OAK_TRAPDOOR); // table top look
        b.hangingLantern(0, 3, 0, Material.LANTERN, 4);
        b.hangingLantern(-3, 3, 0, Material.LANTERN, 4);
        b.hangingLantern(3, 3, 2, Material.LANTERN, 4);

        return b.build("cottage", "Spruce cottage with chimney, porch, and kitchen", 0, 0, front + 2);
    }

    /** Classic plains-village style oak + cobble house. */
    public static @NotNull BaseBlueprint village() {
        Builder b = new Builder();
        int minX = -3;
        int maxX = 3;
        int minZ = -2;
        int maxZ = 3;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                b.set(x, -1, z, Material.COBBLESTONE);
                b.set(x, 0, z, Material.COBBLESTONE);
            }
        }
        for (int x = minX + 1; x <= maxX - 1; x++) {
            for (int z = minZ + 1; z <= maxZ - 1; z++) {
                b.set(x, 1, z, Material.AIR);
                b.set(x, 2, z, Material.AIR);
            }
        }

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean wall = x == minX || x == maxX || z == minZ || z == maxZ;
                if (!wall) {
                    continue;
                }
                boolean corner = (x == minX || x == maxX) && (z == minZ || z == maxZ);
                Material mat = corner ? Material.OAK_LOG : Material.OAK_PLANKS;
                b.set(x, 1, z, mat);
                b.set(x, 2, z, mat);
            }
        }

        b.door(0, 1, maxZ, Material.OAK_DOOR, BlockFace.SOUTH);

        b.set(minX, 2, 0, Material.GLASS_PANE);
        b.set(maxX, 2, 0, Material.GLASS_PANE);
        b.set(-1, 2, minZ, Material.GLASS_PANE);
        b.set(1, 2, minZ, Material.GLASS_PANE);

        // Continuous A-frame roof
        LuxuryBaseTemplates.gabledRoof(
                b, minX, maxX, minZ, maxZ, 3,
                Material.OAK_STAIRS, Material.OAK_PLANKS,
                Material.OAK_SLAB, Material.OAK_PLANKS
        );

        b.bed(-2, 1, 0, Material.WHITE_BED, BlockFace.EAST);
        b.facing(2, 1, 1, Material.CHEST, BlockFace.WEST);
        b.set(2, 1, 0, Material.CRAFTING_TABLE);
        b.facing(2, 1, -1, Material.FURNACE, BlockFace.WEST);
        b.set(0, 4, 1, Material.OAK_PLANKS);
        b.hangingLantern(0, 3, 1, Material.LANTERN, 4);
        b.set(-2, 1, 2, Material.BARREL);

        // Front path
        b.set(0, 0, maxZ + 1, Material.COBBLESTONE_SLAB);
        b.set(0, 0, maxZ + 2, Material.DIRT_PATH);

        return b.build("village", "Classic plains-village oak and cobble house", 0, 0, maxZ + 2);
    }

    /** Mutable template builder - last write wins per cell. */
    static final class Builder {
        private final Map<Long, RelBlock> cells = new LinkedHashMap<>();

        void set(int x, int y, int z, @NotNull Material material) {
            put(new RelBlock(x, y, z, material, null, null, null, false, null, false));
        }

        void facing(int x, int y, int z, @NotNull Material material, @NotNull BlockFace facing) {
            put(new RelBlock(x, y, z, material, facing, null, null, false, null, false));
        }

        void stairs(int x, int y, int z, @NotNull Material material, @NotNull BlockFace facing) {
            put(new RelBlock(x, y, z, material, facing, null, null, false, null, false));
        }

        void slab(int x, int y, int z, @NotNull Material material, @NotNull Slab.Type type) {
            put(new RelBlock(x, y, z, material, null, type, null, false, null, false));
        }

        void door(int x, int y, int z, @NotNull Material material, @NotNull BlockFace facing) {
            door(x, y, z, material, facing, null);
        }

        void door(
                int x,
                int y,
                int z,
                @NotNull Material material,
                @NotNull BlockFace facing,
                @Nullable Door.Hinge hinge
        ) {
            put(new RelBlock(x, y, z, material, facing, null, null, false, hinge, false));
            put(new RelBlock(x, y + 1, z, material, facing, null, null, true, hinge, false));
        }

        void doubleDoor(int xLeft, int y, int z, @NotNull Material material, @NotNull BlockFace facing) {
            door(xLeft, y, z, material, facing, Door.Hinge.LEFT);
            door(xLeft + 1, y, z, material, facing, Door.Hinge.RIGHT);
        }

        void bed(int x, int y, int z, @NotNull Material material, @NotNull BlockFace facing) {
            put(new RelBlock(x, y, z, material, facing, null, Bed.Part.FOOT, false, null, false));
            int hx = x + facing.getModX();
            int hz = z + facing.getModZ();
            put(new RelBlock(hx, y, hz, material, facing, null, Bed.Part.HEAD, false, null, false));
        }

        /**
         * Hanging lantern under a ceiling: lantern at {@code y}, chains up to {@code attachY - 1}.
         * Place/keep a solid block at {@code attachY} (roof) so the chain connects.
         */
        void hangingLantern(int x, int y, int z, @NotNull Material lantern, int attachY) {
            Material type = lantern == Material.SOUL_LANTERN ? Material.SOUL_LANTERN : Material.LANTERN;
            if (attachY <= y + 1) {
                // Too tight for a chain — still hang from the block above if possible
                put(new RelBlock(x, y, z, type, null, null, null, false, null, true));
                if (attachY == y + 1) {
                    // caller must provide ceiling at attachY
                }
                return;
            }
            put(new RelBlock(x, y, z, type, null, null, null, false, null, true));
            for (int cy = y + 1; cy < attachY; cy++) {
                set(x, cy, z, Material.IRON_CHAIN);
            }
        }

        private void put(@NotNull RelBlock block) {
            cells.put(pack(block.dx(), block.dy(), block.dz()), block);
        }

        private static long pack(int x, int y, int z) {
            return ((long) (x + 512) & 0x3FF)
                    | (((long) (y + 64) & 0xFF) << 10)
                    | (((long) (z + 512) & 0x3FF) << 18);
        }

        @NotNull BaseBlueprint build(@NotNull String id, @NotNull String description) {
            int maxZ = 0;
            for (RelBlock block : cells.values()) {
                maxZ = Math.max(maxZ, block.dz());
            }
            return build(id, description, 0, 0, maxZ + 2);
        }

        @NotNull BaseBlueprint build(
                @NotNull String id,
                @NotNull String description,
                int spawnDx,
                int spawnDy,
                int spawnDz
        ) {
            List<RelBlock> list = new ArrayList<>(cells.values());
            return new BaseBlueprint(id, description, List.copyOf(list), spawnDx, spawnDy, spawnDz);
        }
    }

    /**
     * Relative block. Facing/slab/bedPart are in local structure space (front = SOUTH/+Z).
     */
    public record RelBlock(
            int dx,
            int dy,
            int dz,
            @NotNull Material material,
            @Nullable BlockFace facing,
            @Nullable Slab.Type slabType,
            @Nullable Bed.Part bedPart,
            boolean upperHalf,
            @Nullable Door.Hinge hinge,
            boolean hanging
    ) {
    }

    public record BaseBlueprint(
            @NotNull String id,
            @NotNull String description,
            @NotNull List<RelBlock> blocks,
            int spawnDx,
            int spawnDy,
            int spawnDz
    ) {
        public @NotNull String id() {
            return id.toLowerCase(Locale.ROOT);
        }
    }
}
