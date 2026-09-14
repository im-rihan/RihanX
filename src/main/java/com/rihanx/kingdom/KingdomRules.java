package com.rihanx.kingdom;

/**
 * Interior sanctum toggles. Defaults match a sealed Old Kingdom citadel.
 */
public final class KingdomRules {

    private boolean pvp;
    private boolean mobSpawn;
    private boolean fireSpread;
    private boolean explosions;

    public KingdomRules() {
        this.pvp = false;
        this.mobSpawn = false;
        this.fireSpread = false;
        this.explosions = false;
    }

    public boolean pvp() {
        return pvp;
    }

    public void setPvp(boolean pvp) {
        this.pvp = pvp;
    }

    public boolean mobSpawn() {
        return mobSpawn;
    }

    public void setMobSpawn(boolean mobSpawn) {
        this.mobSpawn = mobSpawn;
    }

    public boolean fireSpread() {
        return fireSpread;
    }

    public void setFireSpread(boolean fireSpread) {
        this.fireSpread = fireSpread;
    }

    public boolean explosions() {
        return explosions;
    }

    public void setExplosions(boolean explosions) {
        this.explosions = explosions;
    }

    public boolean toggle(@org.jetbrains.annotations.NotNull String key) {
        return switch (key) {
            case "pvp" -> {
                pvp = !pvp;
                yield pvp;
            }
            case "mobs", "mob", "mob-spawn", "spawn" -> {
                mobSpawn = !mobSpawn;
                yield mobSpawn;
            }
            case "fire", "fire-spread" -> {
                fireSpread = !fireSpread;
                yield fireSpread;
            }
            case "explosions", "tnt", "explode" -> {
                explosions = !explosions;
                yield explosions;
            }
            default -> throw new IllegalArgumentException(key);
        };
    }
}
