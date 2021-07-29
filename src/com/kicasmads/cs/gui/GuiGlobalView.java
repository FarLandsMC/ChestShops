package com.kicasmads.cs.gui;

import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.data.SkullCache;
import com.kicasmads.cs.data.Shop;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GuiGlobalView extends Gui {
    private final Map<UUID, List<Shop>> ownerGroupedShops;
    private final List<OfflinePlayer> shopOwners;
    private OfflinePlayer currentViewedOwner;
    private int ownersPage, shopsPage;

    public GuiGlobalView() {
        super(54, "Shop Owners");
        this.ownerGroupedShops = new HashMap<>();
        this.shopOwners = new ArrayList<>();
        this.currentViewedOwner = null;
        this.ownersPage = 0;
        this.shopsPage = 0;

        ChestShops.getDataHandler().getAllShops().stream().filter(Shop::isNotEmpty).forEach(shop -> {
            List<Shop> ownerShops = ownerGroupedShops.get(shop.getOwner());
            if (ownerShops == null) {
                ownerShops = new ArrayList<>();
                ownerShops.add(shop);
                ownerGroupedShops.put(shop.getOwner(), ownerShops);

                // Intentionally refresh the profile
                shopOwners.add(shop.getOwnerOfflinePlayer());
            } else
                ownerShops.add(shop);
        });

        // Sort by username
        // Removed for now.  May add back later, if fixes bug.
         shopOwners.sort(Comparator.comparing(OfflinePlayer::getName));
    }

    private void changeOwnersPage(int move) {
        ownersPage += move;
        newInventory(54, "Shop Owners");
    }

    private void changeShopsPage(int move) {
        shopsPage += move;
        newInventory(54, currentViewedOwner.getName() + "'s Shops");
    }

    @Override
    protected void populateInventory() {
        if (currentViewedOwner == null) {
            if (shopOwners.size() <= 54) {
                int slot = 0;
                for (OfflinePlayer owner : shopOwners) {
                    displayOwner(slot, owner);
                    ++ slot;
                }
            } else {
                for (int i = ownersPage * 45;i < Math.min((ownersPage + 1) * 45, shopOwners.size());++ i) {
                    displayOwner(i % 45, shopOwners.get(i));
                }

                if (ownersPage == 0)
                    addLabel(45, Material.REDSTONE_BLOCK, ChatColor.RED + "No Previous Page");
                else
                    addActionItem(45, Material.EMERALD_BLOCK, ChatColor.GREEN + "Previous Page", () -> changeOwnersPage(-1));

                if ((ownersPage + 1) * 45 >= shopOwners.size())
                    addLabel(53, Material.REDSTONE_BLOCK, ChatColor.RED + "No Next Page");
                else
                    addActionItem(53, Material.EMERALD_BLOCK, ChatColor.GREEN + "Next Page", () -> changeOwnersPage(1));
            }
        } else {
            displayShops(
                    ownerGroupedShops.get(currentViewedOwner.getUniqueId()),
                    false,
                    false,
                    shopsPage,
                    45,
                    this::changeShopsPage,
                    shop -> shop.tryTransaction(user, false)
            );
            addActionItem(49, Material.NETHER_STAR, ChatColor.RESET + "Back", this::displayOwners);
        }
    }

    private void displayOwners() {
        currentViewedOwner = null;
        newInventory(54, "Shop Owners");
    }

    private void displayShops(OfflinePlayer owner) {
        currentViewedOwner = owner;
        shopsPage = 0;
        newInventory(54, owner.getName() + "'s Shops");
    }

    private void displayOwner(int slot, OfflinePlayer owner) {
        ItemStack head = SkullCache.getHead(owner);
        ItemMeta meta = head.getItemMeta();

        meta.setDisplayName(ChatColor.RESET + owner.getName());
        meta.setLore(List.of(
            ChatColor.WHITE + "Click to View Shops.",
            ChatColor.GRAY + "Shops: " + ownerGroupedShops.get(owner.getUniqueId()).size()
        ));
        head.setItemMeta(meta);
        addActionItem(slot, head, () -> displayShops(owner));
    }
}
