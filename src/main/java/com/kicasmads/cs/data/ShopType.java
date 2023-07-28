package com.kicasmads.cs.data;

public enum ShopType {
    BUY("Buying"),
    SELL("Selling"),
    BARTER("Bartering"),
    ;

    private final String verb;

    ShopType(String verb) {
        this.verb = verb;
    }

    public String verb() {
        return this.verb;
    }
}
