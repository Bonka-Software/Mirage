package gg.bonka.mirage.chunks.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedLevelChunkData;
import io.papermc.paper.antixray.ChunkPacketInfo;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.CraftWorld;

public class ChunkPacket extends PacketContainer {

    /**
     * Constructs a ChunkPacket object with the specified coordinates and chunk data.
     *
     * @param x The X coordinate of the chunk.
     * @param z The Z coordinate of the chunk.
     * @param chunk The Chunk object containing the chunk data.
     */
    public ChunkPacket(int x, int z, Chunk chunk) {
        super(PacketType.Play.Server.MAP_CHUNK);

        LevelChunk levelChunk = ((CraftWorld) chunk.getWorld()).getHandle().getChunk(chunk.getX(), chunk.getZ());
        ClientboundLevelChunkWithLightPacket chunkWithLightPacket = new ClientboundLevelChunkWithLightPacket(levelChunk, levelChunk.getLevel().getLightEngine(), null, null, false);

        ChunkPacketInfo<BlockState> chunkPacketInfo = new ChunkPacketInfo<>(chunkWithLightPacket, levelChunk);
        ClientboundLevelChunkPacketData chunkPacket = new ClientboundLevelChunkPacketData(levelChunk, chunkPacketInfo);

        WrappedLevelChunkData.ChunkData chunkData = new WrappedLevelChunkData.ChunkData(chunkPacket);

        getIntegers().write(0, x);
        getIntegers().write(1, z);

        getLevelChunkData().write(0, chunkData);
        getLightUpdateData().write(0, new WrappedLevelChunkData.LightData(chunkWithLightPacket.getLightData()));
    }
}
