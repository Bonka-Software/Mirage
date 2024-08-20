package gg.bonka.mirage.filesystem;

import gg.bonka.mirage.Mirage;
import gg.bonka.mirage.filesystem.eventhandlers.WorldInitHandler;
import gg.bonka.mirage.misc.Chat;
import gg.bonka.mirage.misc.ChatColor;
import gg.bonka.mirage.misc.ConsoleLogger;
import gg.bonka.mirage.world.MirageWorld;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The WorldsDirectoryManager class is responsible for managing the worlds directory
 * and saving worlds to that directory.
 */
@Getter
public class WorldsDirectoryManager {

    private static final File serverPath = Bukkit.getPluginsFolder().getParentFile();

    private static final String relativeSaveDirectory = "mirage/saved-worlds";

    private static final File activeDirectory = Bukkit.getWorldContainer();
    private static final File saveDirectory = new File(serverPath, relativeSaveDirectory);

    @Getter
    private static WorldsDirectoryManager instance;

    private final HashSet<MirageWorld> worlds = new HashSet<>();

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
        boolean netherEnabled = Boolean.parseBoolean(serverProperties.getProperty("allow-nether"));

        //Save the existing worlds when this plugin is enabled for the first time!
        if(saveDirectory.mkdirs()) {
            initializeMirage(worldName, netherEnabled);
            return;
        }

        File[] worldDirectories = saveDirectory.listFiles((dir, name) -> dir.isDirectory());

        assert worldDirectories != null;
        worlds.addAll(Arrays.stream(worldDirectories).map(file -> new MirageWorld(file.getName(), file)).collect(Collectors.toList()));

