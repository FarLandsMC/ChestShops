package com.kicasmads.cs.data;

import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.Utils;
import com.kicasmads.cs.event.ShopCreateEvent;

import com.mojang.authlib.GameProfile;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Sign;
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

    public ShopBuilder(ShopType type, Player owner, Location sign, Location chest, int buyAmount, int sellAmount) {
        this.type = type;
        this.owner = owner;
        this.sign = sign;
        this.chest = chest;
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

        formatSign(true);
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
            owner.sendMessage(ChatColor.GOLD + "Please left-click the sign with the item you wish to buy.");
        } else if (sellItem == null)
            owner.sendMessage(ChatColor.GOLD + "Please left-click the sign with the item you wish to sell.");
        else {
            createShop();
            return;
        }

        owner.playSound(owner.getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);
    }

    private void createShop() {
        Shop shop = new Shop(type, new GameProfile(owner.getUniqueId(), owner.getName()), sign, chest, buyItem, sellItem, buyAmount, sellAmount);
        ShopCreateEvent event = new ShopCreateEvent(owner, shop);
        ChestShops.getInstance().getServer().getPluginManager().callEvent(event);
        ChestShops.getDataHandler().removeCachedBuilder(sign);

        if (event.isCancelled())
            return;

        formatSign(false);

        ChestShops.getDataHandler().addShop(shop, chest, sign);
        owner.sendMessage(ChatColor.GREEN + "Shop successfully created!");
        owner.playSound(owner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
    }

    private void formatSign(boolean red) {
        Sign signBlock = (Sign) sign.getBlock().getState();

        signBlock.setLine(0, (red ? ChatColor.RED.toString() : "") + ChatColor.BOLD + ChestShops.SHOP_HEADER);
        switch(type) {
            case SELL:
                signBlock.setLine(1, (red ? ChatColor.RED : "") + "Selling: " + (red ? ChatColor.BOLD : "") + sellAmount);
                signBlock.setLine(2, (red ? ChatColor.RED : ChatColor.GREEN) + "" + buyAmount + " " +
                        Utils.getItemName(buyItem) + (buyAmount > 1 ? "s" : ""));
                break;
            case BUY:
                signBlock.setLine(1, (red ? ChatColor.RED : "") + "Buying: " + (red ? ChatColor.BOLD : "") + buyAmount);
                signBlock.setLine(2, (red ? ChatColor.RED : ChatColor.GREEN) + "" + sellAmount + " " +
                        Utils.getItemName(sellItem) + (sellAmount > 1 ? "s" : ""));
                break;
            case BARTER:
                signBlock.setLine(1, (red ? ChatColor.RED : "") + "Bartering");
                signBlock.setLine(2, (red ? ChatColor.RED : ChatColor.GREEN) + "" + buyAmount + " for " + sellAmount);
        }
        signBlock.setLine(3, (red ? ChatColor.RED : "") + owner.getName());
        signBlock.update();
    }

    public Player getOwner() {
        return owner;
    }

    public boolean isOwner(Player player) {
        return owner.getUniqueId().equals(player.getUniqueId());
    }
}
