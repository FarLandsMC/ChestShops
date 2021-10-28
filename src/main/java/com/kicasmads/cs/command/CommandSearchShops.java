package com.kicasmads.cs.command;

import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.Utils;
import com.kicasmads.cs.data.DataHandler;
import com.kicasmads.cs.data.Shop;
import com.kicasmads.cs.data.ShopType;
import com.kicasmads.cs.gui.GuiShopsView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandSearchShops implements CommandExecutor, TabCompleter {

    private final DataHandler dataHandler;

    public CommandSearchShops() {
        this.dataHandler = ChestShops.getDataHandler();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("You must be in-game to use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            return false;
        }

        boolean searchBuy = args.length > 1 && "buy".equalsIgnoreCase(args[1]);
        args[0] = args[0].toLowerCase();
        List<Shop> shops = dataHandler.getAllShops().stream()
            .filter(
                shop ->
                    searchBuy == (shop.getType() == ShopType.BUY) &&
                        Utils.searchableName(searchBuy ? shop.getBuyItem() : shop.getSellItem()).contains(args[0])
            )
            .collect(Collectors.toList());

        if (shops.isEmpty()) {
            player.sendMessage(Component.text("There are no shops that sell this item.").color(NamedTextColor.RED));
            return true;
        }

        new GuiShopsView(
            shops,
            "Shops",
            true,
            true
        ).openGui(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, @NotNull String[] args) {
        switch (args.length) {
            case 1:
                List<String> suggestions = Arrays.stream(Material.values())
                    .map(Utils::formattedName)
                    .collect(Collectors.toCollection(ArrayList::new));
                suggestions.addAll(Utils.ENCHANTMENT_NAMES);
                return Utils.filterStartingWith(args[0], suggestions);

            case 2:
                return Arrays.asList("buy", "sell");

            default:
                return Collections.emptyList();
        }
    }
}
