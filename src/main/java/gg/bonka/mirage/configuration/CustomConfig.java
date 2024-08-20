package gg.bonka.mirage.configuration;

import gg.bonka.mirage.Mirage;
import gg.bonka.mirage.misc.ConsoleLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class CustomConfig {

    private final FileConfiguration config;

    /**
     * Constructs a CustomConfig object.
     *
     * @param directory the directory in which the config file is located
     * @param fileName the name of the config file
     */
    public CustomConfig(File directory, String fileName) {
        File configFile = new File(directory, fileName);
        if (!configFile.exists()) {
            InputStream stream = Mirage.getInstance().getResource(fileName);

            if(stream == null) {
                throw new RuntimeException(String.format("No resource found with name: %s", fileName));
            }

            try {
                //noinspection ResultOfMethodCallIgnored
                configFile.createNewFile();
                Files.copy(stream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            ConsoleLogger.error(String.format("Failed to load config file: %s", fileName));
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
}