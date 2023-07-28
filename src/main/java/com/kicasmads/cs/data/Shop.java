package com.kicasmads.cs.data;

import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.Utils;
import com.kicasmads.cs.event.ShopRemoveEvent;
import com.kicasmads.cs.event.ShopTransactionEvent;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.UUID;

public class Shop {
    private final ShopType    type;
    private final UUID        owner;

    private final Location    sign;
    private final Location    chest;

    private final ItemStack   buyItem;
    private final ItemStack   sellItem;
    private final int         sellAmount;
    private final int         buyAmount;

    private       UUID        buyItemEntity;
    private       UUID        sellItemEntity;

    private final ShopDisplay display;

    public Shop(ShopType type, UUID owner, Location sign, Location chest, ItemStack buyItem, ItemStack sellItem,
                int buyAmount, int sellAmount) {
        this.type       = type;
        this.owner      = owner;

        this.sign       = sign;
        this.chest      = chest;

        this.buyItem    = buyItem;
        this.sellItem   = sellItem;
        this.buyAmount  = buyAmount;
        this.sellAmount = sellAmount;

        display = new ShopDisplay(chest, this);

        removeIfInvalidChest(); // If the chest is missing, remove the shop
    }

    public Shop(CompoundBinaryTag tag) {
        type  = ShopType.values()[tag.getInt("type")];

        owner = UUID.fromString(tag.getString("owner"));

        sign  = Utils.locationFromNBT(tag.getCompound("signLocation"));
        chest = Utils.locationFromNBT(tag.getCompound("chestLocation"));

        buyItem    = Utils.itemStackFromNBT(tag.getByteArray("buyItem"));
        sellItem   = Utils.itemStackFromNBT(tag.getByteArray("sellItem"));

        buyAmount  = tag.getInt("buyAmount");
        sellAmount = tag.getInt("sellAmount");

        CompoundBinaryTag displayNBT = (CompoundBinaryTag) tag.get("display");
        if (displayNBT != null) {
            display = new ShopDisplay(displayNBT, chest, this);
        } else {
            display = new ShopDisplay(chest, this);
        }
        try {
            if (tag.keySet().contains("buyDisplayItem")) {
                buyItemEntity = UUID.fromString(tag.getString("buyDisplayItem"));
                Bukkit.getEntity(buyItemEntity).remove();
            }
            if (tag.keySet().contains("sellDisplayItem")) {
                sellItemEntity = UUID.fromString(tag.getString("sellDisplayItem"));
                Bukkit.getEntity(sellItemEntity).remove();
            }
        }catch (Exception ignored){}
    }

    public ItemStack getDisplayItem(boolean showOwner) {
        // these don't actually need to be initialised here, but Java's kinda dumb
        int a_qty = 0;
        int price_qty = 0;
        ItemStack price_item = null;

        ItemStack is = switch (this.getType()) {
            case BUY, BARTER -> {
                ItemStack i = this.getBuyItem().clone();
                if (this.getBuyAmount() <= 127) {
                    i.setAmount(this.getBuyAmount());
                }
                a_qty = this.getBuyAmount();
                price_qty = this.getSellAmount();
                price_item = this.getSellItem();
                yield i;
            }
            case SELL -> {
                ItemStack i = this.getSellItem().clone();
                if (this.getSellAmount() <= 127) {
                    i.setAmount(this.getSellAmount());
                }
                a_qty = this.getSellAmount();
                price_qty = this.getBuyAmount();
                price_item = this.getBuyItem();
                yield i;
            }
        };

        String verb = this.getType().verb();

        ItemMeta meta = is.getItemMeta();
        meta.displayName(
            Component.empty() // append to empty so that styles don't affect each other
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text("%s %dx".formatted(verb, a_qty), NamedTextColor.GOLD))
                .append(Component.space())
                .append(is.displayName())
        );

