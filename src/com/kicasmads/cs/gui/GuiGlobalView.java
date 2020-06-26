package com.kicasmads.cs.gui;

import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.data.Shop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class GuiGlobalView extends Gui {
    private final Map<UUID, List<Shop>> allShops;
    private final List<UUID> shopOwners;
    private OfflinePlayer currentViewedOwner;
    private int ownersPage, shopsPage;

    public GuiGlobalView() {
        super(54, "Shop Owners");
        this.allShops = new HashMap<>();
        this.shopOwners = new ArrayList<>();
        this.currentViewedOwner = null;
        this.ownersPage = 0;
        this.shopsPage = 0;

        ChestShops.getDataHandler().getAllShops().forEach(shop -> {
            List<Shop> shops = allShops.get(shop.getOwner());
            if (shops == null) {
                shops = new ArrayList<>();
                shops.add(shop);
                allShops.put(shop.getOwner(), shops);
            } else
                shops.add(shop);

            shopOwners.add(shop.getOwner());
        });

        // Sort by username
        shopOwners.sort(Comparator.comparing(a -> Bukkit.getOfflinePlayer(a).getName()));
    }

    private void changeOwnersPage(int move) {
        ownersPage += move;
        newInventory(54, "Shop Owners");
    }

    private void changeShopsPage(int move) {
        shopsPage += move;
        newInventory(54, currentViewedOwner.getName() + "\'s Shops");
    }

    @Override
    protected void populateInventory() {
        if (currentViewedOwner == null) {
            if (shopOwners.size() <= 54) {
                int slot = 0;
                for (UUID owner : shopOwners) {
                    displayOwner(slot, owner);
                    ++ slot;
                }
            } else {
                for (int i = ownersPage * 45;i < Math.min((ownersPage + 1) * 45, shopOwners.size());++ i) {
                    displayOwner(i, shopOwners.get(i));
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
            displayShops(allShops.get(currentViewedOwner.getUniqueId()), false, shopsPage, 45, this::changeShopsPage);
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
        newInventory(54, owner.getName() + "\'s Shops");
    }

    private void displayOwner(int slot, UUID owner) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        OfflinePlayer player = Bukkit.getOfflinePlayer(owner);
        meta.setOwningPlayer(player);
        meta.setDisplayName(ChatColor.RESET + player.getName());
        meta.setLore(Collections.singletonList("Shops: " + allShops.get(owner).size()));
        head.setItemMeta(meta);
        addActionItem(slot, head, () -> displayShops(player));
    }
}
