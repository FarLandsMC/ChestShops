package com.kicasmads.cs.gui;

import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.data.Shop;
import com.kicasmads.cs.Utils;

import com.kicasmads.cs.data.ShopType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
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
        this.inv = Bukkit.createInventory(null, size, Component.text(displayName));
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

    protected void setItem(int slot, Material material, Component name, Component... lore) {
        setStack(slot, new ItemStack(material), name, lore);
    }

    protected void setStack(int slot, ItemStack stack, Component name, Component... lore) {
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name);
        meta.lore(Stream.of(lore).filter(component -> PlainTextComponentSerializer.plainText()
            .serialize(component).length() > 1).collect(Collectors.toList()));
        stack.setItemMeta(meta);
        inv.setItem(slot, stack);
    }

    protected void displayShop(int slot, Shop shop, boolean showTransaction, boolean showOwner, Consumer<Shop> shopAction) {
        Component name = Component.text(shop.getBuyAmount() + " " + Utils.getItemName(shop.getBuyItem()) +
            " -> " + shop.getSellAmount() + " " + Utils.getItemName(shop.getSellItem())).color(NamedTextColor.WHITE);

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
                    Component.text("Buying: " + shop.getBuyAmount() + "x " + Utils.getItemName(shop.getBuyItem())),
                    Component.text("Selling: " + shop.getSellAmount() + "x " + Utils.getItemName(shop.getSellItem())),
                    Component.text(showOwner ? "Owned by " + shop.getOwnerName() : "")
            );
        }
        // Display the location
        else {
            addActionItem(
                    slot,
                    displayItem,
                    name,
                    action,
                    Component.text("At: " + shop.getChestLocation().getBlockX() + " " + shop.getChestLocation().getBlockY() + " " +
                            shop.getChestLocation().getBlockZ()),
                    Component.text(showOwner ? "Owned by " + shop.getOwnerName() : "")
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
                addLabel(45, Material.REDSTONE_BLOCK, Component.text("No Previous Page").color(NamedTextColor.RED));
            else
                addActionItem(45, Material.EMERALD_BLOCK,
                    Component.text("Previous Page").color(NamedTextColor.GREEN), () -> pageChanger.accept(-1));

            if ((page + 1) * 45 >= shops.size())
                addLabel(53, Material.REDSTONE_BLOCK, Component.text("No Next Page").color(NamedTextColor.RED));
            else
                addActionItem(53, Material.EMERALD_BLOCK,
                    Component.text("Next Page").color(NamedTextColor.GREEN), () -> pageChanger.accept(1));
        }
    }

    protected void addActionItem(int slot, Material material, Component name, Runnable action, Component... lore) {
        setItem(slot, material, name, lore);
        clickActions.put(slot, action);
    }

    protected void addActionItem(int slot, ItemStack stack, Component name, Runnable action, Component... lore) {
        setStack(slot, stack, name, lore);
        clickActions.put(slot, action);
    }

    protected void addLabel(int slot, Material material, Component name, Component... lore) {
        addActionItem(slot, material, name, Utils.NO_ACTION, lore);
    }

    protected void addActionItem(int slot, ItemStack stack, Runnable action) {
        inv.setItem(slot, stack);
        clickActions.put(slot, action);
    }

    protected void newInventory(int size, Component displayName) {
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
