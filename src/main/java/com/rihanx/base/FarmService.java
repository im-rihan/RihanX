package com.rihanx.base;

import com.rihanx.RihanX;
import com.rihanx.farm.FarmBlueprint;
import com.rihanx.farm.FarmPlan;
import com.rihanx.farm.FarmRegistry;
import com.rihanx.farm.MaterialCalculator;
import com.rihanx.managers.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Farm template listing + paste via {@link BaseService#pasteBlueprint}.
 * Preview / validate / info use {@link FarmRegistry} dry-run plans (no world mutation).
 */
public final class FarmService {

    private final @NotNull RihanX plugin;
    private final @NotNull MessageManager messages;
    private final @NotNull Map<String, BaseTemplates.BaseBlueprint> templates = FarmTemplates.all();
    private final @NotNull FarmRegistry registry = FarmRegistry.get();

    public FarmService(@NotNull RihanX plugin, @NotNull MessageManager messages) {
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
        messages.send(player, "farm-list-header", MessageManager.placeholders("count", templates.size()));
        for (BaseTemplates.BaseBlueprint blueprint : templates.values()) {
            messages.send(player, "farm-list-line", MessageManager.placeholders(
                    "name", blueprint.id(),
                    "description", blueprint.description(),
                    "blocks", blueprint.blocks().size()
            ));
        }
    }

    public void openMenu(@NotNull Player player) {
        new FarmSelectGui(plugin).open(player);
    }

    public void paste(@NotNull Player player, @NotNull String id) {
        BaseTemplates.BaseBlueprint blueprint = get(id);
        if (blueprint == null) {
            messages.send(player, "farm-missing", MessageManager.placeholders(
                    "name", id,
                    "options", String.join(", ", listIds())
            ));
            return;
        }
        plugin.getBaseService().pasteBlueprint(player, blueprint, "farm");
    }

    public void preview(@NotNull Player player, @NotNull String id) {
        FarmPlan plan = requirePlan(player, id);
        if (plan == null) {
            return;
        }
        FarmBlueprint bp = registry.get(id);
        List<String> errors = bp == null ? List.of("missing") : bp.validate(null, plan);
        player.sendMessage(Component.text(MaterialCalculator.formatPreview(plan, errors), NamedTextColor.GRAY));
    }

    public void validate(@NotNull Player player, @NotNull String id) {
        FarmPlan plan = requirePlan(player, id);
        if (plan == null) {
            return;
        }
        FarmBlueprint bp = registry.get(id);
        List<String> errors = bp == null ? List.of("missing") : bp.validate(null, plan);
        if (errors.isEmpty()) {
            player.sendMessage(Component.text(
                    "[RihanX AutoFarm] Validation PASS — " + plan.farmId()
                            + " (" + plan.placements().size() + " blocks, "
                            + plan.width() + "x" + plan.length() + "x" + plan.height() + ")",
                    NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text(
                    "[RihanX AutoFarm] Validation FAIL — " + errors.size() + " error(s):",
                    NamedTextColor.RED));
            for (String e : errors) {
                player.sendMessage(Component.text("  - " + e, NamedTextColor.RED));
            }
        }
    }

    public void info(@NotNull Player player, @NotNull String id) {
        FarmPlan plan = requirePlan(player, id);
        if (plan == null) {
            return;
        }
        MaterialCalculator.Report r = MaterialCalculator.calculate(plan);
        player.sendMessage(Component.text(
                "Farm: " + r.farmId()
                        + "\n" + plan.description()
                        + "\nDimensions: " + r.width() + " x " + r.length() + " x " + r.height()
                        + "\nBlocks: " + r.totalBlocks()
                        + " | Functional: " + r.functionalBlocks()
                        + " | Containers: " + r.containers()
                        + " | Redstone: " + r.redstone()
                        + "\nDocs: docs/FARM_MECHANICS.md",
                NamedTextColor.AQUA));
    }

    private @Nullable FarmPlan requirePlan(@NotNull Player player, @NotNull String id) {
        String key = id.toLowerCase(Locale.ROOT).trim();
        if (get(key) == null) {
            messages.send(player, "farm-missing", MessageManager.placeholders(
                    "name", id,
                    "options", String.join(", ", listIds())
            ));
            return null;
        }
        return registry.plan(key);
    }
}
