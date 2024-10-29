package gg.bonka.mirage.chunks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import gg.bonka.mirage.Mirage;
import gg.bonka.mirage.chunks.packets.ChunkPacket;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

public class ChunkRenderingSystem {

    @Getter
    private static ChunkRenderingSystem instance;

    private final HashMap<Player, ChunkRenderSettings> playerRenderSettings = new HashMap<>();

    public ChunkRenderingSystem() {
        if(instance != null) {
            throw new IllegalStateException("Mirage PerPlayerChunkSystem instance already exists!");
        }

        instance = this;

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(Mirage.getInstance(), PacketType.Play.Server.MAP_CHUNK) {
            @Override
            public void onPacketSending(PacketEvent event) {
                int chunkX = event.getPacket().getIntegers().read(0);
                int chunkZ = event.getPacket().getIntegers().read(1);

                event.setPacket(getChunkPacket(event.getPlayer(), chunkX, chunkZ));
            }
        });

        // Ensures interacting with mirage blocks doesn't remove the ghost blocks.
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(Mirage.getInstance(), ListenerPriority.LOWEST, PacketType.Play.Server.BLOCK_CHANGE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                ChunkRenderSettings renderSettings = playerRenderSettings.get(player);

                if(renderSettings == null)
                    return;

                BlockPosition position = event.getPacket().getBlockPositionModifier().read(0);

                Location location = new Location(player.getWorld(), position.getX(), position.getY(), position.getZ());

                WrappedBlockData worldBlockData = WrappedBlockData.createData(location.getBlock().getBlockData());
                WrappedBlockData packetBlockData = event.getPacket().getBlockData().read(0);

                // Means this block is already a fake block that is most likely being sent by another plugins
                // We don't want to intervene with those packets!
                if(!worldBlockData.equals(packetBlockData))
                    return;

                World renderAsWorld = renderSettings.getRenderWorldAs().get(location.getWorld());
                Chunk renderAsChunk = renderSettings.getRenderChunkAs().get(location.getChunk());

                if(renderAsWorld == null && renderAsChunk == null)
                    return;

                // Get the fake block, and simply swap the type of the packet
                Location renderLocation = new Location(renderAsWorld != null ? renderAsWorld : renderAsChunk.getWorld(), position.getX(), position.getY(), position.getZ());
                WrappedBlockData data = WrappedBlockData.createData(renderLocation.getBlock().getBlockData());

                event.getPacket().getBlockData().write(0, data);
            }
        });
    }

    /**
     * Updates the chunks around a player based on the specified render distance.
     *
     * @param player the player for whom the chunks are updated
     */
    public void updateChunks(Player player) {
        Location location = player.getLocation();
        Optional<World> randomWorld = Bukkit.getWorlds().stream().filter(world -> world != player.getWorld()).findFirst();

        randomWorld.ifPresent(world -> player.teleport(world.getSpawnLocation()));
        player.teleport(location);
    }

    /**
     * Renders the world as the visualizer world for the given player.
     *
     * @apiNote This does not automatically update the client side chunks that are currently loaded!
     * @see ChunkRenderingSystem#updateChunks(Player) updateChunks to update the loaded chunks automatically.
     *
     * @param player the player for whom the world is rendered
     * @param world the original world to render
     * @param visualizer the visualizer world to render
     */
    public void renderWorldAs(Player player, World world, World visualizer) {
        ChunkRenderSettings chunkRenderSettings = playerRenderSettings.get(player);

        if(chunkRenderSettings == null) {
            playerRenderSettings.put(player, new ChunkRenderSettings(world, visualizer));
            return;
        }

        chunkRenderSettings.getRenderWorldAs().put(world, visualizer);
    }

    /**
     * Renders the specified chunk as the visualizer chunk for the given player.
     *
     * @apiNote This does not automatically update the client side chunks that are currently loaded!
     * @see ChunkRenderingSystem#updateChunks(Player) updateChunks to update the loaded chunks automatically.
     *
     * @param player the player for whom the chunk is rendered
     * @param chunk the original chunk to render
     * @param visualizer the visualizer chunk to render
     */
    public void renderChunkAs(Player player, Chunk chunk, Chunk visualizer) {
        ChunkRenderSettings chunkRenderSettings = playerRenderSettings.get(player);

        if(chunkRenderSettings == null) {
            playerRenderSettings.put(player, new ChunkRenderSettings(chunk, visualizer));
            return;
        }

        chunkRenderSettings.getRenderChunkAs().put(chunk, visualizer);
    }

    /**
     * Removes the rendering settings associated with the specified player.
     *
     * @apiNote This does not automatically update the client side chunks that are currently loaded!
     * @see ChunkRenderingSystem#updateChunks(Player) updateChunks to update the loaded chunks automatically.
     *
     * @param player the player for whom to remove the rendering settings
     */
    public void removeRendering(Player player) {
        playerRenderSettings.remove(player);
    }

    /**
     * Removes the specified world from the rendering settings associated with the player.
     * If the player's rendering settings are not found, nothing happens.
     *
     * @apiNote This does not automatically update the client side chunks that are currently loaded!
     *          Also doesn't completely remove rendering from the world, specific chunks will keep rendering differently.
     *
     * @see ChunkRenderingSystem#updateChunks(Player) updateChunks to update the loaded chunks automatically.
     * @see ChunkRenderingSystem#removeRendering(Player) removeRendering use this instead if you want to remove all rendering from the player.
     *
     * @param player the player whose rendering settings are considered
     * @param world the world to remove from rendering settings
     */
    public void removeWorldRendering(Player player, World world) {
        ChunkRenderSettings chunkRenderSettings = playerRenderSettings.get(player);

        if(chunkRenderSettings == null) {
            return;
        }

        chunkRenderSettings.getRenderWorldAs().remove(world);
    }

    /**
     * Removes the specified chunk from the rendering settings associated with the player.
     *
     * @apiNote This does not automatically update the client side chunks that are currently loaded!
     * @see ChunkRenderingSystem#updateChunks(Player) updateChunks to update the loaded chunks automatically.
     *
     * @param player the player whose rendering settings are considered
     * @param chunk the chunk to remove from rendering settings
     */
    public void removeChunkRendering(Player player, Chunk chunk) {
        ChunkRenderSettings chunkRenderSettings = playerRenderSettings.get(player);

        if(chunkRenderSettings == null) {
            return;
        }

        chunkRenderSettings.getRenderChunkAs().remove(chunk);
    }

    private ChunkPacket getChunkPacket(Player player, int chunkX, int chunkZ) {
        ChunkRenderSettings renderSettings = playerRenderSettings.get(player);
        Chunk chunk = player.getWorld().getChunkAt(chunkX, chunkZ);

        if(renderSettings == null)
            return new ChunkPacket(chunkX, chunkZ, chunk);

        World renderWorld = renderSettings.getRenderWorldAs().get(player.getWorld());
        Chunk renderChunk = renderWorld != null ? renderWorld.getChunkAt(chunkX, chunkZ) : renderSettings.getRenderChunkAs().get(chunk);

        return new ChunkPacket(chunkX, chunkZ, Objects.requireNonNullElse(renderChunk, chunk));
    }
}
