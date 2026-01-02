package gg.bonka.mirage.chunks;

import lombok.Getter;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Objects;

@Getter
class ChunkRenderSettings {

    private final HashMap<World, World> renderWorldAs = new HashMap<>();
    private final HashMap<Long, World> renderChunkAs = new HashMap<>();

    private final HashMap<Long, World> clientsideChunks = new HashMap<>();

    public ChunkRenderSettings(World world, World visualizer) {
        renderWorldAs.put(world, visualizer);
    }

    public ChunkRenderSettings(Chunk chunk, Chunk visualizer) {
        long chunkKey = chunk.getChunkKey();
        renderChunkAs.put(chunkKey, visualizer.getWorld());
    }

    public World getRenderChunk(World world, int x, int z) {
        long chunkKey = ChunkUtil.getChunkKey(x, z);

        if(renderChunkAs.containsKey(chunkKey))
            return renderChunkAs.get(chunkKey);

        return Objects.requireNonNullElse(renderWorldAs.get(world), world);
    }
}
