package gg.bonka.mirage;

import co.aikar.commands.PaperCommandManager;
import gg.bonka.mirage.filesystem.WorldsDirectoryManager;
import gg.bonka.mirage.misc.ConsoleLogger;
import gg.bonka.mirage.multiverse.commands.WorldCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class Mirage extends JavaPlugin {

    private final static String version = "0.0.1";

    @Override
    public void onEnable() {
        ConsoleLogger.info(String.format("Mirage [%s] has been enabled!", version));

        try {
            new WorldsDirectoryManager();
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while trying to set up the mirage file directory system", e);
        }

        //Register commands
        PaperCommandManager commandManager = new PaperCommandManager(this);

        commandManager.registerCommand(new WorldCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
