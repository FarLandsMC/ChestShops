package com.kicasmads.cs;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.xml.stream.Location;
import java.util.UUID;

public class Shop {
    protected final ShopType type;
    protected final UUID owner;
    protected final Location chest;
    protected final ItemStack buyItem;
    protected final ItemStack sellItem;

    public Shop(ShopType type, UUID owner, Location chest, ItemStack buyItem, ItemStack sellItem) {
        this.type = type;
        this.owner = owner;
        this.chest = chest;
        this.buyItem = buyItem;
        this.sellItem = sellItem;
    }

    public final ShopType getType() {
        return type;
    }

    public void tryTransaction(Player player) {

    }
}
