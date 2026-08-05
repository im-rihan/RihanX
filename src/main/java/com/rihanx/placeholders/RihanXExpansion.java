package com.rihanx.placeholders;

import com.rihanx.RihanX;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion. Loaded only when PAPI is on the server (soft-depend).
 */
public final class RihanXExpansion extends PlaceholderExpansion {

    private final @NotNull RihanX plugin;

    public RihanXExpansion(@NotNull RihanX plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "rihanx";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Rihan";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(@Nullable Player player, @NotNull String params) {
        return RihanXPlaceholders.resolve(plugin, player, params);
    }
}
