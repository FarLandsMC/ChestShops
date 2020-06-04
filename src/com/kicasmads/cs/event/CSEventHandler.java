package com.kicasmads.cs.event;

import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.ShopBuilder;
import com.kicasmads.cs.ShopType;
import com.kicasmads.cs.Utils;
import org.bukkit.ChatColor;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.SignChangeEvent;

import java.util.Arrays;
import java.util.stream.Collectors;

public class CSEventHandler implements Listener {
    @EventHandler(ignoreCancelled=true)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        String[] lines = event.getLines();

        if (lines.length == 4 && ChestShops.SHOP_HEADER.equalsIgnoreCase(lines[0]) && event.getBlock().getBlockData() instanceof WallSign) {
            int buyAmount, sellAmount;

            try {
                buyAmount = Integer.parseInt(lines[1]);
            } catch (NumberFormatException ex) {
                player.sendMessage(ChatColor.RED + "The number you entered for the buy amount is invalid.");
                return;
            }

            try {
                sellAmount = Integer.parseInt(lines[2]);
            } catch (NumberFormatException ex) {
                player.sendMessage(ChatColor.RED + "The number you entered for the sell amount is invalid.");
                return;
            }

            if (buyAmount <= 0 || sellAmount <= 0) {
                player.sendMessage(ChatColor.RED + "The buy counts and sell amounts must be greater than or equal to zero.");
                return;
            }

            ShopType type = Utils.valueOfFormattedName(lines[3], ShopType.class);
            if (type == null) {
                player.sendMessage(ChatColor.RED + "Invalid shop type: \"" + lines[3] + "\". Valid shop types include: " +
                        Arrays.stream(ShopType.values()).map(Utils::formattedName).collect(Collectors.joining(", ")));
                return;
            }

            ShopBuilder builder = new ShopBuilder(type, player, event.getBlock().getLocation(), buyAmount, sellAmount);
            ChestShops.getDataHandler().cacheBuilder(event.getBlock().getLocation(), builder);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();

        if(event.getBlock().getBlockData() instanceof WallSign && player.getInventory().getItemInMainHand().getAmount() != 0) {
            ShopBuilder builder = ChestShops.getDataHandler().getCachedBuilder(event.getBlock().getLocation(), player);
            if(builder != null) {
                builder.update(player.getInventory().getItemInMainHand().clone());
                event.setCancelled(true);
            } else
                player.sendMessage(ChatColor.RED + "You cannot set the trade of a shop you do not own.");
        }
    }
}
