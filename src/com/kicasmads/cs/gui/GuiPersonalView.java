package com.kicasmads.cs.gui;

import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.data.Shop;

import java.util.List;
import java.util.UUID;

public class GuiPersonalView extends Gui {
    private final List<Shop> shops;
    private int page;

    public GuiPersonalView(UUID owner) {
        super(54, "My Shops");
        this.shops = ChestShops.getDataHandler().getShops(owner);
        this.page = 0;
    }

    private void changePage(int move) {
        page += move;
        newInventory(54, "My Shops");
    }

    @Override
    protected void populateInventory() {
        displayShops(shops, true, page, 54, this::changePage, null);
    }
}
