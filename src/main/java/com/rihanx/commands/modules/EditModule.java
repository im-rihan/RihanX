package com.rihanx.commands.modules;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.commands.CommandSupport;
import com.rihanx.edit.Cuboid;
import com.rihanx.edit.EditService;
import com.rihanx.managers.MessageManager;
import com.rihanx.utils.MessageUtil;
import com.rihanx.utils.NumberUtil;
import com.rihanx.utils.PermissionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * WorldEdit-lite command handler with large-edit confirm GUI.
 */
public final class EditModule {

    private final @NotNull RihanX plugin;

    public EditModule(@NotNull RihanX plugin) {
        this.plugin = plugin;
    }

    public boolean handle(
            @NotNull CommandSender sender,
            @NotNull String[] args,
            @NotNull MessageManager messages
    ) {
        if (!PermissionUtil.hasOpOnly(sender, PermissionNodes.EDIT)) {
            messages.send(sender, "no-permission");
            return true;
        }
        Player player = CommandSupport.requirePlayer(sender, messages);
        if (player == null) {
            return true;
        }
        if (args.length == 0) {
            CommandSupport.usage(messages, sender,
                    "/rx edit <wand|pos1|pos2|size|count|set|replace|walls|outline|hollow|clear|copy|paste|rotate|expand|contract|undo|redo>");
            return true;
        }
        EditService edit = plugin.getEditService();
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "wand" -> {
                if (!CommandSupport.checkOpPerm(player, PermissionNodes.EDIT_WAND, messages)) return true;
                edit.giveWand(player);
            }
            case "pos1", "pos2" -> {
                if (!CommandSupport.checkOpPerm(player, PermissionNodes.EDIT_WAND, messages)) return true;
                edit.setPos(player, sub.equals("pos1") ? 1 : 2, player.getLocation());
            }
            case "size" -> edit.sendSize(player);
            case "count" -> edit.count(player, args.length >= 2 ? args[1] : null);
            case "set" -> {
                if (args.length < 2) {
                    CommandSupport.usage(messages, sender, "/rx edit set <material>");
                    return true;
                }
                confirmLarge(player, messages, edit, () -> edit.set(player, args[1]));
            }
            case "replace" -> {
                if (args.length < 3) {
                    CommandSupport.usage(messages, sender, "/rx edit replace <from> <to>");
                    return true;
                }
                confirmLarge(player, messages, edit, () -> edit.replace(player, args[1], args[2]));
            }
            case "walls" -> {
                if (args.length < 2) {
                    CommandSupport.usage(messages, sender, "/rx edit walls <material>");
                    return true;
                }
                edit.walls(player, args[1]);
            }
            case "outline" -> {
                if (args.length < 2) {
                    CommandSupport.usage(messages, sender, "/rx edit outline <material>");
                    return true;
                }
                edit.outline(player, args[1]);
            }
            case "hollow" -> {
                if (args.length < 2) {
                    CommandSupport.usage(messages, sender, "/rx edit hollow <material>");
                    return true;
                }
                edit.hollow(player, args[1]);
            }
            case "clear" -> confirmLarge(player, messages, edit, () -> edit.clear(player));
            case "copy" -> {
                if (!CommandSupport.checkOpPerm(player, PermissionNodes.EDIT_CLIPBOARD, messages)) return true;
                edit.copy(player);
            }
            case "paste" -> {
                if (!CommandSupport.checkOpPerm(player, PermissionNodes.EDIT_CLIPBOARD, messages)) return true;
                edit.paste(player);
            }
            case "rotate" -> {
                if (!CommandSupport.checkOpPerm(player, PermissionNodes.EDIT_CLIPBOARD, messages)) return true;
                if (args.length < 2) {
                    CommandSupport.usage(messages, sender, "/rx edit rotate <90|180|270>");
                    return true;
                }
                Integer degrees = NumberUtil.parseInt(args[1]);
                if (degrees == null) {
                    CommandSupport.invalidNumber(sender, messages, args[1]);
                    return true;
                }
                edit.rotate(player, degrees);
            }
            case "expand" -> {
                if (args.length < 2) {
                    CommandSupport.usage(messages, sender, "/rx edit expand <amount> [direction]");
                    return true;
                }
                Integer amount = NumberUtil.parseInt(args[1]);
                if (amount == null) {
                    CommandSupport.invalidNumber(sender, messages, args[1]);
                    return true;
                }
                String direction = args.length >= 3 ? args[2] : "all";
                edit.expand(player, amount, direction);
            }
            case "contract" -> {
                if (args.length < 2) {
                    CommandSupport.usage(messages, sender, "/rx edit contract <amount> [direction]");
                    return true;
                }
                Integer amount = NumberUtil.parseInt(args[1]);
                if (amount == null) {
                    CommandSupport.invalidNumber(sender, messages, args[1]);
                    return true;
                }
                String direction = args.length >= 3 ? args[2] : "all";
                edit.contract(player, amount, direction);
            }
            case "undo" -> {
                if (!CommandSupport.checkOpPerm(player, PermissionNodes.EDIT_HISTORY, messages)) return true;
                edit.undo(player);
            }
            case "redo" -> {
                if (!CommandSupport.checkOpPerm(player, PermissionNodes.EDIT_HISTORY, messages)) return true;
                edit.redo(player);
            }
            default -> CommandSupport.usage(messages, sender,
                    "/rx edit <wand|pos1|pos2|size|count|set|replace|walls|outline|hollow|clear|copy|paste|rotate|expand|contract|undo|redo>");
        }
        return true;
    }

    private void confirmLarge(
            @NotNull Player player,
            @NotNull MessageManager messages,
            @NotNull EditService edit,
            @NotNull Runnable action
    ) {
        Cuboid cuboid = edit.requireSelection(player);
        if (cuboid == null) {
            return;
        }
        if (cuboid.volume() > edit.getConfirmAbove()) {
            plugin.getGuiManager().confirm(
                    MessageUtil.parse("<gold>Confirm edit of " + cuboid.volume() + " blocks?</gold>"),
                    confirmed -> action.run(),
                    cancelled -> messages.send(cancelled, "confirm-cancelled")
            ).open(player);
        } else {
            action.run();
        }
    }
}
