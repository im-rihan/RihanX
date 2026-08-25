package com.rihanx.farm;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Slab;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One deterministic relative block in local farm space (front = +Z / SOUTH before rotation).
 */
public record BlockPlacement(
        int dx,
        int dy,
        int dz,
        @NotNull Material material,
        @Nullable BlockFace facing,
        @Nullable Slab.Type slabType,
        @Nullable Door.Hinge hinge,
        boolean upperHalf,
        boolean hanging,
        @NotNull BuildStage stage
) {
    public enum BuildStage {
        FOUNDATION,
        FRAME,
        WALLS,
        FLUIDS,
        TERRAIN,
        FUNCTIONAL,
        REDSTONE,
        COLLECTION,
        MOB_SYSTEMS,
        CONTAINERS,
        DECORATIVE,
        INVENTORY,
        FINAL
    }

    public static @NotNull BlockPlacement of(int dx, int dy, int dz, @NotNull Material material) {
        return new BlockPlacement(dx, dy, dz, material, null, null, null, false, false, BuildStage.FUNCTIONAL);
    }

    public static @NotNull BlockPlacement facing(
            int dx, int dy, int dz, @NotNull Material material, @NotNull BlockFace facing
    ) {
        return new BlockPlacement(dx, dy, dz, material, facing, null, null, false, false, BuildStage.FUNCTIONAL);
    }

    public @NotNull BlockPlacement withStage(@NotNull BuildStage stage) {
        return new BlockPlacement(dx, dy, dz, material, facing, slabType, hinge, upperHalf, hanging, stage);
    }

    public long packKey() {
        return ((long) (dx + 512) & 0x3FF)
                | (((long) (dy + 64) & 0xFF) << 10)
                | (((long) (dz + 512) & 0x3FF) << 18);
    }
}
