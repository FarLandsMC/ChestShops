package com.kicasmads.cs.event;

import com.kicasmads.cs.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

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

            Bukkit.getScheduler().runTaskLater(ChestShops.getInstance(), () -> {
                Sign signBlock = (Sign) event.getBlock().getState();
                signBlock.setEditable(true);

                signBlock.setLine(0, ChatColor.RED + ChestShops.SHOP_HEADER);
                switch(type) {
                    case SELL:
                        signBlock.setLine(1, ChatColor.RED + "Selling: " + sellAmount);
                        signBlock.setLine(2, ChatColor.RED + "" + sellAmount + " " + Utils.getItemName(ChestShops.getCurrencyStack()) + (sellAmount > 1 ? "s" : ""));
                        break;
                    case BUY:
                        signBlock.setLine(1, ChatColor.RED + "Buying: " + buyAmount);

                        signBlock.setLine(2, ChatColor.RED + "" + buyAmount + " " + Utils.getItemName(ChestShops.getCurrencyStack()) + (buyAmount > 1 ? "s" : ""));
                        break;
                    case BARTER:
                        signBlock.setLine(1, ChatColor.RED + "Bartering");
                        signBlock.setLine(2, ChatColor.RED + "" + buyAmount + " for " + sellAmount);
                }
                signBlock.setLine(3, ChatColor.RED + player.getDisplayName());
                signBlock.update(true);
            }, 1);

            ShopBuilder builder = new ShopBuilder(type, player, event.getBlock().getLocation(), buyAmount, sellAmount);
            ChestShops.getDataHandler().cacheBuilder(event.getBlock().getLocation(), builder);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        event.setCancelled(signDamage(event, event.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        event.setCancelled(signDamage(event, event.getPlayer()));
    }

    private <T extends BlockEvent> boolean signDamage(T event, Player player) {
        Shop shop = ChestShops.getDataHandler().getShop(event.getBlock().getLocation());

        if(event.getBlock().getBlockData() instanceof WallSign && player.getInventory().getItemInMainHand().getAmount() != 0 && ChestShops.getDataHandler().getCachedBuilder(event.getBlock().getLocation(), player) != null) {
            ShopBuilder builder = ChestShops.getDataHandler().getCachedBuilder(event.getBlock().getLocation(), player);
            if (builder != null) {
                ItemStack stack = player.getInventory().getItemInMainHand();
                stack.setAmount(1);
                builder.update(stack);
                return true;
            } else
                player.sendMessage(ChatColor.RED + "You cannot set the trade of a shop you do not own.");

        // remove the shop if the owner breaks it
        } else if(shop != null && shop.getOwner().equals(player.getUniqueId()) && event instanceof BlockBreakEvent) {
            ChestShops.getDataHandler().removeShop(shop);
            player.sendMessage(ChatColor.GOLD + "You have destroyed your shop.");
        }

        // Stop anyone else from breaking the shop
        else if(shop != null) {
            player.sendMessage(ChatColor.RED + "You can't destroy someone else's shop.");
            return true;
        }

        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && event.getClickedBlock().getBlockData() instanceof WallSign && ChestShops.getDataHandler().getShop(event.getClickedBlock().getLocation()) != null)
            ChestShops.getDataHandler().getShop(event.getClickedBlock().getLocation()).tryTransaction(event.getPlayer());
    }
}
