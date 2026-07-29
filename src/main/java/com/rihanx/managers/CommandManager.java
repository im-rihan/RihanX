package com.rihanx.managers;

import com.rihanx.RihanX;
import com.rihanx.commands.RihanXCommand;
import com.rihanx.tabcomplete.RihanXTabCompleter;
import org.bukkit.command.PluginCommand;
import org.jetbrains.annotations.NotNull;

/**
 * Registers plugin commands (root /rx and standalone module commands).
 */
public final class CommandManager {

    private final @NotNull RihanX plugin;

    public CommandManager(@NotNull RihanX plugin) {
        this.plugin = plugin;
    }

    public void register() {
        RihanXCommand executor = new RihanXCommand(plugin);
        RihanXTabCompleter tabCompleter = new RihanXTabCompleter(plugin);

        bind("rihanx", executor, tabCompleter);
        for (String name : RihanXCommand.MODULE_COMMANDS) {
            bind(name, executor, tabCompleter);
        }
    }

    private void bind(
            @NotNull String name,
            @NotNull RihanXCommand executor,
            @NotNull RihanXTabCompleter tabCompleter
    ) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().severe("Command '" + name + "' not defined in plugin.yml");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(tabCompleter);
    }
}
