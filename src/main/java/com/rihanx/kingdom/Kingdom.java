package com.rihanx.kingdom;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A fully self-contained protected realm: identity, sealed volume, towers, gates, and rules.
 */
public final class Kingdom {

    private final @NotNull String id;
    private @NotNull String displayName;
    private @NotNull UUID sovereign;
    private @NotNull KingdomBounds bounds;
    private final @NotNull Map<UUID, KingdomRole> members = new ConcurrentHashMap<>();
    private final @NotNull EnumSet<WardLayer> sealed = EnumSet.allOf(WardLayer.class);
    private final @NotNull Map<String, KingdomGate> gates = new LinkedHashMap<>();
    private final @NotNull Map<TowerType, KingdomTower> towers = new EnumMap<>(TowerType.class);
    private final @NotNull KingdomRules rules = new KingdomRules();
    private @NotNull BeaconColor beaconColor = new BeaconColor(BeaconColor.DEFAULT_COPPER, false);
    private int towerHeight = 128;
    private boolean towersBuilt;
    private @Nullable String bannerMaterial;
    private @Nullable String treasuryWorld;
    private int treasuryX;
    private int treasuryY;
    private int treasuryZ;
    private boolean treasurySet;
    private final @NotNull Set<UUID> pendingInvites = ConcurrentHashMap.newKeySet();
    private long foundedAt = System.currentTimeMillis();

