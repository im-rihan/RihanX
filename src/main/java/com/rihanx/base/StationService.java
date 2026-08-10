package com.rihanx.base;

import com.rihanx.RihanX;
import com.rihanx.managers.MessageManager;
import com.rihanx.portal.PortalService;
import com.rihanx.portal.PortalStore;
import com.rihanx.station.RailPathLogic;
import com.rihanx.station.StationLinkLogic;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Train station / railway templates + named stop linking (via {@link PortalService}).
 * {@code /station link} also builds a powered rail path between the two stops.
 */
public final class StationService {

    private final @NotNull RihanX plugin;
    private final @NotNull MessageManager messages;
    private final @NotNull Map<String, BaseTemplates.BaseBlueprint> templates = StationTemplates.all();

    public StationService(@NotNull RihanX plugin, @NotNull MessageManager messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public @NotNull List<String> listIds() {
        return List.copyOf(templates.keySet());
    }

    public @Nullable BaseTemplates.BaseBlueprint get(@NotNull String id) {
        return templates.get(id.toLowerCase(Locale.ROOT).trim());
    }

    public void sendList(@NotNull Player player) {
        messages.send(player, "station-list-header", MessageManager.placeholders("count", templates.size()));
        for (BaseTemplates.BaseBlueprint blueprint : templates.values()) {
            messages.send(player, "station-list-line", MessageManager.placeholders(
                    "name", blueprint.id(),
                    "description", blueprint.description(),
                    "blocks", blueprint.blocks().size()
            ));
        }
    }

    public void paste(@NotNull Player player, @NotNull String id) {
        paste(player, id, null);
    }

    /**
     * Paste a station template. If {@code stopName} is set, also registers a link pad stop at the player's feet.
     */
    public void paste(@NotNull Player player, @NotNull String id, @Nullable String stopName) {
        BaseTemplates.BaseBlueprint blueprint = get(id);
        if (blueprint == null) {
            messages.send(player, "station-missing", MessageManager.placeholders(
                    "name", id,
                    "options", String.join(", ", listIds())
            ));
            return;
        }
        plugin.getBaseService().pasteBlueprint(player, blueprint, "station");
        if (stopName != null && !stopName.isBlank()) {
            registerStop(player, stopName, true);
        } else {
            messages.send(player, "station-link-hint");
        }
    }

    /** Place/register a named station stop portal pad at the player's feet. */
    public void registerStop(@NotNull Player player, @NotNull String rawName) {
        registerStop(player, rawName, false);
    }

    private void registerStop(@NotNull Player player, @NotNull String rawName, boolean afterPaste) {
        PortalService portals = plugin.getPortalService();
        Set<String> existing = new HashSet<>(portals.getStore().list());
        StationLinkLogic.RegisterResult can = StationLinkLogic.canRegister(existing, rawName);
        if (can == StationLinkLogic.RegisterResult.EMPTY_NAME) {
            messages.send(player, "invalid-argument", MessageManager.placeholders("input", rawName));
            return;
        }
        if (can == StationLinkLogic.RegisterResult.EXISTS) {
            messages.send(player, "station-stop-exists", MessageManager.placeholders(
                    "stop", StationLinkLogic.normalize(rawName)
            ));
            return;
        }

        String id = StationLinkLogic.portalIdForStop(rawName);
        PortalService.CreateResult created = portals.createAt(id, player.getLocation(), true);
        if (created != PortalService.CreateResult.CREATED) {
            messages.send(player, "internal-error");
            return;
        }
        messages.send(player, afterPaste ? "station-stop-registered-paste" : "station-stop-registered",
                MessageManager.placeholders("stop", id));
        messages.send(player, "station-stop-link-hint", MessageManager.placeholders("stop", id));
    }

    public void linkStops(@NotNull Player player, @NotNull String a, @NotNull String b) {
        PortalService portals = plugin.getPortalService();
        Set<String> existing = new HashSet<>(portals.getStore().list());
        StationLinkLogic.LinkResult can = StationLinkLogic.canLink(existing, a, b);
        if (can == StationLinkLogic.LinkResult.EMPTY_NAME) {
            messages.send(player, "invalid-argument", MessageManager.placeholders("input", a + " / " + b));
            return;
        }
        if (can == StationLinkLogic.LinkResult.SELF) {
            messages.send(player, "station-link-self");
            return;
        }
        if (can == StationLinkLogic.LinkResult.MISSING_LEFT || can == StationLinkLogic.LinkResult.MISSING_RIGHT) {
            String missing = StationLinkLogic.missingName(can, a, b);
            messages.send(player, "station-stop-missing", MessageManager.placeholders("stop", missing));
            return;
        }

        PortalService.LinkResult linked = portals.linkNamed(a, b);
        if (linked != PortalService.LinkResult.LINKED) {
            messages.send(player, "internal-error");
            return;
        }

        String left = StationLinkLogic.normalize(a);
        String right = StationLinkLogic.normalize(b);
        messages.send(player, "station-linked", MessageManager.placeholders(
                "stop", left,
                "target", right
        ));

        if (plugin.getConfig().getBoolean("station.rail-link", true)) {
            buildRailBetween(player, left, right);
        }
    }

    private void buildRailBetween(@NotNull Player player, @NotNull String left, @NotNull String right) {
        PortalStore store = plugin.getPortalService().getStore();
        PortalStore.StoredPortal portalA = store.get(left);
        PortalStore.StoredPortal portalB = store.get(right);
        if (portalA == null || portalB == null) {
            return;
        }
        if (!portalA.worldName().equals(portalB.worldName())) {
            messages.send(player, "station-rail-world-mismatch");
            return;
        }
        World world = Bukkit.getWorld(portalA.worldName());
        if (world == null) {
            messages.send(player, "station-rail-world-mismatch");
            return;
        }
        if (!world.equals(player.getWorld())) {
            messages.send(player, "station-rail-wrong-world", MessageManager.placeholders(
                    "world", world.getName()
            ));
            return;
        }

        int maxDist = Math.max(16, plugin.getConfig().getInt("station.max-rail-distance", 512));
        int poweredEvery = Math.max(2, plugin.getConfig().getInt("station.powered-every", 4));
        int joinLen = Math.max(0, plugin.getConfig().getInt("station.rail-join-length", 8));
        int exitLen = Math.max(1, plugin.getConfig().getInt("station.rail-exit-length", 3));

        int x1 = (int) Math.floor(portalA.x());
        int y1 = (int) Math.floor(portalA.y());
        int z1 = (int) Math.floor(portalA.z());
        int x2 = (int) Math.floor(portalB.x());
        int y2 = (int) Math.floor(portalB.y());
        int z2 = (int) Math.floor(portalB.z());

        // Join each station's platform rails, exit out the front toward the other stop, then L-link.
        RailPathLogic.Plan plan = RailPathLogic.planStationLink(
                x1, y1, z1, portalA.yaw(),
                x2, y2, z2, portalB.yaw(),
                maxDist, poweredEvery, joinLen, exitLen
        );
        if (plan.result() == RailPathLogic.PlanResult.TOO_FAR) {
            messages.send(player, "station-rail-too-far", MessageManager.placeholders(
                    "distance", RailPathLogic.horizontalDistance(x1, z1, x2, z2),
                    "max", maxDist
            ));
            return;
        }
        if (plan.result() != RailPathLogic.PlanResult.OK) {
            messages.send(player, "station-rail-skipped");
            return;
        }

        List<BaseService.AbsolutePlacement> placements = new ArrayList<>(plan.cells().size());
        for (RailPathLogic.Cell cell : plan.cells()) {
            Material material = switch (cell.layer()) {
                case SUPPORT -> Material.STONE_BRICKS;
                case GLOW -> Material.GLOWSTONE;
                case BED -> Material.GRAVEL;
                case RAIL -> Material.RAIL;
                case POWERED -> Material.POWERED_RAIL;
                case REDSTONE -> Material.REDSTONE_BLOCK;
                case CLEAR -> Material.AIR;
            };
            placements.add(new BaseService.AbsolutePlacement(cell.x(), cell.y(), cell.z(), material));
        }

        String railName = "rail:" + left + "-" + right;
        boolean started = plugin.getBaseService().pasteAbsolute(player, "station", railName, placements);
        if (started) {
            messages.send(player, "station-rail-linked", MessageManager.placeholders(
                    "stop", left,
                    "target", right,
                    "length", plan.trackLength(),
                    "blocks", placements.size()
            ));
        }
    }

    public void deleteStop(@NotNull Player player, @NotNull String rawName) {
        String id = StationLinkLogic.normalize(rawName);
        PortalStore store = plugin.getPortalService().getStore();
        if (!store.delete(id)) {
            messages.send(player, "station-stop-missing", MessageManager.placeholders("stop", id));
            return;
        }
        messages.send(player, "station-stop-deleted", MessageManager.placeholders("stop", id));
    }

    public void sendStops(@NotNull Player player) {
        List<String> names = plugin.getPortalService().getStore().list();
        if (names.isEmpty()) {
            messages.send(player, "station-stops-list", MessageManager.placeholders(
                    "count", 0,
                    "stops", "none"
            ));
            return;
        }
        StringBuilder joined = new StringBuilder();
        for (String name : names) {
            PortalStore.StoredPortal portal = plugin.getPortalService().getStore().get(name);
            if (joined.length() > 0) {
                joined.append(", ");
            }
            joined.append(name);
            if (portal != null && portal.link() != null) {
                joined.append("→").append(portal.link());
            }
        }
        messages.send(player, "station-stops-list", MessageManager.placeholders(
                "count", names.size(),
                "stops", joined.toString()
        ));
    }
}
