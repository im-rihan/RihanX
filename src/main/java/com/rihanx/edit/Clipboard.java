package com.rihanx.edit;

import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Relative clipboard contents anchored at copy origin.
 */
public final class Clipboard {

    private final int originX;
    private final int originY;
    private final int originZ;
    private final int width;
    private final int height;
    private final int length;
    private final @NotNull List<RelativeBlock> blocks;

    public Clipboard(
            int originX,
            int originY,
            int originZ,
            int width,
            int height,
            int length,
            @NotNull List<RelativeBlock> blocks
    ) {
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.width = width;
        this.height = height;
        this.length = length;
        this.blocks = List.copyOf(blocks);
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginY() {
        return originY;
    }

    public int getOriginZ() {
        return originZ;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLength() {
        return length;
    }

    public @NotNull List<RelativeBlock> getBlocks() {
        return blocks;
    }

    public long size() {
        return blocks.size();
    }

    public @NotNull Clipboard rotateY(int degrees) {
        int normalized = ((degrees % 360) + 360) % 360;
        if (normalized == 0) {
            return this;
        }
        List<RelativeBlock> rotated = new ArrayList<>(blocks.size());
        int newWidth = width;
        int newLength = length;
        for (RelativeBlock block : blocks) {
            int rx = block.dx();
            int rz = block.dz();
            int nx;
            int nz;
            switch (normalized) {
                case 90 -> {
                    nx = rz;
                    nz = width - 1 - rx;
                    newWidth = length;
                    newLength = width;
                }
                case 180 -> {
                    nx = width - 1 - rx;
                    nz = length - 1 - rz;
                }
                case 270 -> {
                    nx = length - 1 - rz;
                    nz = rx;
                    newWidth = length;
                    newLength = width;
                }
                default -> {
                    nx = rx;
                    nz = rz;
                }
            }
            rotated.add(new RelativeBlock(nx, block.dy(), nz, block.data()));
        }
        return new Clipboard(originX, originY, originZ, newWidth, height, newLength, rotated);
    }

    public record RelativeBlock(int dx, int dy, int dz, @NotNull BlockData data) {
        public RelativeBlock {
            data = data.clone();
        }
    }
}
