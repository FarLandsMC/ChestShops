package com.kicasmads.cs.command;

import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.Utils;
import com.kicasmads.cs.data.DataHandler;
import com.kicasmads.cs.data.Shop;
import com.kicasmads.cs.gui.GuiGlobalView;
import com.kicasmads.cs.gui.GuiShopsView;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandShops implements CommandExecutor, TabCompleter {

    private final DataHandler dataHandler;

    public CommandShops() {
        this.dataHandler = ChestShops.getDataHandler();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "You must be in-game to use this command.");
            return true;
        }

        if ( // `/shops me` and player has shops
            args.length == 1 && args[0].equals("me") &&
                !ChestShops.getDataHandler().getShops(player).isEmpty()
        ) {
            new GuiShopsView(
                dataHandler.getShops(player.getUniqueId()),
                "My Shops",
                false,
                false
            ).openGui(player);
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("everyone")) {
            new GuiGlobalView().openGui(player);
            return true;
        }

        Shop shop = dataHandler.getAllShops()
            .stream()
            .filter(s -> s.getOwnerName().equalsIgnoreCase(args[0]) && s.isNotEmpty())
            .findFirst()
            .orElse(null);

        if (shop == null) {
            player.sendMessage(ChatColor.RED + "This player does not have any shops.");
            return true;
        }

        new GuiShopsView(
            dataHandler.getShops(shop.getOwner()),
            shop.getOwnerName() + "'s Shops",
            true,
            false
        ).openGui(player);
        return true;

    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            List<String> tabComplete = new ArrayList<>(List.of("me", "everyone"));
            tabComplete.addAll(
                dataHandler
                    .getAllOwners()
                    .stream()
                    .map(OfflinePlayer::getName)
                    .collect(Collectors.toList())
            );
            return Utils.filterStartingWith(args[0], tabComplete);
        }
        return Collections.emptyList();
    }
}
