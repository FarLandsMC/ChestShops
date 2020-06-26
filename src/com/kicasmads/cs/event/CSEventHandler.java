package com.kicasmads.cs.event;

import com.kicasmads.cs.*;
import com.kicasmads.cs.Utils;
import com.kicasmads.cs.data.Shop;
import com.kicasmads.cs.data.ShopBuilder;
import com.kicasmads.cs.data.ShopType;

import net.minecraft.server.v1_16_R1.InventoryEnderChest;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.stream.Collectors;

public class CSEventHandler implements Listener {
    @EventHandler(ignoreCancelled=true)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        String[] lines = event.getLines();

        if (lines.length == 4 && ChestShops.SHOP_HEADER.equalsIgnoreCase(lines[0]) && event.getBlock().getBlockData() instanceof WallSign) {
            // Make sure the sign is actually attached to a chest
            Block attachedTo = event.getBlock().getRelative(((WallSign)event.getBlock().getBlockData()).getFacing().getOppositeFace());
            if (attachedTo.getType() != Material.CHEST)
                return;

            // There's already a shop in place at the location
            if (ChestShops.getDataHandler().getShop(attachedTo.getLocation()) != null)
                return;

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

            // Run a few ticks later so that the sign updates correctly
            Bukkit.getScheduler().runTaskLater(ChestShops.getInstance(), () -> {
                ShopBuilder builder = new ShopBuilder(type, player, event.getBlock().getLocation(),
                        attachedTo.getLocation(), buyAmount, sellAmount);
                ChestShops.getDataHandler().cacheBuilder(event.getBlock().getLocation(), builder);
            }, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        event.setCancelled(builderUpdate(event, event.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (builderUpdate(event, event.getPlayer()))
            event.setCancelled(true);
        else if (event.getBlock().getBlockData() instanceof WallSign) {
            Shop shop = ChestShops.getDataHandler().getShop(event.getBlock().getLocation());
            if (shop != null) {
                if (!shop.isOwner(event.getPlayer()) && !event.getPlayer().hasPermission("cs.shops.destroy-unowned")) {
                    event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to remove this shop.");
                    event.setCancelled(true);
                }

                ShopRemoveEvent removeEvent = new ShopRemoveEvent(event.getPlayer(), shop);
                ChestShops.getInstance().getServer().getPluginManager().callEvent(removeEvent);
                if (!removeEvent.isCancelled()) {
                    ChestShops.getDataHandler().removeShop(shop);
                    event.getPlayer().sendMessage(ChatColor.GOLD + "This shop has been removed.");
                }

                event.setCancelled(removeEvent.isCancelled());
            }
        } else if (event.getBlock().getType() == Material.CHEST && ChestShops.getDataHandler().getShop(event.getBlock().getLocation()) != null) {
            event.getPlayer().sendMessage(ChatColor.RED + "If you wish to remove this shop, please break the sign instead.");
            event.setCancelled(true);
        }
    }

    private <T extends BlockEvent> boolean builderUpdate(T event, Player player) {
        if(event.getBlock().getBlockData() instanceof WallSign && player.getInventory().getItemInMainHand().getAmount() != 0) {
            ShopBuilder builder = ChestShops.getDataHandler().getCachedBuilder(event.getBlock().getLocation());

            if (builder != null) {
                if (builder.isOwner(player)) {
                    ItemStack stack = player.getInventory().getItemInMainHand().clone();
                    stack.setAmount(1);
                    builder.update(stack);
                    return true;
                } else
                    player.sendMessage(ChatColor.RED + "You cannot set the trade of a shop you do not own.");
            }
        }

        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Shop shop = ChestShops.getDataHandler().getShop(event.getClickedBlock().getLocation());
            if (shop != null && event.getClickedBlock().getBlockData() instanceof WallSign) {
                shop.tryTransaction(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory() instanceof BlockInventoryHolder) ||
                event.getView().getTopInventory() instanceof InventoryEnderChest)
            return;

        Shop shop = ChestShops.getDataHandler().getShop(event.getView().getTopInventory().getLocation());
        if (shop == null)
            return;

        if (emptySlots(event.getView().getTopInventory()) > shop.getRequiredOpenSlots())
            return;

        InventoryAction action = event.getAction();
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            if (action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE ||
                    action == InventoryAction.PLACE_SOME || action == InventoryAction.HOTBAR_SWAP) {
                event.setCancelled(event.getClickedInventory().getItem(event.getRawSlot()) == null);
            }
        } else {
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY)
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory() instanceof BlockInventoryHolder) ||
                event.getView().getTopInventory() instanceof InventoryEnderChest)
            return;

        Shop shop = ChestShops.getDataHandler().getShop(event.getView().getTopInventory().getLocation());
        if (shop == null)
            return;

        event.setCancelled(emptySlots(event.getView().getTopInventory()) - event.getInventorySlots().size() <= shop.getRequiredOpenSlots());
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        Shop shop = ChestShops.getDataHandler().getShop(event.getDestination().getLocation());
        if (shop != null)
            event.setCancelled(emptySlots(event.getDestination()) <= shop.getRequiredOpenSlots());
    }

    private int emptySlots(Inventory inventory) {
        int emptyCount = 0;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null || stack.getType() == Material.AIR)
                ++ emptyCount;
        }

        return emptyCount;
    }
}
