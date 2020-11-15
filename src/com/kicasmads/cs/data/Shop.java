package com.kicasmads.cs.data;

import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.Utils;
import com.kicasmads.cs.event.ShopTransactionEvent;

import net.minecraft.server.v1_16_R3.NBTTagCompound;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;

public class Shop {
    private final ShopType  type;
    private final UUID      owner;

    private final Location  sign;
    private final Location  chest;

    private final ItemStack buyItem;
    private final ItemStack sellItem;
    private final int       sellAmount;
    private final int       buyAmount;

    private       UUID      buyItemEntity;
    private       UUID      sellItemEntity;

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
    }

    public Shop(NBTTagCompound tag) {
        type  = ShopType.values()[tag.getInt("type")];
        owner = UUID.fromString(tag.getString("owner"));

        sign  = Utils.locationFromNBT(tag.getCompound("signLocation"));
        chest = Utils.locationFromNBT(tag.getCompound("chestLocation"));

        buyItem    = Utils.itemStackFromNBT(tag.getCompound("buyItem"));
        sellItem   = Utils.itemStackFromNBT(tag.getCompound("sellItem"));
        buyAmount  = tag.getInt("buyAmount");
        sellAmount = tag.getInt("sellAmount");

        NBTTagCompound displayNBT = (NBTTagCompound) tag.get("display");
        if(displayNBT != null){
            display = new ShopDisplay(displayNBT, chest, this);
        }else{
            display = new ShopDisplay(chest, this);
        }

        if (tag.hasKey("buyDisplayItem"))
            buyItemEntity = UUID.fromString(tag.getString("buyDisplayItem"));
        if (tag.hasKey("sellDisplayItem"))
            sellItemEntity = UUID.fromString(tag.getString("sellDisplayItem"));
    }

    public final ShopType getType() {
        return type;
    }

    public UUID getOwner() {
        return owner;
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
        Chest shopChest = (Chest) chest.getBlock().getState();
        Inventory chestinventory = shopChest.getInventory();
        return Utils.firstSimilar(sellItem, chestinventory) < 0;
    }

    public void tryTransaction(Player player, boolean requireHoldingBuyItem) {
        Chest shopChest = (Chest) chest.getBlock().getState();
        Inventory chestinventory = shopChest.getInventory();

        // Make sure shop can pay out
        if (!chestinventory.containsAtLeast(sellItem, sellAmount)) {
            player.sendMessage(ChatColor.RED + "This shop is out of stock. Come back later.");
            return;
        }

        if (chestinventory.firstEmpty() == -1 && Utils.firstInsertableStack(chestinventory, buyItem) == -1) {
            player.sendMessage(ChatColor.RED + "Chest is full and cannot accept any more transactions");
            return;
        }

        PlayerInventory playerInventory = player.getInventory();

        // Make sure player can pay
        if (!playerInventory.containsAtLeast(buyItem, buyAmount)) {
            player.sendMessage(ChatColor.RED + "You need " + buyAmount + " " + Utils.getItemName(buyItem) +
                    " in order to buy this.");
            return;
        }

        // The player must be holding the buy item
        if (requireHoldingBuyItem && !playerInventory.getItemInMainHand().isSimilar(buyItem)) {
            player.sendMessage(ChatColor.RED + "You must be holding item this shop requires from you.");
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
                player.sendMessage(ChatColor.RED + "Your inventory was full, so you dropped the item.");
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

    public NBTTagCompound toNbt() {
        NBTTagCompound shopTag = new NBTTagCompound();

        shopTag.setInt("buyAmount", buyAmount);
        shopTag.setInt("sellAmount", sellAmount);
        shopTag.setInt("type", type.ordinal());
        shopTag.setString("owner", owner.toString());
        shopTag.set("sellItem", Utils.itemStackToNBT(sellItem));
        shopTag.set("buyItem",  Utils.itemStackToNBT(buyItem));

        shopTag.set("display", display.toNBT());

        if (chest != null)
            shopTag.set("chestLocation", Utils.locationToNBT(chest));
        if (sign != null)
            shopTag.set("signLocation",  Utils.locationToNBT(sign));

        if (buyItemEntity != null)
            shopTag.setString("buyDisplayItem",  buyItemEntity.toString());
        if (sellItemEntity != null)
            shopTag.setString("sellDisplayItem", sellItemEntity.toString());

        return shopTag;
    }

    public Location getSignLocation() {
        return sign;
    }

    public Location getChestLocation() {
        return chest;
    }
}
