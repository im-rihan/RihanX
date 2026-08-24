package com.rihanx.mob;

import com.rihanx.RihanX;
import com.rihanx.utils.NumberUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Advanced player-spot spawning for villagers and zombies (persistent, nametag,
 * optional minecart). Also used by {@code /farm iron} after paste.
 */
public final class MobSpawnService {

    public static final int DEFAULT_MAX_AMOUNT = 16;
    public static final List<String> TYPES = List.of("villager", "zombie");
    public static final List<String> PROFESSIONS = List.of(
            "none", "farmer", "librarian", "armorer", "butcher", "cartographer", "cleric",
            "fisherman", "fletcher", "leatherworker", "mason", "nitwit", "shepherd",
            "toolsmith", "weaponsmith"
    );
    public static final List<String> FLAGS = List.of("minecart", "nametag", "noname");

    private final @NotNull RihanX plugin;

    public MobSpawnService(@NotNull RihanX plugin) {
        this.plugin = plugin;
    }

    public int maxAmount() {
        return Math.max(1, plugin.getConfig().getInt("mob.max-amount", DEFAULT_MAX_AMOUNT));
    }

    public int spawnAtPlayer(@NotNull Player player, @NotNull SpawnRequest request) {
        Location pad = centerFeet(player.getLocation());
        int spawned = 0;
        for (int i = 0; i < request.amount(); i++) {
            double ox = (i % 3) * 0.35;
            double oz = (i / 3) * 0.35;
            Location dest = pad.clone().add(ox, 0, oz);
            if (spawnOne(player.getWorld(), pad, dest, request, null, null) != null) {
                spawned++;
            }
        }
        return spawned;
    }

    /**
     * Spawn on {@code pad} (open space Paper accepts), then move to {@code dest}.
     */
    public @Nullable LivingEntity spawnOne(
            @NotNull World world,
            @NotNull Location pad,
            @NotNull Location dest,
            @NotNull SpawnRequest request,
            @Nullable Location home,
            @Nullable Location jobSite
    ) {
        prepareStandingCell(dest, Material.STONE_BRICKS);
        if (request.kind() == Kind.VILLAGER) {
            Villager villager = spawnAtPadThenMove(world, pad, dest, Villager.class, v -> {
                v.setAdult();
                v.setAware(true);
                v.setCanPickupItems("farmer".equals(request.professionId()));
                v.setProfession(resolveProfession(request.professionId()));
                v.setVillagerLevel(1);
                applyIdentity(v, request, "Villager");
            });
            if (villager != null) {
                bindVillageMemory(villager, home, jobSite);
            }
            return villager;
        }
        if (request.minecart()) {
            return spawnZombieInMinecart(world, pad, dest, request);
        }
        return spawnAtPadThenMove(world, pad, dest, Zombie.class, z -> configureZombie(z, request));
    }

    private @Nullable Zombie spawnZombieInMinecart(
            @NotNull World world,
            @NotNull Location pad,
            @NotNull Location dest,
            @NotNull SpawnRequest request
    ) {
        Location padC = centerFeet(pad);
        try {
            RideableMinecart cart = world.spawn(padC, RideableMinecart.class,
                    CreatureSpawnEvent.SpawnReason.CUSTOM, c -> {
                        c.setPersistent(true);
                        c.setMaxSpeed(0.0);
                        c.setSlowWhenEmpty(true);
                    });
            Zombie zombie = world.spawn(padC, Zombie.class, CreatureSpawnEvent.SpawnReason.CUSTOM, z -> {
                z.setPersistent(true);
                z.setRemoveWhenFarAway(false);
                z.setAI(false);
                z.setCollidable(false);
                configureZombie(z, request);
            });
            cart.addPassenger(zombie);
            cart.teleport(centerFeet(dest));
            cart.setMaxSpeed(0.0);
            zombie.setCollidable(true);
            zombie.setAI(true);
            return zombie;
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not spawn nametag zombie in minecart", ex);
            return spawnAtPadThenMove(world, pad, dest, Zombie.class, z -> configureZombie(z, request));
        }
    }

    private static void configureZombie(@NotNull Zombie zombie, @NotNull SpawnRequest request) {
        zombie.setAdult();
        zombie.setAware(true);
        zombie.setShouldBurnInDay(false);
        zombie.setSilent(true);
        zombie.setCanPickupItems(false);
        applyIdentity(zombie, request, "Zombie");
        if (request.nametag()) {
            zombie.getEquipment().setHelmet(new ItemStack(Material.NAME_TAG));
            zombie.getEquipment().setHelmetDropChance(0f);
        }
    }

    private static void bindVillageMemory(
            @NotNull Villager villager,
            @Nullable Location home,
            @Nullable Location jobSite
    ) {
        if (home != null) {
            try {
                villager.setMemory(MemoryKey.HOME, home);
            } catch (RuntimeException ignored) {
                // Paper memory keys vary by version — spawn still succeeds
            }
        }
        if (jobSite != null) {
            try {
                villager.setMemory(MemoryKey.JOB_SITE, jobSite);
            } catch (RuntimeException ignored) {
            }
        }
    }

