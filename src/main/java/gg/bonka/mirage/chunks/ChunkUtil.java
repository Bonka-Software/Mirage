package gg.bonka.mirage.chunks;

public final class ChunkUtil {

    public static long getChunkKey(int x, int z) {
        return ((long)x >> 4) & 4294967295L | (((long)z >> 4) & 4294967295L) << 32;
    }
}
