package com.rihanx.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Message parsing and delivery using MiniMessage and legacy color codes.
 */
public final class MessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND =
            LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION =
            LegacyComponentSerializer.legacySection();

    private MessageUtil() {
    }

    public static @NotNull Component parse(@NotNull String input) {
        if (input.isEmpty()) {
            return Component.empty();
        }
        if (containsMiniMessage(input)) {
            try {
                return MINI_MESSAGE.deserialize(input);
            } catch (Exception ignored) {
                // Fall through to legacy parsing
            }
        }
        if (input.indexOf('&') >= 0) {
            return LEGACY_AMPERSAND.deserialize(input);
        }
        if (input.indexOf('§') >= 0) {
            return LEGACY_SECTION.deserialize(input);
        }
        return Component.text(input);
    }

    public static @NotNull Component parse(@NotNull String input, @NotNull Map<String, String> placeholders) {
        return parse(applyPlaceholders(input, placeholders));
    }

    public static @NotNull String applyPlaceholders(@NotNull String input, @NotNull Map<String, String> placeholders) {
        String result = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace("{" + key + "}", value);
            result = result.replace("%" + key + "%", value);
        }
        return result;
    }

    public static void send(@NotNull CommandSender sender, @NotNull Component component) {
        sender.sendMessage(component);
    }

    public static void send(@NotNull CommandSender sender, @NotNull String raw) {
        send(sender, parse(raw));
    }

    public static void send(@NotNull CommandSender sender, @NotNull String raw, @NotNull Map<String, String> placeholders) {
        send(sender, parse(raw, placeholders));
    }

    public static void sendActionBar(@NotNull Player player, @NotNull String raw) {
        player.sendActionBar(parse(raw));
    }

    public static void sendActionBar(@NotNull Player player, @NotNull String raw, @NotNull Map<String, String> placeholders) {
        player.sendActionBar(parse(raw, placeholders));
    }

    public static @NotNull String plain(@NotNull Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static boolean containsMiniMessage(@NotNull String input) {
        return input.indexOf('<') >= 0 && input.indexOf('>') >= 0;
    }

    public static @NotNull MiniMessage miniMessage() {
        return MINI_MESSAGE;
    }

    public static @Nullable String colorizeLegacy(@Nullable String input) {
        if (input == null) {
            return null;
        }
        return input.replace('&', '§');
    }
}
