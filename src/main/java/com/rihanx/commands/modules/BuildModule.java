package com.rihanx.commands.modules;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.build.BuildToolService;
import com.rihanx.commands.CommandSupport;
import com.rihanx.managers.MessageManager;
import com.rihanx.utils.NumberUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Handles /build and standalone builder tools (/platform, /wall, /cyl, …).
 */
public final class BuildModule {

    private final @NotNull RihanX plugin;

    public BuildModule(@NotNull RihanX plugin) {
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

        String tool = command.toLowerCase(Locale.ROOT);
        if (tool.equals("build") || tool.equals("rxbuild")) {
            if (!CommandSupport.checkPerm(player, PermissionNodes.BUILD, messages)) {
                return true;
            }
            if (args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equals("?")) {
                messages.send(player, "build-usage");
                return true;
            }
            tool = args[0].toLowerCase(Locale.ROOT);
            args = shift(args);
        }

        if (tool.equals("undo")) {
            if (!CommandSupport.checkPerm(player, PermissionNodes.BUILD_UNDO, messages)) {
                return true;
            }
            plugin.getBuildToolService().undo(player);
            return true;
        }

        return dispatchTool(player, tool, args, messages);
    }

    private boolean dispatchTool(
            @NotNull Player player,
            @NotNull String tool,
            @NotNull String[] args,
            @NotNull MessageManager messages
    ) {
        if (args.length > 0 && (args[0].equalsIgnoreCase("undo") || args[0].equalsIgnoreCase("help"))) {
            if (args[0].equalsIgnoreCase("help")) {
                messages.send(player, "build-usage");
                return true;
            }
            if (!CommandSupport.checkPerm(player, PermissionNodes.BUILD_UNDO, messages)) {
                return true;
            }
            plugin.getBuildToolService().undo(player);
            return true;
        }

        BuildToolService build = plugin.getBuildToolService();
        return switch (tool) {
            case "platform", "pad" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.BUILD_PLATFORM, messages)) {
                    yield true;
                }
                Integer size = args.length >= 1 ? NumberUtil.parseInt(args[0]) : null;
                String material = null;
                if (size != null && args.length >= 2) {
                    material = args[1];
                } else if (size == null && args.length >= 1) {
                    material = args[0];
                }
                build.platform(player, size, material);
                yield true;
            }
            case "plain", "clearland", "clearpad", "plot" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.BUILD_PLATFORM, messages)) {
                    yield true;
                }
                Integer size = args.length >= 1 ? NumberUtil.parseInt(args[0]) : null;
                String material = null;
                if (size != null && args.length >= 2) {
                    material = args[1];
                } else if (size == null && args.length >= 1) {
                    material = args[0];
                }
                build.plain(player, size, material);
                yield true;
            }
            case "wall" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.BUILD_WALL, messages)) {
                    yield true;
                }
                Integer length = args.length >= 1 ? NumberUtil.parseInt(args[0]) : null;
                Integer height = null;
                String material = null;
                if (length != null && args.length >= 2) {
                    Integer h = NumberUtil.parseInt(args[1]);
                    if (h != null) {
                        height = h;
                        if (args.length >= 3) {
                            material = args[2];
                        }
                    } else {
                        material = args[1];
                    }
                } else if (length == null && args.length >= 1) {
                    material = args[0];
                }
                build.wall(player, length, height, material);
                yield true;
            }
            case "pillar", "tower" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.BUILD_PILLAR, messages)) {
                    yield true;
                }
                Integer height = args.length >= 1 ? NumberUtil.parseInt(args[0]) : null;
                String material = null;
                if (height != null && args.length >= 2) {
                    material = args[1];
                } else if (height == null && args.length >= 1) {
                    material = args[0];
                }
                build.pillar(player, height, material);
                yield true;
            }
            case "cyl", "cylinder" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.BUILD_CYL, messages)) {
                    yield true;
                }
                yield cylinder(player, build, false, args, messages);
            }
            case "hcyl", "hcylinder" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.BUILD_CYL, messages)) {
                    yield true;
                }
                yield cylinder(player, build, true, args, messages);
            }
            case "sphere" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.BUILD_SPHERE, messages)) {
                    yield true;
                }
                yield sphere(player, build, false, args, messages);
            }
            case "hsphere" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.BUILD_SPHERE, messages)) {
                    yield true;
                }
                yield sphere(player, build, true, args, messages);
            }
            case "tunnel" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.BUILD_TUNNEL, messages)) {
                    yield true;
                }
                Integer length = args.length >= 1 ? NumberUtil.parseInt(args[0]) : null;
                Integer width = null;
                Integer height = null;
                String material = null;
                int i = 0;
                if (length != null) {
                    i = 1;
                }
                if (args.length > i) {
                    Integer w = NumberUtil.parseInt(args[i]);
                    if (w != null) {
                        width = w;
                        i++;
                        if (args.length > i) {
                            Integer h = NumberUtil.parseInt(args[i]);
                            if (h != null) {
                                height = h;
                                i++;
                            }
                        }
                    }
                }
                if (args.length > i) {
                    material = args[i];
                }
                if (length == null && material == null && args.length >= 1) {
                    material = args[0];
                }
                build.tunnel(player, length, width, height, material);
                yield true;
            }
            case "flatten", "level" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.BUILD_FLATTEN, messages)) {
                    yield true;
                }
                Integer radius = args.length >= 1 ? NumberUtil.parseInt(args[0]) : null;
                build.flatten(player, radius);
                yield true;
            }
            case "drain" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.BUILD_DRAIN, messages)) {
                    yield true;
                }
                Integer radius = args.length >= 1 ? NumberUtil.parseInt(args[0]) : null;
                build.drain(player, radius);
                yield true;
            }
            case "bridge" -> bridgeViaBuild(player, build, args, messages);
            case "pyramid" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.BUILD_PYRAMID, messages)) {
                    yield true;
                }
                yield pyramid(player, build, false, args, messages);
            }
            case "hpyramid" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.BUILD_PYRAMID, messages)) {
                    yield true;
                }
                yield pyramid(player, build, true, args, messages);
            }
            case "stairs", "stair" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.BUILD_STAIRS, messages)) {
                    yield true;
                }
                Integer length = args.length >= 1 ? NumberUtil.parseInt(args[0]) : null;
                Integer width = null;
                String material = null;
                if (length != null && args.length >= 2) {
                    Integer w = NumberUtil.parseInt(args[1]);
                    if (w != null) {
                        width = w;
                        if (args.length >= 3) {
                            material = args[2];
                        }
                    } else {
                        material = args[1];
                    }
                } else if (length == null && args.length >= 1) {
                    material = args[0];
                }
                build.stairs(player, length, width, material);
                yield true;
            }
            case "stack" -> {
                if (!player.isOp()
                        && !player.hasPermission(PermissionNodes.BUILD_STACK)
                        && !player.hasPermission(PermissionNodes.EDIT_CLIPBOARD)
                        && !player.hasPermission(PermissionNodes.BUILD_ALL)
                        && !player.hasPermission(PermissionNodes.EDIT_ALL)) {
                    messages.send(player, "no-permission");
                    yield true;
                }
                yield stack(player, args, messages);
            }
            default -> {
                messages.send(player, "build-usage");
                yield true;
            }
        };
    }

    private boolean pyramid(
            @NotNull Player player,
            @NotNull BuildToolService build,
            boolean hollow,
            @NotNull String[] args,
            @NotNull MessageManager messages
    ) {
        Integer size = args.length >= 1 ? NumberUtil.parseInt(args[0]) : null;
        String material = null;
        if (size != null && args.length >= 2) {
            material = args[1];
        } else if (size == null && args.length >= 1) {
            messages.send(player, "build-pyramid-usage");
            return true;
        }
        if (size == null) {
            messages.send(player, "build-pyramid-usage");
            return true;
        }
        build.pyramid(player, hollow, size, material);
        return true;
    }

    private boolean stack(
            @NotNull Player player,
            @NotNull String[] args,
            @NotNull MessageManager messages
    ) {
        if (args.length < 1) {
            messages.send(player, "build-stack-usage");
            return true;
        }
        Integer count = NumberUtil.parseInt(args[0]);
        if (count == null) {
            CommandSupport.invalidNumber(player, messages, args[0]);
            return true;
        }
        String direction = args.length >= 2 ? args[1] : "forward";
        plugin.getEditService().stack(player, count, direction);
        return true;
    }

    private boolean bridgeViaBuild(
            @NotNull Player player,
            @NotNull BuildToolService build,
            @NotNull String[] args,
            @NotNull MessageManager messages
    ) {
        if (!player.isOp()
                && !player.hasPermission(PermissionNodes.BRIDGE_BUILD)
                && !player.hasPermission(PermissionNodes.BUILD_BRIDGE)
                && !player.hasPermission(PermissionNodes.BUILD_ALL)) {
            messages.send(player, "no-permission");
            return true;
        }
        Integer length = args.length >= 1 ? NumberUtil.parseInt(args[0]) : null;
        Integer width = null;
        String material = null;
        if (length != null && args.length >= 2) {
            Integer w = NumberUtil.parseInt(args[1]);
            if (w != null) {
                width = w;
                if (args.length >= 3) {
                    material = args[2];
                }
            } else {
                material = args[1];
            }
        } else if (length == null && args.length >= 1) {
            material = args[0];
        }
        build.bridge(player, length, width, material);
        return true;
    }

    private boolean cylinder(
            @NotNull Player player,
            @NotNull BuildToolService build,
            boolean hollow,
            @NotNull String[] args,
            @NotNull MessageManager messages
    ) {
        Integer radius = args.length >= 1 ? NumberUtil.parseInt(args[0]) : null;
        Integer height = null;
        String material = null;
        if (radius != null && args.length >= 2) {
            Integer h = NumberUtil.parseInt(args[1]);
            if (h != null) {
                height = h;
                if (args.length >= 3) {
                    material = args[2];
                }
            } else {
                material = args[1];
            }
        } else if (radius == null && args.length >= 1) {
            // /cyl <material> requires radius — show usage
            messages.send(player, "build-cyl-usage");
            return true;
        }
        if (radius == null) {
            messages.send(player, "build-cyl-usage");
            return true;
        }
        build.cylinder(player, hollow, radius, height, material);
        return true;
    }

    private boolean sphere(
            @NotNull Player player,
            @NotNull BuildToolService build,
            boolean hollow,
            @NotNull String[] args,
            @NotNull MessageManager messages
    ) {
        Integer radius = args.length >= 1 ? NumberUtil.parseInt(args[0]) : null;
        String material = null;
        if (radius != null && args.length >= 2) {
            material = args[1];
        } else if (radius == null) {
            messages.send(player, "build-sphere-usage");
            return true;
        }
        build.sphere(player, hollow, radius, material);
        return true;
    }

    private static @NotNull String[] shift(@NotNull String[] args) {
        if (args.length <= 1) {
            return new String[0];
        }
        String[] next = new String[args.length - 1];
        System.arraycopy(args, 1, next, 0, next.length);
        return next;
    }
}
