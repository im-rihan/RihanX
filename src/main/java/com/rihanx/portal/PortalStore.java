package com.rihanx.portal;

import com.rihanx.RihanX;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * YAML-backed portal pads ({@code portals.yml}).
 * <p>{@code portals.<name>} → world, x, y, z, yaw, pitch, link
 */
public final class PortalStore {

    private final @Nullable Logger logger;
    private final @NotNull File file;
    private final @NotNull Map<String, StoredPortal> portals = new ConcurrentHashMap<>();

    public PortalStore(@NotNull RihanX plugin) {
        this(new File(plugin.getDataFolder(), "portals.yml"), plugin.getLogger());
    }

    /** Test / custom-file constructor (logger may be null). */
    public PortalStore(@NotNull File file, @Nullable Logger logger) {
        this.file = file;
        this.logger = logger;
        load();
    }

    public void load() {
        portals.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("portals");
        if (root == null) {
            return;
        }
        for (String name : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(name);
            if (section == null) {
                continue;
            }
            StoredPortal portal = StoredPortal.read(section);
            if (portal != null) {
                portals.put(normalize(name), portal);
            }
        }
    }

    public void save() {
        FileConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, StoredPortal> entry : portals.entrySet()) {
            entry.getValue().write(yaml, "portals." + entry.getKey());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                if (logger != null) {
                    logger.warning("Could not create data folder for portals.yml");
                }
            }
            yaml.save(file);
        } catch (IOException ex) {
            if (logger != null) {
                logger.log(Level.SEVERE, "Failed to save portals.yml", ex);
            }
        }
    }

    /** Insert a portal without a live {@link Location} (tests / restore). */
    public boolean putRaw(@NotNull String name, @NotNull StoredPortal portal) {
        portals.put(normalize(name), portal);
        save();
        return true;
    }

    public @Nullable StoredPortal get(@NotNull String name) {
        return portals.get(normalize(name));
    }

    public boolean set(@NotNull String name, @NotNull Location location, @Nullable String link) {
        StoredPortal portal = StoredPortal.from(location, link == null ? null : normalize(link));
        if (portal == null) {
            return false;
        }
        portals.put(normalize(name), portal);
        save();
        return true;
    }

    public boolean setLink(@NotNull String name, @Nullable String link) {
        StoredPortal existing = portals.get(normalize(name));
        if (existing == null) {
            return false;
        }
        portals.put(normalize(name), existing.withLink(link == null ? null : normalize(link)));
        save();
        return true;
    }

    public boolean delete(@NotNull String name) {
        String key = normalize(name);
        StoredPortal removed = portals.remove(key);
        if (removed == null) {
            return false;
        }
        for (Map.Entry<String, StoredPortal> entry : portals.entrySet()) {
            if (key.equals(entry.getValue().link())) {
                portals.put(entry.getKey(), entry.getValue().withLink(null));
            }
        }
        save();
        return true;
    }

    /** Remove every portal / station stop. Returns how many were deleted. */
    public int clearAll() {
        int count = portals.size();
        if (count == 0) {
            return 0;
        }
        portals.clear();
        save();
        return count;
    }

    public boolean has(@NotNull String name) {
        return portals.containsKey(normalize(name));
    }

    public @NotNull List<String> list() {
        if (portals.isEmpty()) {
            return List.of();
        }
        List<String> names = new ArrayList<>(portals.keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return Collections.unmodifiableList(names);
    }

    public @NotNull Map<String, StoredPortal> all() {
        return Map.copyOf(portals);
    }

    static @NotNull String normalize(@NotNull String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public record StoredPortal(
            @NotNull String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            @Nullable String link
    ) {
        static @Nullable StoredPortal from(@NotNull Location location, @Nullable String link) {
            World world = location.getWorld();
            if (world == null) {
                return null;
            }
            return new StoredPortal(
                    world.getName(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch(),
                    link
            );
        }

        static @Nullable StoredPortal read(@NotNull ConfigurationSection section) {
            String world = section.getString("world");
            if (world == null || world.isBlank()) {
                return null;
            }
            String link = section.getString("link");
            if (link != null && link.isBlank()) {
                link = null;
            }
            return new StoredPortal(
                    world,
                    section.getDouble("x"),
                    section.getDouble("y"),
                    section.getDouble("z"),
                    (float) section.getDouble("yaw"),
                    (float) section.getDouble("pitch"),
                    link == null ? null : normalize(link)
            );
        }

        void write(@NotNull FileConfiguration yaml, @NotNull String path) {
            yaml.set(path + ".world", worldName);
            yaml.set(path + ".x", x);
            yaml.set(path + ".y", y);
            yaml.set(path + ".z", z);
            yaml.set(path + ".yaw", yaw);
            yaml.set(path + ".pitch", pitch);
            yaml.set(path + ".link", link);
        }

        @Nullable Location toLocation() {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return null;
            }
            return new Location(world, x, y, z, yaw, pitch);
        }

        @NotNull StoredPortal withLink(@Nullable String newLink) {
            return new StoredPortal(worldName, x, y, z, yaw, pitch, newLink);
        }
    }
}
