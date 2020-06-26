package com.kicasmads.cs.gui;

import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.data.Shop;

import org.bukkit.entity.Player;

import java.util.List;

public class GuiPersonalView extends Gui {
    private final List<Shop> shops;
    private int page;

    public GuiPersonalView(Player player) {
        super(54, "My Shops");
        this.shops = ChestShops.getDataHandler().getShops(player);
        this.page = 0;
    }

    private void changePage(int move) {
        page += move;
        newInventory(54, "My Shops");
    }

    @Override
    protected void populateInventory() {
        displayShops(shops, true, page, 54, this::changePage);
    }
}
