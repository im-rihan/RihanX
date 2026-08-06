package com.rihanx.commands.modules;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.chat.ChatService;
import com.rihanx.commands.CommandSupport;
import com.rihanx.managers.MessageManager;
import com.rihanx.utils.TextUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Handles /msg (/tell /w) and /reply (/r) and /rx msg / /rx reply.
 */
public final class ChatModule {

    private final @NotNull RihanX plugin;

    public ChatModule(@NotNull RihanX plugin) {
        this.plugin = plugin;
    }

    public boolean handle(
            @NotNull CommandSender sender,
            @NotNull String command,
            @NotNull String[] args,
            @NotNull MessageManager messages
    ) {
        Player player = CommandSupport.requirePlayer(sender, messages);
        if (player == null) {
            return true;
        }

        ChatService chat = plugin.getChatService();
        String cmd = command.toLowerCase(Locale.ROOT);

        return switch (cmd) {
            case "msg", "tell", "w" -> handleMsg(player, args, messages, chat);
            case "reply", "r" -> handleReply(player, args, messages, chat);
            default -> {
                CommandSupport.usage(messages, sender, "/msg <player> <message> | /r <message>");
                yield true;
            }
        };
    }

    private boolean handleMsg(
            @NotNull Player player,
            @NotNull String[] args,
            @NotNull MessageManager messages,
            @NotNull ChatService chat
    ) {
        if (!CommandSupport.checkPerm(player, PermissionNodes.MSG, messages)) {
            return true;
        }
        if (args.length < 2) {
            CommandSupport.usage(messages, player, "/msg <player> <message>");
            return true;
        }
        Player target = CommandSupport.resolveNamedPlayer(player, args[0], messages);
        if (target == null) {
            return true;
        }
        chat.msg(player, target, TextUtil.joinFrom(1, " ", args));
        return true;
    }

    private boolean handleReply(
            @NotNull Player player,
            @NotNull String[] args,
            @NotNull MessageManager messages,
            @NotNull ChatService chat
    ) {
        if (!CommandSupport.checkPerm(player, PermissionNodes.REPLY, messages)) {
            return true;
        }
        if (args.length < 1) {
            CommandSupport.usage(messages, player, "/r <message>");
            return true;
        }
        chat.reply(player, TextUtil.joinFrom(0, " ", args));
        return true;
    }
}
