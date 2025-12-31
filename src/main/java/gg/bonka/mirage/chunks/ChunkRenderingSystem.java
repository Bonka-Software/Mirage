package gg.bonka.mirage.chunks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import gg.bonka.mirage.Mirage;
import gg.bonka.mirage.chunks.events.FinishPlayerWorldRenderingReloadEvent;
import gg.bonka.mirage.chunks.events.StartPlayerWorldRenderingReloadEvent;
import gg.bonka.mirage.chunks.packets.ChunkPacket;
import gg.bonka.mirage.chunks.packets.MultiBlockPacket;
import lombok.Getter;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.bukkit.*;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ChunkRenderingSystem implements Listener {

    @Getter
    private static ChunkRenderingSystem instance;

    private final Map<UUID, ChunkRenderSettings> playerRenderSettings = new HashMap<>();
    private final Map<UUID, BukkitRunnable> playerRenderingTasks = new HashMap<>();

    public ChunkRenderingSystem() {
        if(instance != null) {
            throw new IllegalStateException("Mirage PerPlayerChunkSystem instance already exists!");
        }

        instance = this;

        Bukkit.getPluginManager().registerEvents(this, Mirage.getInstance());

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(Mirage.getInstance(), PacketType.Play.Server.MAP_CHUNK) {
            @Override
            public void onPacketSending(PacketEvent event) {
                int chunkX = event.getPacket().getIntegers().read(0);
                int chunkZ = event.getPacket().getIntegers().read(1);

                ChunkPacket chunkPacket = getChunkPacket(event.getPlayer(), chunkX, chunkZ);
                event.setPacket(chunkPacket);

                ChunkRenderSettings playerSettings = playerRenderSettings.get(event.getPlayer().getUniqueId());
                if(playerSettings != null) {
                    Chunk chunk = chunkPacket.getChunk();
                    playerSettings.getClientsideChunks().put(chunk.getChunkKey(), chunk.getWorld());
                }
            }
        });

        // Ensures interacting with mirage blocks doesn't remove the ghost blocks.
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(Mirage.getInstance(), ListenerPriority.LOWEST, PacketType.Play.Server.BLOCK_CHANGE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                ChunkRenderSettings renderSettings = playerRenderSettings.get(player.getUniqueId());

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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeRendering(event.getPlayer());
    }

    /**
     * Updates the chunks around a player based on the specified render distance.
     * The chunks are updated in order of distance from the player (closest first).
     *
     * @param player the player for whom the chunks are updated
     */
    public void updateChunks(Player player) {
        new StartPlayerWorldRenderingReloadEvent(player).callEvent();

        World world = player.getWorld();
        Location playerLocation = player.getLocation();
        int playerX = playerLocation.getBlockX() >> 4;
        int playerZ = playerLocation.getBlockZ() >> 4;
        int viewDistance = player.getViewDistance();

        List<Chunk> chunksToUpdate = new ArrayList<>();

        for (int x = playerX - viewDistance; x <= playerX + viewDistance; x++) {
            for (int z = playerZ - viewDistance; z <= playerZ + viewDistance; z++) {
                if (world.isChunkLoaded(x, z)) {
                    chunksToUpdate.add(world.getChunkAt(x, z));
                }
            }
        }

        // Sort by distance from player (closest first)
        chunksToUpdate.sort(Comparator.comparingDouble(chunk -> {
            double dx = chunk.getX() - playerX;
            double dz = chunk.getZ() - playerZ;
            return dx * dx + dz * dz;
        }));

        List<MultiBlockPacket> packets = new ArrayList<>();
        for(Chunk chunk : chunksToUpdate) {
            packets.addAll(updateChunk(player, chunk));
        }

        sendSectionPackets(player, packets);
    }

    private void sendSectionPackets(Player player, List<MultiBlockPacket> packets) {
        BukkitRunnable previousTask = playerRenderingTasks.remove(player.getUniqueId());
        if (previousTask != null)
            previousTask.cancel();

        // Run over multiple ticks to prevent client-side stuttering
        BukkitRunnable task = new BukkitRunnable() {
            private final Iterator<MultiBlockPacket> iterator = packets.iterator();

            @Override
            public void run() {
                if (!player.isOnline() || !iterator.hasNext()) {
                    if (player.isOnline())
                        new FinishPlayerWorldRenderingReloadEvent(player).callEvent();

                    this.cancel();
                    playerRenderingTasks.remove(player.getUniqueId());
                    return;
                }

                for(int i = 0; i < 8 && iterator.hasNext(); i++) {
                    MultiBlockPacket packet = iterator.next();
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
                }
            }
        };

        playerRenderingTasks.put(player.getUniqueId(), task);
        task.runTaskTimer(Mirage.getInstance(), 0, 1);
    }

    /**
     * Updates the specified chunk for the player using chunk update packets.
     *
     * @param player the player for whom the chunk is updated
     * @param chunk the chunk to update
     */
    public List<MultiBlockPacket> updateChunk(Player player, Chunk chunk) {
        List<MultiBlockPacket> packets = new ArrayList<>();

        ChunkRenderSettings chunkRenderSettings = playerRenderSettings.get(player.getUniqueId());
        World renderWorld = chunkRenderSettings.getRenderWorldAs().get(chunk.getWorld());
        World previousWorld = Objects.requireNonNullElse(chunkRenderSettings.getClientsideChunks().get(chunk.getChunkKey()), chunk.getWorld());

        LevelChunk nmsRenderChunk = ((CraftWorld) renderWorld).getHandle().getChunkSource().getChunk(chunk.getX(), chunk.getZ(), false);
        LevelChunk nmsOriginalChunk = ((CraftWorld) previousWorld).getHandle().getChunkSource().getChunk(chunk.getX(), chunk.getZ(), false);

        if(nmsRenderChunk == null || nmsOriginalChunk == null)
            return packets;

        LevelChunkSection[] sections = nmsRenderChunk.getSections();
        LevelChunkSection[] originalSections = nmsOriginalChunk.getSections();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];

            if (section == null || isSameSection(section, originalSections[i])) {
                continue;
            }

            int sectionY = nmsRenderChunk.getSectionYFromSectionIndex(i);
            SectionPos sectionPos = SectionPos.of(chunk.getX(), sectionY, chunk.getZ());

            packets.add(new MultiBlockPacket(sectionPos, section));
        }

        chunkRenderSettings.getClientsideChunks().put(chunk.getChunkKey(), renderWorld);
        return packets;
    }

    boolean isSameSection(LevelChunkSection section, LevelChunkSection originalSection) {
        for(int i = 0; i < 4096; i++)
            if(!section.getStates().get(i).is(originalSection.getStates().get(i).getBlock()))
                return false;

        return true;
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
        ChunkRenderSettings chunkRenderSettings = playerRenderSettings.get(player.getUniqueId());

        if(chunkRenderSettings == null) {
            playerRenderSettings.put(player.getUniqueId(), new ChunkRenderSettings(world, visualizer));
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
        ChunkRenderSettings chunkRenderSettings = playerRenderSettings.get(player.getUniqueId());

        if(chunkRenderSettings == null) {
            playerRenderSettings.put(player.getUniqueId(), new ChunkRenderSettings(chunk, visualizer));
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
        playerRenderSettings.remove(player.getUniqueId());
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
        ChunkRenderSettings chunkRenderSettings = playerRenderSettings.get(player.getUniqueId());

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
        ChunkRenderSettings chunkRenderSettings = playerRenderSettings.get(player.getUniqueId());

        if(chunkRenderSettings == null) {
            return;
        }

        chunkRenderSettings.getRenderChunkAs().remove(chunk);
    }

    private Chunk getRenderChunk(Player player, int chunkX, int chunkZ) {
        ChunkRenderSettings renderSettings = playerRenderSettings.get(player.getUniqueId());
        Chunk chunk = player.getWorld().getChunkAt(chunkX, chunkZ);

        if(renderSettings == null)
            return chunk;

        World renderWorld = renderSettings.getRenderWorldAs().get(player.getWorld());
        Chunk renderChunk = renderWorld != null ? renderWorld.getChunkAt(chunkX, chunkZ) : renderSettings.getRenderChunkAs().get(chunk);

        return Objects.requireNonNullElse(renderChunk, chunk);
    }

    private ChunkPacket getChunkPacket(Player player, int chunkX, int chunkZ) {
        return new ChunkPacket(chunkX, chunkZ, getRenderChunk(player, chunkX, chunkZ));
    }
}
