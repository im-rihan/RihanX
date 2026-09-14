package com.rihanx.kingdom;

import org.jetbrains.annotations.NotNull;

import java.util.BitSet;

/**
 * Corner or central spire. Lighting is stored per observation-deck tier.
 */
public final class KingdomTower {

    private final @NotNull TowerType type;
    private final int x;
    private final int z;
    private int groundY;
    private int height;
    private boolean built;
    private final BitSet litTiers = new BitSet();
    private boolean allLit = true;

    public KingdomTower(@NotNull TowerType type, int x, int z, int groundY, int height) {
        this.type = type;
        this.x = x;
        this.z = z;
        this.groundY = groundY;
        this.height = Math.max(8, height);
    }

    public @NotNull TowerType type() {
        return type;
    }

    public @NotNull String name(@NotNull String kingdomName) {
        return kingdomName + "-" + type.label();
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }

    public int groundY() {
        return groundY;
    }

    public void setGroundY(int groundY) {
        this.groundY = groundY;
    }

    public int height() {
        return height;
    }

    public void setHeight(int height) {
        this.height = Math.max(8, height);
    }

    public int topY() {
        return groundY + height;
    }

    public boolean built() {
        return built;
    }

    public void setBuilt(boolean built) {
        this.built = built;
    }

    public boolean allLit() {
        return allLit;
    }

    public void setAllLit(boolean allLit) {
        this.allLit = allLit;
        if (allLit) {
            litTiers.clear();
        }
    }

    public void setTierLit(int tier, boolean lit) {
        allLit = false;
        if (lit) {
            litTiers.set(tier);
        } else {
            litTiers.clear(tier);
        }
    }

    public boolean isTierLit(int tier) {
        return allLit || litTiers.get(tier);
    }

    public int tierCount(int spacing) {
        int step = Math.max(8, spacing);
        return Math.max(1, height / step);
    }
}
