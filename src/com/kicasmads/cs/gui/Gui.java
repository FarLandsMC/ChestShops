package com.kicasmads.cs.gui;

import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.data.Shop;
import com.kicasmads.cs.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        stack.setItemMeta(meta);
        inv.setItem(slot, stack);
    }

    protected void displayShop(int slot, Shop shop, boolean showTransaction) {
        String name = ChatColor.RESET.toString() + shop.getBuyAmount() + " " + Utils.getItemName(shop.getBuyItem()) +
                " -> " + shop.getSellAmount() + " " + Utils.getItemName(shop.getSellItem());

        // Display what it's buying and selling
        if (showTransaction) {
            addLabel(
                    slot, Material.CHEST, name,
                    "Buying: " + shop.getBuyAmount() + "x " + Utils.getItemName(shop.getBuyItem()),
                    "Selling: " + shop.getSellAmount() + "x " + Utils.getItemName(shop.getSellItem())
            );
        }
        // Display the location
        else {
            addLabel(
                    slot, Material.CHEST, name,
                    "At: " + shop.getChestLocation().getBlockX() + " " + shop.getChestLocation().getBlockY() + " " +
                            shop.getChestLocation().getBlockZ()
            );
        }
    }

    protected void displayShops(List<Shop> shops, boolean showTransaction, int page, int pageCut, Consumer<Integer> pageChanger) {
        if (shops.size() <= pageCut) {
            int slot = 0;
            for (Shop shop : shops) {
                displayShop(slot, shop, showTransaction);
            }
        } else {
            for (int i = page * 45;i < Math.min((page + 1) * 45, shops.size());++ i) {
                displayShop(i, shops.get(i), showTransaction);
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
