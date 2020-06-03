package com.kicasmads.cs.event;

import com.kicasmads.cs.Shop;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class ShopTransactionEvent extends ShopEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    public ShopTransactionEvent(Player player, Shop shop) {
        super(player, shop);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
