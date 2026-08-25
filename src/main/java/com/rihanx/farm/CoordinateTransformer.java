package com.rihanx.farm;

import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

/**
 * Local structure space uses front = SOUTH (+Z). Transforms match {@link com.rihanx.base.BaseService}.
 */
public final class CoordinateTransformer {

    private CoordinateTransformer() {
    }

    /** Rotate local (dx,dz) so +Z is the structure front / player facing. */
    public static int @NotNull [] rotate(int dx, int dz, @NotNull BlockFace facing) {
        return switch (facing) {
            case SOUTH -> new int[]{dx, dz};
            case NORTH -> new int[]{-dx, -dz};
            case EAST -> new int[]{dz, -dx};
            case WEST -> new int[]{-dz, dx};
            default -> new int[]{dx, dz};
        };
    }

    public static @NotNull BlockFace mapFacing(@NotNull BlockFace local, @NotNull BlockFace structureFront) {
        if (local == BlockFace.UP || local == BlockFace.DOWN) {
            return local;
        }
        int turns = switch (structureFront) {
            case SOUTH -> 0;
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
        BlockFace face = local;
        for (int i = 0; i < turns; i++) {
            face = rotateClockwise(face);
        }
        return face;
    }

    private static @NotNull BlockFace rotateClockwise(@NotNull BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> face;
        };
    }
}
