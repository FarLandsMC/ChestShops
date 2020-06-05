package com.kicasmads.cs.event;

import com.kicasmads.cs.data.Shop;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public abstract class ShopEvent extends Event implements Cancellable {
    private boolean cancelled;

    private final Player player;
    private final Shop shop;

    public ShopEvent(Player player, Shop shop) {
        this.player = player;
        this.shop = shop;
        this.cancelled = false;
    }

    public Player getPlayer() {
        return player;
    }

    public Shop getShop() {
        return shop;
    }

    @Override
    public void setCancelled(boolean value) {
        cancelled = value;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}
