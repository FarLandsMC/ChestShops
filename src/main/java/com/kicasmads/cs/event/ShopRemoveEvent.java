package com.kicasmads.cs.event;

import com.kicasmads.cs.data.Shop;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ShopRemoveEvent extends ShopEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public ShopRemoveEvent(Player player, Shop shop) {
        super(player, shop);
    }

    public Player getRemover() {
        return getPlayer();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
