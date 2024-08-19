package gg.bonka.mirage.filesystem.eventhandlers;

import gg.bonka.mirage.Mirage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

import java.io.IOException;

public class WorldInitHandler implements Listener {

    private final String worldName;
    private final WorldInitCallback callback;

    public WorldInitHandler(String worldName, WorldInitCallback callback) {
        this.worldName = worldName;
        this.callback = callback;

        Bukkit.getPluginManager().registerEvents(this, Mirage.getInstance());
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) throws IOException {
        if(!event.getWorld().getName().equals(worldName))
            return;

        //The world init event is useless for us if we don't save the world to disk first.
        //The world is only in memory when the WorldInitEvent get called.
        event.getWorld().save();

        callback.callback();
        HandlerList.unregisterAll(this);
    }
}
