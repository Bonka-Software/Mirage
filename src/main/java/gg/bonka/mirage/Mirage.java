package gg.bonka.mirage;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Mirage extends JavaPlugin {

    @Override
    public void onEnable() {
        Bukkit.getLogger().info("Hello World!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
