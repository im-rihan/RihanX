package com.rihanx.bridge;

import com.rihanx.RihanX;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thin wrapper — bridge builds live in {@link com.rihanx.build.BuildToolService}.
 */
public final class BridgeService {

    private final @NotNull RihanX plugin;

    public BridgeService(@NotNull RihanX plugin) {
        this.plugin = plugin;
    }

    public void build(
            @NotNull Player player,
            @Nullable Integer lengthArg,
            @Nullable Integer widthArg,
            @Nullable String materialArg
    ) {
        plugin.getBuildToolService().bridge(player, lengthArg, widthArg, materialArg);
    }

    public void undo(@NotNull Player player) {
        plugin.getBuildToolService().undo(player);
    }
}
