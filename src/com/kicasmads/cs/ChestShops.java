package com.kicasmads.cs;

import com.kicasmads.cs.event.EventHandler;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ChestShops extends JavaPlugin {
    private static ChestShops instance;

    public ChestShops() {
        instance = this;
    }

    public static ChestShops getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        initConfig();
        Bukkit.getPluginManager().registerEvents(new EventHandler(), this);
    }

    private void initConfig() {
        FileConfiguration config = getConfig();

        // Add defaults here

        config.options().copyDefaults(true);
        saveConfig();
    }
}