    public Kingdom(
            @NotNull String id,
            @NotNull String displayName,
            @NotNull UUID sovereign,
            @NotNull KingdomBounds bounds
    ) {
        this.id = id.toLowerCase(Locale.ROOT);
        this.displayName = displayName;
        this.sovereign = sovereign;
        this.bounds = bounds;
        this.members.put(sovereign, KingdomRole.SOVEREIGN);
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull String displayName() {
        return displayName;
    }

    public void setDisplayName(@NotNull String displayName) {
        this.displayName = displayName;
    }

    public @NotNull UUID sovereign() {
        return sovereign;
    }

    public void setSovereign(@NotNull UUID sovereign) {
        members.remove(this.sovereign);
        this.sovereign = sovereign;
        members.put(sovereign, KingdomRole.SOVEREIGN);
    }

    public @NotNull KingdomBounds bounds() {
        return bounds;
    }

    public void setBounds(@NotNull KingdomBounds bounds) {
        this.bounds = bounds;
    }

    public @NotNull KingdomRules rules() {
        return rules;
    }

    public @NotNull BeaconColor beaconColor() {
        return beaconColor;
    }

    public void setBeaconColor(@NotNull BeaconColor beaconColor) {
        this.beaconColor = beaconColor;
    }

    public int towerHeight() {
        return towerHeight;
    }

    public void setTowerHeight(int towerHeight) {
        this.towerHeight = Math.max(8, towerHeight);
        for (KingdomTower tower : towers.values()) {
            int height = tower.type().isCentral() ? this.towerHeight + 16 : this.towerHeight;
            tower.setHeight(height);
        }
    }

    public boolean towersBuilt() {
        return towersBuilt;
    }

    public void setTowersBuilt(boolean towersBuilt) {
        this.towersBuilt = towersBuilt;
    }

    public @Nullable String bannerMaterial() {
        return bannerMaterial;
    }

    public void setBannerMaterial(@Nullable String bannerMaterial) {
        this.bannerMaterial = bannerMaterial;
    }

    public long foundedAt() {
        return foundedAt;
    }

    public void setFoundedAt(long foundedAt) {
        this.foundedAt = foundedAt;
    }

    public @NotNull String regionName() {
        return "rxk_" + id;
    }

    public boolean isSealed(@NotNull WardLayer layer) {
        return sealed.contains(layer);
    }

    public void sealAll() {
        sealed.addAll(EnumSet.allOf(WardLayer.class));
    }

    public void unseal(@NotNull WardLayer layer) {
        sealed.remove(layer);
    }

    public void seal(@NotNull WardLayer layer) {
        sealed.add(layer);
    }

    public @NotNull Set<WardLayer> sealedLayers() {
        return EnumSet.copyOf(sealed);
    }

    public @Nullable KingdomRole roleOf(@NotNull UUID playerId) {
        return members.get(playerId);
    }

    public boolean isMember(@NotNull UUID playerId) {
        return members.containsKey(playerId);
    }

    public boolean canBuild(@NotNull UUID playerId) {
        KingdomRole role = members.get(playerId);
        return role != null && role.canBuild();
    }

    public boolean canManage(@NotNull UUID playerId) {
        KingdomRole role = members.get(playerId);
        return role != null && role.canManage();
    }

    public boolean canInvite(@NotNull UUID playerId) {
        KingdomRole role = members.get(playerId);
        return role != null && role.canInvite();
    }

    public void setRole(@NotNull UUID playerId, @NotNull KingdomRole role) {
        if (role == KingdomRole.SOVEREIGN) {
            setSovereign(playerId);
            return;
        }
        if (playerId.equals(sovereign)) {
            return;
        }
        members.put(playerId, role);
    }

    public boolean kick(@NotNull UUID playerId) {
        if (playerId.equals(sovereign)) {
            return false;
        }
        return members.remove(playerId) != null;
    }

    public @NotNull Map<UUID, KingdomRole> members() {
        return Map.copyOf(members);
    }

    public void addInvite(@NotNull UUID playerId) {
        pendingInvites.add(playerId);
    }

    public boolean consumeInvite(@NotNull UUID playerId) {
        return pendingInvites.remove(playerId);
    }

    public boolean hasInvite(@NotNull UUID playerId) {
        return pendingInvites.contains(playerId);
    }

    public @NotNull Collection<UUID> pendingInvites() {
        return Set.copyOf(pendingInvites);
    }

    public @NotNull Map<String, KingdomGate> gates() {
        return gates;
    }

    public @Nullable KingdomGate gate(@NotNull String name) {
        return gates.get(name.toLowerCase(Locale.ROOT));
    }

    public void putGate(@NotNull KingdomGate gate) {
        gates.put(gate.name(), gate);
    }

    public boolean removeGate(@NotNull String name) {
        return gates.remove(name.toLowerCase(Locale.ROOT)) != null;
    }

    public boolean isOpenGate(int x, int y, int z, long nowMs) {
        for (KingdomGate gate : gates.values()) {
            if (gate.allowsPassage(x, y, z, nowMs)) {
                return true;
            }
        }
        return false;
    }

    public @NotNull Map<TowerType, KingdomTower> towers() {
        return towers;
    }

    public @Nullable KingdomTower tower(@NotNull TowerType type) {
        return towers.get(type);
    }

    public @Nullable KingdomTower towerByName(@NotNull String name) {
        String key = name.toLowerCase(Locale.ROOT);
        for (KingdomTower tower : towers.values()) {
            if (tower.name(displayName).equalsIgnoreCase(name)
                    || tower.name(id).equalsIgnoreCase(name)
                    || tower.type().key().equals(key)
                    || tower.type().label().equalsIgnoreCase(name)) {
                return tower;
            }
        }
        return null;
    }

    public void putTower(@NotNull KingdomTower tower) {
        towers.put(tower.type(), tower);
    }

    public @Nullable KingdomTower nearestTower(int x, int z) {
        KingdomTower best = null;
        long bestDist = Long.MAX_VALUE;
        for (KingdomTower tower : towers.values()) {
            long dx = (long) tower.x() - x;
            long dz = (long) tower.z() - z;
            long dist = dx * dx + dz * dz;
            if (dist < bestDist) {
                bestDist = dist;
                best = tower;
            }
        }
        return best;
    }

    public void setTreasury(int x, int y, int z, @NotNull String worldName) {
        this.treasuryWorld = worldName;
        this.treasuryX = x;
        this.treasuryY = y;
        this.treasuryZ = z;
        this.treasurySet = true;
    }

    public boolean hasTreasury() {
        return treasurySet && treasuryWorld != null;
    }

    public @Nullable String treasuryWorld() {
        return treasuryWorld;
    }

    public int treasuryX() {
        return treasuryX;
    }

    public int treasuryY() {
        return treasuryY;
    }

    public int treasuryZ() {
        return treasuryZ;
    }

    public boolean isTreasuryBlock(int x, int y, int z, @NotNull String worldName) {
        return treasurySet
                && treasuryWorld != null
                && treasuryWorld.equals(worldName)
                && treasuryX == x
                && treasuryY == y
                && treasuryZ == z;
    }
}
