package com.rihanx.base;

import com.rihanx.RihanX;
import com.rihanx.managers.MessageManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Farm template listing + paste via {@link BaseService#pasteBlueprint}.
 */
public final class FarmService {

    private final @NotNull RihanX plugin;
    private final @NotNull MessageManager messages;
    private final @NotNull Map<String, BaseTemplates.BaseBlueprint> templates = FarmTemplates.all();

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
}
