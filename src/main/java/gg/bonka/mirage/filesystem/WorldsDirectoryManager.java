package gg.bonka.mirage.filesystem;

import gg.bonka.mirage.Mirage;
import gg.bonka.mirage.misc.ConsoleLogger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The WorldsDirectoryManager class is responsible for managing the worlds directory
 * and saving worlds to that directory.
 */
public class WorldsDirectoryManager {

    @Getter
    private static WorldsDirectoryManager instance;

    private final File worldsDirectory = new File(Bukkit.getWorldContainer(), "worlds");

    @Getter
    private final HashSet<String> worldNames = new HashSet<>();

    /**
     * The WorldsDirectoryManager class is responsible for managing the worlds directory
     * and saving worlds to that directory.
     */
    public WorldsDirectoryManager() throws IOException {
        if(instance != null) {
            throw new IllegalStateException("Singleton WorldsDirectoryManager has already been initialized");
        }

        instance = this;

        Properties serverProperties = new Properties();
        serverProperties.load(Files.newInputStream(new File(Bukkit.getWorldContainer(), "server.properties").toPath()));

        String worldName = serverProperties.getProperty("level-name");
        boolean netherEnabled = Boolean.getBoolean(serverProperties.getProperty("allow-nether"));

        //Save the existing worlds when this plugin is enabled for the first time!
        if(worldsDirectory.mkdirs()) {
            initializeMirage(worldName, netherEnabled);
            return;
        }

        File[] worlds = worldsDirectory.listFiles((dir, name) -> dir.isDirectory());

        assert worlds != null;
        worldNames.addAll(Arrays.stream(worlds).map(File::getName).collect(Collectors.toList()));

        //TODO: Add the option to only load a world when requested in a config file!
        loadWorlds(worldNames.toArray(new String[0]));
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
     * Asynchronously saves the specified world to the worlds directory.
     *
     * @param worldName The name of the world to save.
     * @param callback  The callback to be called after saving the world. It receives a boolean indicating the success status and a message providing more information.
     */
    public void saveWorldAsync(String worldName, AsyncWorldDirectoryCallback callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    saveWorld(worldName);

                    callback.callback(true, "");
                    this.cancel();
                } catch (IOException e) {
                    callback.callback(false, String.format("Something went wrong while trying to save %s, the error stack trace has been printed to the console.", worldName));
                    this.cancel();

                    throw new RuntimeException(e);
                }
            }
        }.runTaskAsynchronously(Mirage.getInstance());
    }

    /**
     * Saves the specified world to the worlds directory.<br>
     * <b>This operation is very CPU heavy, only use on startup!</b>
     * @see WorldsDirectoryManager#saveWorldAsync(String, AsyncWorldDirectoryCallback)
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
     * Asynchronously removes the specified world from the worlds directory.
     * This operation may not be executed in the main thread.
     *
     * @param worldName The name of the world to be removed.
     * @param callback  The callback to be called after removing the world. It receives a boolean indicating the success status and a message providing more information.
     */
    public void removeWorldAsync(String worldName, AsyncWorldDirectoryCallback callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    removeWorld(worldName);

                    callback.callback(true, "");
                    this.cancel();
                } catch (IOException e) {
                    callback.callback(false, String.format("Something went wrong while trying to remove %s, the error stack trace has been printed to the console.", worldName));
                    this.cancel();

                    throw new RuntimeException(e);
                }
            }
        }.runTaskAsynchronously(Mirage.getInstance());
    }

    /**
     * Removes the specified world from the worlds directory. This method deletes the world directory and the save directory associated with the specified world.
     * <br><br>
     * <b>This operation is very CPU heavy, only use on startup!</b>
     * @see WorldsDirectoryManager#removeWorldAsync(String, AsyncWorldDirectoryCallback)
     *
     * @param worldName The name of the world to remove.
     * @throws IOException If an error occurs while removing the world.
     */
    public void removeWorld(String worldName) throws IOException {
        File worldDirectory = new File(Bukkit.getWorldContainer(), worldName);
        File saveDirectory = new File(worldsDirectory, worldName);

        if(worldDirectory.exists()) {
            FileUtils.deleteDirectory(worldDirectory);
        }

        if(saveDirectory.exists()) {
            FileUtils.deleteDirectory(saveDirectory);
        }

        worldNames.remove(worldName);
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
     * Loads the specified world asynchronously.
     *
     * @param worldName The name of the world to load.
     * @param callback The callback to be called after loading the world. It receives a boolean indicating the success status and a message providing more information.
     */
    public void loadWorldAsync(String worldName, AsyncWorldDirectoryCallback callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    loadWorld(worldName);

                    callback.callback(true, "");
                    this.cancel();
                } catch (IOException e) {
                    callback.callback(false, String.format("Something went wrong while trying to load %s, the error stack trace has been printed to the console.", worldName));
                    this.cancel();

                    throw new RuntimeException(e);
                }
            }
        }.runTaskAsynchronously(Mirage.getInstance());
    }

    /**
     * Loads the specified world into the game.
     * <br><br>
     * <b>This operation is very CPU heavy, only use on startup!</b>
     * @see WorldsDirectoryManager#loadWorldAsync(String, AsyncWorldDirectoryCallback)
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
     * Copies a world from a source directory to a target directory asynchronously.
     *
     * @param sourceWorldName The name of the source world to be copied.
     * @param targetWorldName The name of the target world where the copy will be saved.
     * @param callback        The callback to be called after copying the world. It receives a boolean indicating the success status and a message providing more information.
     */
    public void copyWorldAsync(String sourceWorldName, String targetWorldName, AsyncWorldDirectoryCallback callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Path sourceWorldDirectory = new File(worldsDirectory, sourceWorldName).toPath();
                File targetDirectory = new File(worldsDirectory, targetWorldName);

                //noinspection ResultOfMethodCallIgnored
                targetDirectory.mkdirs();

                try {
                    copyDirectory(sourceWorldDirectory, targetDirectory.toPath(), targetWorldName, path -> !path.getFileName().toString().contains("uid.dat"));
                    worldNames.add(targetWorldName);

                    callback.callback(true, "");
                    this.cancel();
                } catch (IOException e) {
                    callback.callback(false, String.format("Something went wrong while copying world %s to %s, the error stack trace has been printed to the console.", sourceWorldName, targetWorldName));
                    this.cancel();

                    throw new RuntimeException(e);
                }
            }
        }.runTaskAsynchronously(Mirage.getInstance());
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

    /**
     * Initializes Mirage by creating and saving the names of the worlds to be managed.
     *
     * @param worldName        The name of the main world to be managed.
     * @param isNetherEnabled  A boolean indicating whether the Nether world should be enabled.
     * @throws IOException     If an error occurs while saving the worlds.
     */
    private void initializeMirage(String worldName, boolean isNetherEnabled) throws IOException {
        String[] worldNames = new String[isNetherEnabled ? 3 : 2];
        worldNames[0] = worldName;
        worldNames[1] = String.format("%s_the_end", worldName);

        if(isNetherEnabled) {
            worldNames[2] = String.format("%s_nether", worldName);
        }

        this.worldNames.addAll(List.of(worldNames));

        saveWorlds(worldNames);
    }
}
