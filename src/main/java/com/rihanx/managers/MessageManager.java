package com.rihanx.managers;

import com.rihanx.RihanX;
import com.rihanx.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Loads messages.yml and sends prefixed Adventure components.
 */
public final class MessageManager {

    private final @NotNull RihanX plugin;
    private final @NotNull ConfigManager configManager;
    private FileConfiguration messages;
    private File messagesFile;

    public MessageManager(@NotNull RihanX plugin, @NotNull ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        reload();
    }

    public void reload() {
        String fileName = configManager.getLanguageFile();
        messagesFile = new File(plugin.getDataFolder(), fileName);
        if (!messagesFile.exists()) {
            plugin.saveResource(fileName, false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        try (InputStream stream = plugin.getResource(fileName)) {
            if (stream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));
                messages.setDefaults(defaults);
                messages.options().copyDefaults(true);
                messages.save(messagesFile);
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not merge default messages", ex);
        }
    }

    public @NotNull String raw(@NotNull String path) {
        String value = messages.getString(path);
        if (value == null) {
            return "<red>Missing message: " + path + "</red>";
        }
        return value;
    }

    public @NotNull String prefixed(@NotNull String path) {
        String prefix = messages.getString("prefix", configManager.getPrefix());
        return prefix + raw(path);
    }

    public @NotNull Component component(@NotNull String path) {
        return MessageUtil.parse(prefixed(path));
    }

    public @NotNull Component component(@NotNull String path, @NotNull Map<String, String> placeholders) {
        return MessageUtil.parse(prefixed(path), placeholders);
    }

    public void send(@NotNull CommandSender sender, @NotNull String path) {
        MessageUtil.send(sender, component(path));
    }

    public void send(@NotNull CommandSender sender, @NotNull String path, @NotNull Map<String, String> placeholders) {
        MessageUtil.send(sender, component(path, placeholders));
    }

    public void sendRaw(@NotNull CommandSender sender, @NotNull String path) {
        MessageUtil.send(sender, MessageUtil.parse(raw(path)));
    }

    public void sendActionBar(@NotNull Player player, @NotNull String path, @NotNull Map<String, String> placeholders) {
        MessageUtil.sendActionBar(player, MessageUtil.applyPlaceholders(raw(path), placeholders));
    }

    public static @NotNull Map<String, String> placeholders(@NotNull Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders must be key/value pairs");
        }
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            String key = String.valueOf(pairs[i]);
            Object value = pairs[i + 1];
            map.put(key, value == null ? "" : String.valueOf(value));
        }
        return map;
    }

    public static @NotNull Map<String, String> empty() {
        return Collections.emptyMap();
    }
}
