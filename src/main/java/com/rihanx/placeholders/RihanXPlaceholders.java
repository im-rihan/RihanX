package com.rihanx.placeholders;

import com.rihanx.RihanX;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Soft-depend PlaceholderAPI hook. Registers only when PAPI is present at runtime.
 */
public final class RihanXPlaceholders {

    private RihanXPlaceholders() {
    }

    public static void tryRegister(@NotNull RihanX plugin) {
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
        } catch (ClassNotFoundException ignored) {
            return;
        }
        try {
            Class<?> expansionClass = Class.forName("com.rihanx.placeholders.RihanXExpansion");
            Constructor<?> ctor = expansionClass.getConstructor(RihanX.class);
            Object expansion = ctor.newInstance(plugin);
            Method register = expansionClass.getMethod("register");
            Object result = register.invoke(expansion);
            if (Boolean.FALSE.equals(result)) {
                plugin.getLogger().warning("PlaceholderAPI expansion failed to register.");
            } else {
                plugin.getLogger().info("PlaceholderAPI expansion registered (%rihanx_*%).");
            }
        } catch (Throwable ex) {
            plugin.getLogger().log(Level.WARNING, "Could not register PlaceholderAPI expansion", ex);
        }
    }

    static @Nullable String resolve(@NotNull RihanX plugin, @Nullable Player player, @NotNull String params) {
        String key = params.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "homes_count" -> player == null
                    ? "0"
                    : String.valueOf(plugin.getHomeService().listHomes(player).size());
            case "homes" -> {
                if (player == null) {
                    yield "";
                }
                List<String> homes = plugin.getHomeService().listHomes(player);
                yield homes.isEmpty() ? "" : String.join(", ", homes);
            }
            case "warps_count" -> String.valueOf(plugin.getWarpService().listWarps().size());
            default -> null;
        };
    }
}
