package gg.bonka.mirage.chunks;

import lombok.Getter;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.HashMap;

@Getter
class ChunkRenderSettings {

    private final HashMap<World, World> renderWorldAs = new HashMap<>();
    private final HashMap<Chunk, Chunk> renderChunkAs = new HashMap<>();

    public ChunkRenderSettings(World world, World visualizer) {
        renderWorldAs.put(world, visualizer);
    }

    public ChunkRenderSettings(Chunk chunk, Chunk visualizer) {
        renderChunkAs.put(chunk, visualizer);
    }
}
