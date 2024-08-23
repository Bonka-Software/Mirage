package gg.bonka.mirage.configuration;

import gg.bonka.mirage.Mirage;
import gg.bonka.mirage.misc.ConsoleLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * The CustomConfig class represents a configuration file for Mirage.
 *
 * @see "Credits to: <a href="https://github.com/Casb02"> Casb02</a>
 * who came up with this class and wrote it for another Bonka project.
 * This version is modified."
 */
public class CustomConfig {

    private final File configFile;
    private final FileConfiguration config;

    /**
     * Constructs a CustomConfig object.
     *
     * @param directory the directory in which the config file is located
     * @param fileName the name of the config file
     */
    public CustomConfig(File directory, String fileName) {
        configFile = new File(directory, fileName);
        config = new YamlConfiguration();

        try {
            if(!configFile.exists()) {
                InputStream stream = Mirage.getInstance().getResource(fileName);

                if(stream == null)
                    throw new RuntimeException(String.format("No resource found with name: %s", fileName));

                config.loadFromString(new String(stream.readAllBytes()));
            } else {
                config.load(configFile);
            }
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            ConsoleLogger.error(String.format("Failed to load config file: %s error: %s", fileName, e));
        }
    }

    /**
     * Get a string key from the config
     *
     * @param key the key to get
     * @return the string key
     */
    public String getStringKey(String key) {
        return config.getString(key);
    }

    /**
     * Sets the value of a specific key in the configuration.
     *
     * @param key the key to set
     * @param value the value to assign to the key
     */
    public void put(String key, Object value) {
        config.set(key, value);
    }

    /**
     * Saves the current configuration to a file.
     *
     * @throws IOException if an I/O error occurs while saving the configuration
     */
    public void save() throws IOException {
        if(!configFile.exists())
            //noinspection ResultOfMethodCallIgnored
            configFile.createNewFile();

        config.save(configFile);
    }
}