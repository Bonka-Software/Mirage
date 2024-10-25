package gg.bonka.mirage.world.rollback;

import gg.bonka.mirage.filesystem.WorldsDirectoryManager;
import gg.bonka.mirage.world.MirageWorld;
import lombok.Getter;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldInitEvent;

import java.util.HashMap;
import java.util.HashSet;

public class WorldsTracker implements Listener {

    @Getter
    private static WorldsTracker instance;

    private final HashMap<String, TrackedWorld> trackedWorlds = new HashMap<>();

    public WorldsTracker() {
        if(instance != null)
            throw new IllegalStateException("Singleton WorldsTracker has already been initialized");

        instance = this;
    }

    public TrackedWorld getTrackedWorld(String worldName) {
        return trackedWorlds.get(worldName);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        World world = event.getWorld();

        if(trackedWorlds.containsKey(world.getName()))
            trackedWorlds.get(world.getName()).putChunkData(event.getChunk());
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        checkForMirageWorldsUpdate();

        World world = event.getWorld();

        if(trackedWorlds.containsKey(world.getName()))
            trackedWorlds.get(world.getName()).setTime(world.getTime());
    }

    @EventHandler
    public void onBlockUpdate(BlockPhysicsEvent event) {
        registerBlockUpdate(event.getBlock());
    }

    private void registerBlockUpdate(Block block) {
        TrackedWorld trackedWorld = trackedWorlds.get(block.getWorld().getName());

        if(trackedWorld != null)
            trackedWorld.addBlockUpdate(block.getLocation());
    }

    /**
     * Checks for updates in the MirageWorlds directory and updates the tracked worlds accordingly.
     */
    public void checkForMirageWorldsUpdate() {
        HashSet<MirageWorld> mirageWorlds = WorldsDirectoryManager.getInstance().getWorlds();

        if(trackedWorlds.size() == mirageWorlds.size())
            return;

        //We first check for worlds that we may not have to track anymore!
        HashSet<String> toRemove = new HashSet<>();

        for(String worldName : trackedWorlds.keySet()) {
            if(mirageWorlds.stream().noneMatch(mirageWorld -> mirageWorld.getWorldName().equals(worldName))) {
                toRemove.add(worldName);
            }
        }

        toRemove.forEach(trackedWorlds::remove);

        //Then we check for worlds that aren't getting tracked yet.
        //This happens when a world is created during runtime.
        for(MirageWorld world : mirageWorlds) {
            if (!trackedWorlds.containsKey(world.getWorldName())) {
                trackedWorlds.put(world.getWorldName(), new TrackedWorld(world));
            }
        }
    }
}
