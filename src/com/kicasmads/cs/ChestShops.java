package com.kicasmads.cs;

import com.kicasmads.cs.event.CSEventHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class ChestShops extends JavaPlugin {
    private Material currencyItem;
    private ItemStack currencyStack;

    public static final String SHOP_HEADER = "[shop]";
    private static ChestShops instance;

    public ChestShops() {
        instance = this;
    }

    public static ChestShops getInstance() {
        return instance;
    }

    public static Material getCurrencyItem() {
        return instance.currencyItem;
    }

    public static ItemStack getCurrencyStack() {
        return instance.currencyStack;
    }

    @Override
    public void onEnable() {
        initConfig();
        Bukkit.getPluginManager().registerEvents(new CSEventHandler(), this);
    }

    private void initConfig() {
        FileConfiguration config = getConfig();

        config.addDefault("general.currency-item", Utils.formattedName(Material.DIAMOND));

        config.options().copyDefaults(true);
        saveConfig();

        String currencyItemString = config.getString("general.currency-item");
        currencyItem = Utils.valueOfFormattedName(currencyItemString, Material.class);
        if (currencyItem == null) {
            error("Invalid currency item: " + currencyItemString);
            currencyItem = Material.DIAMOND;
        }
        currencyStack = new ItemStack(currencyItem);
    }

    /**
     * Logs an object to the console.
     *
     * @param x the object to log.
     */
    public static void log(Object x) {
        Bukkit.getLogger().info("[ChestShops] " + x);
    }

    /**
     * Logs an error to the console.
     *
     * @param x the error to log.
     */
    public static void error(Object x) {
        Bukkit.getLogger().severe("[ChestShops] " + x);
    }
}
