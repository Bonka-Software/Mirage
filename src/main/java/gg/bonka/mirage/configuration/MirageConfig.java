package gg.bonka.mirage.configuration;

import gg.bonka.mirage.Mirage;
import lombok.Getter;

import java.io.IOException;

public class MirageConfig {

    @Getter
    private final boolean useRealtimeWorldLoading;

    @Getter
    private final int maxRealtimeWorldLoadingRenderDistance;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public MirageConfig() {
        Mirage.getInstance().getDataFolder().mkdirs();
        CustomConfig config = new CustomConfig(Mirage.getInstance().getDataFolder(), "Config.yml");

        useRealtimeWorldLoading = Boolean.parseBoolean(config.getStringKey("use-realtime-world-loading"));
        maxRealtimeWorldLoadingRenderDistance = Integer.parseInt(config.getStringKey("max-realtime-world-loading-render-distance"));

        try {
            config.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
