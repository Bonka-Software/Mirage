package gg.bonka.mirage.chunks.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import it.unimi.dsi.fastutil.shorts.ShortArraySet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class MultiBlockPacket extends PacketContainer {

    private static final ShortSet ALL_POSITIONS = new ShortArraySet(4096);

    static {
        for (short i = 0; i < 4096; i++) {
            ALL_POSITIONS.add(i);
        }
    }

    public MultiBlockPacket(SectionPos sectionPos, LevelChunkSection section) {
        super(PacketType.Play.Server.MULTI_BLOCK_CHANGE, new ClientboundSectionBlocksUpdatePacket(sectionPos, ALL_POSITIONS, section));
    }
}
