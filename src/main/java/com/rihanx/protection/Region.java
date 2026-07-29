package com.rihanx.protection;

import com.rihanx.edit.Cuboid;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Named protected cuboid region.
 */
public final class Region {

    private final @NotNull String name;
    private final @NotNull String worldName;
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;
    private final @NotNull Set<UUID> owners = new HashSet<>();
    private final @NotNull Set<UUID> members = new HashSet<>();
    private final @NotNull Map<ProtectionFlag, FlagValue> flags = new EnumMap<>(ProtectionFlag.class);

    public Region(
            @NotNull String name,
            @NotNull String worldName,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ
    ) {
        this.name = name.toLowerCase(Locale.ROOT);
        this.worldName = worldName;
        setBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public Region(@NotNull String name, @NotNull Cuboid cuboid) {
        this(name, cuboid.getWorld().getName(), cuboid.getMinX(), cuboid.getMinY(), cuboid.getMinZ(),
                cuboid.getMaxX(), cuboid.getMaxY(), cuboid.getMaxZ());
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull String getWorldName() {
        return worldName;
    }

    public void setBounds(int x1, int y1, int z1, int x2, int y2, int z2) {
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public void setBounds(@NotNull Cuboid cuboid) {
        setBounds(cuboid.getMinX(), cuboid.getMinY(), cuboid.getMinZ(),
                cuboid.getMaxX(), cuboid.getMaxY(), cuboid.getMaxZ());
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean contains(@NotNull Location location) {
        if (location.getWorld() == null || !location.getWorld().getName().equals(worldName)) {
            return false;
        }
        return contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public @Nullable Cuboid toCuboid(@NotNull World world) {
        if (!world.getName().equals(worldName)) {
            return null;
        }
        return new Cuboid(world, minX, minY, minZ, maxX, maxY, maxZ);
    }

    public @NotNull Set<UUID> getOwners() {
        return Collections.unmodifiableSet(owners);
    }

    public @NotNull Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public void addOwner(@NotNull UUID uuid) {
        owners.add(uuid);
    }

    public void removeOwner(@NotNull UUID uuid) {
        owners.remove(uuid);
    }

    public void addMember(@NotNull UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(@NotNull UUID uuid) {
        members.remove(uuid);
    }

    public boolean isOwner(@NotNull UUID uuid) {
        return owners.contains(uuid);
    }

    public boolean isMember(@NotNull UUID uuid) {
        return members.contains(uuid) || owners.contains(uuid);
    }

    public boolean isMember(@NotNull Player player) {
        return isMember(player.getUniqueId());
    }

    public void setFlag(@NotNull ProtectionFlag flag, @NotNull FlagValue value) {
        if (value == FlagValue.UNSET) {
            flags.remove(flag);
        } else {
            flags.put(flag, value);
        }
    }

    public @NotNull FlagValue getFlag(@NotNull ProtectionFlag flag) {
        return flags.getOrDefault(flag, FlagValue.UNSET);
    }

    public @NotNull Map<ProtectionFlag, FlagValue> getFlags() {
        return Collections.unmodifiableMap(flags);
    }

    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }
}
