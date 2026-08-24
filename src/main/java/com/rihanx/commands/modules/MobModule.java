package com.rihanx.commands.modules;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.commands.CommandSupport;
import com.rihanx.managers.MessageManager;
import com.rihanx.mob.MobSpawnService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Advanced spawn-at-feet: {@code /mob villager|zombie} (also {@code /spawnmob}, {@code /rx mob}).
 */
public final class MobModule {

    private final @NotNull RihanX plugin;

    public MobModule(@NotNull RihanX plugin) {
        this.plugin = plugin;
    }

    public boolean handle(
            @NotNull CommandSender sender,
            @NotNull String[] args,
            @NotNull MessageManager messages
    ) {
        Player player = CommandSupport.requirePlayer(sender, messages);
        if (player == null) {
            return true;
        }
        if (!CommandSupport.checkOpPerm(player, PermissionNodes.MOB, messages)) {
            return true;
        }

        MobSpawnService mobs = plugin.getMobSpawnService();
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            CommandSupport.usage(messages, player, "/mob <villager|zombie> [amount] [farmer|minecart|nametag]");
            messages.send(player, "mob-help");
            return true;
        }

        MobSpawnService.SpawnRequest request = MobSpawnService.parse(args, mobs.maxAmount());
        if (request == null) {
            messages.send(player, "mob-unknown-type", MessageManager.placeholders("input", args[0]));
            CommandSupport.usage(messages, player, "/mob <villager|zombie> [amount] [farmer|minecart|nametag]");
            return true;
        }

        String name = request.kind() == MobSpawnService.Kind.VILLAGER
                ? (request.professionId() != null && !"none".equals(request.professionId())
                ? Character.toUpperCase(request.professionId().charAt(0)) + request.professionId().substring(1)
                : "Villager")
                : "Zombie";
        request = request.withName(name);

        int spawned = mobs.spawnAtPlayer(player, request);
        if (spawned == 0) {
            messages.send(player, "mob-failed", MessageManager.placeholders(
                    "type", request.kind().name().toLowerCase()
            ));
            return true;
        }
        messages.send(player, "mob-spawned", MessageManager.placeholders(
                "count", spawned,
                "type", request.kind().name().toLowerCase(),
                "spot", "your feet"
        ));
        return true;
    }
}
