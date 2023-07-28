package com.kicasmads.cs.gui;

import com.kicasmads.cs.data.Shop;
import com.kicasmads.cs.data.ShopType;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class GuiShopsView extends Gui {

    private final           List<Shop> shops;
    private final           String     title;
    private final           boolean    allowTransactions;
    private final           boolean    showOwner;
    private                 int        page;

    public GuiShopsView(List<Shop> shops, String title, boolean allowTransactions, boolean showOwner, @Nullable ShopType typeFilter) {
        super(54, title);
        if (typeFilter != null) {
            this.shops = shops.stream().filter(s -> s.getType() == typeFilter).toList();
        } else {
            this.shops = shops.stream().sorted(Comparator.comparing(Shop::getType)).toList();
        }
        this.title = title;
        this.allowTransactions = allowTransactions;
        this.showOwner = showOwner;
        this.page = 0;
    }

    private void changePage(int move) {
        page += move;
        newInventory(54, Component.text(title));
    }

    @Override
    protected void populateInventory() {
        displayShops(shops, showOwner, page, 54, this::changePage, allowTransactions
            ? shop -> shop.tryTransaction(user, false) : null);
    }
}
