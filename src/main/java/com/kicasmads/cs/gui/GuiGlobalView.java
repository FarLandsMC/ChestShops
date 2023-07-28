package com.kicasmads.cs.gui;

import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.Utils;
import com.kicasmads.cs.data.Shop;

import com.kicasmads.cs.data.ShopType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.CreativeCategory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GuiGlobalView extends Gui {
    private final Map<UUID, List<Shop>> ownerGroupedShops;
    private final List<OfflinePlayer> shopOwners;
    private final @Nullable ShopType typeFilter;
    private OfflinePlayer currentViewedOwner;
    private int ownersPage, shopsPage;

    private static final ItemStack BACK_BUTTON;

    static {
        BACK_BUTTON = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = BACK_BUTTON.getItemMeta();
        meta.displayName(Component.text("Back", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        BACK_BUTTON.setItemMeta(meta);
    }

    public GuiGlobalView(@Nullable ShopType typeFilter) {
        super(54, "Shop Owners");
        this.ownerGroupedShops = new HashMap<>();
        this.shopOwners = new ArrayList<>();
        this.currentViewedOwner = null;
        this.ownersPage = 0;
        this.shopsPage = 0;
        this.typeFilter = typeFilter;

        // TODO: Don't show chests that are out of stock (Too much lag to load the inventories)
        ChestShops.getDataHandler().getAllShops().forEach(shop -> {
            List<Shop> ownerShops = ownerGroupedShops.get(shop.getOwner());

            if (ownerShops == null) {
                ownerShops = new ArrayList<>();
                ownerGroupedShops.put(shop.getOwner(), ownerShops);
                shopOwners.add(shop.getOwnerOfflinePlayer());
            }

            if (typeFilter == null || shop.getType() == typeFilter) {
                ownerShops.add(shop);
            }
        });

        // Sort by username
        shopOwners.sort(Comparator.comparing(OfflinePlayer::getName));
    }

    private void changeOwnersPage(int move) {
        ownersPage += move;
        newInventory(54, Component.text("Shop Owners"));
    }

    private void changeShopsPage(int move) {
        shopsPage += move;
        newInventory(54, Component.text(currentViewedOwner.getName() + "'s Shops"));
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
                    addLabel(45, Material.REDSTONE_BLOCK, Component.text("No Previous Page").color(NamedTextColor.RED));
                else
                    addActionItem(45, Material.EMERALD_BLOCK,
                        Component.text("Previous Page").color(NamedTextColor.GREEN), () -> changeOwnersPage(-1));

                if ((ownersPage + 1) * 45 >= shopOwners.size())
                    addLabel(53, Material.REDSTONE_BLOCK, Component.text("No Next Page").color(NamedTextColor.RED));
                else
                    addActionItem(53, Material.EMERALD_BLOCK,
                        Component.text("Next Page").color(NamedTextColor.GREEN), () -> changeOwnersPage(1));
            }
        } else {
            displayShops(
                ownerGroupedShops.get(currentViewedOwner.getUniqueId())
                    .stream()
                    .sorted(Comparator.comparing(Shop::getType))
                    .toList(),
                false,
                shopsPage,
                45,
                this::changeShopsPage,
                shop -> shop.tryTransaction(user, false)
            );
            addActionItem(49, Material.NETHER_STAR, Component.text("Back"), this::displayOwners);
        }
    }

    private void displayOwners() {
        currentViewedOwner = null;
        newInventory(54, Component.text("Shop Owners"));
    }

    private void displayShops(OfflinePlayer owner) {
        currentViewedOwner = owner;
        shopsPage = 0;
        newInventory(54, Component.text(owner.getName() + "'s Shops"));
    }

    private void displayOwner(int slot, OfflinePlayer owner) {
        ItemStack head = Utils.getHead(owner);
        ItemMeta meta = head.getItemMeta();

        meta.displayName(
            Component.text(owner.getName())
                .decoration(TextDecoration.ITALIC, false)
        );
        meta.lore(List.of(
            Component.text("Click to View Shops.").color(NamedTextColor.WHITE),
            Component.text("Shops: " + ownerGroupedShops.get(owner.getUniqueId()).size()).color(NamedTextColor.GRAY)
        ));
        head.setItemMeta(meta);
        addActionItem(slot, head, () -> displayShops(owner));
    }
}
