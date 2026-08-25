package com.rihanx.farm;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Deterministic build context for a single farm generation attempt.
 * Relative placements are resolved as {@code origin + rotated(dx,dy,dz)}.
 */
public final class FarmBuildContext {

    public static final String GENERATION_VERSION = "1";

    private final @NotNull World world;
    private final int originX;
    private final int originY;
    private final int originZ;
    private final @NotNull BlockFace facing;
    private final @NotNull String farmId;
    private final @Nullable Player owner;
    private final @NotNull String generationVersion;

    public FarmBuildContext(
            @NotNull World world,
            int originX,
            int originY,
            int originZ,
            @NotNull BlockFace facing,
            @NotNull String farmId,
            @Nullable Player owner
    ) {
        this(world, originX, originY, originZ, facing, farmId, owner, GENERATION_VERSION);
    }

    public FarmBuildContext(
            @NotNull World world,
            int originX,
            int originY,
            int originZ,
            @NotNull BlockFace facing,
            @NotNull String farmId,
            @Nullable Player owner,
            @NotNull String generationVersion
    ) {
        this.world = world;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.facing = facing;
        this.farmId = farmId;
        this.owner = owner;
        this.generationVersion = generationVersion;
    }

    public @NotNull World world() {
        return world;
    }

    public int originX() {
        return originX;
    }

    public int originY() {
        return originY;
    }

    public int originZ() {
        return originZ;
    }

    public @NotNull BlockFace facing() {
        return facing;
    }

    public @NotNull String farmId() {
        return farmId;
    }

    public @Nullable Player owner() {
        return owner;
    }

    public @NotNull String generationVersion() {
        return generationVersion;
    }

    /** World Y of a relative Y (no rotation). */
    public int worldY(int dy) {
        return originY + dy;
    }
}
