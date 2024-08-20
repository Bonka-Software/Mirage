package gg.bonka.mirage.world;

import gg.bonka.mirage.configuration.CustomConfig;
import lombok.Getter;

import java.io.File;

public class MirageWorld {

    @Getter
    private final String worldName;
    @Getter
    private final File saveDirectory;

    private CustomConfig config;

    private Boolean persistent;
    private Boolean loadOnStart;
    private Boolean keepInMemory;

    public MirageWorld(String worldName, File saveDirectory) {
        this.worldName = worldName;
        this.saveDirectory = saveDirectory;
    }

    public void save() {
        getConfig();
    }

    private CustomConfig getConfig() {
        if(config == null) {
            config = new CustomConfig(saveDirectory, "mirage-world.yml");
        }

        return config;
    }

    public boolean getPersistent() {
        return getBoolean(persistent,"persistent");
    }

    public boolean getLoadOnStart() {
        return getBoolean(loadOnStart, "load-on-start");
    }

    public boolean getKeepInMemory() {
        return getBoolean(keepInMemory, "keep-in-memory");
    }

    private boolean getBoolean(Boolean bool, String key) {
        if(bool == null) {
            bool = Boolean.parseBoolean(getConfig().getStringKey(key));
        }

        return Boolean.TRUE.equals(bool);
    }
}
