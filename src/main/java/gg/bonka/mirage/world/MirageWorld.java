package gg.bonka.mirage.world;

import gg.bonka.mirage.configuration.CustomConfig;
import lombok.Getter;

import java.io.File;
import java.io.IOException;

public class MirageWorld {

    @Getter
    private final String worldName;
    @Getter
    private final File saveDirectory;

    private CustomConfig config;

    private Boolean persistent;
    private Boolean backup;
    private Boolean loadOnStart;
    private Boolean keepInMemory;

    public MirageWorld(String worldName, File saveDirectory) {
        this.worldName = worldName;
        this.saveDirectory = saveDirectory;
    }

    public void save() {
        getConfig();

        config.put("persistent", getPersistent());
        config.put("backup", getBackup());
        config.put("load-on-start", getLoadOnStart());
        config.put("keep-in-memory", getKeepInMemory());

        try {
            config.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public void setBackup(boolean backup) {
        this.backup = backup;
    }

    public void setLoadOnStart(boolean loadOnStart) {
        this.loadOnStart = loadOnStart;
    }

    public void setKeepInMemory(boolean keepInMemory) {
        this.keepInMemory = keepInMemory;
    }

    public boolean getPersistent() {
        return getBoolean(persistent,"persistent");
    }

    public boolean getBackup() {
        return getBoolean(backup,"save-changes-to-backup");
    }

    public boolean getLoadOnStart() {
        return getBoolean(loadOnStart, "load-on-start");
    }

    public boolean getKeepInMemory() {
        return getBoolean(keepInMemory, "keep-in-memory");
    }

    private CustomConfig getConfig() {
        if(config == null) {
            config = new CustomConfig(saveDirectory, "mirage-world.yml");
        }

        return config;
    }

    private boolean getBoolean(Boolean bool, String key) {
        if(bool == null) {
            bool = Boolean.parseBoolean(getConfig().getStringKey(key));
        }

        return Boolean.TRUE.equals(bool);
    }
}
