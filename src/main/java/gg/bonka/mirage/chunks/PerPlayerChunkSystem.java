package gg.bonka.mirage.chunks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import gg.bonka.mirage.Mirage;
import gg.bonka.mirage.chunks.packets.ChunkPacket;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class PerPlayerChunkSystem {

    @Getter
    private static PerPlayerChunkSystem instance;

    private final ProtocolManager protocolManager;

    private final HashMap<Player, ChunkRenderSettings> playerRenderSettings = new HashMap<>();

    public PerPlayerChunkSystem() {
        if(instance != null) {
            throw new IllegalStateException("Mirage PerPlayerChunkSystem instance already exists!");
        }

        instance = this;
        protocolManager = ProtocolLibrary.getProtocolManager();

        protocolManager.addPacketListener(new PacketAdapter(Mirage.getInstance(), PacketType.Play.Server.MAP_CHUNK) {
            @Override
            public void onPacketSending(PacketEvent event) {
                ChunkRenderSettings renderSettings = playerRenderSettings.get(event.getPlayer());

                if(renderSettings == null) {
                    return;
                }

                int chunkX = event.getPacket().getIntegers().read(0);
                int chunkZ = event.getPacket().getIntegers().read(1);

                Chunk chunk = event.getPlayer().getWorld().getChunkAt(chunkX, chunkZ);

                World renderWorld = renderSettings.getRenderWorldAs().get(event.getPlayer().getWorld());
                Chunk renderChunk = renderWorld != null ? renderWorld.getChunkAt(chunkX, chunkZ) : renderSettings.getRenderChunkAs().get(chunk);

                event.setPacket(new ChunkPacket(chunkX, chunkZ, renderChunk));
            }
        });
    }

    /**
     * Updates the chunks around a player based on the specified render distance.
     *
     * @param player the player for whom the chunks are updated
     * @param useClientRenderDistance a boolean indicating whether to use the client's render distance (more expensive but visually better)
     */
    public void updateChunks(Player player, boolean useClientRenderDistance) {
        int renderDistance = useClientRenderDistance ? player.getClientViewDistance() : Bukkit.getViewDistance();
        Chunk playerChunk = player.getChunk();

        for(int x = -renderDistance; x < renderDistance; x++) {
            for(int z = -renderDistance; z < renderDistance; z++) {
                if (Math.sqrt(x * x + z * z) > renderDistance) {
                    continue;
                }

                int chunkX = playerChunk.getX() + x / 16;
                int chunkZ = playerChunk.getZ() + z / 16;

                Chunk chunk = player.getWorld().getChunkAt(chunkX, chunkZ);
                protocolManager.sendServerPacket(player, new ChunkPacket(chunkX, chunkZ, chunk));
            }
        }
    }

    /**
     * Renders the world as the visualizer world for the given player.
     *
     * @apiNote This does not automatically update the client side chunks that are currently loaded!
     * @see PerPlayerChunkSystem#updateChunks(Player, boolean) updateChunks to update the loaded chunks automatically.
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
     * @see PerPlayerChunkSystem#updateChunks(Player, boolean) updateChunks to update the loaded chunks automatically.
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
     * @see PerPlayerChunkSystem#updateChunks(Player, boolean) updateChunks to update the loaded chunks automatically.
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
     * @see PerPlayerChunkSystem#updateChunks(Player, boolean) updateChunks to update the loaded chunks automatically.
     * @see PerPlayerChunkSystem#removeRendering(Player) removeRendering use this instead if you want to remove all rendering from the player.
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
     * @see PerPlayerChunkSystem#updateChunks(Player, boolean) updateChunks to update the loaded chunks automatically.
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
}
