package gg.bonka.mirage.chunks.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedLevelChunkData;
import gg.bonka.mirage.chunks.ChunkUtil;
import io.papermc.paper.antixray.ChunkPacketInfo;
import lombok.Getter;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;

@Getter
public class ChunkPacket extends PacketContainer {

    private final long chunkKey;
    private final World world;

    private final boolean isValid;

    /**
     * Represents a packet for sending chunk data to clients in a Minecraft world. The packet
     * includes chunk data and lighting information necessary for rendering the specified chunk
     * at given coordinates.
     *
     * @param x the x-coordinate of the chunk to be sent
     * @param z the z-coordinate of the chunk to be sent
     * @param world the world containing the chunk to construct the packet from
     */
    public ChunkPacket(int x, int z, World world) {
        super(PacketType.Play.Server.MAP_CHUNK);
        this.world = world;
        this.chunkKey = ChunkUtil.getChunkKey(x, z);

        ServerChunkCache chunkCache = ((CraftWorld) world).getHandle().getChunkSource();
        LevelChunk levelChunk = chunkCache.getChunk(x, z, !chunkCache.hasChunk(x, z));

        if(levelChunk == null) {
            this.isValid = false;
            return;
        }

        ClientboundLevelChunkWithLightPacket chunkWithLightPacket = new ClientboundLevelChunkWithLightPacket(levelChunk, levelChunk.getLevel().getLightEngine(), null, null, false);

        ChunkPacketInfo<BlockState> chunkPacketInfo = new ChunkPacketInfo<>(chunkWithLightPacket, levelChunk);
        ClientboundLevelChunkPacketData chunkPacket = new ClientboundLevelChunkPacketData(levelChunk, chunkPacketInfo);

        WrappedLevelChunkData.ChunkData chunkData = new WrappedLevelChunkData.ChunkData(chunkPacket);

        getIntegers().write(0, x);
        getIntegers().write(1, z);

        getLevelChunkData().write(0, chunkData);
        getLightUpdateData().write(0, new WrappedLevelChunkData.LightData(chunkWithLightPacket.getLightData()));

        this.isValid = true;
    }
}
