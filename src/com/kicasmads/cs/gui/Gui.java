package com.kicasmads.cs.gui;

import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.data.Shop;
import com.kicasmads.cs.Utils;

import com.kicasmads.cs.data.ShopType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Gui {
    // Slot: Action
    protected final Map<Integer, Runnable> clickActions;
    protected Inventory inv;
    protected Player user;
    private boolean ignoreClose;

    protected Gui(int size, String displayName) {
        this.clickActions = new HashMap<>();
        this.inv = Bukkit.createInventory(null, size, displayName);
        this.user = null;
        this.ignoreClose = false;
    }

    public Inventory getInventory() {
        return inv;
    }

    public boolean matches(Inventory inventory) {
        return inv.equals(inventory);
    }

    protected abstract void populateInventory();

    public void openGui(Player player) {
        user = player;
        populateInventory();
        player.openInventory(inv);
        ChestShops.getGuiHandler().registerActiveGui(this);
    }

    protected void setItem(int slot, Material material, String name, String... lore) {
        setStack(slot, new ItemStack(material), name, lore);
    }

    protected void setStack(int slot, ItemStack stack, String name, String... lore) {
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Stream.of(lore).filter(Objects::nonNull).collect(Collectors.toList()));
        stack.setItemMeta(meta);
        inv.setItem(slot, stack);
    }

    protected void displayShop(int slot, Shop shop, boolean showTransaction, boolean showOwner, Consumer<Shop> shopAction) {
        String name = ChatColor.RESET.toString() + shop.getBuyAmount() + " " + Utils.getItemName(shop.getBuyItem()) +
                " -> " + shop.getSellAmount() + " " + Utils.getItemName(shop.getSellItem());

        ItemStack displayItem;
        if (shop.getType() == ShopType.BUY)
            displayItem = new ItemStack(Material.CHEST);
        else {
            displayItem = shop.getSellItem().clone();
            displayItem.setAmount(shop.getSellAmount());
        }

        // Display what it's buying and selling
        Runnable action = shopAction == null ? Utils.NO_ACTION : () -> shopAction.accept(shop);
        if (showTransaction) {
            addActionItem(
                    slot,
                    displayItem,
                    name,
                    action,
                    "Buying: " + shop.getBuyAmount() + "x " + Utils.getItemName(shop.getBuyItem()),
                    "Selling: " + shop.getSellAmount() + "x " + Utils.getItemName(shop.getSellItem()),
                    showOwner ? "Owned by " + Bukkit.getOfflinePlayer(shop.getOwner()).getName() : null
            );
        }
        // Display the location
        else {
            addActionItem(
                    slot,
                    displayItem,
                    name,
                    action,
                    "At: " + shop.getChestLocation().getBlockX() + " " + shop.getChestLocation().getBlockY() + " " +
                            shop.getChestLocation().getBlockZ(),
                    showOwner ? "Owned by " + Bukkit.getOfflinePlayer(shop.getOwner()).getName() : null
            );
        }
    }

    protected void displayShops(
            List<Shop> shops,
            boolean showTransaction,
            boolean showOwner,
            int page,
            int pageCut,
            Consumer<Integer> pageChanger,
            Consumer<Shop> shopAction
    ) {
        if (shops.size() <= pageCut) {
            int slot = 0;
            for (Shop shop : shops) {
                displayShop(slot, shop, showTransaction, showOwner, shopAction);
                ++ slot;
            }
        } else {
            for (int i = page * 45;i < Math.min((page + 1) * 45, shops.size());++ i) {
                displayShop(i % 45, shops.get(i), showTransaction, showOwner, shopAction);
            }

            if (page == 0)
                addLabel(45, Material.REDSTONE_BLOCK, ChatColor.RED + "No Previous Page");
            else
                addActionItem(45, Material.EMERALD_BLOCK, ChatColor.GREEN + "Previous Page", () -> pageChanger.accept(-1));

            if ((page + 1) * 45 >= shops.size())
                addLabel(53, Material.REDSTONE_BLOCK, ChatColor.RED + "No Next Page");
            else
                addActionItem(53, Material.EMERALD_BLOCK, ChatColor.GREEN + "Next Page", () -> pageChanger.accept(1));
        }
    }

    protected void addActionItem(int slot, Material material, String name, Runnable action, String... lore) {
        setItem(slot, material, name, lore);
        clickActions.put(slot, action);
    }

    protected void addActionItem(int slot, ItemStack stack, String name, Runnable action, String... lore) {
        setStack(slot, stack, name, lore);
        clickActions.put(slot, action);
    }

    protected void addLabel(int slot, Material material, String name, String... lore) {
        addActionItem(slot, material, name, Utils.NO_ACTION, lore);
    }

    protected void addActionItem(int slot, ItemStack stack, Runnable action) {
        inv.setItem(slot, stack);
        clickActions.put(slot, action);
    }

    protected void newInventory(int size, String displayName) {
        ignoreClose = true;
        user.closeInventory();
        inv = Bukkit.createInventory(null, size, displayName);
        user.openInventory(inv);
        refreshInventory();
    }

    protected void refreshInventory() {
        inv.clear();
        clickActions.clear();
        populateInventory();
    }

    public void onItemClick(InventoryClickEvent event) {
        Runnable action = clickActions.get(event.getRawSlot());
        if(action != null) {
            action.run();
            event.setCancelled(true);
        }
    }

    public void onInventoryClosed() {
        if(ignoreClose) {
            ignoreClose = false;
        }else{
            onClose();
            ChestShops.getGuiHandler().removeActiveGui(this);
        }
    }

    protected void onClose() { }

    protected static ItemStack clone(ItemStack stack) {
        return stack == null ? null : stack.clone();
    }
}
