package com.rihanx.managers;

import com.rihanx.RihanX;
import com.rihanx.commands.RihanXCommand;
import com.rihanx.tabcomplete.RihanXTabCompleter;
import org.bukkit.command.PluginCommand;
import org.jetbrains.annotations.NotNull;

/**
 * Registers plugin commands.
 */
public final class CommandManager {

    private final @NotNull RihanX plugin;

    public CommandManager(@NotNull RihanX plugin) {
        this.plugin = plugin;
    }

    public void register() {
        RihanXCommand executor = new RihanXCommand(plugin);
        RihanXTabCompleter tabCompleter = new RihanXTabCompleter(plugin);
        PluginCommand command = plugin.getCommand("rihanx");
        if (command == null) {
            plugin.getLogger().severe("Command 'rihanx' not defined in plugin.yml");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(tabCompleter);
    }
}