        for(MirageWorld world : worlds) {
            if(world.getLoadOnStart()) {
                loadWorld(world);
            }
        }
    }

    public static MirageWorld getMirageWorld(String name) {
        return new MirageWorld(name, new File(saveDirectory, name));
    }

    /**
     * Saves the specified world asynchronously to the worlds directory.
     *
     * @param world    The MirageWorld object representing the world to be saved.
     * @param callback The callback to be called after saving the world.
     */
    public void saveWorldAsync(MirageWorld world, AsyncWorldDirectoryCallback callback) {
        saveWorldAsync(new File(activeDirectory, world.getWorldName()), world, callback);
    }

    /**
     * Saves the specified world asynchronously to the worlds directory.
     *
     * @param source   The source directory where the world will be saved.
     * @param world    The MirageWorld object representing the world to be saved.
     * @param callback The callback to be called after saving the world. It receives a boolean indicating the success status and a message providing more information.
     */
    private void saveWorldAsync(File source, MirageWorld world, AsyncWorldDirectoryCallback callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    saveWorld(source, world);

                    callback.callback(true, "");
                    this.cancel();
                } catch (IOException e) {
                    callback.callback(false, String.format("Something went wrong while trying to save %s, the error stack trace has been printed to the console.", world.getWorldName()));
                    this.cancel();

                    throw new RuntimeException(e);
                }
            }
        }.runTaskAsynchronously(Mirage.getInstance());
    }

    /**
     * Saves the specified world to the given source directory.
     *
     * @param source The source directory where the world will be saved.
     * @param world The MirageWorld object representing the world to be saved.
     * @throws IOException if an I/O error occurs while saving the world
     */
    public void saveWorld(File source, MirageWorld world) throws IOException {
        Path worldDirectory = source.toPath();

        //noinspection ResultOfMethodCallIgnored
        world.getSaveDirectory().mkdirs();

        copyDirectory(worldDirectory, world.getSaveDirectory().toPath(), world.getWorldName(), path -> !path.getFileName().toString().endsWith(".lock"));
        world.save();

        worlds.add(world);
    }

    /**
     * Asynchronously removes the specified world from the worlds directory.
     * This operation may not be executed in the main thread.
     *
     * @param world The name of the world to be removed.
     * @param callback  The callback to be called after removing the world. It receives a boolean indicating the success status and a message providing more information.
     */
    public void removeWorldAsync(MirageWorld world, AsyncWorldDirectoryCallback callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    removeWorld(world);

                    callback.callback(true, "");
                    this.cancel();
                } catch (IOException e) {
                    callback.callback(false, String.format("Something went wrong while trying to remove %s, the error stack trace has been printed to the console.", world.getWorldName()));
                    this.cancel();

                    throw new RuntimeException(e);
                }
            }
        }.runTaskAsynchronously(Mirage.getInstance());
    }

    /**
     * Removes the specified MirageWorld from the worlds directory.
     * If the world directory exists, it will be deleted along with its save directory.
     * <br><br>
     * <b>This operation is very CPU heavy, only use on startup!</b>
     * @see WorldsDirectoryManager#removeWorldAsync(MirageWorld, AsyncWorldDirectoryCallback)
     *
     * @param world The MirageWorld object representing the world to be removed.
     * @throws IOException If an I/O error occurs while removing the world.
     */
    public void removeWorld(MirageWorld world) throws IOException {
        File worldDirectory = new File(activeDirectory, world.getWorldName());

        if(worldDirectory.exists()) {
            FileUtils.deleteDirectory(worldDirectory);
        }

        if(world.getSaveDirectory().exists()) {
            FileUtils.deleteDirectory(world.getSaveDirectory());
        }

        worlds.remove(world);
    }

    /**
     * Asynchronously loads the specified world into the game.
     *
     * @param world    The MirageWorld object representing the world to load.
     * @param callback The callback to be called after loading the world. It receives a boolean indicating the success status and a message providing more information.
     * @throws RuntimeException if an IOException occurs while trying to load the world
     */
    public void loadWorldAsync(MirageWorld world, AsyncWorldDirectoryCallback callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    loadWorld(world);

                    callback.callback(true, "");
                    this.cancel();
                } catch (IOException e) {
                    callback.callback(false, String.format("Something went wrong while trying to load %s, the error stack trace has been printed to the console.", world.getWorldName()));
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
     * @see WorldsDirectoryManager#loadWorldAsync(MirageWorld, AsyncWorldDirectoryCallback)
     *
     * @param world The name of the world to load.
     * @throws IOException If an error occurs while loading the world.
     */
    public void loadWorld(MirageWorld world) throws IOException {
        File worldDirectory = new File(activeDirectory, world.getWorldName());
        File saveDirectory = world.getSaveDirectory();

        if(!saveDirectory.exists()) {
            ConsoleLogger.error(String.format("The world %s could not be found!", world));
            return;
        }

        FileUtils.deleteDirectory(worldDirectory);
        copyDirectory(saveDirectory.toPath(), worldDirectory.toPath(), world.getWorldName(), path -> true);
    }

    /**
     * Unloads the specified world from the game and deletes its directory.
     *
     * @param worldName the name of the world to unload
     */
    public void unloadWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if(world != null) {
            for(Player player : world.getPlayers()) {
                player.kick(Chat.format("World closing", ChatColor.ERROR));
            }
        }

        Bukkit.unloadWorld(worldName, false);

        //The main world can't be deleted like this
        if(Bukkit.getWorlds().get(0).getName().equals(worldName)) {
           return;
        }

        File worldDirectory = new File(activeDirectory, worldName);
        try {
            FileUtils.deleteDirectory(worldDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Asynchronously copies a MirageWorld from a source world to a target world directory.
     * <br><br>
     * The operation is performed asynchronously using a separate thread.
     * After the copy operation is completed, the provided callback is called to inform the caller about the success or failure of the operation.
     *
     * @param sourceWorld         The source MirageWorld object representing the world to be copied.
     * @param targetWorldName     The name of the target world directory where the world will be copied.
     * @param callback            The callback to be called after the copy operation. It receives a boolean indicating the success status and a message providing more information
     * .
     */
    public void copyWorldAsync(MirageWorld sourceWorld, String targetWorldName, AsyncWorldDirectoryCallback callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                File targetDirectory = new File(saveDirectory, targetWorldName);

                //noinspection ResultOfMethodCallIgnored
                targetDirectory.mkdirs();

                try {
                    copyDirectory(sourceWorld.getSaveDirectory().toPath(), targetDirectory.toPath(), targetWorldName, path -> !path.getFileName().toString().contains("uid.dat"));
                    worlds.add(new MirageWorld(targetWorldName, targetDirectory));

                    callback.callback(true, "");
                    this.cancel();
                } catch (IOException e) {
                    callback.callback(false, String.format("Something went wrong while copying world %s to %s, the error stack trace has been printed to the console.", sourceWorld, targetWorldName));
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
     */
    private void initializeMirage(String worldName, boolean isNetherEnabled) {
        HashSet<MirageWorld> worldNames = new HashSet<>();
        worldNames.add(getMirageWorld(worldName));
        worldNames.add(getMirageWorld(String.format("%s_the_end", worldName)));

        if(isNetherEnabled)
            worldNames.add(getMirageWorld(String.format("%s_nether", worldName)));

        this.worlds.addAll(worldNames);

        for(MirageWorld world : worldNames) {
            new WorldInitHandler(world.getWorldName(), () ->
                    saveWorld(new File(Bukkit.getWorldContainer(), world.getWorldName()), world)
            );
        }
    }
}
