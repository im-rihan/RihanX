package com.rihanx.kingdom;

import com.rihanx.RihanX;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * YAML persistence for kingdoms, membership, towers, and gates.
 */
public final class KingdomStore {

    private final @NotNull RihanX plugin;
    private final @NotNull File file;
    private final @NotNull Map<String, Kingdom> byId = new ConcurrentHashMap<>();

    public KingdomStore(@NotNull RihanX plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kingdoms.yml");
        load();
    }

    public void load() {
        byId.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("kingdoms");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            try {
                byId.put(id.toLowerCase(Locale.ROOT), readKingdom(id, section));
            } catch (RuntimeException ex) {
                plugin.getLogger().log(Level.WARNING, "Skipping corrupt kingdom '" + id + "'", ex);
            }
        }
    }

    public void save() {
        FileConfiguration yaml = new YamlConfiguration();
        for (Kingdom kingdom : byId.values()) {
            writeKingdom(yaml, kingdom);
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create data folder for kingdoms.yml");
            }
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save kingdoms.yml", ex);
        }
    }

    public @Nullable Kingdom get(@NotNull String id) {
        return byId.get(id.toLowerCase(Locale.ROOT));
    }

    public void put(@NotNull Kingdom kingdom) {
        byId.put(kingdom.id(), kingdom);
        save();
    }

    public boolean remove(@NotNull String id) {
        Kingdom removed = byId.remove(id.toLowerCase(Locale.ROOT));
        if (removed != null) {
            save();
            return true;
        }
        return false;
    }

    public @NotNull Collection<Kingdom> all() {
        return List.copyOf(byId.values());
    }

    public int size() {
        return byId.size();
    }

    private @NotNull Kingdom readKingdom(@NotNull String id, @NotNull ConfigurationSection section) {
        UUID sovereign = UUID.fromString(section.getString("sovereign", UUID.randomUUID().toString()));
        KingdomBounds bounds = new KingdomBounds(
                section.getString("world", "world"),
                section.getInt("center-x"),
                section.getInt("center-z"),
                Math.max(1, section.getInt("radius", 128)),
                section.getInt("min-y", -64),
                section.getInt("max-y", 320)
        );
        Kingdom kingdom = new Kingdom(id, section.getString("display-name", id), sovereign, bounds);
        kingdom.setFoundedAt(section.getLong("founded-at", System.currentTimeMillis()));
        kingdom.setTowerHeight(section.getInt("tower-height", 128));
        kingdom.setTowersBuilt(section.getBoolean("towers-built", false));
        kingdom.setBannerMaterial(section.getString("banner"));
        kingdom.setBeaconColor(BeaconColor.parse(section.getString("beacon", "#B87333")));
        if (section.getBoolean("beacon-rainbow", false)) {
            kingdom.setBeaconColor(new BeaconColor(kingdom.beaconColor().rgb(), true));
        }

        ConfigurationSection members = section.getConfigurationSection("members");
        if (members != null) {
            for (String raw : members.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(raw);
                    KingdomRole role = KingdomRole.fromKey(members.getString(raw, "citizen"));
                    if (role != null) {
                        kingdom.setRole(uuid, role);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        List<String> sealed = section.getStringList("sealed");
        if (!sealed.isEmpty()) {
            for (WardLayer layer : WardLayer.values()) {
                kingdom.unseal(layer);
            }
            for (String key : sealed) {
                WardLayer layer = WardLayer.fromKey(key);
                if (layer != null) {
                    kingdom.seal(layer);
                }
            }
        }

        ConfigurationSection rules = section.getConfigurationSection("rules");
        if (rules != null) {
            kingdom.rules().setPvp(rules.getBoolean("pvp", false));
            kingdom.rules().setMobSpawn(rules.getBoolean("mobs", false));
            kingdom.rules().setFireSpread(rules.getBoolean("fire", false));
            kingdom.rules().setExplosions(rules.getBoolean("explosions", false));
        }

        ConfigurationSection gates = section.getConfigurationSection("gates");
        if (gates != null) {
            for (String gateName : gates.getKeys(false)) {
                ConfigurationSection g = gates.getConfigurationSection(gateName);
                if (g == null) {
                    continue;
                }
                GateSide side = GateSide.fromKey(g.getString("side", "north"));
                if (side == null) {
                    continue;
                }
                KingdomGate gate = new KingdomGate(gateName, side, g.getInt("x"), g.getInt("y"), g.getInt("z"));
                gate.setLanterns(g.getBoolean("lanterns", true));
                gate.setOpenUntilEpochMs(g.getLong("open-until", 0L));
                kingdom.putGate(gate);
            }
        }

        ConfigurationSection towers = section.getConfigurationSection("towers");
        if (towers != null) {
            for (String key : towers.getKeys(false)) {
                ConfigurationSection t = towers.getConfigurationSection(key);
                TowerType type = TowerType.fromKey(key);
                if (t == null || type == null) {
                    continue;
                }
                KingdomTower tower = new KingdomTower(
                        type,
                        t.getInt("x"),
                        t.getInt("z"),
                        t.getInt("ground-y", 64),
                        t.getInt("height", kingdom.towerHeight())
                );
                tower.setBuilt(t.getBoolean("built", false));
                tower.setAllLit(t.getBoolean("all-lit", true));
                kingdom.putTower(tower);
            }
        }

        if (section.contains("treasury.x")) {
            kingdom.setTreasury(
                    section.getInt("treasury.x"),
                    section.getInt("treasury.y"),
                    section.getInt("treasury.z"),
                    section.getString("treasury.world", bounds.worldName())
            );
        }
        return kingdom;
    }

    private void writeKingdom(@NotNull FileConfiguration yaml, @NotNull Kingdom kingdom) {
        String path = "kingdoms." + kingdom.id();
        yaml.set(path + ".display-name", kingdom.displayName());
        yaml.set(path + ".sovereign", kingdom.sovereign().toString());
        yaml.set(path + ".world", kingdom.bounds().worldName());
        yaml.set(path + ".center-x", kingdom.bounds().centerX());
        yaml.set(path + ".center-z", kingdom.bounds().centerZ());
        yaml.set(path + ".radius", kingdom.bounds().radius());
        yaml.set(path + ".min-y", kingdom.bounds().minY());
        yaml.set(path + ".max-y", kingdom.bounds().maxY());
        yaml.set(path + ".founded-at", kingdom.foundedAt());
        yaml.set(path + ".tower-height", kingdom.towerHeight());
        yaml.set(path + ".towers-built", kingdom.towersBuilt());
        yaml.set(path + ".banner", kingdom.bannerMaterial());
        yaml.set(path + ".beacon", kingdom.beaconColor().hex());
        yaml.set(path + ".beacon-rainbow", kingdom.beaconColor().rainbow());

        for (Map.Entry<UUID, KingdomRole> entry : kingdom.members().entrySet()) {
            yaml.set(path + ".members." + entry.getKey(), entry.getValue().name());
        }

        List<String> sealed = new ArrayList<>();
        for (WardLayer layer : kingdom.sealedLayers()) {
            sealed.add(layer.key());
        }
        yaml.set(path + ".sealed", sealed);

        yaml.set(path + ".rules.pvp", kingdom.rules().pvp());
        yaml.set(path + ".rules.mobs", kingdom.rules().mobSpawn());
        yaml.set(path + ".rules.fire", kingdom.rules().fireSpread());
        yaml.set(path + ".rules.explosions", kingdom.rules().explosions());

        for (KingdomGate gate : kingdom.gates().values()) {
            String g = path + ".gates." + gate.name();
            yaml.set(g + ".side", gate.side().name());
            yaml.set(g + ".x", gate.x());
            yaml.set(g + ".y", gate.y());
            yaml.set(g + ".z", gate.z());
            yaml.set(g + ".lanterns", gate.lanterns());
            yaml.set(g + ".open-until", gate.openUntilEpochMs());
        }

        for (KingdomTower tower : kingdom.towers().values()) {
            String t = path + ".towers." + tower.type().key();
            yaml.set(t + ".x", tower.x());
            yaml.set(t + ".z", tower.z());
            yaml.set(t + ".ground-y", tower.groundY());
            yaml.set(t + ".height", tower.height());
            yaml.set(t + ".built", tower.built());
            yaml.set(t + ".all-lit", tower.allLit());
        }

        if (kingdom.hasTreasury()) {
            yaml.set(path + ".treasury.world", kingdom.treasuryWorld());
            yaml.set(path + ".treasury.x", kingdom.treasuryX());
            yaml.set(path + ".treasury.y", kingdom.treasuryY());
            yaml.set(path + ".treasury.z", kingdom.treasuryZ());
        }
    }
}
