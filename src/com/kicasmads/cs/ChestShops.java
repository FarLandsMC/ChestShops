package com.kicasmads.cs;

import com.kicasmads.cs.data.DataHandler;
import com.kicasmads.cs.event.CSEventHandler;
import com.kicasmads.cs.gui.GuiGlobalView;
import com.kicasmads.cs.gui.GuiHandler;
import com.kicasmads.cs.gui.GuiPersonalView;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;

public class ChestShops extends JavaPlugin {
    private final DataHandler dataHandler;
    private final GuiHandler guiHandler;
    private Material currencyItem;
    private ItemStack currencyStack;

    public static final String SHOP_HEADER = "[shop]";
    private static ChestShops instance;

    public ChestShops() {
        this.dataHandler = new DataHandler();
        this.guiHandler = new GuiHandler();
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
        dataHandler.load("shops.nbt");
        Bukkit.getPluginManager().registerEvents(guiHandler, this);
        Bukkit.getPluginManager().registerEvents(new CSEventHandler(), this);

        PluginCommand shopsCommand = getCommand("shops");
        shopsCommand.setExecutor((sender, command, alias, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You must be in-game to use this command.");
                return true;
            }
            Player player = (Player) sender;

            boolean selfView = (args.length == 0 || !"everyone".equals(args[0])) && !ChestShops.getDataHandler().getShops(player).isEmpty();
            if (selfView)
                (new GuiPersonalView(player)).openGui(player);
            else
                (new GuiGlobalView()).openGui(player);

            return true;
        });
        shopsCommand.setTabCompleter(
                (sender, command, alias, args) ->
                args.length == 1
                ? Collections.singletonList("everyone")
                : Collections.emptyList()
        );
    }

    @Override
    public void onDisable() {
        dataHandler.save("shops.nbt");
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

    public static DataHandler getDataHandler() {
        return instance.dataHandler;
    }

    public static GuiHandler getGuiHandler() {
        return instance.guiHandler;
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
