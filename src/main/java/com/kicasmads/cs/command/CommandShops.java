package com.kicasmads.cs.command;

import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.Utils;
import com.kicasmads.cs.data.DataHandler;
import com.kicasmads.cs.data.Shop;
import com.kicasmads.cs.data.ShopType;
import com.kicasmads.cs.gui.GuiGlobalView;
import com.kicasmads.cs.gui.GuiShopsView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class CommandShops implements CommandExecutor, TabCompleter {

    private final DataHandler dataHandler;

    public CommandShops() {
        this.dataHandler = ChestShops.getDataHandler();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("You must be in-game to use this command.").color(NamedTextColor.RED));
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
                false,
                null
            ).openGui(player);
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("everyone")) {
            new GuiGlobalView(args.length <= 1 ? null : Utils.valueOfFormattedName(args[1], ShopType.class)).openGui(player);
            return true;
        }

        Shop shop = dataHandler.getAllShops()
            .stream()
            .filter(s -> s.getOwnerName().equalsIgnoreCase(args[0]))
            .findFirst()
            .orElseGet(
                () -> dataHandler.getAllShops()
                    .stream()
                    .filter(s -> s.getOwnerName().toLowerCase().startsWith(args[0].toLowerCase()))
                    .findFirst()
                    .orElse(null)
            );

        if (shop == null) {
            player.sendMessage(Component.text("This player does not have any shops.", NamedTextColor.RED));
            return true;
        }

        new GuiShopsView(
            dataHandler.getShops(shop.getOwner()),
            shop.getOwnerName() + "'s Shops",
            true,
            false,
            args.length == 1 ? null : Utils.valueOfFormattedName(args[1], ShopType.class)
        ).openGui(player);
        return true;

    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, @NotNull String[] args) {
        switch (args.length) {
            case 1 -> {
                List<String> tabComplete = new ArrayList<>(List.of("me", "everyone"));
                dataHandler
                    .getAllOwners()
                    .stream()
                    .map(OfflinePlayer::getName)
                    .forEach(tabComplete::add);
                return Utils.filterStartingWith(args[0], tabComplete);
            }
            case 2 -> {
                return Utils.filterStartingWith(args[1], Arrays.stream(ShopType.values()).map(Utils::formattedName).toList());
            }
        }
        return Collections.emptyList();
    }
}
