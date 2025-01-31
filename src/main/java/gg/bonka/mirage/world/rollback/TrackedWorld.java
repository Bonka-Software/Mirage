package gg.bonka.mirage.world.rollback;

import gg.bonka.mirage.world.MirageWorld;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import org.bukkit.*;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class TrackedWorld {

    private final MirageWorld mirageWorld;
    private final ConcurrentHashMap<Long, ChunkSnapshot> chunks = new ConcurrentHashMap<>();

    private final HashSet<LightWeightLocation> changedBlocks = new HashSet<>();
    private final ArrayList<CompoundTag> entities = new ArrayList<>();

    @Setter
    private long time;

    public TrackedWorld(MirageWorld mirageWorld) {
        this.mirageWorld = mirageWorld;
    }

    public void addBlockUpdate(Location location) {
        LightWeightLocation lightWeightLocation = new LightWeightLocation(location);
        changedBlocks.add(lightWeightLocation);
    }

    public void putChunkData(Chunk chunk) {
        if(chunks.containsKey(chunk.getChunkKey()))
            return;

        chunks.put(chunk.getChunkKey(), chunk.getChunkSnapshot());

        for(Entity entity : chunk.getEntities())
            entities.add(getSerializedEntity(entity));
    }

    public void updateSave() {
        World world = Bukkit.getWorld(mirageWorld.getWorldName());

        if(world == null)
            return;

        changedBlocks.clear();
        chunks.clear();
        entities.clear();
        this.time = world.getTime();

        for(Chunk chunk : world.getLoadedChunks()) {
            putChunkData(chunk);
        }
    }

    public void reset() {
        World world = Bukkit.getWorld(mirageWorld.getWorldName());

        if(world == null)
            return;

        LightWeightLocation[] changed = changedBlocks.toArray(LightWeightLocation[]::new);

        for(LightWeightLocation lightWeightLocation : changed) {
            Location worldLocation = lightWeightLocation.toLocation(world);

            int x = lightWeightLocation.x - worldLocation.getChunk().getX() * 16;
            int z = lightWeightLocation.z - worldLocation.getChunk().getZ() * 16;

            worldLocation.getBlock().setBlockData(chunks.get(worldLocation.getChunk().getChunkKey()).getBlockData(x, lightWeightLocation.y, z));
        }

        changedBlocks.clear();

        //Then we reset all entities besides players
        for(Entity entity : world.getEntities()) {
            if(!(entity instanceof Player))
                entity.remove();
        }

        for(CompoundTag entityNBT : entities) {
            spawnEntity(world, entityNBT);
        }

        //Then we do some finishing touches by setting the time!
        world.setTime(time);
    }

    /**
     * Spawns an entity in the given world using the provided entity data.
     * I modified the code linked in <b>also see</b>
     *
     * @see <a href="https://forums.papermc.io/threads/save-an-entity-as-a-string-and-create-a-new-one-from-it.926/">All the credits go to this paper forum post!</a>
     *
     * @param world the world in which to spawn the entity
     * @param data the entity data in the form of a CompoundTag
     */
    private void spawnEntity(World world, CompoundTag data) {
        ServerLevel worldServer = ((CraftWorld) world).getHandle();
        net.minecraft.world.entity.Entity spawnedEntity = EntityType.loadEntityRecursive(data, worldServer, EntitySpawnReason.LOAD, entity -> entity);

        if (spawnedEntity != null)
            worldServer.addFreshEntityWithPassengers(spawnedEntity);
    }

    /**
     * Retrieves the save data (CompoundTag) for the specified entity.
     * I modified the code linked in <b>also see</b>
     *
     * @see <a href="https://forums.papermc.io/threads/save-an-entity-as-a-string-and-create-a-new-one-from-it.926/">All the credits go to this paper forum post!</a>
     *
     * @param entity the entity for which to retrieve the save data
     * @return the save data for the entity
     */
    private CompoundTag getSerializedEntity(Entity entity) {
        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) entity).getHandle();
        CompoundTag compound = new CompoundTag();
        nmsEntity.save(compound);

        return compound;
    }

    @Getter
    @AllArgsConstructor
    private static final class LightWeightLocation {
        private final int x;
        private final int y;
        private final int z;

        public LightWeightLocation(Location location) {
            this.x = location.getBlockX();
            this.y = location.getBlockY();
            this.z = location.getBlockZ();
        }

        public Location toLocation(World world) {
            return new Location(world, x, y, z);
        }
    }
}
