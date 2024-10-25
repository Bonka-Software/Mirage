package gg.bonka.mirage;

import co.aikar.commands.PaperCommandManager;
import gg.bonka.mirage.filesystem.WorldsDirectoryManager;
import gg.bonka.mirage.misc.ConsoleLogger;
import gg.bonka.mirage.commands.WorldCommand;
import gg.bonka.mirage.chunks.ChunkRenderingSystem;
import gg.bonka.mirage.world.MirageWorld;
import gg.bonka.mirage.world.rollback.WorldsTracker;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

@Getter
public final class Mirage extends JavaPlugin {

    private final static String version = "1.1.3";

    private PaperCommandManager commandManager;

    private WorldsDirectoryManager worldsDirectoryManager;
    private WorldsTracker worldsTracker;
    private ChunkRenderingSystem chunkRenderingSystem;

    @Override
    public void onEnable() {
        ConsoleLogger.info(String.format("Mirage [%s] has been enabled!", version));

        try {
            worldsDirectoryManager = new WorldsDirectoryManager();
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while trying to set up the mirage file directory system", e);
        }

        worldsTracker = new WorldsTracker();
        chunkRenderingSystem = new ChunkRenderingSystem();

        commandManager = new PaperCommandManager(this);

        //Register commands
        commandManager.registerCommand(new WorldCommand(worldsDirectoryManager, worldsTracker, chunkRenderingSystem));

        //Register events
        Bukkit.getPluginManager().registerEvents(new WorldsTracker(), this);
    }

    @Override
    public void onDisable() {
        //Speeds up the server shutdown a lot!
        //Bukkit will automatically save the worlds by default, but we obviously don't want that!
        for(MirageWorld world : worldsDirectoryManager.getWorlds()) {
            try {
                worldsDirectoryManager.unloadWorld(world.getWorldName());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Mirage getInstance() {
        return (Mirage) Bukkit.getPluginManager().getPlugin("Mirage");
    }
}
