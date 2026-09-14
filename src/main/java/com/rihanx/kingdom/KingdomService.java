package com.rihanx.kingdom;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.managers.MessageManager;
import com.rihanx.protection.FlagValue;
import com.rihanx.protection.ProtectionFlag;
import com.rihanx.protection.Region;
import com.rihanx.teleport.TeleportManager;
import com.rihanx.utils.MessageUtil;
import com.rihanx.utils.PermissionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Facade for founding, sealing, lighting, and defending Old Kingdom citadels.
 */
public final class KingdomService {

    public static final String COMPASS_NAME = "Warden's Compass";
    private static final Set<Material> LIGHT_ITEMS = EnumSet.of(
            Material.TORCH,
            Material.SOUL_TORCH,
            Material.LANTERN,
            Material.SOUL_LANTERN,
            Material.GLOWSTONE,
            Material.SEA_LANTERN,
            Material.SHROOMLIGHT,
            Material.JACK_O_LANTERN,
            Material.GLOW_INK_SAC,
            Material.COPPER_BULB
    );

    private final @NotNull RihanX plugin;
    private final @NotNull MessageManager messages;
    private final @NotNull TeleportManager teleportManager;
    private final @NotNull KingdomStore store;
    private final @NotNull KingdomSettings settings;
    private final @NotNull KingdomBuilder builder;
    private final @NotNull Map<String, Map<Long, String>> chunkIndex = new ConcurrentHashMap<>();
    private final @NotNull Map<UUID, Boolean> wardShow = new ConcurrentHashMap<>();
    private final @NotNull Map<UUID, Boolean> kingdomChat = new ConcurrentHashMap<>();
    private final @NotNull Map<UUID, String> pendingDissolve = new ConcurrentHashMap<>();
    private final @NotNull Map<UUID, Location> lastPlayerLight = new ConcurrentHashMap<>();
    private final @NotNull Deque<FluidHistoryEntry> fluidHistory = new ArrayDeque<>();
    private final @NotNull Map<String, Long> lastBreachFlash = new ConcurrentHashMap<>();
    private @Nullable BukkitTask lightingTask;
    private long tick;

    public KingdomService(
            @NotNull RihanX plugin,
            @NotNull MessageManager messages,
            @NotNull TeleportManager teleportManager
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.teleportManager = teleportManager;
        this.settings = new KingdomSettings(plugin.getConfig());
        this.store = new KingdomStore(plugin);
        this.builder = new KingdomBuilder(plugin, this);
        rebuildChunkIndex();
        startLighting();
    }

    public @NotNull KingdomSettings settings() {
        return settings;
    }

    public @NotNull KingdomStore store() {
        return store;
    }

    public void reload() {
        settings.refresh(plugin.getConfig());
        store.load();
        rebuildChunkIndex();
    }

    public void save() {
        store.save();
    }

    public void shutdown() {
        if (lightingTask != null) {
            lightingTask.cancel();
            lightingTask = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            restorePlayerLight(player);
        }
        store.save();
    }

    public @NotNull List<Kingdom> all() {
        return List.copyOf(store.all());
    }

    public @Nullable Kingdom byId(@NotNull String id) {
        return store.get(id);
    }

