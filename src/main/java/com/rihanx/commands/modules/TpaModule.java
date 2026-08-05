package com.rihanx.commands.modules;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.commands.CommandSupport;
import com.rihanx.managers.MessageManager;
import com.rihanx.teleport.TpaService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Handles /tpa, /tpahere, /tpaccept, /tpdeny, /tpcancel.
 */
public final class TpaModule {

    private final @NotNull RihanX plugin;

    public TpaModule(@NotNull RihanX plugin) {
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

        TpaService tpa = plugin.getTpaService();
        String cmd = command.toLowerCase(Locale.ROOT);

        return switch (cmd) {
            case "tpa" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.TPA, messages)) {
                    yield true;
                }
                if (args.length < 1) {
                    CommandSupport.usage(messages, sender, "/tpa <player>");
                    yield true;
                }
                Player target = CommandSupport.resolveNamedPlayer(sender, args[0], messages);
                if (target == null) {
                    yield true;
                }
                tpa.tpa(player, target);
                yield true;
            }
            case "tpahere" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.TPA_HERE, messages)) {
                    yield true;
                }
                if (args.length < 1) {
                    CommandSupport.usage(messages, sender, "/tpahere <player>");
                    yield true;
                }
                Player target = CommandSupport.resolveNamedPlayer(sender, args[0], messages);
                if (target == null) {
                    yield true;
                }
                tpa.tpahere(player, target);
                yield true;
            }
            case "tpaccept" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.TPA_ACCEPT, messages)) {
                    yield true;
                }
                tpa.accept(player);
                yield true;
            }
            case "tpdeny" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.TPA_DENY, messages)) {
                    yield true;
                }
                tpa.deny(player);
                yield true;
            }
            case "tpcancel" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.TPA_CANCEL, messages)) {
                    yield true;
                }
                tpa.cancel(player);
                yield true;
            }
            default -> {
                CommandSupport.usage(messages, sender, "/tpa|/tpahere <player> | /tpaccept | /tpdeny | /tpcancel");
                yield true;
            }
        };
    }
}