        ArrayList<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(
            Component.empty() // append to empty so that styles don't affect each other
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text("for %dx ".formatted(price_qty), NamedTextColor.AQUA))
                .append(price_item.displayName())
        );

        if (showOwner) {
            lore.add(
                Component.text("Owner: %s".formatted(this.getOwnerName()), NamedTextColor.BLUE)
                    .decoration(TextDecoration.ITALIC, false)
            );
        }

        if (this.isEmpty()) {
            lore.add(Component.text("This shop is empty.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);

        is.setItemMeta(meta);

        return is;
    }

    public final @NotNull ShopType getType() {
        return type;
    }

    public OfflinePlayer getOwnerOfflinePlayer() {
        return Bukkit.getOfflinePlayer(owner);
    }

    public UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return getOwnerOfflinePlayer().getName();
    }

    public boolean isOwner(Player player) {
        return owner.equals(player.getUniqueId());
    }

    public ItemStack getBuyItem() {
        return buyItem;
    }

    public ItemStack getSellItem() {
        return sellItem;
    }

    public int getBuyAmount() {
        return buyAmount;
    }

    public int getSellAmount() {
        return sellAmount;
    }

    public ShopDisplay getDisplay() { return display; }

    public int getRequiredOpenSlots() {
        if (removeIfInvalidChest()){
            return 0;
        }

        double changeOnTransaction = ((double) buyAmount) / buyItem.getMaxStackSize() -
                ((double) sellAmount) / sellItem.getMaxStackSize();

        // The number of items in the chest decreases when a transaction occurs
        if (changeOnTransaction <= 0)
            return 1;
        else {
            Inventory chestinventory = ((Chest) chest.getBlock().getState()).getInventory();
            return chestinventory.getSize() - (int) (
                    (((double) chestinventory.getSize()) * sellAmount * buyItem.getMaxStackSize()) /
                    (buyAmount * sellItem.getMaxStackSize())
            );
        }
    }

    public boolean isEmpty() {
        if (removeIfInvalidChest()){
            return false;
        }
        Chest shopChest = (Chest) chest.getBlock().getState();
        Inventory chestinventory = shopChest.getInventory();
        return Utils.firstSimilar(sellItem, chestinventory) == -1;
    }

    public void tryTransaction(Player player, boolean requireHoldingBuyItem) {
        if(removeIfInvalidChest()){
            player.sendMessage(
                Component.text("This shop has an error.")
                    .color(NamedTextColor.RED)
            );
            return;
        }
        Chest shopChest = (Chest) chest.getBlock().getState();
        Inventory chestinventory = shopChest.getInventory();

        // Make sure shop can pay out
        if (!chestinventory.containsAtLeast(sellItem, sellAmount)) {
            player.sendMessage(
                Component.text("This shop is out of stock. Come back later.", NamedTextColor.RED)
            );
            return;
        }

        if (Utils.emptySlots(chestinventory) < this.getRequiredOpenSlots() && Utils.firstInsertableStack(chestinventory, buyItem) == -1) {
            player.sendMessage(
                Component.text("Chest is full and cannot accept any more transactions", NamedTextColor.RED)
            );
            return;
        }

        PlayerInventory playerInventory = player.getInventory();

        // Make sure player can pay
        if (!playerInventory.containsAtLeast(buyItem, buyAmount)) {
            player.sendMessage(
                Component.text("You need " + buyAmount + " " + Utils.getItemName(buyItem) + " in order to buy this.", NamedTextColor.RED)
            );
            return;
        }

        // The player must be holding the buy item
        if (requireHoldingBuyItem && !playerInventory.getItemInMainHand().isSimilar(buyItem)) {
            player.sendMessage(
                Component.text("You must be holding item this shop requires from you.", NamedTextColor.RED)
            );
            return;
        }

        ShopTransactionEvent event = new ShopTransactionEvent(player, this);
        ChestShops.getInstance().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        removePlayerCost(playerInventory);
        removeSellItems();
        givePlayerItems(player);
        putBuyItems();
    }

    private void removeSellItems() {
        Chest shopChest = (Chest) chest.getBlock().getState();
        int numRemoved = 0;

        while (numRemoved < sellAmount) {
            // Get first instance of sell item
            ItemStack stack = shopChest.getInventory().getItem(shopChest.getInventory().first(sellItem.getType()));
            numRemoved += stack.getAmount();
            // Set the stack to either how many we have left to remove or 0
            stack.setAmount(Math.max(stack.getAmount() - sellAmount, 0));
            // Remove the stack if it has an amount of 0
            if (stack.getAmount() == 0)
                shopChest.getInventory().remove(stack);
        }
    }

    private void removePlayerCost(Inventory playerInv) {
        int numRemoved = 0;

        while (numRemoved < buyAmount) {
            ItemStack stack = playerInv.getItem(playerInv.first(buyItem.getType()));
            numRemoved += stack.getAmount();

            stack.setAmount(Math.max(stack.getAmount() - buyAmount, 0));
            if (stack.getAmount() == 0)
                playerInv.remove(stack);
        }
    }

    private void givePlayerItems(Player player) {
        Inventory playerInv = player.getInventory();
        int numGiven = 0;
        while (numGiven < sellAmount) {
            int amount = Math.min(sellAmount - numGiven, sellItem.getMaxStackSize());
            numGiven += amount;
            ItemStack stack = sellItem.clone();
            stack.setAmount(amount);

            if (playerInv.firstEmpty() > -1)
                playerInv.addItem(stack);
            else {
                player.getWorld().dropItem(player.getLocation(), stack);
                player.sendMessage(
                    Component.text("Your inventory was full, so you dropped the item.")
                        .color(NamedTextColor.RED)
                );
            }
        }
    }

    private void putBuyItems() {
        Inventory chestInv = ((Chest) chest.getBlock().getState()).getInventory();
        int numGiven = 0;

        // Loop until we have no more left to give or we can't insert any more items
        while (numGiven < buyAmount && (chestInv.firstEmpty() != -1 || Utils.firstInsertableStack(chestInv, buyItem) != -1)) {
            // See if we can insert into an already existing stack
            if (Utils.firstInsertableStack(chestInv, buyItem) == -1) {
                ItemStack[] contents = chestInv.getStorageContents();
                ItemStack giveItem = buyItem.clone();
                // Set the stack size to either the max stack or the amount we have left to give
                giveItem.setAmount(Math.min(buyAmount - numGiven, buyItem.getMaxStackSize()));
                numGiven += giveItem.getAmount();
                // Insert the new stack into the chest's inventory
                contents[chestInv.firstEmpty()] = giveItem;
                chestInv.setContents(contents);
            } else {
                ItemStack stack = chestInv.getItem(Utils.firstInsertableStack(chestInv, buyItem));
                int ogAmount = stack.getAmount();
                // Set the stack to either the amount we have left + the current amount or the max size
                stack.setAmount(Math.min(stack.getAmount() + (buyAmount - numGiven), buyItem.getMaxStackSize()));
                // Only count items we added, not ones that were already there
                numGiven += stack.getAmount() - ogAmount;
            }
        }
    }

    public boolean removeIfInvalidChest(boolean removeFromList){
        if(!(chest.getBlock().getState() instanceof Chest)) {
            display.removeShopDisplay();
            ChestShops.error(
                "Removing shop at ([%s] %d, %d, %d). The block is not CHEST, it is %s. Shop Owner: %s".formatted(
                    chest.getWorld().getName(),
                    chest.getBlockX(), chest.getBlockY(), chest.getBlockZ(),
                    chest.getBlock().getType().name(),
                    getOwnerName()
                )
            );
            if (sign.getBlock().getState() instanceof Sign signBlock) {
                SignSide side = signBlock.getSide(Side.FRONT);
                side.line(0, Component.text("[SHOP]").style(Style.style(NamedTextColor.RED, TextDecoration.BOLD)));
                side.line(1, Component.text("This chest").color(NamedTextColor.RED));
                side.line(2, Component.text("is missing.").color(NamedTextColor.RED));
                side.line(3, Component.text("Shop Removed.").color(NamedTextColor.RED));
                signBlock.update();
            }
            if(removeFromList){
                ChestShops.getDataHandler().removeShop(this);
            }
            ChestShops.getInstance().getServer().getPluginManager().callEvent(new ShopRemoveEvent(null, this));
            return true;
        }
        return false;
    }

    public boolean removeIfInvalidChest() {
     return removeIfInvalidChest(true);
    }

    public CompoundBinaryTag toNbt() {
        CompoundBinaryTag shopTag = CompoundBinaryTag.builder()
            .putInt("buyAmount", buyAmount)
            .putInt("sellAmount", sellAmount)
            .putInt("type", type.ordinal())
            .putString("owner", owner.toString())
            .putByteArray("sellItem", Utils.itemStackToNBT(sellItem))
            .putByteArray("buyItem",  Utils.itemStackToNBT(buyItem))
            .put("display", display.toNBT())
        .build();

        if (chest != null)
            shopTag = shopTag.put("chestLocation", Utils.locationToNBT(chest));
        if (sign != null)
            shopTag = shopTag.put("signLocation",  Utils.locationToNBT(sign));

        if (buyItemEntity != null)
            shopTag = shopTag.putString("buyDisplayItem",  buyItemEntity.toString());
        if (sellItemEntity != null)
            shopTag = shopTag.putString("sellDisplayItem", sellItemEntity.toString());

        return shopTag;
    }

    public Location getSignLocation() {
        return sign;
    }

    public Location getChestLocation() {
        return chest;
    }

    public void resetItemEntities(){
        buyItemEntity = null;
        sellItemEntity = null;
    }

    public UUID getBuyItemEntity() {
        return buyItemEntity;
    }

    public UUID getSellItemEntity() {
        return sellItemEntity;
    }
}