    public @Nullable Kingdom at(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        return at(world.getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public @Nullable Kingdom at(@NotNull String worldName, int x, int y, int z) {
        Map<Long, String> chunks = chunkIndex.get(worldName);
        if (chunks != null) {
            String id = chunks.get(chunkKey(x >> 4, z >> 4));
            if (id != null) {
                Kingdom candidate = store.get(id);
                if (candidate != null && candidate.bounds().contains(x, y, z)) {
                    return candidate;
                }
            }
        }
        for (Kingdom kingdom : store.all()) {
            if (kingdom.bounds().containsWorld(worldName, x, y, z)) {
                return kingdom;
            }
        }
        return null;
    }

    public @Nullable Kingdom resolve(@NotNull Player player, @Nullable String name) {
        if (name != null && !name.isBlank()) {
            return store.get(name);
        }
        Kingdom standing = at(player.getLocation());
        if (standing != null && standing.isMember(player.getUniqueId())) {
            return standing;
        }
        Kingdom owned = null;
        for (Kingdom kingdom : store.all()) {
            if (kingdom.sovereign().equals(player.getUniqueId())) {
                if (owned != null) {
                    return standing;
                }
                owned = kingdom;
            }
        }
        return owned != null ? owned : standing;
    }

    public boolean hasBypass(@Nullable Player player) {
        if (player == null) {
            return false;
        }
        return PermissionUtil.has(player, PermissionNodes.KINGDOM_BYPASS)
                || plugin.getProtectionService().hasBypass(player);
    }

    public void create(@NotNull Player player, @NotNull String rawName, @Nullable Integer radiusArg) {
        int owned = 0;
        for (Kingdom kingdom : store.all()) {
            if (kingdom.sovereign().equals(player.getUniqueId())) {
                owned++;
            }
        }
        if (!KingdomFounding.validName(rawName)) {
            messages.send(player, "kingdom-name-invalid");
            return;
        }
        String id = KingdomFounding.normalizeId(rawName);
        if (store.get(id) != null) {
            messages.send(player, "kingdom-exists", MessageManager.placeholders("name", rawName));
            return;
        }
        if (!player.isOp() && owned >= settings.maxKingdomsPerPlayer()) {
            messages.send(player, "kingdom-limit", MessageManager.placeholders("max", settings.maxKingdomsPerPlayer()));
            return;
        }
        int radius = KingdomFounding.resolveRadius(
                radiusArg,
                settings.defaultRadius(),
                settings.minRadius(),
                settings.maxRadius()
        );
        if (radius < 0) {
            messages.send(player, "kingdom-radius-invalid", MessageManager.placeholders(
                    "min", settings.minRadius(),
                    "max", settings.maxRadius()
            ));
            return;
        }
        World world = player.getWorld();
        int minY = Math.max(world.getMinHeight(), settings.foundationY());
        int maxY = Math.min(world.getMaxHeight() - 1, settings.ceilingY());
        KingdomBounds bounds = new KingdomBounds(
                world.getName(),
                player.getLocation().getBlockX(),
                player.getLocation().getBlockZ(),
                radius,
                minY,
                maxY
        );
        List<KingdomBounds> existing = new ArrayList<>();
        for (Kingdom kingdom : store.all()) {
            existing.add(kingdom.bounds());
        }
        if (KingdomFounding.overlapsAny(bounds, existing)) {
            Kingdom overlap = null;
            for (Kingdom kingdom : store.all()) {
                if (kingdom.bounds().overlaps(bounds)) {
                    overlap = kingdom;
                    break;
                }
            }
            messages.send(player, "kingdom-overlap", MessageManager.placeholders(
                    "name", overlap == null ? "another kingdom" : overlap.displayName()
            ));
            return;
        }

        Kingdom kingdom = new Kingdom(id, rawName, player.getUniqueId(), bounds);
        kingdom.setTowerHeight(Math.min(settings.defaultTowerHeight(), settings.maxTowerHeight()));
        if (settings.beaconRainbow()) {
            kingdom.setBeaconColor(new BeaconColor(BeaconColor.parse(settings.beaconColor()).rgb(), true));
        } else {
            kingdom.setBeaconColor(BeaconColor.parse(settings.beaconColor()));
        }
        ensureTowers(kingdom, world);
        store.put(kingdom);
        syncProtectionRegion(kingdom);
        rebuildChunkIndex();
        giveCompass(player);
        messages.send(player, "kingdom-created", MessageManager.placeholders(
                "name", kingdom.displayName(),
                "radius", radius
        ));
        raiseCitadel(player, kingdom);
    }

    private void raiseCitadel(@NotNull Player player, @NotNull Kingdom kingdom) {
        if (settings.citadelKeep()) {
            Location at = player.getLocation();
            if (!kingdom.hasTreasury()) {
                kingdom.setTreasury(at.getBlockX(), at.getBlockY() + 1, at.getBlockZ(), kingdom.bounds().worldName());
                store.put(kingdom);
            }
            messages.send(player, "kingdom-citadel-rising", MessageManager.placeholders(
                    "name", kingdom.displayName()
            ));
            boolean started = plugin.getBaseService().pasteForKingdom(
                    player,
                    settings.citadelTemplate(),
                    () -> {
                        if (player.isOnline() && settings.autoBuildOnCreate()) {
                            builder.buildTowers(player, kingdom);
                        }
                    }
            );
            if (!started && settings.autoBuildOnCreate()) {
                builder.buildTowers(player, kingdom);
            }
            return;
        }
        if (settings.autoBuildOnCreate()) {
            builder.buildTowers(player, kingdom);
        }
    }

    public void expand(@NotNull Player player, @NotNull Kingdom kingdom, int blocks) {
        if (!kingdom.canManage(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-warden");
            return;
        }
        int next = kingdom.bounds().radius() + blocks;
        if (next < settings.minRadius() || next > settings.maxRadius()) {
            messages.send(player, "kingdom-radius-invalid", MessageManager.placeholders(
                    "min", settings.minRadius(),
                    "max", settings.maxRadius()
            ));
            return;
        }
        KingdomBounds expanded = kingdom.bounds().expanded(blocks);
        for (Kingdom existing : store.all()) {
            if (!existing.id().equals(kingdom.id()) && existing.bounds().overlaps(expanded)) {
                messages.send(player, "kingdom-overlap", MessageManager.placeholders("name", existing.displayName()));
                return;
            }
        }
        kingdom.setBounds(expanded);
        ensureTowers(kingdom, player.getWorld());
        store.put(kingdom);
        syncProtectionRegion(kingdom);
        rebuildChunkIndex();
        messages.send(player, "kingdom-expanded", MessageManager.placeholders(
                "name", kingdom.displayName(),
                "radius", expanded.radius()
        ));
    }

    public void seal(@NotNull Player player, @NotNull Kingdom kingdom) {
        if (!kingdom.canManage(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-warden");
            return;
        }
        kingdom.sealAll();
        store.put(kingdom);
        syncProtectionRegion(kingdom);
        messages.send(player, "kingdom-sealed", MessageManager.placeholders("name", kingdom.displayName()));
    }

    public void unseal(@NotNull Player player, @NotNull Kingdom kingdom, @NotNull WardLayer layer) {
        if (!kingdom.canManage(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-warden");
            return;
        }
        kingdom.unseal(layer);
        store.put(kingdom);
        syncProtectionRegion(kingdom);
        messages.send(player, "kingdom-unsealed", MessageManager.placeholders(
                "name", kingdom.displayName(),
                "layer", layer.display()
        ));
    }

    public void requestDissolve(@NotNull Player player, @NotNull Kingdom kingdom) {
        if (!kingdom.sovereign().equals(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-sovereign");
            return;
        }
        pendingDissolve.put(player.getUniqueId(), kingdom.id());
        plugin.getSchedulerUtil().runSyncLater(() -> pendingDissolve.remove(player.getUniqueId()), 20L * 15);
        messages.send(player, "kingdom-dissolve-confirm", MessageManager.placeholders("name", kingdom.displayName()));
    }

    public void confirmDissolve(@NotNull Player player) {
        String id = pendingDissolve.remove(player.getUniqueId());
        if (id == null) {
            messages.send(player, "kingdom-dissolve-none");
            return;
        }
        Kingdom kingdom = store.get(id);
        if (kingdom == null) {
            messages.send(player, "kingdom-missing", MessageManager.placeholders("name", id));
            return;
        }
        plugin.getProtectionService().getRegions().remove(kingdom.bounds().worldName(), kingdom.regionName());
        store.remove(id);
        rebuildChunkIndex();
        messages.send(player, "kingdom-dissolved", MessageManager.placeholders("name", kingdom.displayName()));
    }

    public void sendInfo(@NotNull Player player, @NotNull Kingdom kingdom) {
        messages.send(player, "kingdom-info-header", MessageManager.placeholders("name", kingdom.displayName()));
        messages.send(player, "kingdom-info-line", MessageManager.placeholders(
                "key", "Sovereign",
                "value", nameOf(kingdom.sovereign())
        ));
        messages.send(player, "kingdom-info-line", MessageManager.placeholders(
                "key", "Bounds",
                "value", kingdom.bounds().radius() + "r  Y " + kingdom.bounds().minY() + "→" + kingdom.bounds().maxY()
        ));
        StringBuilder wards = new StringBuilder();
        for (WardLayer layer : WardLayer.values()) {
            if (!wards.isEmpty()) {
                wards.append(", ");
            }
            wards.append(layer.display()).append(kingdom.isSealed(layer) ? " ✓" : " ✗");
        }
        messages.send(player, "kingdom-info-line", MessageManager.placeholders("key", "Wards", "value", wards.toString()));
        messages.send(player, "kingdom-info-line", MessageManager.placeholders(
                "key", "Beacon",
                "value", kingdom.beaconColor().rainbow() ? "rainbow" : kingdom.beaconColor().hex()
        ));
        messages.send(player, "kingdom-info-line", MessageManager.placeholders(
                "key", "Members",
                "value", kingdom.members().size()
        ));
        messages.send(player, "kingdom-info-line", MessageManager.placeholders(
                "key", "Gates",
                "value", kingdom.gates().size()
        ));
        messages.send(player, "kingdom-info-line", MessageManager.placeholders(
                "key", "Towers",
                "value", kingdom.towersBuilt() ? "raised (" + kingdom.towerHeight() + "m)" : "not built"
        ));
        KingdomRole role = kingdom.roleOf(player.getUniqueId());
        messages.send(player, "kingdom-info-line", MessageManager.placeholders(
                "key", "Your rank",
                "value", role == null ? "outsider" : role.display()
        ));
    }

    public void sendList(@NotNull Player player) {
        List<Kingdom> all = all();
        if (all.isEmpty()) {
            messages.send(player, "kingdom-list-empty");
            return;
        }
        messages.send(player, "kingdom-list-header", MessageManager.placeholders("count", all.size()));
        for (Kingdom kingdom : all) {
            messages.send(player, "kingdom-list-line", MessageManager.placeholders(
                    "name", kingdom.displayName(),
                    "radius", kingdom.bounds().radius(),
                    "world", kingdom.bounds().worldName()
            ));
        }
    }

    public void invite(@NotNull Player player, @NotNull Kingdom kingdom, @NotNull Player target) {
        if (!kingdom.canInvite(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-warden");
            return;
        }
        if (kingdom.isMember(target.getUniqueId())) {
            messages.send(player, "kingdom-already-member", MessageManager.placeholders("player", target.getName()));
            return;
        }
        kingdom.addInvite(target.getUniqueId());
        store.put(kingdom);
        messages.send(player, "kingdom-invite-sent", MessageManager.placeholders(
                "player", target.getName(),
                "name", kingdom.displayName()
        ));
        messages.send(target, "kingdom-invite-received", MessageManager.placeholders("name", kingdom.displayName()));
    }

    public void acceptInvite(@NotNull Player player, @NotNull Kingdom kingdom) {
        if (!kingdom.hasInvite(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-invite-none", MessageManager.placeholders("name", kingdom.displayName()));
            return;
        }
        kingdom.consumeInvite(player.getUniqueId());
        kingdom.setRole(player.getUniqueId(), KingdomRole.CITIZEN);
        store.put(kingdom);
        syncProtectionRegion(kingdom);
        messages.send(player, "kingdom-joined", MessageManager.placeholders("name", kingdom.displayName()));
        notifyWardens(kingdom, "kingdom-member-joined", MessageManager.placeholders(
                "player", player.getName(),
                "name", kingdom.displayName()
        ));
    }

    public void kick(@NotNull Player player, @NotNull Kingdom kingdom, @NotNull UUID targetId, @NotNull String targetName) {
        if (!kingdom.canManage(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-warden");
            return;
        }
        KingdomRole targetRole = kingdom.roleOf(targetId);
        KingdomRole actorRole = kingdom.roleOf(player.getUniqueId());
        if (targetRole != null && actorRole != null && targetRole.rank() >= actorRole.rank() && !hasBypass(player)) {
            messages.send(player, "kingdom-kick-rank");
            return;
        }
        if (!kingdom.kick(targetId)) {
            messages.send(player, "kingdom-not-member", MessageManager.placeholders("player", targetName));
            return;
        }
        store.put(kingdom);
        syncProtectionRegion(kingdom);
        messages.send(player, "kingdom-kicked", MessageManager.placeholders(
                "player", targetName,
                "name", kingdom.displayName()
        ));
        Player online = Bukkit.getPlayer(targetId);
        if (online != null) {
            messages.send(online, "kingdom-you-kicked", MessageManager.placeholders("name", kingdom.displayName()));
        }
    }

    public void setMemberRole(
            @NotNull Player player,
            @NotNull Kingdom kingdom,
            @NotNull UUID targetId,
            @NotNull String targetName,
            @NotNull KingdomRole role
    ) {
        if (!kingdom.canManage(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-warden");
            return;
        }
        if (role == KingdomRole.SOVEREIGN && !kingdom.sovereign().equals(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-sovereign");
            return;
        }
        kingdom.setRole(targetId, role);
        store.put(kingdom);
        syncProtectionRegion(kingdom);
        messages.send(player, "kingdom-role-set", MessageManager.placeholders(
                "player", targetName,
                "role", role.display(),
                "name", kingdom.displayName()
        ));
    }

    public void toggleChat(@NotNull Player player) {
        boolean enabled = !Boolean.TRUE.equals(kingdomChat.get(player.getUniqueId()));
        if (enabled) {
            kingdomChat.put(player.getUniqueId(), true);
        } else {
            kingdomChat.remove(player.getUniqueId());
        }
        messages.send(player, enabled ? "kingdom-chat-on" : "kingdom-chat-off");
    }

    public boolean isKingdomChat(@NotNull Player player) {
        return Boolean.TRUE.equals(kingdomChat.get(player.getUniqueId()));
    }

    public void broadcastChat(@NotNull Player sender, @NotNull Component message) {
        Kingdom kingdom = at(sender.getLocation());
        if (kingdom == null || !kingdom.isMember(sender.getUniqueId())) {
            messages.send(sender, "kingdom-not-inside");
            return;
        }
        String plain = PlainTextComponentSerializer.plainText().serialize(message);
        Map<String, String> placeholders = MessageManager.placeholders(
                "name", kingdom.displayName(),
                "player", sender.getName(),
                "role", String.valueOf(kingdom.roleOf(sender.getUniqueId())),
                "message", plain
        );
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (kingdom.isMember(online.getUniqueId())) {
                messages.send(online, "kingdom-chat", placeholders);
            }
        }
    }

    public void sendChat(@NotNull Player sender, @NotNull String message) {
        broadcastChat(sender, Component.text(message));
    }

    public void toggleRule(@NotNull Player player, @NotNull Kingdom kingdom, @NotNull String key) {
        if (!kingdom.canManage(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-warden");
            return;
        }
        try {
            boolean value = kingdom.rules().toggle(key.toLowerCase(Locale.ROOT));
            store.put(kingdom);
            syncProtectionRegion(kingdom);
            messages.send(player, "kingdom-rule-set", MessageManager.placeholders(
                    "rule", key,
                    "value", value ? "allow" : "deny"
            ));
        } catch (IllegalArgumentException ex) {
            messages.send(player, "kingdom-rule-unknown", MessageManager.placeholders("rule", key));
        }
    }

    public void createGate(@NotNull Player player, @NotNull Kingdom kingdom, @NotNull String name, @NotNull GateSide side) {
        if (!kingdom.canManage(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-warden");
            return;
        }
        if (kingdom.gates().size() >= settings.maxGates()) {
            messages.send(player, "kingdom-gate-limit", MessageManager.placeholders("max", settings.maxGates()));
            return;
        }
        if (kingdom.gate(name) != null) {
            messages.send(player, "kingdom-gate-exists", MessageManager.placeholders("gate", name));
            return;
        }
        int[] anchor = kingdom.bounds().gateAnchor(player.getLocation().getBlockY(), side);
        KingdomGate gate = new KingdomGate(name, side, anchor[0], anchor[1], anchor[2]);
        gate.setLanterns(settings.gateLanterns());
        kingdom.putGate(gate);
        store.put(kingdom);
        builder.placeGateLanterns(player, player.getWorld(), gate, kingdom);
        if (settings.gateGuards()) {
            spawnGateGuard(player.getWorld(), gate, kingdom);
        }
        messages.send(player, "kingdom-gate-created", MessageManager.placeholders(
                "gate", gate.name(),
                "side", side.display()
        ));
    }

    public void openGate(@NotNull Player player, @NotNull Kingdom kingdom, @NotNull String name, @Nullable String durationRaw) {
        if (!kingdom.canManage(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-warden");
            return;
        }
        KingdomGate gate = kingdom.gate(name);
        if (gate == null) {
            messages.send(player, "kingdom-gate-missing", MessageManager.placeholders("gate", name));
            return;
        }
        long millis = DurationParser.millis(durationRaw, settings.defaultGateOpenMillis());
        if (millis < 0) {
            messages.send(player, "invalid-number", MessageManager.placeholders("input", durationRaw));
            return;
        }
        gate.openFor(millis, System.currentTimeMillis());
        store.put(kingdom);
        messages.send(player, "kingdom-gate-open", MessageManager.placeholders(
                "gate", gate.name(),
                "duration", DurationParser.format(millis)
        ));
        plugin.getSchedulerUtil().runSyncLater(() -> {
            if (!gate.isOpen(System.currentTimeMillis())) {
                store.put(kingdom);
            }
        }, Math.max(1L, millis / 50L));
    }

    public void closeGate(@NotNull Player player, @NotNull Kingdom kingdom, @NotNull String name) {
        if (!kingdom.canManage(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-warden");
            return;
        }
        KingdomGate gate = kingdom.gate(name);
        if (gate == null) {
            messages.send(player, "kingdom-gate-missing", MessageManager.placeholders("gate", name));
            return;
        }
        gate.close();
        store.put(kingdom);
        messages.send(player, "kingdom-gate-closed", MessageManager.placeholders("gate", gate.name()));
    }

    public void listGates(@NotNull Player player, @NotNull Kingdom kingdom) {
        if (kingdom.gates().isEmpty()) {
            messages.send(player, "kingdom-gate-list-empty");
            return;
        }
        long now = System.currentTimeMillis();
        messages.send(player, "kingdom-gate-list-header", MessageManager.placeholders("name", kingdom.displayName()));
        for (KingdomGate gate : kingdom.gates().values()) {
            messages.send(player, "kingdom-gate-list-line", MessageManager.placeholders(
                    "gate", gate.name(),
                    "side", gate.side().display(),
                    "state", gate.isOpen(now) ? "open " + DurationParser.format(gate.remainingMs(now)) : "sealed"
            ));
        }
    }

    public void setBeaconColor(@NotNull Player player, @NotNull Kingdom kingdom, @NotNull String raw) {
        if (!kingdom.canManage(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-warden");
            return;
        }
        if (!BeaconColor.isValidHex(raw) && !raw.equalsIgnoreCase("rainbow")) {
            messages.send(player, "kingdom-beacon-invalid", MessageManager.placeholders("input", raw));
            return;
        }
        kingdom.setBeaconColor(BeaconColor.parse(raw));
        store.put(kingdom);
        messages.send(player, "kingdom-beacon-set", MessageManager.placeholders(
                "color", kingdom.beaconColor().rainbow() ? "rainbow" : kingdom.beaconColor().hex()
        ));
    }

    public void setTowerHeight(@NotNull Player player, @NotNull Kingdom kingdom, int height) {
        if (!kingdom.canManage(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-warden");
            return;
        }
        if (height < 8 || height > settings.maxTowerHeight()) {
            messages.send(player, "kingdom-tower-height-invalid", MessageManager.placeholders(
                    "max", settings.maxTowerHeight()
            ));
            return;
        }
        kingdom.setTowerHeight(height);
        store.put(kingdom);
        messages.send(player, "kingdom-tower-height", MessageManager.placeholders("height", height));
    }

    public void lightTowers(@NotNull Player player, @NotNull Kingdom kingdom, @NotNull String which) {
        if (!kingdom.canManage(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-warden");
            return;
        }
        if (which.equalsIgnoreCase("all")) {
            for (KingdomTower tower : kingdom.towers().values()) {
                tower.setAllLit(true);
            }
            store.put(kingdom);
            messages.send(player, "kingdom-towers-lit-all");
            return;
        }
        int tier;
        try {
            tier = Integer.parseInt(which);
        } catch (NumberFormatException ex) {
            messages.send(player, "invalid-number", MessageManager.placeholders("input", which));
            return;
        }
        for (KingdomTower tower : kingdom.towers().values()) {
            tower.setTierLit(tier, true);
        }
        store.put(kingdom);
        messages.send(player, "kingdom-towers-lit-tier", MessageManager.placeholders("tier", tier));
    }

    public void toggleWardShow(@NotNull Player player) {
        boolean enabled = !Boolean.TRUE.equals(wardShow.get(player.getUniqueId()));
        if (enabled) {
            wardShow.put(player.getUniqueId(), true);
        } else {
            wardShow.remove(player.getUniqueId());
        }
        messages.send(player, enabled ? "kingdom-ward-show-on" : "kingdom-ward-show-off");
    }

    public void setBanner(@NotNull Player player, @NotNull Kingdom kingdom) {
        if (!kingdom.canManage(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-warden");
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR || !hand.getType().name().endsWith("BANNER")) {
            messages.send(player, "kingdom-banner-need");
            return;
        }
        kingdom.setBannerMaterial(hand.getType().name());
        store.put(kingdom);
        messages.send(player, "kingdom-banner-set", MessageManager.placeholders("banner", hand.getType().name()));
    }

    public void giveCompass(@NotNull Player player) {
        ItemStack compass = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<gradient:#8B7355:#B87333><bold>" + COMPASS_NAME + "</bold></gradient>"));
            meta.lore(List.of(
                    MessageUtil.parse("<gray>Hold to see the ward lines.</gray>"),
                    MessageUtil.parse("<dark_gray>Old Kingdom relic</dark_gray>")
            ));
            meta.getPersistentDataContainer().set(compassKey(), PersistentDataType.BYTE, (byte) 1);
            compass.setItemMeta(meta);
        }
        player.getInventory().addItem(compass);
        messages.send(player, "kingdom-compass-given");
    }

    public boolean isWardenCompass(@Nullable ItemStack item) {
        if (item == null || item.getType() != Material.RECOVERY_COMPASS) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(compassKey(), PersistentDataType.BYTE);
    }

    public void drain(@NotNull Player player, @NotNull Kingdom kingdom) {
        if (!kingdom.canManage(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-warden");
            return;
        }
        builder.drain(player, kingdom);
    }

    public void restoreFluids(@NotNull Player player, @NotNull Kingdom kingdom) {
        if (!kingdom.canManage(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-warden");
            return;
        }
        World world = Bukkit.getWorld(kingdom.bounds().worldName());
        if (world == null) {
            messages.send(player, "world-not-found", MessageManager.placeholders("world", kingdom.bounds().worldName()));
            return;
        }
        int restored = 0;
        List<FluidHistoryEntry> keep = new ArrayList<>();
        for (FluidHistoryEntry entry : fluidHistory) {
            if (!entry.worldName().equals(kingdom.bounds().worldName())
                    || !kingdom.bounds().contains(entry.x(), entry.y(), entry.z())) {
                keep.add(entry);
                continue;
            }
            Material previous;
            try {
                previous = Material.valueOf(entry.previousMaterial());
            } catch (IllegalArgumentException ex) {
                previous = Material.AIR;
            }
            world.getBlockAt(entry.x(), entry.y(), entry.z()).setType(previous, false);
            restored++;
        }
        fluidHistory.clear();
        fluidHistory.addAll(keep);
        messages.send(player, "kingdom-restore-done", MessageManager.placeholders("count", restored));
    }

    public void recordFluid(@NotNull Block block, @NotNull Material previous) {
        while (fluidHistory.size() >= settings.maxFluidHistory()) {
            fluidHistory.pollFirst();
        }
        World world = block.getWorld();
        fluidHistory.addLast(new FluidHistoryEntry(
                world.getName(),
                block.getX(),
                block.getY(),
                block.getZ(),
                previous.name(),
                System.currentTimeMillis()
        ));
    }

    public void teleportToKingdom(@NotNull Player player, @NotNull Kingdom kingdom) {
        if (!kingdom.isMember(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-member-self");
            return;
        }
        World world = Bukkit.getWorld(kingdom.bounds().worldName());
        if (world == null) {
            messages.send(player, "world-not-found", MessageManager.placeholders("world", kingdom.bounds().worldName()));
            return;
        }
        int x = kingdom.bounds().centerX();
        int z = kingdom.bounds().centerZ();
        int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING) + 1;
        Location dest = new Location(world, x + 0.5, y, z + 0.5);
        teleportManager.teleport(player, dest, "kingdom");
        playArrival(player, kingdom);
    }

    public void teleportToTower(@NotNull Player player, @NotNull Kingdom kingdom, @NotNull KingdomTower tower) {
        if (!kingdom.isMember(player.getUniqueId()) && !hasBypass(player)) {
            messages.send(player, "kingdom-not-member-self");
            return;
        }
        World world = Bukkit.getWorld(kingdom.bounds().worldName());
        if (world == null) {
            messages.send(player, "world-not-found", MessageManager.placeholders("world", kingdom.bounds().worldName()));
            return;
        }
        int y = tower.groundY() + Math.min(4, tower.height() / 4);
        Location dest = new Location(world, tower.x() + 0.5, y, tower.z() + 0.5);
        teleportManager.teleport(player, dest, "tower");
        playArrival(player, kingdom);
        messages.send(player, "kingdom-scout-tower", MessageManager.placeholders("tower", tower.name(kingdom.displayName())));
    }

    public @Nullable KingdomTower findTower(@NotNull String name) {
        for (Kingdom kingdom : store.all()) {
            KingdomTower tower = kingdom.towerByName(name);
            if (tower != null) {
                return tower;
            }
            if (name.equalsIgnoreCase(kingdom.displayName() + "-Central")
                    || name.equalsIgnoreCase(kingdom.id() + "-central")) {
                return kingdom.tower(TowerType.CENTRAL);
            }
        }
        return null;
    }

    public @Nullable Kingdom kingdomForTower(@NotNull KingdomTower tower) {
        for (Kingdom kingdom : store.all()) {
            if (kingdom.towers().containsValue(tower) || kingdom.tower(tower.type()) == tower) {
                return kingdom;
            }
            KingdomTower owned = kingdom.tower(tower.type());
            if (owned != null && owned.x() == tower.x() && owned.z() == tower.z()) {
                return kingdom;
            }
        }
        return null;
    }

    public void playArrival(@NotNull Player player, @NotNull Kingdom kingdom) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        int rgb = kingdom.beaconColor().rgbAtTick(tick, settings.beaconCycleSpeed());
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(rgb), 1.6f);
        world.spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 40, 0.6, 1.0, 0.6, 0.01, dust);
        world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 24, 0.3, 0.8, 0.3, 0.02);
        world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.4f);
    }

    public void handleBreach(@NotNull Kingdom kingdom, @NotNull Location at, @Nullable Player actor) {
        if (!settings.breachAlert()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastBreachFlash.get(kingdom.id());
        if (last != null && now - last < 1500L) {
            return;
        }
        lastBreachFlash.put(kingdom.id(), now);
        KingdomTower nearest = kingdom.nearestTower(at.getBlockX(), at.getBlockZ());
        World world = at.getWorld();
        if (world != null && nearest != null) {
            Location flash = new Location(world, nearest.x() + 0.5, nearest.topY(), nearest.z() + 0.5);
            world.spawnParticle(Particle.DUST, flash, 80, 0.4, 4.0, 0.4, 0.01, new Particle.DustOptions(Color.RED, 2.0f));
            world.playSound(flash, Sound.BLOCK_BEACON_DEACTIVATE, 1.2f, 0.6f);
        }
        Map<String, String> placeholders = MessageManager.placeholders(
                "name", kingdom.displayName(),
                "player", actor == null ? "unknown" : actor.getName(),
                "x", at.getBlockX(),
                "y", at.getBlockY(),
                "z", at.getBlockZ()
        );
        notifyWardens(kingdom, "kingdom-breach", placeholders);
    }

    public void notifyWardens(@NotNull Kingdom kingdom, @NotNull String path, @NotNull Map<String, String> placeholders) {
        for (Map.Entry<UUID, KingdomRole> entry : kingdom.members().entrySet()) {
            if (!entry.getValue().receivesAlerts()) {
                continue;
            }
            Player online = Bukkit.getPlayer(entry.getKey());
            if (online != null) {
                messages.send(online, path, placeholders);
            }
        }
    }

    public void ensureTowers(@NotNull Kingdom kingdom, @NotNull World world) {
        int inset = settings.towerInset();
        if (settings.citadelKeep()) {
            int maxInset = Math.max(0, kingdom.bounds().radius() - KingdomStructurePlanner.CITADEL_KEEP_CLEARANCE);
            inset = Math.min(inset, maxInset);
        }
        int height = kingdom.towerHeight();
        List<TowerType> types = new ArrayList<>();
        if (settings.cornerCount() >= 4) {
            types.add(TowerType.NORTH_WEST);
            types.add(TowerType.NORTH_EAST);
            types.add(TowerType.SOUTH_WEST);
            types.add(TowerType.SOUTH_EAST);
        }
        if (settings.centralSpire() && !settings.citadelKeep()) {
            types.add(TowerType.CENTRAL);
        }
        for (TowerType type : types) {
            int[] xz = kingdom.bounds().towerAnchor(type, inset);
            int ground = world.getHighestBlockYAt(xz[0], xz[1], HeightMap.MOTION_BLOCKING);
            int towerHeight = type.isCentral() ? height + 16 : height;
            KingdomTower existing = kingdom.tower(type);
            if (existing == null) {
                KingdomTower tower = new KingdomTower(type, xz[0], xz[1], ground, towerHeight);
                tower.setAllLit(settings.autoLightTiers());
                kingdom.putTower(tower);
            } else {
                existing.setHeight(towerHeight);
            }
        }
    }

    public void syncProtectionRegion(@NotNull Kingdom kingdom) {
        KingdomBounds bounds = kingdom.bounds();
        Region region = plugin.getProtectionService().getRegions().get(bounds.worldName(), kingdom.regionName());
        if (region == null) {
            region = new Region(
                    kingdom.regionName(),
                    bounds.worldName(),
                    bounds.minX(),
                    bounds.minY(),
                    bounds.minZ(),
                    bounds.maxX(),
                    bounds.maxY(),
                    bounds.maxZ()
            );
        } else {
            region.setBounds(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
        }
        region.setPriority(1000);
        for (UUID member : List.copyOf(region.getMembers())) {
            region.removeMember(member);
        }
        for (UUID owner : List.copyOf(region.getOwners())) {
            region.removeOwner(owner);
        }
        for (Map.Entry<UUID, KingdomRole> entry : kingdom.members().entrySet()) {
            if (entry.getValue() == KingdomRole.SOVEREIGN) {
                region.addOwner(entry.getKey());
            } else if (entry.getValue().canBuild()) {
                region.addMember(entry.getKey());
            }
        }
        region.setFlag(ProtectionFlag.ENTRY, FlagValue.ALLOW);
        region.setFlag(ProtectionFlag.BUILD, FlagValue.DENY);
        region.setFlag(ProtectionFlag.BREAK, FlagValue.DENY);
        region.setFlag(ProtectionFlag.PLACE, FlagValue.DENY);
        region.setFlag(ProtectionFlag.CHEST_ACCESS, FlagValue.DENY);
        region.setFlag(ProtectionFlag.PVP, kingdom.rules().pvp() ? FlagValue.ALLOW : FlagValue.DENY);
        region.setFlag(ProtectionFlag.FIRE_SPREAD, kingdom.rules().fireSpread() ? FlagValue.ALLOW : FlagValue.DENY);
        region.setFlag(ProtectionFlag.FIRE_DESTROY, kingdom.rules().fireSpread() ? FlagValue.ALLOW : FlagValue.DENY);
        FlagValue boom = kingdom.rules().explosions() ? FlagValue.ALLOW : FlagValue.DENY;
        region.setFlag(ProtectionFlag.TNT, boom);
        region.setFlag(ProtectionFlag.CREEPER_EXPLOSION, boom);
        region.setFlag(ProtectionFlag.OTHER_EXPLOSION, boom);
        plugin.getProtectionService().getRegions().put(region);
    }

    public void rebuildChunkIndex() {
        chunkIndex.clear();
        for (Kingdom kingdom : store.all()) {
            KingdomBounds bounds = kingdom.bounds();
            Map<Long, String> worldChunks = chunkIndex.computeIfAbsent(bounds.worldName(), w -> new ConcurrentHashMap<>());
            int minCx = bounds.minX() >> 4;
            int maxCx = bounds.maxX() >> 4;
            int minCz = bounds.minZ() >> 4;
            int maxCz = bounds.maxZ() >> 4;
            for (int cx = minCx; cx <= maxCx; cx++) {
                for (int cz = minCz; cz <= maxCz; cz++) {
                    worldChunks.put(chunkKey(cx, cz), kingdom.id());
                }
            }
        }
    }

    private static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }

    private @NotNull NamespacedKey compassKey() {
        return new NamespacedKey(plugin, "warden_compass");
    }

    private @NotNull String nameOf(@NotNull UUID id) {
        String name = Bukkit.getOfflinePlayer(id).getName();
        return name == null ? id.toString().substring(0, 8) : name;
    }

    private void spawnGateGuard(@NotNull World world, @NotNull KingdomGate gate, @NotNull Kingdom kingdom) {
        Location loc = new Location(world, gate.x() + 0.5, gate.y(), gate.z() + 0.5);
        world.spawn(loc, ArmorStand.class, stand -> {
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setCustomNameVisible(true);
            stand.customName(Component.text(kingdom.displayName() + " Guard", NamedTextColor.GOLD));
            stand.getEquipment().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
            stand.getEquipment().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
            stand.setPersistent(true);
        });
    }

    private void startLighting() {
        lightingTask = plugin.getSchedulerUtil().runSyncTimer(this::tickLighting, 20L, settings.lightingUpdateInterval());
    }

    private void tickLighting() {
        tick++;
        for (Player player : Bukkit.getOnlinePlayers()) {
            tickPlayerLight(player);
            tickWardVisuals(player);
            tickClearWeather(player);
        }
        if (settings.beaconEnabled()) {
            tickBeacons();
        }
    }

    private void tickClearWeather(@NotNull Player player) {
        Kingdom kingdom = at(player.getLocation());
        if (kingdom != null && kingdom.isSealed(WardLayer.SKY) && kingdom.isSealed(WardLayer.ELEMENTAL)) {
            if (player.getPlayerWeather() != org.bukkit.WeatherType.CLEAR) {
                player.setPlayerWeather(org.bukkit.WeatherType.CLEAR);
            }
        } else if (player.getPlayerWeather() == org.bukkit.WeatherType.CLEAR) {
            player.resetPlayerWeather();
        }
    }

    private void tickPlayerLight(@NotNull Player player) {
        restorePlayerLight(player);
        if (!settings.dynamicPlayers()) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        if (!isLightItem(hand) && !isLightItem(off)) {
            return;
        }
        if (settings.waterExtinguish() && player.getLocation().getBlock().isLiquid()) {
            Kingdom kingdom = at(player.getLocation());
            if (kingdom == null || !kingdom.isSealed(WardLayer.ELEMENTAL)) {
                return;
            }
        }
        Location loc = player.getLocation().getBlock().getRelative(BlockFace.UP).getLocation();
        if (!loc.getBlock().getType().isAir()) {
            loc = player.getLocation().getBlock().getLocation();
            if (!loc.getBlock().getType().isAir()) {
                return;
            }
        }
        BlockData light = Material.LIGHT.createBlockData();
        if (light instanceof Light lightBlock) {
            lightBlock.setLevel(15);
        }
        player.sendBlockChange(loc, light);
        lastPlayerLight.put(player.getUniqueId(), loc.clone());
    }

    private void restorePlayerLight(@NotNull Player player) {
        Location previous = lastPlayerLight.remove(player.getUniqueId());
        if (previous == null || previous.getWorld() == null) {
            return;
        }
        player.sendBlockChange(previous, previous.getBlock().getBlockData());
    }

    private boolean isLightItem(@NotNull ItemStack item) {
        return LIGHT_ITEMS.contains(item.getType());
    }

    private void tickWardVisuals(@NotNull Player player) {
        if (!settings.visualizeForMembers()) {
            return;
        }
        boolean show = Boolean.TRUE.equals(wardShow.get(player.getUniqueId()))
                || isWardenCompass(player.getInventory().getItemInMainHand())
                || isWardenCompass(player.getInventory().getItemInOffHand());
        if (!show) {
            return;
        }
        Kingdom kingdom = at(player.getLocation());
        if (kingdom == null || !kingdom.isMember(player.getUniqueId())) {
            return;
        }
        drawWardLines(player, kingdom);
    }

    private void drawWardLines(@NotNull Player player, @NotNull Kingdom kingdom) {
        KingdomBounds bounds = kingdom.bounds();
        int y = player.getLocation().getBlockY();
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(0x4A90D9), 0.85f);
        int step = Math.max(2, bounds.radius() / 32);
        for (int x = bounds.minX(); x <= bounds.maxX(); x += step) {
            spawnTo(player, dust, x, y, bounds.minZ());
            spawnTo(player, dust, x, y, bounds.maxZ());
            spawnTo(player, dust, x, bounds.maxY(), bounds.minZ() + (x - bounds.minX()) * 0);
        }
        for (int z = bounds.minZ(); z <= bounds.maxZ(); z += step) {
            spawnTo(player, dust, bounds.minX(), y, z);
            spawnTo(player, dust, bounds.maxX(), y, z);
        }
        spawnTo(player, dust, bounds.minX(), y, bounds.minZ());
        spawnTo(player, dust, bounds.maxX(), y, bounds.minZ());
        spawnTo(player, dust, bounds.minX(), y, bounds.maxZ());
        spawnTo(player, dust, bounds.maxX(), y, bounds.maxZ());
        int[] cornersX = {bounds.minX(), bounds.maxX()};
        int[] cornersZ = {bounds.minZ(), bounds.maxZ()};
        for (int cx : cornersX) {
            for (int cz : cornersZ) {
                for (int vy = bounds.minY(); vy <= bounds.maxY(); vy += Math.max(8, settings.tierSpacing() / 2)) {
                    spawnTo(player, dust, cx, vy, cz);
                }
            }
        }
    }

    private void spawnTo(@NotNull Player player, @NotNull Particle.DustOptions dust, int x, int y, int z) {
        player.spawnParticle(Particle.DUST, x + 0.5, y + 0.2, z + 0.5, 1, 0, 0, 0, 0, dust);
    }

    private void tickBeacons() {
        int visible = settings.beaconVisibleDistance();
        int cycle = settings.beaconCycleSpeed();
        for (Kingdom kingdom : store.all()) {
            if (!kingdom.isSealed(WardLayer.BEACON) || !kingdom.towersBuilt()) {
                continue;
            }
            World world = Bukkit.getWorld(kingdom.bounds().worldName());
            if (world == null) {
                continue;
            }
            int rgb = kingdom.beaconColor().rgbAtTick(tick, cycle);
            Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(rgb), 1.35f);
            for (KingdomTower tower : kingdom.towers().values()) {
                if (!tower.built()) {
                    continue;
                }
                int top = Math.min(world.getMaxHeight() - 1, tower.topY());
                for (Player viewer : world.getPlayers()) {
                    if (viewer.getLocation().distanceSquared(new Location(world, tower.x(), top, tower.z()))
                            > (long) visible * visible) {
                        continue;
                    }
                    for (int y = top; y <= Math.min(world.getMaxHeight() - 1, top + 48); y += 3) {
                        viewer.spawnParticle(Particle.DUST, tower.x() + 0.5, y + 0.5, tower.z() + 0.5, 1, 0, 0, 0, 0, dust);
                        if (y % 6 == 0) {
                            viewer.spawnParticle(Particle.END_ROD, tower.x() + 0.5, y + 0.5, tower.z() + 0.5, 1, 0, 0, 0, 0);
                        }
                    }
                }
            }
        }
    }

    public @NotNull MessageManager messages() {
        return messages;
    }

    public @NotNull RihanX plugin() {
        return plugin;
    }

    public @NotNull KingdomBuilder builder() {
        return builder;
    }

    public @NotNull List<String> kingdomNames() {
        List<String> names = new ArrayList<>();
        for (Kingdom kingdom : store.all()) {
            names.add(kingdom.displayName());
        }
        return names;
    }

    public @NotNull List<String> towerNames() {
        List<String> names = new ArrayList<>();
        for (Kingdom kingdom : store.all()) {
            for (KingdomTower tower : kingdom.towers().values()) {
                names.add(tower.name(kingdom.displayName()));
            }
        }
        return names;
    }
}
