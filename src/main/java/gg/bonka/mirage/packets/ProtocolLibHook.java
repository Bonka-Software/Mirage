package gg.bonka.mirage.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedLevelChunkData;
import com.destroystokyo.paper.antixray.ChunkPacketInfo;
import gg.bonka.mirage.Mirage;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;

public class ProtocolLibHook {

    public ProtocolLibHook() {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        manager.addPacketListener(new PacketAdapter(Mirage.getInstance(), PacketType.Play.Server.MAP_CHUNK) {
            @Override
            public void onPacketSending(PacketEvent event) {
                int chunkX = event.getPacket().getIntegers().read(0);
                int chunkZ = event.getPacket().getIntegers().read(1);

                PacketContainer packet = new PacketContainer(PacketType.Play.Server.MAP_CHUNK);

                //Chunk X Z
                packet.getIntegers().write(0, chunkX);
                packet.getIntegers().write(1, chunkZ);

                //TODO: Currently renders the start chunk (0, 0) as a test, but there should be some sort of "render as" function for both worlds and specific chunks
                LevelChunk levelChunk = ((CraftWorld) event.getPlayer().getWorld()).getHandle().getChunk(0, 0);

                ClientboundLevelChunkWithLightPacket chunkWithLightPacket = new ClientboundLevelChunkWithLightPacket(levelChunk, levelChunk.getLevel().getLightEngine(), null, null, false);

                ChunkPacketInfo<BlockState> chunkPacketInfo = new ChunkPacketInfo<>(chunkWithLightPacket, levelChunk);
                ClientboundLevelChunkPacketData chunkPacket = new ClientboundLevelChunkPacketData(levelChunk, chunkPacketInfo);

                WrappedLevelChunkData.ChunkData chunkData = new WrappedLevelChunkData.ChunkData(chunkPacket);

                packet.getLevelChunkData().write(0, chunkData);
                packet.getLightUpdateData().write(0, new WrappedLevelChunkData.LightData(chunkWithLightPacket.getLightData()));

                event.setPacket(packet);
            }
        });
    }
}
