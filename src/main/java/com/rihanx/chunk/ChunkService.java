package com.rihanx.chunk;

import com.rihanx.managers.MessageManager;
import com.rihanx.utils.BiomeUtil;
import com.rihanx.utils.ChunkUtil;
import com.rihanx.utils.SlimeUtil;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Chunk inspection and manipulation.
 */
public final class ChunkService {

    private final @NotNull MessageManager messages;

    public ChunkService(@NotNull MessageManager messages) {
        this.messages = messages;
    }

    public void sendInfo(@NotNull Player player) {
        Location loc = player.getLocation();
        World world = player.getWorld();
        Chunk chunk = loc.getChunk();
        messages.send(player, "chunk-info-header", MessageManager.placeholders(
                "x", chunk.getX(),
                "z", chunk.getZ()
        ));
        sendLine(player, "Biome", BiomeUtil.displayName(loc.getBlock().getBiome()));
        sendLine(player, "World", world.getName());
        sendLine(player, "Slime", SlimeUtil.isSlimeChunk(world, chunk.getX(), chunk.getZ()) ? "Yes" : "No");
        sendLine(player, "Loaded", world.isChunkLoaded(chunk.getX(), chunk.getZ()) ? "Yes" : "No");
        sendLine(player, "Light", String.valueOf(ChunkUtil.getLightLevel(loc)));
        sendLine(player, "Highest", String.valueOf(world.getHighestBlockYAt(loc)));
        sendLine(player, "Entities", String.valueOf(countEntities(chunk)));
        sendLine(player, "Tile Entities", String.valueOf(countTileEntities(chunk)));
    }

    public boolean load(@NotNull Player player) {
        Chunk chunk = player.getLocation().getChunk();
        return player.getWorld().loadChunk(chunk.getX(), chunk.getZ(), true);
    }

    public boolean unload(@NotNull Player player) {
        Chunk chunk = player.getLocation().getChunk();
        return player.getWorld().unloadChunk(chunk.getX(), chunk.getZ(), true);
    }

    public boolean regenerate(@NotNull Player player) {
        Chunk chunk = player.getLocation().getChunk();
        World world = player.getWorld();
        try {
            world.regenerateChunk(chunk.getX(), chunk.getZ());
            return true;
        } catch (UnsupportedOperationException | NoSuchMethodError ex) {
            messages.send(player, "feature-disabled");
            return false;
        }
    }

    public void sendBorder(@NotNull Player player) {
        Chunk chunk = player.getLocation().getChunk();
        int[] border = ChunkUtil.borderBlocks(chunk.getX(), chunk.getZ());
        messages.send(player, "chunk-border", MessageManager.placeholders(
                "minX", border[0],
                "maxX", border[1],
                "minZ", border[2],
                "maxZ", border[3]
        ));
    }

    public void sendEntityCount(@NotNull Player player) {
        Chunk chunk = player.getLocation().getChunk();
        messages.send(player, "chunk-entities", MessageManager.placeholders("count", countEntities(chunk)));
    }

    public void sendTileEntityCount(@NotNull Player player) {
        Chunk chunk = player.getLocation().getChunk();
        messages.send(player, "chunk-tileentities", MessageManager.placeholders("count", countTileEntities(chunk)));
    }

    private int countEntities(@NotNull Chunk chunk) {
        int count = 0;
        for (Entity ignored : chunk.getEntities()) {
            count++;
        }
        return count;
    }

    private int countTileEntities(@NotNull Chunk chunk) {
        int count = 0;
        for (BlockState ignored : chunk.getTileEntities()) {
            count++;
        }
        return count;
    }

    private void sendLine(@NotNull CommandSender sender, @NotNull String key, @NotNull String value) {
        messages.send(sender, "chunk-info-line", MessageManager.placeholders("key", key, "value", value));
    }
}
