package gg.bonka.mirage;

import co.aikar.commands.PaperCommandManager;
import gg.bonka.mirage.filesystem.WorldsDirectoryManager;
import gg.bonka.mirage.misc.ConsoleLogger;
import gg.bonka.mirage.multiverse.commands.WorldCommand;
import gg.bonka.mirage.world.MirageWorld;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class Mirage extends JavaPlugin {

    private final static String version = "0.0.1";

    @Getter
    private static Mirage instance;

    @Getter
    private PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        if(instance != null) {
            throw new IllegalStateException("Mirage instance already exists!");
        }

        instance = this;
        ConsoleLogger.info(String.format("Mirage [%s] has been enabled!", version));

        try {
            new WorldsDirectoryManager();
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while trying to set up the mirage file directory system", e);
        }

        commandManager = new PaperCommandManager(this);

        //Register commands
        commandManager.registerCommand(new WorldCommand());
    }

    @Override
    public void onDisable() {
        //Speeds up the server shutdown a lot!
        //Bukkit will automatically save the worlds by default, but we obviously don't want that!
        for(MirageWorld world : WorldsDirectoryManager.getInstance().getWorlds()) {
            WorldsDirectoryManager.getInstance().unloadWorld(world.getWorldName());
        }
    }
}
