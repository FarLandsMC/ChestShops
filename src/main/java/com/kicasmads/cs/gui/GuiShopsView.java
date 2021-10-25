package com.kicasmads.cs.gui;

import com.kicasmads.cs.data.Shop;

import java.util.List;

public class GuiShopsView extends Gui {
    private final List<Shop> shops;
    private final String title;
    private final boolean allowTransactions;
    private final boolean showOwner;
    private int page;

    public GuiShopsView(List<Shop> shops, String title, boolean allowTransactions, boolean showOwner) {
        super(54, title);
        this.shops = shops;
        this.title = title;
        this.allowTransactions = allowTransactions;
        this.showOwner = showOwner;
        this.page = 0;
    }

    private void changePage(int move) {
        page += move;
        newInventory(54, title);
    }

    @Override
    protected void populateInventory() {
        displayShops(shops, true, showOwner, page, 54, this::changePage, allowTransactions ? shop -> shop.tryTransaction(user, false) : null);
    }
}
