package com.kicasmads.cs.data;

import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.Utils;
import com.kicasmads.cs.event.ShopCreateEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import static net.kyori.adventure.text.format.NamedTextColor.RED;

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
            case BUY -> sellItem = ChestShops.getCurrencyStack();
            case SELL -> buyItem = ChestShops.getCurrencyStack();
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
            owner.sendMessage(Component.text("Please left-click the sign with the item you wish to buy.").color(NamedTextColor.GOLD));
        } else if (sellItem == null)
            owner.sendMessage(Component.text("Please left-click the sign with the item you wish to sell.").color(NamedTextColor.GOLD));
        else {
            createShop();
            return;
        }

        owner.playSound(owner.getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);
    }

    private void createShop() {
        Shop shop = new Shop(type, owner.getUniqueId(), sign, chest, buyItem, sellItem, buyAmount, sellAmount);

        Block above = chest.getBlock().getRelative(BlockFace.UP);
        if (!above.getType().isAir()) {
            shop.getDisplay().setDisplayType(DisplayType.OFF);
        }
        ShopCreateEvent event = new ShopCreateEvent(owner, shop);
        ChestShops.getInstance().getServer().getPluginManager().callEvent(event);
        ChestShops.getDataHandler().removeCachedBuilder(sign);

        if (event.isCancelled())
            return;

        formatSign(false);

        ChestShops.getDataHandler().addShop(shop, chest, sign);
        owner.sendMessage(Component.text("Shop successfully created!").color(NamedTextColor.GREEN));
        owner.playSound(owner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
    }

    private void formatSign(boolean red) {
        Sign signBlock = (Sign) sign.getBlock().getState();

        signBlock.line(0, ChestShops.SHOP_HEADER.decorate(TextDecoration.BOLD)
            .color(red ? NamedTextColor.RED : NamedTextColor.BLACK));
        switch (type) {
            case SELL -> {
                signBlock.line(1,
                    Component.text("Selling: ").color(red ? NamedTextColor.RED : NamedTextColor.BLACK)
                        .append(Component.text(sellAmount).decoration(TextDecoration.BOLD, red))
                );
                signBlock.line(2,
                    Component.text(buyAmount + " ").color(red ? NamedTextColor.RED : NamedTextColor.GREEN)
                        .append(Component.text(Utils.getItemName(buyItem)  + (buyAmount > 1 ? "s" : "")))
                );
            }
            case BUY -> {
                signBlock.line(1,
                    Component.text("Buying: ").color(red ? NamedTextColor.RED : NamedTextColor.BLACK)
                        .append(Component.text(buyAmount).decoration(TextDecoration.BOLD, red))
                );
                signBlock.line(2,
                    Component.text(sellAmount + " ").color(red ? NamedTextColor.RED : NamedTextColor.GREEN)
                        .append(Component.text(Utils.getItemName(sellItem)  + (sellAmount > 1 ? "s" : "")))
                );
            }
            case BARTER -> {
                signBlock.line(1, Component.text("Bartering")
                    .color(red ? NamedTextColor.RED : NamedTextColor.BLACK));
                signBlock.line(2, Component.text(buyAmount + " for " + sellAmount)
                        .color(red ? NamedTextColor.RED : NamedTextColor.GREEN));
            }
        }
        signBlock.line(3, Component.text(owner.getName()).color(red ? NamedTextColor.RED : NamedTextColor.BLACK));
        signBlock.update();
    }

    public Player getOwner() {
        return owner;
    }

    public boolean isOwner(Player player) {
        return owner.getUniqueId().equals(player.getUniqueId());
    }
}
