package gg.bonka.mirage.filesystem;

import gg.bonka.mirage.misc.ConsoleLogger;
import org.bukkit.Bukkit;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The WorldsDirectoryManager class is responsible for managing the worlds directory
 * and saving worlds to that directory.
 */
public class WorldsDirectoryManager {

    private final File worldsDirectory = new File(Bukkit.getWorldContainer(), "worlds");

    /**
     * The WorldsDirectoryManager class is responsible for managing the worlds directory
     * and saving worlds to that directory.
     */
    public WorldsDirectoryManager() throws IOException {
        Properties serverProperties = new Properties();
        serverProperties.load(Files.newInputStream(new File(Bukkit.getWorldContainer(), "server.properties").toPath()));

        String worldName = serverProperties.getProperty("level-name");
        boolean netherEnabled = Boolean.getBoolean(serverProperties.getProperty("allow-nether"));

        String[] worldNames = new String[netherEnabled ? 3 : 2];
        worldNames[0] = worldName;
        worldNames[1] = String.format("%s_the_end", worldName);

        if(netherEnabled) {
            worldNames[2] = String.format("%s_nether", worldName);
        }

        if(worldsDirectory.mkdirs()) {
            //Save the existing worlds when this plugin is enabled for the first time!
            saveWorlds(worldNames);
            return;
        }

        loadWorlds(worldNames);
    }

    /**
     * Saves the specified worlds to the worlds directory.
     *
     * @param worldNames The names of the worlds to save.
     * @throws IOException If an error occurs while saving the worlds.
     */
    public void saveWorlds(String... worldNames) throws IOException {
        for(String worldName : worldNames) {
            saveWorld(worldName);
        }
    }

    /**
     * Saves the specified world to the worlds directory.
     *
     * @param worldName the name of the world
     * @throws IOException if there is an error saving the world
     */
    public void saveWorld(String worldName) throws IOException {
        Path worldDirectory = new File(Bukkit.getWorldContainer(), worldName).toPath();
        File saveDirectory = new File(worldsDirectory, worldName);

        //noinspection ResultOfMethodCallIgnored
        saveDirectory.mkdirs();

        copyDirectory(worldDirectory, saveDirectory.toPath(), worldName, path -> !path.getFileName().toString().endsWith(".lock"));
    }

    /**
     * Loads the specified worlds into the game.
     *
     * @param worldNames The names of the worlds to load.
     * @throws IOException If an error occurs while loading the worlds.
     */
    public void loadWorlds(String... worldNames) throws IOException {
        for(String worldName : worldNames) {
            loadWorld(worldName);
        }
    }

    /**
     * Loads the specified world into the game.
     *
     * @param worldName The name of the world to load.
     * @throws IOException If an error occurs while loading the world.
     */
    public void loadWorld(String worldName) throws IOException {
        File worldDirectory = new File(Bukkit.getWorldContainer(), worldName);
        File saveDirectory = new File(worldsDirectory, worldName);

        if(!saveDirectory.exists()) {
            ConsoleLogger.error(String.format("The world %s could not be found!", worldName));
            return;
        }

        FileUtils.deleteDirectory(worldDirectory);
        copyDirectory(saveDirectory.toPath(), worldDirectory.toPath(), worldName, path -> true);
    }

    /**
     * Copies a directory from a source path to a save directory, filtering the files with a given predicate.
     *
     * @param source        the source path of the directory to copy
     * @param saveDirectory the save directory where the copy of the directory will be saved
     * @param worldName     the name of the world being saved
     * @param predicate     a predicate used to filter the files to be copied
     * @throws IOException if an I/O error occurs during the copy process
     */
    private void copyDirectory(Path source, Path saveDirectory, String worldName, Predicate<Path> predicate) throws IOException {
        try(Stream<Path> stream = Files.walk(source)) {
            stream.filter(predicate).forEach(path -> {
                try {
                    Path targetPath = saveDirectory.resolve(source.relativize(path));
                    Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(String.format("Error occurred while saving world: %s to the worlds directory.", worldName), e);
                }
            });
        }
    }
}
