package com.kicasmads.cs;

import com.kicasmads.cs.event.ShopCreateEvent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ShopBuilder {
    private final ShopType type;
    private final Player owner;
    private final Location sign;
    private final Location chest;
    private final int buyAmount;
    private final int sellAmount;
    protected ItemStack buyItem;
    protected ItemStack sellItem;

    public ShopBuilder(ShopType type, Player owner, Location sign, int buyAmount, int sellAmount) {
        this.type = type;
        this.owner = owner;
        this.sign = sign;
        // Extrapolate the chest location
        this.chest = sign.getBlock().getRelative(((WallSign) sign.getBlock()).getFacing().getOppositeFace()).getLocation();
        this.buyAmount = buyAmount;
        this.sellAmount = sellAmount;
        this.buyItem = null;
        this.sellItem = null;

        switch (type) {
            case BUY:
                sellItem = ChestShops.getCurrencyStack();
                break;
            case SELL:
                buyItem = ChestShops.getCurrencyStack();
                break;
        }

        prompt();
    }

    public void update(ItemStack stack) {
        switch (type) {
            case BUY:
                buyItem = stack;
                break;
            case SELL:
                sellItem = stack;
                break;
            case BARTER:
                if (buyItem == null)
                    buyItem = stack;
                else
                    sellItem = stack;
                break;
        }

        prompt();
    }

    private void prompt() {
        if (buyItem == null) {
            owner.sendMessage(ChatColor.GOLD + "Please right-click the sign with the item you wish to buy.");
        } else if (sellItem == null)
            owner.sendMessage(ChatColor.GOLD + "Please right-click the sign with the item you wish to sell.");
        else {
            createShop();
            return;
        }

        owner.playSound(owner.getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);
    }

    private void createShop() {
        owner.sendMessage(ChatColor.GREEN + "Shop successfully created!");
        owner.playSound(owner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
    }
}