    public <T extends LivingEntity> @Nullable T spawnAtPadThenMove(
            @NotNull World world,
            @NotNull Location playerPad,
            @NotNull Location dest,
            @NotNull Class<T> type,
            @NotNull Consumer<T> setup
    ) {
        Location pad = centerFeet(playerPad);
        pad.getBlock().setType(Material.AIR, false);
        pad.clone().add(0, 1, 0).getBlock().setType(Material.AIR, false);
        try {
            T entity = world.spawn(pad, type, CreatureSpawnEvent.SpawnReason.CUSTOM, spawned -> {
                spawned.setPersistent(true);
                spawned.setRemoveWhenFarAway(false);
                spawned.setAI(false);
                spawned.setCollidable(false);
                spawned.setGravity(false);
                setup.accept(spawned);
            });
            entity.teleport(centerFeet(dest));
            entity.setGravity(true);
            entity.setCollidable(true);
            entity.setAI(true);
            return entity;
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Could not spawn " + type.getSimpleName() + ": " + ex.getMessage());
            return null;
        }
    }

    public static void prepareStandingCell(@NotNull Location feet, @NotNull Material floorFallback) {
        Block block = feet.getBlock();
        Material here = block.getType();
        if (here.isSolid() && !here.name().endsWith("_BED") && here != Material.COMPOSTER
                && here != Material.RAIL && here != Material.POWERED_RAIL
                && here != Material.DETECTOR_RAIL && here != Material.ACTIVATOR_RAIL) {
            block.setType(Material.AIR, false);
        }
        Block head = block.getRelative(0, 1, 0);
        if (head.getType().isSolid()) {
            head.setType(Material.AIR, false);
        }
        Block under = block.getRelative(0, -1, 0);
        if (!under.getType().isSolid()) {
            under.setType(floorFallback, false);
        }
    }

    public static @NotNull Location centerFeet(@NotNull Location loc) {
        Location at = loc.clone();
        at.setX(at.getBlockX() + 0.5);
        at.setY(at.getBlockY());
        at.setZ(at.getBlockZ() + 0.5);
        return at;
    }

    private static void applyIdentity(
            @NotNull LivingEntity entity,
            @NotNull SpawnRequest request,
            @NotNull String fallbackName
    ) {
        String name = request.customName() != null ? request.customName() : fallbackName;
        entity.customName(Component.text(name));
        entity.setCustomNameVisible(request.nametag());
    }

    public static @Nullable SpawnRequest parse(@NotNull String[] args, int maxAmount) {
        if (args.length == 0) {
            return null;
        }
        Kind kind = Kind.fromToken(args[0]);
        if (kind == null) {
            return null;
        }
        int amount = 1;
        boolean minecart = false;
        boolean nametag = true;
        String professionId = kind == Kind.VILLAGER ? "none" : null;
        for (int i = 1; i < args.length; i++) {
            String token = args[i].toLowerCase(Locale.ROOT).trim();
            if (token.isEmpty()) {
                continue;
            }
            Integer parsed = NumberUtil.parseInt(token);
            if (parsed != null) {
                amount = Math.max(1, Math.min(maxAmount, parsed));
                continue;
            }
            if (token.equals("minecart") || token.equals("cart") || token.equals("rail")) {
                minecart = true;
                continue;
            }
            if (token.equals("nametag") || token.equals("name") || token.equals("named")) {
                nametag = true;
                continue;
            }
            if (token.equals("noname") || token.equals("unnamed")) {
                nametag = false;
                continue;
            }
            if (kind == Kind.VILLAGER && isProfession(token)) {
                professionId = token.equals("unemployed") ? "none" : token;
                continue;
            }
            return null;
        }
        if (kind == Kind.ZOMBIE) {
            professionId = null;
        }
        return new SpawnRequest(kind, amount, minecart, nametag, professionId, null);
    }

    public static boolean isProfession(@NotNull String token) {
        String t = token.toLowerCase(Locale.ROOT);
        if (t.equals("unemployed")) {
            return true;
        }
        return PROFESSIONS.contains(t);
    }

    public static @NotNull Villager.Profession resolveProfession(@Nullable String id) {
        if (id == null) {
            return Villager.Profession.NONE;
        }
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "farmer" -> Villager.Profession.FARMER;
            case "librarian" -> Villager.Profession.LIBRARIAN;
            case "armorer" -> Villager.Profession.ARMORER;
            case "butcher" -> Villager.Profession.BUTCHER;
            case "cartographer" -> Villager.Profession.CARTOGRAPHER;
            case "cleric" -> Villager.Profession.CLERIC;
            case "fisherman" -> Villager.Profession.FISHERMAN;
            case "fletcher" -> Villager.Profession.FLETCHER;
            case "leatherworker" -> Villager.Profession.LEATHERWORKER;
            case "mason" -> Villager.Profession.MASON;
            case "nitwit" -> Villager.Profession.NITWIT;
            case "shepherd" -> Villager.Profession.SHEPHERD;
            case "toolsmith" -> Villager.Profession.TOOLSMITH;
            case "weaponsmith" -> Villager.Profession.WEAPONSMITH;
            default -> Villager.Profession.NONE;
        };
    }

    public enum Kind {
        VILLAGER,
        ZOMBIE;

        public static @Nullable Kind fromToken(@NotNull String token) {
            return switch (token.toLowerCase(Locale.ROOT)) {
                case "villager", "villagers", "v" -> VILLAGER;
                case "zombie", "zombies", "z" -> ZOMBIE;
                default -> null;
            };
        }
    }

    public record SpawnRequest(
            @NotNull Kind kind,
            int amount,
            boolean minecart,
            boolean nametag,
            @Nullable String professionId,
            @Nullable String customName
    ) {
        public @NotNull SpawnRequest withName(@NotNull String name) {
            return new SpawnRequest(kind, amount, minecart, nametag, professionId, name);
        }
    }
}
